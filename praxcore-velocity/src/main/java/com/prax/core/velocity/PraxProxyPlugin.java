package com.prax.core.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(id = "praxcore-velocity", name = "PraxCore Velocity", version = "1.0.0")
public class PraxProxyPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final TokenManager tokenManager;
    private final Map<UUID, ScheduledTask> pendingDisconnections = new ConcurrentHashMap<>();

    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("prax:core");

    @Inject
    public PraxProxyPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.tokenManager = new TokenManager();

        // LOG CRÍTICO 1
        logger.info("==========================================");
        logger.info("[PraxProxy] PLUGIN CONSTRUCTOR EJECUTADO");
        logger.info("==========================================");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // LOG CRÍTICO 2
        logger.info("[PraxProxy] EVENTO ProxyInitializeEvent RECIBIDO");

        server.getChannelRegistrar().register(CHANNEL);

        // LOG CRÍTICO 3
        logger.info("[PraxProxy] CANAL 'prax:core' REGISTRADO");
        logger.info("[PraxProxy] Sistema de tokens JWT iniciado");
        logger.info("==========================================");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // LOG CRÍTICO 4 - Este DEBE aparecer cuando Paper envía un mensaje
        logger.info("[PLUGIN_MSG] Mensaje recibido en canal: " + event.getIdentifier());

        if (!CHANNEL.equals(event.getIdentifier())) {
            logger.info("[PLUGIN_MSG] Canal ignorado, no es prax:core");
            return;
        }

        logger.info("[PLUGIN_MSG] ES NUESTRO CANAL prax:core!");

        if (!(event.getSource() instanceof ServerConnection)) {
            logger.info("[PLUGIN_MSG] Fuente no es ServerConnection: " + event.getSource().getClass());
            return;
        }

        ServerConnection connection = (ServerConnection) event.getSource();
        Player player = connection.getPlayer();

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();

        logger.info("[PLUGIN_MSG] SubCanal: '" + subChannel + "' de " + player.getUsername());

        if ("CreateSession".equalsIgnoreCase(subChannel)) {
            UUID uuid = UUID.fromString(in.readUTF());
            logger.info("[SESSION] CREAR sesión para UUID: " + uuid);
            tokenManager.createAndStoreToken(uuid);
            logger.info("[SESSION] Sesión CREADA para " + player.getUsername());

        } else if ("ValidateToken".equalsIgnoreCase(subChannel)) {
            UUID playerUuid = UUID.fromString(in.readUTF());
            logger.info("[VALIDATE] Validando sesión para UUID: " + playerUuid);

            boolean isValid = tokenManager.validateToken(playerUuid);
            logger.info("[VALIDATE] Resultado: " + (isValid ? "VÁLIDO" : "INVÁLIDO"));

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("ValidationResponse");
            out.writeUTF(playerUuid.toString());
            out.writeBoolean(isValid);

            connection.sendPluginMessage(CHANNEL, out.toByteArray());
            logger.info("[VALIDATE] Respuesta enviada al servidor");
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        logger.info("[SWITCH] " + player.getUsername() + " cambió de servidor");

        ScheduledTask pendingTask = pendingDisconnections.remove(playerUuid);
        if (pendingTask != null) {
            pendingTask.cancel();
            logger.info("[SWITCH] Tarea de borrado cancelada");
        }
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        logger.info("[DISCONNECT] " + player.getUsername() + " desconectado, esperando 30s");

        ScheduledTask task = server.getScheduler()
                .buildTask(this, () -> {
                    if (server.getPlayer(playerUuid).isEmpty()) {
                        tokenManager.removeSession(playerUuid);
                        logger.info("[CLEANUP] Sesión eliminada para " + player.getUsername());
                    }
                })
                .delay(30, TimeUnit.SECONDS)
                .schedule();

        pendingDisconnections.put(playerUuid, task);
    }
}