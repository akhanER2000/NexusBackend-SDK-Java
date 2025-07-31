package com.prax.core;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player; // <-- El import para Player
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

// La clase ahora implementa la interfaz correcta desde la API de Bukkit
public class PluginMessageListener implements org.bukkit.plugin.messaging.PluginMessageListener {

    private final PraxCorePlugin plugin;

    public static final Map<String, Integer> serverPlayerCounts = new HashMap<>();

    public PluginMessageListener(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!"BungeeCord".equalsIgnoreCase(channel)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("PlayerCount")) {
            String serverName = in.readUTF();
            int playerCount = in.readInt();
            serverPlayerCounts.put(serverName, playerCount);
        }
    }
}