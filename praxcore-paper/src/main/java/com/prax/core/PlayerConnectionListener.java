// Ubicación: praxcore-paper/src/main/java/com/prax/core/PlayerConnectionListener.java
package com.prax.core;

import org.bukkit.Bukkit;
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

        plugin.getLogger().info("[DEBUG] [" + plugin.getServerType() + "]: " + player.getName() + " conectado. Iniciando validación.");

        plugin.setPendingValidation(playerUuid, true);

        // MODIFICADO: Usar el nuevo sistema de reintentos en lugar del timeout simple
        attemptValidation(player, 0);

        if (plugin.isLobbyServer()) {
            Location spawnPoint = new Location(player.getWorld(), 42, 31, -35, 0, 0);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.teleport(spawnPoint), 1L);
        }
    }

    /**
     * NUEVO METODO: Intenta validar la sesión del jugador con reintentos automáticos
     * @param player El jugador a validar
     * @param attempt Número de intento actual (empieza en 0)
     */
    private void attemptValidation(Player player, int attempt) {
        // Si ya se han hecho 3 intentos, manejar el fallo
        if (attempt > 3) {
            plugin.getLogger().warning("Validación fallida tras 3 intentos para " + player.getName());
            handleValidationFailure(player);
            return;
        }

        // Programar el intento de validación
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Verificar que el jugador sigue online
            if (!player.isOnline()) {
                plugin.getLogger().info("[DEBUG] Jugador " + player.getName() + " ya no está online, cancelando validación.");
                return;
            }

            // Si ya no está pendiente de validación, significa que ya se validó
            if (!plugin.isPendingValidation(player.getUniqueId())) {
                plugin.getLogger().info("[DEBUG] Jugador " + player.getName() + " ya fue validado.");
                return;
            }

            // Enviar mensaje de validación
            plugin.getLogger().info("[DEBUG] Enviando solicitud de validación para " + player.getName() + " (intento " + (attempt + 1) + "/4)");
            plugin.sendValidateTokenMessage(player);

            // Verificar resultado después de 2 segundos
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Si aún está pendiente de validación, reintentar
                if (plugin.isPendingValidation(player.getUniqueId()) && player.isOnline()) {
                    plugin.getLogger().info("[DEBUG] Sin respuesta de validación para " + player.getName() + ", reintentando...");
                    attemptValidation(player, attempt + 1);
                } else if (plugin.isAuthenticated(player.getUniqueId())) {
                    plugin.getLogger().info("[DEBUG] Validación exitosa para " + player.getName() + " en intento " + (attempt + 1));
                }
            }, 40L); // 2 segundos de espera para la respuesta
        }, attempt == 0 ? 1L : 20L); // Primer intento casi inmediato, siguientes con 1 segundo de delay
    }

    /**
     * NUEVO MÉTODO: Maneja el caso cuando la validación falla después de todos los reintentos
     * @param player El jugador cuya validación falló
     */
    private void handleValidationFailure(Player player) {
        UUID playerUuid = player.getUniqueId();

        // Limpiar estados
        plugin.setAuthenticated(playerUuid, false);
        plugin.setPendingValidation(playerUuid, false);

        // Acción según el tipo de servidor
        if (!plugin.isLobbyServer()) {
            // En servidores backend, expulsar al jugador
            plugin.getLogger().severe("[ERROR] No se pudo validar la sesión de " + player.getName() + " en servidor " + plugin.getServerType());
            player.kickPlayer("§cError de validación de sesión.\n§fPor favor, reconéctate al servidor.");
        } else {
            // En el lobby, permitir que el jugador se registre/loguee
            plugin.getLogger().info("[DEBUG] Validación fallida en lobby para " + player.getName() + ", esperando login manual.");
            if (plugin.getDataManager().isPlayerRegistered(playerUuid)) {
                player.sendMessage("§e⚠ No se detectó una sesión activa.");
                player.sendMessage("§ePor favor, inicia sesión con §b/login <contraseña>");
            } else {
                player.sendMessage("§a¡Bienvenido al servidor!");
                player.sendMessage("§ePor favor, regístrate con §b/register <email> <contraseña> <contraseña>");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Congelar al jugador si está en el lobby y no está autenticado
        if (plugin.isLobbyServer() && !plugin.isAuthenticated(event.getPlayer().getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                event.setTo(from);
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // Bloquear comandos si no está autenticado (excepto login y register)
        if (plugin.isLobbyServer() && !plugin.isAuthenticated(event.getPlayer().getUniqueId())) {
            String command = event.getMessage().split(" ")[0].toLowerCase();
            if (!command.equals("/login") && !command.equals("/register")) {
                event.getPlayer().sendMessage("§c❌ Debes iniciar sesión para usar este comando.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        plugin.getLogger().info("[DEBUG] [" + plugin.getServerType() + "]: Jugador " + player.getName() + " se desconecta.");

        // Guardar tiempo de juego si estaba logueado
        Long loginTime = plugin.getLoginTime(playerUuid);
        if (loginTime != null) {
            long sessionDuration = System.currentTimeMillis() - loginTime;
            plugin.getDataManager().addPlaytime(playerUuid, sessionDuration);
            plugin.removeLoginTime(playerUuid);
        }

        // Guardar datos y limpiar estados
        plugin.getDataManager().saveData();
        plugin.setAuthenticated(playerUuid, false);
        plugin.setPendingValidation(playerUuid, false);
    }
}