package com.disasterrelief.repository;

import com.disasterrelief.model.Road;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD repository for roads connecting graph vertices.
 */
public class RoadRepository {
    private final MongoCollection<Document> collection;

    public RoadRepository(MongoDatabase database) {
        this.collection = database.getCollection("roads");
    }

    public void create(Road road) {
        create(null, road);
    }

    public void create(ClientSession session, Road road) {
        Document doc = toDocument(road);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public void createMany(List<Road> roads) {
        if (roads == null || roads.isEmpty()) return;
        List<Document> docs = new ArrayList<>();
        for (Road r : roads) {
            docs.add(toDocument(r));
        }
        collection.insertMany(docs, new com.mongodb.client.model.InsertManyOptions().ordered(true));
    }

    public Road findById(String id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        return doc != null ? fromDocument(doc) : null;
    }

    public List<Road> findAll() {
        List<Road> list = new ArrayList<>();
        for (Document doc : collection.find()) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public boolean updateStatus(String roadId, Road.RoadStatus status, String closureReason) {
        return updateStatus(null, roadId, status, closureReason);
    }

    public boolean updateStatus(ClientSession session, String roadId, Road.RoadStatus status, String closureReason) {
        Document updates = new Document("status", status.name())
                .append("closureReason", closureReason != null ? closureReason : "");
        long count;
        if (session != null) {
            count = collection.updateOne(session, Filters.eq("_id", roadId), new Document("$set", updates)).getModifiedCount();
        } else {
            count = collection.updateOne(Filters.eq("_id", roadId), new Document("$set", updates)).getModifiedCount();
        }
        return count > 0;
    }

    public boolean update(Road road) {
        Document doc = toDocument(road);
        return collection.replaceOne(Filters.eq("_id", road.getId()), doc).getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(Road r) {
        return new Document("_id", r.getId())
                .append("from", r.getFrom())
                .append("to", r.getTo())
                .append("travelTime", r.getTravelTime())
                .append("status", r.getStatus() != null ? r.getStatus().name() : Road.RoadStatus.OPEN.name())
                .append("closureReason", r.getClosureReason() != null ? r.getClosureReason() : "");
    }

    private Road fromDocument(Document doc) {
        Road r = new Road();
        r.setId(doc.getString("_id"));
        r.setFrom(doc.getInteger("from"));
        r.setTo(doc.getInteger("to"));
        r.setTravelTime(doc.getInteger("travelTime"));
        String statusStr = doc.getString("status");
        r.setStatus(statusStr != null ? Road.RoadStatus.valueOf(statusStr) : Road.RoadStatus.OPEN);
        r.setClosureReason(doc.getString("closureReason"));
        return r;
    }
}
