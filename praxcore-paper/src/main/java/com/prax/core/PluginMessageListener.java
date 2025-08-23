// Ubicación: praxcore-paper/src/main/java/com/prax/core/PluginMessageListener.java
package com.prax.core;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
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
            handlePraxCoreMessage(player, message);
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

    private void handlePraxCoreMessage(Player player, byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("ValidationResponse")) {
            UUID playerUuid = UUID.fromString(in.readUTF());
            boolean isValid = in.readBoolean();

            // --- INICIO DE LA CORRECCIÓN DEFINITIVA ---
            // Usamos Bukkit.getScheduler().runTask() para asegurar que toda la lógica que modifica
            // el estado del jugador se ejecute en el hilo principal del servidor.
            // Esto previene condiciones de carrera y garantiza la consistencia del estado.
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player targetPlayer = Bukkit.getPlayer(playerUuid);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    plugin.getLogger().warning("Respuesta de validación recibida para un jugador que ya no está online: " + playerUuid);
                    return;
                }

                plugin.getLogger().info("[DEBUG] [" + plugin.getServerType() + "]: Respuesta de validación para " + targetPlayer.getName() + ": " + (isValid ? "VALIDA" : "INVALIDA"));
                plugin.setPendingValidation(playerUuid, false);

                if (isValid) {
                    // Si la sesión es válida, se establece su estado como autenticado.
                    // Al ejecutarse en el hilo principal, este cambio es inmediatamente visible
                    // para el PlayerMoveEvent, descongelando al jugador.
                    plugin.setAuthenticated(playerUuid, true);
                    plugin.setLoginTime(playerUuid);
                    targetPlayer.sendMessage("§a¡Sesión restaurada! Bienvenido de vuelta.");
                } else {
                    // Si la sesión no es válida, la lógica anterior era correcta.
                    plugin.setAuthenticated(playerUuid, false);
                    if (plugin.isLobbyServer()) {
                        if (plugin.getDataManager().isPlayerRegistered(playerUuid)) {
                            targetPlayer.sendMessage("§ePor favor, inicia sesión con /login <contraseña>");
                        } else {
                            targetPlayer.sendMessage("§e¡Bienvenido! Usa /register para crear una cuenta.");
                        }
                    } else {
                        targetPlayer.kickPlayer("§cTu sesión no es válida. Por favor, vuelve a conectarte.");
                    }
                }
            });
            // --- FIN DE LA CORRECCIÓN DEFINITIVA ---
        }
    }
}