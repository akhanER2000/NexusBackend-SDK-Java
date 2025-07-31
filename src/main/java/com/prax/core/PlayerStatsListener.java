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

        // Al morir el jugador, simplemente llamamos al nuevo método del DataManager.
        plugin.getDataManager().incrementDeaths(playerUuid);

        // Opcional: Enviar un mensaje de depuración para confirmar que el evento se detectó.
        plugin.getLogger().info("Muerte registrada para el jugador: " + player.getName());
    }
}