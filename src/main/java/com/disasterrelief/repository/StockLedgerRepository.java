package com.disasterrelief.repository;

import com.disasterrelief.model.StockLedgerEntry;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only immutable repository recording all stock ledger movements.
 */
public class StockLedgerRepository {
    private final MongoCollection<Document> collection;

    public StockLedgerRepository(MongoDatabase database) {
        this.collection = database.getCollection("stock_ledger");
    }

    public void create(StockLedgerEntry entry) {
        create(null, entry);
    }

    public void create(ClientSession session, StockLedgerEntry entry) {
        Document doc = toDocument(entry);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public List<StockLedgerEntry> findAll() {
        List<StockLedgerEntry> list = new ArrayList<>();
        for (Document doc : collection.find().sort(new Document("timestamp", -1))) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(StockLedgerEntry e) {
        return new Document("_id", e.getId())
                .append("depotId", e.getDepotId())
                .append("user", e.getUser())
                .append("role", e.getRole())
                .append("item", e.getItem())
                .append("delta", e.getDelta())
                .append("actionType", e.getActionType())
                .append("reason", e.getReason())
                .append("timestamp", e.getTimestamp() != null ? e.getTimestamp().toString() : Instant.now().toString());
    }

    private StockLedgerEntry fromDocument(Document doc) {
        StockLedgerEntry e = new StockLedgerEntry();
        e.setId(doc.getString("_id"));
        e.setDepotId(doc.getString("depotId"));
        e.setUser(doc.getString("user"));
        e.setRole(doc.getString("role"));
        e.setItem(doc.getString("item"));
        e.setDelta(doc.getInteger("delta", 0));
        e.setActionType(doc.getString("actionType"));
        e.setReason(doc.getString("reason"));
        String ts = doc.getString("timestamp");
        if (ts != null) {
            try { e.setTimestamp(Instant.parse(ts)); } catch (Exception ignored) {}
        }
        return e;
    }
}
