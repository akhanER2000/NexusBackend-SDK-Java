package com.prax.core;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PluginMessageListener implements org.bukkit.plugin.messaging.PluginMessageListener {

    private final PraxCorePlugin plugin;
    public static final Map<String, Integer> serverPlayerCounts = new HashMap<>();

    public PluginMessageListener(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if ("BungeeCord".equalsIgnoreCase(channel)) {
            handleBungeeCordMessage(message);
            return;
        }

        if ("prax:core".equalsIgnoreCase(channel)) {
            handlePraxCoreMessage(message);
        }
    }

    private void handleBungeeCordMessage(byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (subChannel.equals("PlayerCount")) {
            String serverName = in.readUTF();
            int playerCount = in.readInt();
            serverPlayerCounts.put(serverName, playerCount);
        }
    }

    private void handlePraxCoreMessage(byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("ValidationResponse")) {
            UUID playerUuid = UUID.fromString(in.readUTF());
            boolean isValid = in.readBoolean();

            Player targetPlayer = Bukkit.getPlayer(playerUuid);
            if (targetPlayer == null) return;

            if (isValid) {
                plugin.setAuthenticated(playerUuid, true);
                plugin.setLoginTime(playerUuid);

                // --- LÍNEA CORREGIDA ---
                // Ahora usamos el serverType que leemos desde el config.yml
                targetPlayer.sendMessage("§a¡Sesión validada! Bienvenido a " + plugin.getServerType());
            } else {
                plugin.setAuthenticated(playerUuid, false);
                if (plugin.isLobbyServer()) {
                    if (plugin.getDataManager().isPlayerRegistered(playerUuid)) {
                        targetPlayer.sendMessage("§a¡Bienvenido! Por favor, inicia sesión con /login <contraseña>");
                    } else {
                        targetPlayer.sendMessage("§e¡Bienvenido! Por favor, regístrate con /register <email> <contraseña> <contraseña>");
                    }
                    Location spawnPoint = new Location(targetPlayer.getWorld(), 42, 31, -35, 0, 0);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> targetPlayer.teleport(spawnPoint), 1L);
                } else {
                    targetPlayer.kickPlayer("§cTu sesión no es válida. Por favor, vuelve a conectarte.");
                }
            }
        }
    }
}