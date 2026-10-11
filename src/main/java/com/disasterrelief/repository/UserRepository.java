package com.disasterrelief.repository;

import com.disasterrelief.model.Role;
import com.disasterrelief.model.User;
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
 * CRUD repository for user accounts.
 */
public class UserRepository {
    private final MongoCollection<Document> collection;

    public UserRepository(MongoDatabase database) {
        this.collection = database.getCollection("users");
    }

    public void create(User user) {
        create(null, user);
    }

    public void create(ClientSession session, User user) {
        Document doc = toDocument(user);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public User findByUsername(String username) {
        return findByUsername(null, username);
    }

    public User findByUsername(ClientSession session, String username) {
        Document doc;
        if (session != null) {
            doc = collection.find(session, Filters.eq("username", username)).first();
        } else {
            doc = collection.find(Filters.eq("username", username)).first();
        }
        return doc != null ? fromDocument(doc) : null;
    }

    public User findById(String id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        return doc != null ? fromDocument(doc) : null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        for (Document doc : collection.find()) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public boolean update(User user) {
        return update(null, user);
    }

    public boolean update(ClientSession session, User user) {
        Document updateDoc = new Document("username", user.getUsername())
                .append("passwordHash", user.getPasswordHash())
                .append("salt", user.getSalt())
                .append("role", user.getRole().name())
                .append("assignedLocationIds", user.getAssignedLocationIds())
                .append("active", user.isActive());

        long count;
        if (session != null) {
            count = collection.updateOne(session, Filters.eq("_id", user.getId()), new Document("$set", updateDoc)).getModifiedCount();
        } else {
            count = collection.updateOne(Filters.eq("_id", user.getId()), new Document("$set", updateDoc)).getModifiedCount();
        }
        return count > 0;
    }

    public boolean setActive(String id, boolean active) {
        return collection.updateOne(Filters.eq("_id", id), Updates.set("active", active)).getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        return delete(null, id);
    }

    public boolean delete(ClientSession session, String id) {
        long count;
        if (session != null) {
            count = collection.deleteOne(session, Filters.eq("_id", id)).getDeletedCount();
        } else {
            count = collection.deleteOne(Filters.eq("_id", id)).getDeletedCount();
        }
        return count > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(User user) {
        return new Document("_id", user.getId())
                .append("username", user.getUsername())
                .append("passwordHash", user.getPasswordHash())
                .append("salt", user.getSalt())
                .append("role", user.getRole().name())
                .append("assignedLocationIds", user.getAssignedLocationIds())
                .append("active", user.isActive())
                .append("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : Instant.now().toString());
    }

    @SuppressWarnings("unchecked")
    private User fromDocument(Document doc) {
        User u = new User();
        u.setId(doc.getString("_id"));
        u.setUsername(doc.getString("username"));
        u.setPasswordHash(doc.getString("passwordHash"));
        u.setSalt(doc.getString("salt"));
        String roleStr = doc.getString("role");
        u.setRole(roleStr != null ? Role.valueOf(roleStr) : Role.FIELD_OFFICER);
        List<Integer> locs = (List<Integer>) doc.get("assignedLocationIds");
        u.setAssignedLocationIds(locs != null ? locs : new ArrayList<>());
        Boolean active = doc.getBoolean("active");
        u.setActive(active == null || active);
        String created = doc.getString("createdAt");
        if (created != null) {
            try { u.setCreatedAt(Instant.parse(created)); } catch (Exception ignored) {}
        }
        return u;
    }
}
