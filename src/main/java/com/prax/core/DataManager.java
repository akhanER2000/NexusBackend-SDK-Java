package com.prax.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataManager {

    private final PraxCorePlugin plugin;
    private File customConfigFile;
    private FileConfiguration customConfig;

    private JedisPool jedisPool;
    private final String SESSION_KEY_PREFIX = "prax:session:";
    private final int SESSION_TIMEOUT_SECONDS = 15;

    public DataManager(PraxCorePlugin plugin) {
        this.plugin = plugin;
        setupFile();
        setupRedis();
    }

    public void setupFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        customConfigFile = new File(plugin.getDataFolder(), "players.yml");
        if (!customConfigFile.exists()) {
            try {
                customConfigFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear el archivo players.yml!");
            }
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }

    public void setupRedis() {
        try {
            this.jedisPool = new JedisPool("redis", 6379);
            plugin.getLogger().info("Conexión con Redis establecida correctamente.");
        } catch (Exception e) {
            plugin.getLogger().severe("¡ERROR! No se pudo conectar con el servidor de Redis.");
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
            plugin.getLogger().info("Conexión con Redis cerrada.");
        }
    }

    public void setSessionActive(UUID playerUuid, boolean active) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = SESSION_KEY_PREFIX + playerUuid.toString();
            if (active) {
                jedis.setex(key, SESSION_TIMEOUT_SECONDS, "active");
            } else {
                jedis.del(key);
            }
        }
    }

    public boolean isSessionActive(UUID playerUuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(SESSION_KEY_PREFIX + playerUuid.toString());
        }
    }

    public void refreshSession(UUID playerUuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = SESSION_KEY_PREFIX + playerUuid.toString();
            jedis.expire(key, SESSION_TIMEOUT_SECONDS);
        }
    }

    public FileConfiguration getConfig() {
        return customConfig;
    }

    public void saveData() {
        try {
            getConfig().save(customConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("No se pudo guardar la configuración en " + customConfigFile);
        }
    }

    public boolean isPlayerRegistered(UUID playerUuid) {
        return getConfig().contains("players." + playerUuid.toString());
    }

    public String getPasswordHash(UUID playerUuid) {
        return getConfig().getString("players." + playerUuid.toString() + ".passwordHash");
    }

    public void registerPlayer(UUID playerUuid, String hashedPassword, String email, String ipAddress, String clientType, String version) {
        String basePath = "players." + playerUuid.toString();
        getConfig().set(basePath + ".passwordHash", hashedPassword);
        getConfig().set(basePath + ".email", email);
        getConfig().set(basePath + ".registrationIp", ipAddress);
        getConfig().set(basePath + ".clientType", clientType);
        getConfig().set(basePath + ".versionOnRegister", version);
        saveData();
    }

    public void setFirstLoginDate(UUID playerUuid, String date) {
        getConfig().set("players." + playerUuid.toString() + ".firstLogin", date);
        saveData();
    }

    public void setLastLoginDate(UUID playerUuid, String datetime) {
        getConfig().set("players." + playerUuid.toString() + ".lastLogin", datetime);
        saveData();
    }

    public void incrementLoginCount(UUID playerUuid) {
        String path = "players." + playerUuid.toString() + ".loginCount";
        int currentCount = getConfig().getInt(path, 0);
        getConfig().set(path, currentCount + 1);
        saveData();
    }

    // --- METODO AÑADIDO PARA GUARDAR EL PLAYTIME ---
    public void addPlaytime(UUID playerUuid, long sessionInMillis) {
        String path = "players." + playerUuid.toString() + ".playtime";
        long currentPlaytime = getConfig().getLong(path, 0L);
        long newPlaytime = currentPlaytime + sessionInMillis;
        getConfig().set(path, newPlaytime);
        saveData();
    }

    public void incrementDeaths(UUID playerUuid) {
        String path = "players." + playerUuid.toString() + ".deaths";
        int currentDeaths = getConfig().getInt(path, 0);
        getConfig().set(path, currentDeaths + 1);
        saveData();
    }
}