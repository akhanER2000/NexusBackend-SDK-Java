package com.prax.core;

import com.prax.core.commands.LoginCommand;
import com.prax.core.commands.RegisterCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class PraxCorePlugin extends JavaPlugin {

    private DataManager dataManager;
    private final Set<UUID> authenticatedPlayers = new HashSet<>();
    private String serverType;
    private boolean debugHeartbeat; // Nueva variable para controlar los logs

    // --- NUEVO MAP PARA CONTROLAR EL TIEMPO DE SESIÓN ---
    // Guardará el UUID del jugador y la hora (en milisegundos) en que inició sesión.
    private final Map<UUID, Long> playerLoginTimes = new HashMap<>();


    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        // Leemos la configuración. Por defecto, el debug estará desactivado.
        this.serverType = this.getConfig().getString("server-type", "backend");
        this.debugHeartbeat = this.getConfig().getBoolean("debug.heartbeat", false);

        getLogger().info("Prax Core Plugin activándose en modo: " + serverType);
        this.dataManager = new DataManager(this);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStatsListener(this), this);

        if (isLobbyServer()) {
            getLogger().info("Modo Lobby detectado. Registrando comandos y listener de selector.");
            this.getCommand("register").setExecutor(new RegisterCommand(this));
            this.getCommand("login").setExecutor(new LoginCommand(this));
            getServer().getPluginManager().registerEvents(new SelectorListener(this), this);
        }

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new PluginMessageListener(this));

        startSessionHeartbeat();
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.shutdown();
        }
        getLogger().info("Prax Core Plugin se ha desactivado.");
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    public boolean isLobbyServer() {
        return "lobby".equalsIgnoreCase(this.serverType);
    }

    public boolean isAuthenticated(UUID playerUuid) {
        return authenticatedPlayers.contains(playerUuid);
    }

    public void setAuthenticated(UUID playerUuid, boolean authenticated) {
        if (authenticated) {
            authenticatedPlayers.add(playerUuid);
        } else {
            authenticatedPlayers.remove(playerUuid);
        }
    }

    private void startSessionHeartbeat() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Si no hay jugadores conectados, no hacemos nada.
                if (getServer().getOnlinePlayers().isEmpty()) {
                    return;
                }

                // --- LÓGICA DE DEBUG CONDICIONAL ---
                if (debugHeartbeat) {
                    getLogger().info("[DEBUG-Heartbeat] Ejecutando tarea de heartbeat en servidor modo '" + serverType + "'...");
                }

                for (Player player : getServer().getOnlinePlayers()) {
                    try {
                        // Intentamos refrescar la sesión y registramos el resultado.
                        dataManager.refreshSession(player.getUniqueId());
                        if (debugHeartbeat) {
                            getLogger().info("[DEBUG-Heartbeat] ¡ÉXITO! Sesión refrescada en Redis para " + player.getName());
                        }
                    } catch (Exception e) {
                        // Si hay un error, lo registramos con todo el detalle.
                        if (debugHeartbeat) {
                            getLogger().severe("[DEBUG-Heartbeat] ¡ERROR CRÍTICO! Fallo al refrescar sesión para " + player.getName());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 20L * 5, 20L * 10); // Inicia tras 5 seg, se repite cada 10 seg.

        if (debugHeartbeat) {
            getLogger().info("[DEBUG] Tarea de Heartbeat para Redis iniciada en modo '" + serverType + "'.");
        }
    }

    public DataManager getDataManager() {
        return this.dataManager;
    }

    // --- NUEVOS MÉTODOS PARA GESTIONAR EL MAP ---
    public void setLoginTime(UUID playerUuid) {
        playerLoginTimes.put(playerUuid, System.currentTimeMillis());
    }

    public Long getLoginTime(UUID playerUuid) {
        return playerLoginTimes.get(playerUuid);
    }



    public void removeLoginTime(UUID playerUuid) {
        playerLoginTimes.remove(playerUuid);
    }
}