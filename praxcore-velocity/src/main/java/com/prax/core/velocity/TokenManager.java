// Ubicación: praxcore-velocity/src/main/java/com/prax/core/velocity/TokenManager.java
package com.prax.core.velocity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TokenManager {
    private final Map<UUID, String> activeSessions = new ConcurrentHashMap<>();
    public void storeToken(UUID playerUuid, String token) { activeSessions.put(playerUuid, token); }
    public void removeSession(UUID playerUuid) { activeSessions.remove(playerUuid); }
    public boolean isSessionValid(UUID playerUuid) { return activeSessions.containsKey(playerUuid); }
}