package agzam4.api.auth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import arc.util.Nullable;
import arc.util.Time;

public class AuthTokens {
    private static final long TTL_MS = 5 * 60 * 1000;

    private static class PendingToken {
        final String uuid;
        final long expiresAt;

        PendingToken(String uuid) {
            this.uuid = uuid;
            this.expiresAt = Time.millis() + TTL_MS;
        }

        boolean expired() {
            return Time.millis() > expiresAt;
        }
    }

    private static final ConcurrentHashMap<String, PendingToken> tokens = new ConcurrentHashMap<>();

    public static String create(String uuid) {
        cleanup();
        String token = UUID.randomUUID().toString();
        tokens.put(token, new PendingToken(uuid));
        return token;
    }

    public static @Nullable String verify(String token) {
        PendingToken t = tokens.remove(token);
        if(t == null || t.expired()) return null;
        return t.uuid;
    }

    private static void cleanup() {
        tokens.values().removeIf(PendingToken::expired);
    }
}
