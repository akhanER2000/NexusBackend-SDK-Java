// Ubicación: praxcore-paper/src/main/java/com/prax/core/PlayerConnectionListener.java
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

        plugin.setAuthenticated(playerUuid, false);

        if (plugin.isLobbyServer()) {
            if (plugin.getDataManager().isPlayerRegistered(playerUuid)) {
                player.sendMessage("§a¡Bienvenido de nuevo! Por favor, inicia sesión con /login <contraseña>");
            } else {
                player.sendMessage("§e¡Bienvenido! Por favor, regístrate con /register <email> <contraseña> <contraseña>");
            }
            Location spawnPoint = new Location(player.getWorld(), 42, 31, -35, 0, 0);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.teleport(spawnPoint), 1L);
        } else {
            player.sendMessage("§eValidando tu sesión...");
            plugin.setPendingValidation(playerUuid, true);
            plugin.sendValidateTokenMessage(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        Long loginTime = plugin.getLoginTime(playerUuid);
        if (loginTime != null) {
            long sessionDuration = System.currentTimeMillis() - loginTime;
            plugin.getDataManager().addPlaytime(playerUuid, sessionDuration);
            plugin.removeLoginTime(playerUuid);
        }

        plugin.getDataManager().saveData();
        plugin.setAuthenticated(playerUuid, false);
        plugin.setPendingValidation(playerUuid, false);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // La condición de congelamiento ahora es: "No estás autenticado Y NO estás pendiente de validación"
        if (!plugin.isAuthenticated(event.getPlayer().getUniqueId()) && !plugin.isPendingValidation(event.getPlayer().getUniqueId())) {
            // ¡MEJORA! Forzamos al jugador a volver a su posición anterior.
            // Esto crea un congelamiento 100% efectivo.
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.isAuthenticated(event.getPlayer().getUniqueId())) {
            String command = event.getMessage().split(" ")[0].toLowerCase();
            if (!command.equals("/login") && !command.equals("/register")) {
                event.getPlayer().sendMessage("§cDebes iniciar sesión o esperar a que se valide tu sesión para usar comandos.");
                event.setCancelled(true);
            }
        }
    }
}