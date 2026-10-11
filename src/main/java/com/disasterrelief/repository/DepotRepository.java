package com.disasterrelief.repository;

import com.disasterrelief.model.Depot;
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
 * CRUD repository for resource warehouses.
 * Implements atomic conditional updates preventing stock double-promising.
 */
public class DepotRepository {
    private final MongoCollection<Document> collection;

    public DepotRepository(MongoDatabase database) {
        this.collection = database.getCollection("depots");
    }

    public void create(Depot depot) {
        create(null, depot);
    }

    public void create(ClientSession session, Depot depot) {
        Document doc = toDocument(depot);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public void createMany(List<Depot> depots) {
        if (depots == null || depots.isEmpty()) return;
        List<Document> docs = new ArrayList<>();
        for (Depot d : depots) {
            docs.add(toDocument(d));
        }
        collection.insertMany(docs, new com.mongodb.client.model.InsertManyOptions().ordered(true));
    }

    public Depot findById(String id) {
        return findById(null, id);
    }

    public Depot findById(ClientSession session, String id) {
        Document doc;
        if (session != null) {
            doc = collection.find(session, Filters.eq("_id", id)).first();
        } else {
            doc = collection.find(Filters.eq("_id", id)).first();
        }
        return doc != null ? fromDocument(doc) : null;
    }

    public List<Depot> findAll() {
        return findAll(null);
    }

    public List<Depot> findAll(ClientSession session) {
        List<Depot> list = new ArrayList<>();
        com.mongodb.client.FindIterable<Document> iter = (session != null) ? collection.find(session) : collection.find();
        for (Document doc : iter) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    /**
     * Atomically reserves medicine and food using a conditional filter.
     * Guarantees modifiedCount == 1 so that double-promising is physically impossible.
     */
    public boolean reserveStock(ClientSession session, String depotId, int medicineQty, int foodQty) {
        if (medicineQty <= 0 && foodQty <= 0) return true;

        List<Bson> filterList = new ArrayList<>();
        filterList.add(Filters.eq("_id", depotId));
        if (medicineQty > 0) {
            filterList.add(Filters.gte("medicineKits", medicineQty));
        }
        if (foodQty > 0) {
            filterList.add(Filters.gte("foodPacks", foodQty));
        }

        Bson filter = Filters.and(filterList);
        Bson update = Updates.combine(
                Updates.inc("medicineKits", -medicineQty),
                Updates.inc("reservedMedicine", medicineQty),
                Updates.inc("foodPacks", -foodQty),
                Updates.inc("reservedFood", foodQty)
        );

        long count;
        if (session != null) {
            count = collection.updateOne(session, filter, update).getModifiedCount();
        } else {
            count = collection.updateOne(filter, update).getModifiedCount();
        }
        return count == 1;
    }

    /**
     * Releases reserved stock back to available stock.
     */
    public boolean releaseStock(ClientSession session, String depotId, int medicineQty, int foodQty) {
        Bson filter = Filters.eq("_id", depotId);
        Bson update = Updates.combine(
                Updates.inc("medicineKits", medicineQty),
                Updates.inc("reservedMedicine", -medicineQty),
                Updates.inc("foodPacks", foodQty),
                Updates.inc("reservedFood", -foodQty)
        );

        long count;
        if (session != null) {
            count = collection.updateOne(session, filter, update).getModifiedCount();
        } else {
            count = collection.updateOne(filter, update).getModifiedCount();
        }
        return count == 1;
    }

    /**
     * Consumes reserved stock upon successful delivery.
     */
    public boolean consumeReservedStock(ClientSession session, String depotId, int medicineQty, int foodQty) {
        Bson filter = Filters.eq("_id", depotId);
        Bson update = Updates.combine(
                Updates.inc("reservedMedicine", -medicineQty),
                Updates.inc("reservedFood", -foodQty)
        );

        long count;
        if (session != null) {
            count = collection.updateOne(session, filter, update).getModifiedCount();
        } else {
            count = collection.updateOne(filter, update).getModifiedCount();
        }
        return count == 1;
    }

    /**
     * Adjusts available stock for replenishment or manual adjustments.
     */
    public boolean adjustStock(ClientSession session, String depotId, int medicineDelta, int foodDelta) {
        Bson filter = Filters.eq("_id", depotId);
        Bson update = Updates.combine(
                Updates.inc("medicineKits", medicineDelta),
                Updates.inc("foodPacks", foodDelta)
        );

        long count;
        if (session != null) {
            count = collection.updateOne(session, filter, update).getModifiedCount();
        } else {
            count = collection.updateOne(filter, update).getModifiedCount();
        }
        return count == 1;
    }

    public boolean update(Depot depot) {
        Document doc = toDocument(depot);
        return collection.replaceOne(Filters.eq("_id", depot.getId()), doc).getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(Depot d) {
        return new Document("_id", d.getId())
                .append("name", d.getName())
                .append("node", d.getNode())
                .append("medicineKits", d.getMedicineKits())
                .append("foodPacks", d.getFoodPacks())
                .append("reservedMedicine", d.getReservedMedicine())
                .append("reservedFood", d.getReservedFood());
    }

    private Depot fromDocument(Document doc) {
        Depot d = new Depot();
        d.setId(doc.getString("_id"));
        d.setName(doc.getString("name"));
        d.setNode(doc.getInteger("node"));
        int med = doc.getInteger("medicineKits", 0);
        int food = doc.getInteger("foodPacks", 0);
        int resMed = doc.getInteger("reservedMedicine", 0);
        int resFood = doc.getInteger("reservedFood", 0);
        d.hydrate(med, food, resMed, resFood);
        return d;
    }
}
