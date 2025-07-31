package com.prax.core;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerConnectionListener implements Listener {

    private final PraxCorePlugin plugin;

    public PlayerConnectionListener(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (plugin.getDataManager().isSessionActive(playerUuid)) {
            plugin.setAuthenticated(playerUuid, true);

            // --- LÍNEA AÑADIDA: Reiniciamos el cronómetro al cambiar de servidor ---
            plugin.setLoginTime(playerUuid);

        } else {
            plugin.setAuthenticated(playerUuid, false);
            if (plugin.isLobbyServer()) {
                if (plugin.getDataManager().isPlayerRegistered(playerUuid)) {
                    player.sendMessage("§a¡Bienvenido de nuevo! Por favor, inicia sesión con /login <contraseña>");
                } else {
                    player.sendMessage("§e¡Bienvenido! Por favor, regístrate con /register <email> <contraseña> <contraseña>");
                }
            }
        }

        if (plugin.isLobbyServer()) {
            Location spawnPoint = new Location(player.getWorld(), 42, 31, -35, 0, 0);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.teleport(spawnPoint), 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        // --- LÓGICA AÑADIDA PARA CALCULAR Y GUARDAR PLAYTIME ---
        Long loginTime = plugin.getLoginTime(playerUuid);

        if (loginTime != null) {
            // Si encontramos una hora de inicio, calculamos la duración de la sesión.
            long sessionDuration = System.currentTimeMillis() - loginTime;

            // Añadimos la duración al total guardado en players.yml.
            plugin.getDataManager().addPlaytime(playerUuid, sessionDuration);

            // Limpiamos el registro para la próxima sesión.
            plugin.removeLoginTime(playerUuid);
        }

        // Esta lógica se mantiene para limpiar el estado local.
        plugin.setAuthenticated(playerUuid, false);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.isAuthenticated(event.getPlayer().getUniqueId())) {
            String command = event.getMessage().split(" ")[0].toLowerCase();
            if (!command.equals("/login") && !command.equals("/register")) {
                event.getPlayer().sendMessage("§cDebes iniciar sesión para usar este comando.");
                event.setCancelled(true);
            }
        }
    }
}