package com.disasterrelief.repository;

import com.disasterrelief.model.Region;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD repository for disaster-impacted administrative regions.
 */
public class RegionRepository {
    private final MongoCollection<Document> collection;

    public RegionRepository(MongoDatabase database) {
        this.collection = database.getCollection("regions");
    }

    public void create(Region region) {
        create(null, region);
    }

    public void create(ClientSession session, Region region) {
        Document doc = toDocument(region);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public void createMany(List<Region> regions) {
        if (regions == null || regions.isEmpty()) return;
        List<Document> docs = new ArrayList<>();
        for (Region r : regions) {
            docs.add(toDocument(r));
        }
        collection.insertMany(docs, new com.mongodb.client.model.InsertManyOptions().ordered(true));
    }

    public Region findById(int id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        return doc != null ? fromDocument(doc) : null;
    }

    public List<Region> findAll() {
        List<Region> list = new ArrayList<>();
        for (Document doc : collection.find()) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public boolean update(Region region) {
        Document doc = toDocument(region);
        return collection.replaceOne(Filters.eq("_id", region.getId()), doc).getModifiedCount() > 0;
    }

    public boolean delete(int id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(Region r) {
        return new Document("_id", r.getId())
                .append("name", r.getName())
                .append("population", r.getPopulation());
    }

    private Region fromDocument(Document doc) {
        Region r = new Region();
        r.setId(doc.getInteger("_id"));
        r.setName(doc.getString("name"));
        r.setPopulation(doc.getInteger("population"));
        return r;
    }
}
