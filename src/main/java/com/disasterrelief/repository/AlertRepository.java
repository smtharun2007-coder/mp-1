package com.disasterrelief.repository;

import com.disasterrelief.model.AlertEntry;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for control room alerts regarding road closures impacting active missions.
 */
public class AlertRepository {
    private final MongoCollection<Document> collection;

    public AlertRepository(MongoDatabase database) {
        this.collection = database.getCollection("alerts");
    }

    public void create(AlertEntry alert) {
        create(null, alert);
    }

    public void create(ClientSession session, AlertEntry alert) {
        Document doc = toDocument(alert);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public List<AlertEntry> findAll() {
        List<AlertEntry> list = new ArrayList<>();
        for (Document doc : collection.find().sort(new Document("timestamp", -1))) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public boolean acknowledge(String id) {
        return collection.updateOne(Filters.eq("_id", id), Updates.set("acknowledged", true)).getModifiedCount() > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(AlertEntry a) {
        return new Document("_id", a.getId())
                .append("title", a.getTitle())
                .append("message", a.getMessage())
                .append("severity", a.getSeverity())
                .append("relatedId", a.getRelatedId())
                .append("acknowledged", a.isAcknowledged())
                .append("timestamp", a.getTimestamp() != null ? a.getTimestamp().toString() : Instant.now().toString());
    }

    private AlertEntry fromDocument(Document doc) {
        AlertEntry a = new AlertEntry();
        a.setId(doc.getString("_id"));
        a.setTitle(doc.getString("title"));
        a.setMessage(doc.getString("message"));
        a.setSeverity(doc.getString("severity"));
        a.setRelatedId(doc.getString("relatedId"));
        Boolean ack = doc.getBoolean("acknowledged");
        a.setAcknowledged(ack != null && ack);
        String ts = doc.getString("timestamp");
        if (ts != null) {
            try { a.setTimestamp(Instant.parse(ts)); } catch (Exception ignored) {}
        }
        return a;
    }
}
