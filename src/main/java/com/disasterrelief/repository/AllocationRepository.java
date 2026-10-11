package com.disasterrelief.repository;

import com.disasterrelief.model.Allocation;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD repository for disaster resource allocation records.
 */
public class AllocationRepository {
    private final MongoCollection<Document> collection;

    public AllocationRepository(MongoDatabase database) {
        this.collection = database.getCollection("allocations");
    }

    public void create(Allocation allocation) {
        create(null, allocation);
    }

    public void create(ClientSession session, Allocation allocation) {
        Document doc = toDocument(allocation);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public Allocation findById(String id) {
        return findById(null, id);
    }

    public Allocation findById(ClientSession session, String id) {
        Document doc;
        if (session != null) {
            doc = collection.find(session, Filters.eq("_id", id)).first();
        } else {
            doc = collection.find(Filters.eq("_id", id)).first();
        }
        return doc != null ? fromDocument(doc) : null;
    }

    public List<Allocation> findByRequestId(String requestId) {
        List<Allocation> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("requestId", requestId))) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public List<Allocation> findAll() {
        List<Allocation> list = new ArrayList<>();
        for (Document doc : collection.find()) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public boolean updateStatus(ClientSession session, String id, Allocation.AllocationStatus status) {
        Document update = new Document("status", status.name());
        if (status == Allocation.AllocationStatus.DISPATCHED) {
            update.append("dispatchedAt", Instant.now().toString());
        } else if (status == Allocation.AllocationStatus.DELIVERED) {
            update.append("deliveredAt", Instant.now().toString());
        }

        long count;
        if (session != null) {
            count = collection.updateOne(session, Filters.eq("_id", id), new Document("$set", update)).getModifiedCount();
        } else {
            count = collection.updateOne(Filters.eq("_id", id), new Document("$set", update)).getModifiedCount();
        }
        return count > 0;
    }

    public boolean delete(String id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(Allocation a) {
        return new Document("_id", a.getId())
                .append("requestId", a.getRequestId())
                .append("reportId", a.getReportId())
                .append("targetRegionId", a.getTargetRegionId())
                .append("type", a.getType())
                .append("depotId", a.getDepotId())
                .append("depotNode", a.getDepotNode())
                .append("medicineAllocated", a.getMedicineAllocated())
                .append("foodAllocated", a.getFoodAllocated())
                .append("teamId", a.getTeamId())
                .append("shelterId", a.getShelterId())
                .append("shelterNode", a.getShelterNode())
                .append("peopleEvacuated", a.getPeopleEvacuated())
                .append("route", a.getRoute())
                .append("travelTime", a.getTravelTime())
                .append("status", a.getStatus() != null ? a.getStatus().name() : Allocation.AllocationStatus.COMMITTED.name())
                .append("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : Instant.now().toString())
                .append("dispatchedAt", a.getDispatchedAt() != null ? a.getDispatchedAt().toString() : null)
                .append("deliveredAt", a.getDeliveredAt() != null ? a.getDeliveredAt().toString() : null)
                .append("notes", a.getNotes());
    }

    @SuppressWarnings("unchecked")
    private Allocation fromDocument(Document doc) {
        Allocation a = new Allocation();
        a.setId(doc.getString("_id"));
        a.setRequestId(doc.getString("requestId"));
        a.setReportId(doc.getString("reportId"));
        a.setTargetRegionId(doc.getInteger("targetRegionId", 0));
        a.setType(doc.getString("type"));
        a.setDepotId(doc.getString("depotId"));
        a.setDepotNode(doc.getInteger("depotNode", 0));
        a.setMedicineAllocated(doc.getInteger("medicineAllocated", 0));
        a.setFoodAllocated(doc.getInteger("foodAllocated", 0));
        a.setTeamId(doc.getString("teamId"));
        a.setShelterId(doc.getString("shelterId"));
        a.setShelterNode(doc.getInteger("shelterNode", 0));
        a.setPeopleEvacuated(doc.getInteger("peopleEvacuated", 0));
        List<Integer> route = (List<Integer>) doc.get("route");
        a.setRoute(route != null ? route : new ArrayList<>());
        a.setTravelTime(doc.getInteger("travelTime", 0));
        String statusStr = doc.getString("status");
        a.setStatus(statusStr != null ? Allocation.AllocationStatus.valueOf(statusStr) : Allocation.AllocationStatus.COMMITTED);

        String created = doc.getString("createdAt");
        if (created != null) {
            try { a.setCreatedAt(Instant.parse(created)); } catch (Exception ignored) {}
        }
        String disp = doc.getString("dispatchedAt");
        if (disp != null) {
            try { a.setDispatchedAt(Instant.parse(disp)); } catch (Exception ignored) {}
        }
        String deliv = doc.getString("deliveredAt");
        if (deliv != null) {
            try { a.setDeliveredAt(Instant.parse(deliv)); } catch (Exception ignored) {}
        }
        a.setNotes(doc.getString("notes"));
        return a;
    }
}
