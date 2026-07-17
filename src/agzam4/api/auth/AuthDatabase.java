package agzam4.api.auth;

import agzam4.database.Database;
import agzam4.api.auth.AuthTokens.PendingToken;
import agzam4.database.DBFields.DEFAULT;
import agzam4.database.DBFields.FIELD;
import agzam4.database.DBFields.PRIMARY_KEY;
import agzam4.database.Entity;
import agzam4.database.Table;
import arc.files.Fi;
import arc.util.Nullable;
import arc.util.Time;

public class AuthDatabase {
    private static Database db;
    private static Table<SessionEntity> sessions;

    public static class SessionEntity extends Entity {
        public @FIELD @PRIMARY_KEY String id;
        public @FIELD String uuid;
        public @FIELD String usid;
        public @FIELD String ip;
        public @FIELD @DEFAULT("0") long createdAt;
    }

    public static void init(Fi path) throws Exception {
        db = new Database(path);
        sessions = db.createTable("sessions", SessionEntity.class);
    }

    public static void createSession(String id, PendingToken token, String ip) {
        var entity = new SessionEntity();
        entity.id = id;
        entity.uuid = token.uuid;
        entity.usid = token.usid;
        entity.ip = ip;
        entity.createdAt = Time.millis();
        sessions.put(entity);
    }

    public static @Nullable SessionEntity validate(String sid, String ip) {
        var entity = sessions.get(sid);
        if(entity == null) return null;
        if(!entity.ip.equals(ip)) return null;
        return entity;
    }

    public static void remove(String id) {
        if(db == null) return;
        db.update("DELETE FROM sessions WHERE id = ?", id);
    }
}
