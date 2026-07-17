package agzam4.api.auth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import arc.util.Nullable;
import arc.util.Time;

public class AuthTokens {
    private static final long TTL_MS = 5 * 60 * 1000;

    public static class PendingToken {
    	public final String uuid, usid;
        final long expiresAt;

        PendingToken(String uuid, String usid) {
            this.uuid = uuid;
            this.usid = usid;
            this.expiresAt = Time.millis() + TTL_MS;
        }

        boolean expired() {
            return Time.millis() > expiresAt;
        }
    }

    private static final ConcurrentHashMap<String, PendingToken> tokens = new ConcurrentHashMap<>();

    public static String create(String uuid, String usid) {
        cleanup();
        String token = UUID.randomUUID().toString();
        tokens.put(token, new PendingToken(uuid, usid));
        return token;
    }

    public static @Nullable PendingToken verify(String token) {
        PendingToken t = tokens.remove(token);
        if(t == null || t.expired()) return null;
        return t;
    }

    private static void cleanup() {
        tokens.values().removeIf(PendingToken::expired);
    }
}
