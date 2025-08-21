package com.prax.core;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.prax.core.commands.LoginCommand;
import com.prax.core.commands.RegisterCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PraxCorePlugin extends JavaPlugin {

    private DataManager dataManager;
    private String serverType;
    private final Set<UUID> authenticatedPlayers = new HashSet<>();
    private final Set<UUID> pendingValidationPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Long> playerLoginTimes = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.serverType = this.getConfig().getString("server-type", "backend");
        this.dataManager = new DataManager(this);

        // Registrar Listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStatsListener(this), this);

        // Registrar Canales de Mensajería
        PluginMessageListener messageListener = new PluginMessageListener(this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", messageListener);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "prax:core");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "prax:core", messageListener);

        if (isLobbyServer()) {
            getLogger().info("Modo Lobby detectado. Registrando comandos.");
            this.getCommand("register").setExecutor(new RegisterCommand(this));
            this.getCommand("login").setExecutor(new LoginCommand(this));
            getServer().getPluginManager().registerEvents(new SelectorListener(this), this);
        }
    }

    public boolean isLobbyServer() { return "lobby".equalsIgnoreCase(this.serverType); }
    public String getServerType() { return this.serverType; }
    public boolean isAuthenticated(UUID playerUuid) { return authenticatedPlayers.contains(playerUuid); }
    public boolean isPendingValidation(UUID playerUuid) { return pendingValidationPlayers.contains(playerUuid); }

    public void setAuthenticated(UUID playerUuid, boolean authenticated) {
        if (authenticated) {
            authenticatedPlayers.add(playerUuid);
        } else {
            authenticatedPlayers.remove(playerUuid);
        }
        pendingValidationPlayers.remove(playerUuid);
    }

    public void setPendingValidation(UUID playerUuid, boolean pending) {
        if (pending) {
            pendingValidationPlayers.add(playerUuid);
        } else {
            pendingValidationPlayers.remove(playerUuid);
        }
    }

    public void sendStoreTokenMessage(Player player, String token) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("StoreToken");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(token);
        player.sendPluginMessage(this, "prax:core", out.toByteArray());
    }

    public void sendValidateTokenMessage(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ValidateToken");
        out.writeUTF(player.getUniqueId().toString());
        player.sendPluginMessage(this, "prax:core", out.toByteArray());
    }

    public DataManager getDataManager() { return this.dataManager; }
    public void setLoginTime(UUID playerUuid) { playerLoginTimes.put(playerUuid, System.currentTimeMillis()); }
    public Long getLoginTime(UUID playerUuid) { return playerLoginTimes.get(playerUuid); }
    public void removeLoginTime(UUID playerUuid) { playerLoginTimes.remove(playerUuid); }
}