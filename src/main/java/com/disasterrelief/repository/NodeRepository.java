package com.disasterrelief.repository;

import com.disasterrelief.model.Node;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD repository for topological graph nodes.
 */
public class NodeRepository {
    private final MongoCollection<Document> collection;

    public NodeRepository(MongoDatabase database) {
        this.collection = database.getCollection("nodes");
    }

    public void create(Node node) {
        create(null, node);
    }

    public void create(ClientSession session, Node node) {
        Document doc = toDocument(node);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public void createMany(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) return;
        List<Document> docs = new ArrayList<>();
        for (Node n : nodes) {
            docs.add(toDocument(n));
        }
        collection.insertMany(docs, new com.mongodb.client.model.InsertManyOptions().ordered(true));
    }

    public Node findById(int id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        return doc != null ? fromDocument(doc) : null;
    }

    public List<Node> findAll() {
        List<Node> list = new ArrayList<>();
        for (Document doc : collection.find()) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public boolean update(Node node) {
        Document doc = toDocument(node);
        long count = collection.replaceOne(Filters.eq("_id", node.getId()), doc).getModifiedCount();
        return count > 0;
    }

    public boolean delete(int id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(Node n) {
        return new Document("_id", n.getId())
                .append("name", n.getName())
                .append("type", n.getType() != null ? n.getType().name() : Node.NodeType.REGION.name());
    }

    private Node fromDocument(Document doc) {
        Node n = new Node();
        n.setId(doc.getInteger("_id"));
        n.setName(doc.getString("name"));
        String typeStr = doc.getString("type");
        n.setType(typeStr != null ? Node.NodeType.valueOf(typeStr) : Node.NodeType.REGION);
        return n;
    }
}
