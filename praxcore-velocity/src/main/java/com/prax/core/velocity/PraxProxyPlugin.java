package com.prax.core.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.util.UUID;

@Plugin(
        id = "praxcore-velocity",
        name = "PraxCore Velocity",
        version = "1.0.0",
        description = "Gestiona las sesiones de PraxCore en el proxy.",
        authors = {"akhan"}
)
public class PraxProxyPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final TokenManager tokenManager;

    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("prax", "core");

    @Inject
    public PraxProxyPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.tokenManager = new TokenManager();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CHANNEL);
        logger.info("PraxProxyPlugin cargado y canal 'prax:core' registrado.");
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        tokenManager.removeSession(player.getUniqueId());
        logger.info("Sesión eliminada para " + player.getUsername());
    }

    // Este es el metodo correcto, que utiliza el evento de la API 3.1.1
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }

        // El mensaje debe venir de un servidor, no de un jugador
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        ServerConnection sourceServer = (ServerConnection) event.getSource();
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();

        if ("StoreToken".equalsIgnoreCase(subChannel)) {
            UUID uuid = UUID.fromString(in.readUTF());
            String token = in.readUTF();
            tokenManager.storeToken(uuid, token);
            logger.info("Token almacenado para " + uuid);
        } else if ("ValidateToken".equalsIgnoreCase(subChannel)) {
            UUID uuid = UUID.fromString(in.readUTF());
            boolean isValid = tokenManager.isSessionValid(uuid);
            logger.info("Validando token para " + uuid + ". Resultado: " + isValid);

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("ValidationResponse");
            out.writeUTF(uuid.toString());
            out.writeBoolean(isValid);
            sourceServer.sendPluginMessage(CHANNEL, out.toByteArray());
        }
    }
}