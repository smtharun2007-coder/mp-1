package com.disasterrelief.repository;

import com.disasterrelief.model.Shelter;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD repository for disaster shelters with conditional capacity reservations.
 */
public class ShelterRepository {
    private final MongoCollection<Document> collection;

    public ShelterRepository(MongoDatabase database) {
        this.collection = database.getCollection("shelters");
    }

    public void create(Shelter shelter) {
        create(null, shelter);
    }

    public void create(ClientSession session, Shelter shelter) {
        Document doc = toDocument(shelter);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public void createMany(List<Shelter> shelters) {
        if (shelters == null || shelters.isEmpty()) return;
        List<Document> docs = new ArrayList<>();
        for (Shelter s : shelters) {
            docs.add(toDocument(s));
        }
        collection.insertMany(docs, new com.mongodb.client.model.InsertManyOptions().ordered(true));
    }

    public Shelter findById(String id) {
        return findById(null, id);
    }

    public Shelter findById(ClientSession session, String id) {
        Document doc;
        if (session != null) {
            doc = collection.find(session, Filters.eq("_id", id)).first();
        } else {
            doc = collection.find(Filters.eq("_id", id)).first();
        }
        return doc != null ? fromDocument(doc) : null;
    }

    public List<Shelter> findAll() {
        return findAll(null);
    }

    public List<Shelter> findAll(ClientSession session) {
        List<Shelter> list = new ArrayList<>();
        com.mongodb.client.FindIterable<Document> iter = (session != null) ? collection.find(session) : collection.find();
        for (Document doc : iter) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    /**
     * Atomically reserves shelter places using $expr to verify: (capacity - reserved) >= qty.
     */
    public boolean reservePlaces(ClientSession session, String shelterId, int qty) {
        if (qty <= 0) return true;

        // updateOne({_id: shelterId, $expr: {$gte: [{$subtract: ["$capacity", "$reserved"]}, qty]}}, {$inc: {reserved: qty}})
        Document expr = new Document("$expr",
                new Document("$gte", List.of(
                        new Document("$subtract", List.of("$capacity", "$reserved")),
                        qty
                ))
        );

        Bson filter = Filters.and(Filters.eq("_id", shelterId), expr);
        Bson update = Updates.inc("reserved", qty);

        long count;
        if (session != null) {
            count = collection.updateOne(session, filter, update).getModifiedCount();
        } else {
            count = collection.updateOne(filter, update).getModifiedCount();
        }
        return count == 1;
    }

    /**
     * Releases reserved places back to shelter capacity.
     */
    public boolean releasePlaces(ClientSession session, String shelterId, int qty) {
        if (qty <= 0) return true;
        Bson filter = Filters.eq("_id", shelterId);
        Bson update = Updates.inc("reserved", -qty);

        long count;
        if (session != null) {
            count = collection.updateOne(session, filter, update).getModifiedCount();
        } else {
            count = collection.updateOne(filter, update).getModifiedCount();
        }
        return count == 1;
    }

    public boolean update(Shelter shelter) {
        Document doc = toDocument(shelter);
        return collection.replaceOne(Filters.eq("_id", shelter.getId()), doc).getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(Shelter s) {
        return new Document("_id", s.getId())
                .append("name", s.getName())
                .append("node", s.getNode())
                .append("capacity", s.getCapacity())
                .append("reserved", s.getReserved());
    }

    private Shelter fromDocument(Document doc) {
        Shelter s = new Shelter();
        s.setId(doc.getString("_id"));
        s.setName(doc.getString("name"));
        s.setNode(doc.getInteger("node"));
        int cap = doc.getInteger("capacity", 0);
        int res = doc.getInteger("reserved", 0);
        s.hydrate(cap, res);
        return s;
    }
}
