package com.disasterrelief.repository;

import com.disasterrelief.model.AuditLogEntry;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only audit logging repository recording operational and security actions.
 */
public class AuditLogRepository {
    private final MongoCollection<Document> collection;

    public AuditLogRepository(MongoDatabase database) {
        this.collection = database.getCollection("audit_log");
    }

    public void log(AuditLogEntry entry) {
        log(null, entry);
    }

    public void log(ClientSession session, AuditLogEntry entry) {
        Document doc = toDocument(entry);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public void log(String user, String role, String action, String details) {
        log(null, new AuditLogEntry(java.util.UUID.randomUUID().toString(), user, role, action, details));
    }

    public void log(ClientSession session, String user, String role, String action, String details) {
        log(session, new AuditLogEntry(java.util.UUID.randomUUID().toString(), user, role, action, details));
    }

    public List<AuditLogEntry> findAll() {
        List<AuditLogEntry> list = new ArrayList<>();
        for (Document doc : collection.find().sort(new Document("timestamp", -1))) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(AuditLogEntry a) {
        return new Document("_id", a.getId())
                .append("user", a.getUser())
                .append("role", a.getRole())
                .append("action", a.getAction())
                .append("details", a.getDetails())
                .append("timestamp", a.getTimestamp() != null ? a.getTimestamp().toString() : Instant.now().toString());
    }

    private AuditLogEntry fromDocument(Document doc) {
        AuditLogEntry a = new AuditLogEntry();
        a.setId(doc.getString("_id"));
        a.setUser(doc.getString("user"));
        a.setRole(doc.getString("role"));
        a.setAction(doc.getString("action"));
        a.setDetails(doc.getString("details"));
        String ts = doc.getString("timestamp");
        if (ts != null) {
            try { a.setTimestamp(Instant.parse(ts)); } catch (Exception ignored) {}
        }
        return a;
    }
}
