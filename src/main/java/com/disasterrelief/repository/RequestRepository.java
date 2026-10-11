package com.disasterrelief.repository;

import com.disasterrelief.model.*;
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
 * CRUD repository for disaster relief and search-and-rescue requests.
 * Persists polymorphic Request instances using the 'type' discriminator.
 */
public class RequestRepository {
    private final MongoCollection<Document> collection;

    public RequestRepository(MongoDatabase database) {
        this.collection = database.getCollection("requests");
    }

    public void create(Request request) {
        create(null, request);
    }

    public void create(ClientSession session, Request request) {
        Document doc = toDocument(request);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public void createMany(List<Request> requests) {
        if (requests == null || requests.isEmpty()) return;
        List<Document> docs = new ArrayList<>();
        for (Request r : requests) {
            docs.add(toDocument(r));
        }
        collection.insertMany(docs, new com.mongodb.client.model.InsertManyOptions().ordered(true));
    }

    public Request findById(String id) {
        return findById(null, id);
    }

    public Request findById(ClientSession session, String id) {
        Document doc;
        if (session != null) {
            doc = collection.find(session, Filters.eq("_id", id)).first();
        } else {
            doc = collection.find(Filters.eq("_id", id)).first();
        }
        return doc != null ? fromDocument(doc) : null;
    }

    public List<Request> findByReportId(String reportId) {
        List<Request> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("reportId", reportId))) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public List<Request> findAll() {
        return findAll(null);
    }

    public List<Request> findAll(ClientSession session) {
        List<Request> list = new ArrayList<>();
        com.mongodb.client.FindIterable<Document> iter = (session != null) ? collection.find(session) : collection.find();
        for (Document doc : iter) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public List<Request> findWaitingOrPartiallyAllocated() {
        List<Request> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.in("status", "WAITING", "PARTIALLY_ALLOCATED"))) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public List<Request> findWaitingAndPartiallyAllocated() {
        return findWaitingOrPartiallyAllocated();
    }

    public boolean update(Request request) {
        return update(null, request);
    }

    public boolean update(ClientSession session, Request request) {
        Document doc = toDocument(request);
        long count;
        if (session != null) {
            count = collection.replaceOne(session, Filters.eq("_id", request.getId()), doc).getModifiedCount();
        } else {
            count = collection.replaceOne(Filters.eq("_id", request.getId()), doc).getModifiedCount();
        }
        return count > 0;
    }

    public boolean updateStatus(ClientSession session, String id, RequestStatus status) {
        Document updateDoc = new Document("status", status.name())
                .append("updatedAt", Instant.now().toString());
        long count;
        if (session != null) {
            count = collection.updateOne(session, Filters.eq("_id", id), new Document("$set", updateDoc)).getModifiedCount();
        } else {
            count = collection.updateOne(Filters.eq("_id", id), new Document("$set", updateDoc)).getModifiedCount();
        }
        return count > 0;
    }

