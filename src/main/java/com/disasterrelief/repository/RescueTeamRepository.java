package com.disasterrelief.repository;

import com.disasterrelief.model.RescueTeam;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD repository for rescue teams with atomic conditional status transitions.
 */
public class RescueTeamRepository {
    private final MongoCollection<Document> collection;

    public RescueTeamRepository(MongoDatabase database) {
        this.collection = database.getCollection("teams");
    }

    public void create(RescueTeam team) {
        create(null, team);
    }

    public void create(ClientSession session, RescueTeam team) {
        Document doc = toDocument(team);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public void createMany(List<RescueTeam> teams) {
        if (teams == null || teams.isEmpty()) return;
        List<Document> docs = new ArrayList<>();
        for (RescueTeam t : teams) {
            docs.add(toDocument(t));
        }
        collection.insertMany(docs, new com.mongodb.client.model.InsertManyOptions().ordered(true));
    }

    public RescueTeam findById(String id) {
        return findById(null, id);
    }

    public RescueTeam findById(ClientSession session, String id) {
        Document doc;
        if (session != null) {
            doc = collection.find(session, Filters.eq("_id", id)).first();
        } else {
            doc = collection.find(Filters.eq("_id", id)).first();
        }
        return doc != null ? fromDocument(doc) : null;
    }

    public List<RescueTeam> findAll() {
        return findAll(null);
    }

    public List<RescueTeam> findAll(ClientSession session) {
        List<RescueTeam> list = new ArrayList<>();
        com.mongodb.client.FindIterable<Document> iter = (session != null) ? collection.find(session) : collection.find();
        for (Document doc : iter) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    /**
     * Atomically reserves a team only if currently AVAILABLE.
     */
    public boolean reserveTeam(ClientSession session, String teamId) {
        long count;
        if (session != null) {
            count = collection.updateOne(session,
                    Filters.and(Filters.eq("_id", teamId), Filters.eq("status", "AVAILABLE")),
                    Updates.set("status", "BUSY")).getModifiedCount();
        } else {
            count = collection.updateOne(
                    Filters.and(Filters.eq("_id", teamId), Filters.eq("status", "AVAILABLE")),
                    Updates.set("status", "BUSY")).getModifiedCount();
        }
        return count == 1;
    }

    /**
     * Releases team back to AVAILABLE status.
     */
    public boolean releaseTeam(ClientSession session, String teamId) {
        long count;
        if (session != null) {
            count = collection.updateOne(session, Filters.eq("_id", teamId), Updates.set("status", "AVAILABLE")).getModifiedCount();
        } else {
            count = collection.updateOne(Filters.eq("_id", teamId), Updates.set("status", "AVAILABLE")).getModifiedCount();
        }
        return count == 1;
    }

    public boolean update(RescueTeam team) {
        Document doc = toDocument(team);
        return collection.replaceOne(Filters.eq("_id", team.getId()), doc).getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(RescueTeam t) {
        return new Document("_id", t.getId())
                .append("name", t.getName())
                .append("baseNode", t.getBaseNode())
                .append("status", t.getStatus() != null ? t.getStatus().name() : RescueTeam.TeamStatus.AVAILABLE.name());
    }

    private RescueTeam fromDocument(Document doc) {
        RescueTeam t = new RescueTeam();
        t.setId(doc.getString("_id"));
        t.setName(doc.getString("name"));
        t.setBaseNode(doc.getInteger("baseNode"));
        String statusStr = doc.getString("status");
        t.setStatus(statusStr != null ? RescueTeam.TeamStatus.valueOf(statusStr) : RescueTeam.TeamStatus.AVAILABLE);
        return t;
    }
}
