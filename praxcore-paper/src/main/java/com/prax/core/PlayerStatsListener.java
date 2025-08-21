package com.prax.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class PlayerStatsListener implements Listener {

    private final PraxCorePlugin plugin;

    public PlayerStatsListener(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUuid = player.getUniqueId();

        // 1. Registramos la muerte del jugador que murió.
        plugin.getDataManager().incrementDeaths(playerUuid);

        // 2. Verificamos si el asesino fue otro jugador.
        Player killer = player.getKiller(); // getKiller() devuelve un Player si el asesino es un jugador.
        if (killer != null) {
            // 3. Si fue un jugador, registramos el "kill" para el asesino.
            UUID killerUuid = killer.getUniqueId();
            plugin.getDataManager().incrementKills(killerUuid);
            plugin.getLogger().info("Kill registrada para el asesino: " + killer.getName());
        }

        plugin.getLogger().info("Muerte registrada para el jugador: " + player.getName());
    }
}