    public boolean delete(String id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(Request r) {
        Document doc = new Document("_id", r.getId())
                .append("reportId", r.getReportId())
                .append("regionId", r.getRegionId())
                .append("severity", r.getSeverity())
                .append("severityLevel", r.getSeverityLevel())
                .append("severityRuleId", r.getSeverityRuleId())
                .append("severityExplanation", r.getSeverityExplanation())
                .append("needOrder", r.getNeedOrder())
                .append("affectedPopulation", r.getAffectedPopulation())
                .append("arrivalNo", r.getArrivalNo())
                .append("status", r.getStatus() != null ? r.getStatus().name() : RequestStatus.PENDING_REVIEW.name())
                .append("createdBy", r.getCreatedBy())
                .append("type", r.getType())
                .append("mostUrgentNeed", r.getMostUrgentNeed() != null ? r.getMostUrgentNeed().name() : UrgentNeed.RESCUE.name())
                .append("rejectionReason", r.getRejectionReason())
                .append("notes", r.getNotes())
                .append("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : Instant.now().toString())
                .append("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : Instant.now().toString());

        // Serialize items array
        if (r.getItems() != null && !r.getItems().isEmpty()) {
            java.util.List<Document> itemsDocs = new java.util.ArrayList<>();
            for (RequestItem item : r.getItems()) {
                itemsDocs.add(new Document("need", item.getNeed())
                        .append("quantityRequested", item.getQuantityRequested())
                        .append("quantityAllocated", item.getQuantityAllocated())
                        .append("quantityOutstanding", item.getQuantityOutstanding()));
            }
            doc.append("items", itemsDocs);
        }

        if (r instanceof ReliefRequest) {
            ReliefRequest rel = (ReliefRequest) r;
            doc.append("locationCode", rel.getLocationCode())
               .append("foodNeeded", rel.getFoodNeeded())
               .append("foodAllocated", rel.getFoodAllocated())
               .append("medicineNeeded", rel.getMedicineNeeded())
               .append("medicineAllocated", rel.getMedicineAllocated())
               .append("rescueRequired", rel.isRescueRequired())
               .append("shelterPlacesRequested", rel.getShelterPlacesRequested())
               .append("shelterPlacesAllocated", rel.getShelterPlacesAllocated());
        }

        if (r instanceof RescueRequest) {
            RescueRequest rr = (RescueRequest) r;
            doc.append("peopleToEvacuate", rr.getPeopleToEvacuate())
               .append("peopleEvacuated", rr.getPeopleEvacuated())
               .append("rescueCount", rr.getRescueCount())
               .append("shelterCount", rr.getShelterCount())
               .append("assignedTeamId", rr.getAssignedTeamId())
               .append("assignedShelterId", rr.getAssignedShelterId());
        } else if (r instanceof SupplyRequest) {
            SupplyRequest sr = (SupplyRequest) r;
            doc.append("medicineNeeded", sr.getMedicineNeeded())
               .append("medicineAllocated", sr.getMedicineAllocated())
               .append("foodNeeded", sr.getFoodNeeded())
               .append("foodAllocated", sr.getFoodAllocated());
        }
        return doc;
    }

    private Request fromDocument(Document doc) {
        String type = doc.getString("type");
        Request r;
        if ("RESCUE".equalsIgnoreCase(type)) {
            RescueRequest rr = new RescueRequest();
            rr.setPeopleToEvacuate(doc.getInteger("peopleToEvacuate", 0));
            rr.setPeopleEvacuated(doc.getInteger("peopleEvacuated", 0));
            rr.setRescueCount(doc.getInteger("rescueCount", rr.getPeopleToEvacuate()));
            rr.setShelterCount(doc.getInteger("shelterCount", 0));
            rr.setAssignedTeamId(doc.getString("assignedTeamId"));
            rr.setAssignedShelterId(doc.getString("assignedShelterId"));
            r = rr;
        } else {
            SupplyRequest sr = new SupplyRequest();
            sr.setMedicineNeeded(doc.getInteger("medicineNeeded", 0));
            sr.setMedicineAllocated(doc.getInteger("medicineAllocated", 0));
            sr.setFoodNeeded(doc.getInteger("foodNeeded", 0));
            sr.setFoodAllocated(doc.getInteger("foodAllocated", 0));
            r = sr;
        }

        r.setId(doc.getString("_id"));
        r.setReportId(doc.getString("reportId"));
        r.setRegionId(doc.getInteger("regionId", 1));
        r.setSeverity(doc.getInteger("severity", 1));
        r.setSeverityLevel(doc.getInteger("severityLevel", r.getSeverity()));
        r.setSeverityRuleId(doc.getString("severityRuleId"));
        r.setSeverityExplanation(doc.getString("severityExplanation"));
        if (doc.containsKey("needOrder")) {
            r.setNeedOrder(doc.getList("needOrder", String.class));
        }

        // Deserialize items array
        if (doc.containsKey("items")) {
            java.util.List<Document> itemDocs = doc.getList("items", Document.class);
            if (itemDocs != null) {
                java.util.List<RequestItem> items = new java.util.ArrayList<>();
                for (Document idoc : itemDocs) {
                    items.add(new RequestItem(
                            idoc.getString("need"),
                            idoc.getInteger("quantityRequested", 0),
                            idoc.getInteger("quantityAllocated", 0),
                            idoc.getInteger("quantityOutstanding", 0)
                    ));
                }
                r.setItems(items);
            }
        }

        r.setAffectedPopulation(doc.getInteger("affectedPopulation", 0));
        Number arrNo = (Number) doc.get("arrivalNo");
        r.setArrivalNo(arrNo != null ? arrNo.longValue() : 0L);
        String statusStr = doc.getString("status");
        r.setStatus(statusStr != null ? RequestStatus.valueOf(statusStr) : RequestStatus.PENDING_REVIEW);
        r.setCreatedBy(doc.getString("createdBy"));
        String urgentStr = doc.getString("mostUrgentNeed");
        r.setMostUrgentNeed(urgentStr != null ? UrgentNeed.valueOf(urgentStr) : UrgentNeed.RESCUE);
        r.setRejectionReason(doc.getString("rejectionReason"));
        r.setNotes(doc.getString("notes"));

        String created = doc.getString("createdAt");
        if (created != null) {
            try { r.setCreatedAt(Instant.parse(created)); } catch (Exception ignored) {}
        }
        String updated = doc.getString("updatedAt");
        if (updated != null) {
            try { r.setUpdatedAt(Instant.parse(updated)); } catch (Exception ignored) {}
        }
        if (r instanceof ReliefRequest) {
            ReliefRequest rel = (ReliefRequest) r;
            rel.setLocationCode(doc.getString("locationCode"));
            rel.setFoodNeeded(doc.getInteger("foodNeeded", 0));
            rel.setFoodAllocated(doc.getInteger("foodAllocated", 0));
            rel.setMedicineNeeded(doc.getInteger("medicineNeeded", 0));
            rel.setMedicineAllocated(doc.getInteger("medicineAllocated", 0));
            rel.setRescueRequired(doc.getBoolean("rescueRequired", "RESCUE".equalsIgnoreCase(type)));
            rel.setShelterPlacesRequested(doc.getInteger("shelterPlacesRequested", doc.getInteger("shelterCount", 0)));
            rel.setShelterPlacesAllocated(doc.getInteger("shelterPlacesAllocated", doc.getInteger("peopleEvacuated", 0)));
        }

        return r;
    }
}
