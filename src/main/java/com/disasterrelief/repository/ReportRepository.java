package com.disasterrelief.repository;

import com.disasterrelief.model.*;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB repository for managing the 'reports' collection with full ClientSession transaction support.
 */
public class ReportRepository {
    private final MongoCollection<Document> collection;

    public ReportRepository(MongoDatabase database) {
        this.collection = database.getCollection("reports");
    }

    public void createIndexes() {
        collection.createIndex(Indexes.ascending("officerId", "createdAt"), new IndexOptions().name("idx_report_officer_created"));
        collection.createIndex(Indexes.ascending("status", "createdAt"), new IndexOptions().name("idx_report_status_created"));
        collection.createIndex(Indexes.ascending("regionId"), new IndexOptions().name("idx_report_region"));
    }

    public void create(Report report) {
        create(null, report);
    }

    public void create(ClientSession session, Report report) {
        Document doc = toDocument(report);
        if (session != null) {
            collection.insertOne(session, doc);
        } else {
            collection.insertOne(doc);
        }
    }

    public void createMany(List<Report> reports) {
        if (reports == null || reports.isEmpty()) return;
        List<Document> docs = new ArrayList<>();
        for (Report r : reports) {
            docs.add(toDocument(r));
        }
        collection.insertMany(docs);
    }

    public void update(Report report) {
        update(null, report);
    }

    public void update(ClientSession session, Report report) {
        report.setUpdatedAt(Instant.now());
        Document doc = toDocument(report);
        ReplaceOptions opts = new ReplaceOptions().upsert(true);
        if (session != null) {
            collection.replaceOne(session, Filters.eq("_id", report.getId()), doc, opts);
        } else {
            collection.replaceOne(Filters.eq("_id", report.getId()), doc, opts);
        }
    }

    public Report findById(String id) {
        return findById(null, id);
    }

    public Report findById(ClientSession session, String id) {
        Document doc;
        if (session != null) {
            doc = collection.find(session, Filters.eq("_id", id)).first();
        } else {
            doc = collection.find(Filters.eq("_id", id)).first();
        }
        return doc != null ? fromDocument(doc) : null;
    }

    public List<Report> findAll() {
        List<Report> list = new ArrayList<>();
        for (Document doc : collection.find().sort(Indexes.descending("createdAt"))) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public List<Report> findByOfficerId(String officerId) {
        List<Report> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("officerId", officerId)).sort(Indexes.descending("createdAt"))) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public List<Report> findByStatus(String status) {
        List<Report> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("status", status)).sort(Indexes.descending("createdAt"))) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public List<Report> findRecentByOfficerAndRegion(String officerId, int regionId, Instant since) {
        Bson filter = Filters.and(
                Filters.eq("officerId", officerId),
                Filters.eq("regionId", regionId),
                Filters.gte("createdAt", since.toString()),
                Filters.ne("status", "REJECTED")
        );
        List<Report> list = new ArrayList<>();
        for (Document doc : collection.find(filter)) {
            list.add(fromDocument(doc));
        }
        return list;
    }

    public void deleteById(String id) {
        collection.deleteOne(Filters.eq("_id", id));
    }

    public void deleteAll() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(Report r) {
        Document countsDoc = new Document()
                .append("total", r.getCounts().getTotal())
                .append("red", r.getCounts().getRed())
                .append("yellow", r.getCounts().getYellow())
                .append("green", r.getCounts().getGreen())
                .append("black", r.getCounts().getBlack())
                .append("trapped", r.getCounts().getTrapped())
                .append("vulnerable", r.getCounts().getVulnerable());

        Document needsDoc = new Document();
        if (r.getNeeds() != null) {
            if (r.getNeeds().getRescue() != null) {
                needsDoc.append("rescue", new Document("people", r.getNeeds().getRescue().getPeople()));
            }
            if (r.getNeeds().getShelter() != null) {
                needsDoc.append("shelter", new Document("people", r.getNeeds().getShelter().getPeople()));
            }
            if (r.getNeeds().getMedicine() != null) {
                needsDoc.append("medicine", new Document("people", r.getNeeds().getMedicine().getPeople())
                        .append("kitsSuggested", r.getNeeds().getMedicine().getKitsSuggested())
                        .append("kitsRequested", r.getNeeds().getMedicine().getKitsRequested())
                        .append("editReason", r.getNeeds().getMedicine().getEditReason()));
            }
            if (r.getNeeds().getFood() != null) {
                needsDoc.append("food", new Document("people", r.getNeeds().getFood().getPeople())
                        .append("days", r.getNeeds().getFood().getDays())
                        .append("kitsSuggested", r.getNeeds().getFood().getKitsSuggested())
                        .append("kitsRequested", r.getNeeds().getFood().getKitsRequested())
                        .append("editReason", r.getNeeds().getFood().getEditReason()));
            }
        }

        Document severityDoc = new Document();
        if (r.getSeverity() != null) {
            severityDoc.append("calculated", r.getSeverity().getCalculated())
                    .append("ruleId", r.getSeverity().getRuleId())
                    .append("explanation", r.getSeverity().getExplanation())
                    .append("overrideLevel", r.getSeverity().getOverrideLevel())
                    .append("overrideReason", r.getSeverity().getOverrideReason())
                    .append("overriddenBy", r.getSeverity().getOverriddenBy())
                    .append("overriddenAt", r.getSeverity().getOverriddenAt() != null ? r.getSeverity().getOverriddenAt().toString() : null);
        }

        List<Document> historyDocs = new ArrayList<>();
        if (r.getHistory() != null) {
            for (ReportHistoryEntry h : r.getHistory()) {
                historyDocs.add(new Document("version", h.getVersion())
                        .append("timestamp", h.getTimestamp() != null ? h.getTimestamp().toString() : Instant.now().toString())
                        .append("operator", h.getOperator())
                        .append("action", h.getAction())
                        .append("note", h.getNote()));
            }
        }

        return new Document("_id", r.getId())
                .append("officerId", r.getOfficerId())
                .append("regionId", r.getRegionId())
                .append("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : Instant.now().toString())
                .append("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : Instant.now().toString())
                .append("version", r.getVersion())
                .append("status", r.getStatus())
                .append("counts", countsDoc)
                .append("hoursWithoutSupplies", r.getHoursWithoutSupplies())
                .append("ongoingThreat", r.isOngoingThreat())
                .append("roadAccessObserved", r.getRoadAccessObserved())
                .append("needs", needsDoc)
                .append("needOrder", r.getNeedOrder())
                .append("notes", r.getNotes())
                .append("severity", severityDoc)
                .append("linkedRequestIds", r.getLinkedRequestIds())
                .append("rejectionReason", r.getRejectionReason())
                .append("history", historyDocs);
    }

    private Report fromDocument(Document doc) {
        Report r = new Report();
        r.setId(doc.getString("_id"));
        r.setOfficerId(doc.getString("officerId"));
        r.setRegionId(doc.getInteger("regionId", 1));
        r.setVersion(doc.getInteger("version", 1));
        r.setStatus(doc.getString("status"));
        r.setHoursWithoutSupplies(doc.getInteger("hoursWithoutSupplies", 0));
        r.setOngoingThreat(doc.getBoolean("ongoingThreat", false));
        r.setRoadAccessObserved(doc.getString("roadAccessObserved"));
        r.setNotes(doc.getString("notes"));
        r.setRejectionReason(doc.getString("rejectionReason"));

        String created = doc.getString("createdAt");
        if (created != null) {
            try { r.setCreatedAt(Instant.parse(created)); } catch (Exception ignored) {}
        }
        String updated = doc.getString("updatedAt");
        if (updated != null) {
            try { r.setUpdatedAt(Instant.parse(updated)); } catch (Exception ignored) {}
        }

        // Counts
        Document countsDoc = (Document) doc.get("counts");
        if (countsDoc != null) {
            ReportCounts c = new ReportCounts(
                    countsDoc.getInteger("total", 0),
                    countsDoc.getInteger("red", 0),
                    countsDoc.getInteger("yellow", 0),
                    countsDoc.getInteger("green", 0),
                    countsDoc.getInteger("black", 0),
                    countsDoc.getInteger("trapped", 0),
                    countsDoc.getInteger("vulnerable", 0)
            );
            r.setCounts(c);
        }

        // Needs
        Document needsDoc = (Document) doc.get("needs");
        ReportNeeds needs = new ReportNeeds();
        if (needsDoc != null) {
            Document rescueDoc = (Document) needsDoc.get("rescue");
            if (rescueDoc != null) {
                needs.setRescue(new ReportNeeds.RescueNeed(rescueDoc.getInteger("people", 0)));
            }
            Document shelterDoc = (Document) needsDoc.get("shelter");
            if (shelterDoc != null) {
                needs.setShelter(new ReportNeeds.ShelterNeed(shelterDoc.getInteger("people", 0)));
            }
            Document medDoc = (Document) needsDoc.get("medicine");
            if (medDoc != null) {
                needs.setMedicine(new ReportNeeds.MedicineNeed(
                        medDoc.getInteger("people", 0),
                        medDoc.getInteger("kitsSuggested", 0),
                        medDoc.getInteger("kitsRequested", 0),
                        medDoc.getString("editReason")
                ));
            }
            Document foodDoc = (Document) needsDoc.get("food");
            if (foodDoc != null) {
                needs.setFood(new ReportNeeds.FoodNeed(
                        foodDoc.getInteger("people", 0),
                        foodDoc.getInteger("days", 0),
                        foodDoc.getInteger("kitsSuggested", 0),
                        foodDoc.getInteger("kitsRequested", 0),
                        foodDoc.getString("editReason")
                ));
            }
        }
        r.setNeeds(needs);

        // Need order
        if (doc.containsKey("needOrder")) {
            r.setNeedOrder(doc.getList("needOrder", String.class));
        }

        // Severity
        Document sevDoc = (Document) doc.get("severity");
        if (sevDoc != null) {
            ReportSeverity s = new ReportSeverity();
            s.setCalculated(sevDoc.getInteger("calculated", 1));
            s.setRuleId(sevDoc.getString("ruleId"));
            s.setExplanation(sevDoc.getString("explanation"));
            s.setOverrideLevel(sevDoc.getInteger("overrideLevel"));
            s.setOverrideReason(sevDoc.getString("overrideReason"));
            s.setOverriddenBy(sevDoc.getString("overriddenBy"));
            String ovAt = sevDoc.getString("overriddenAt");
            if (ovAt != null) {
                try { s.setOverriddenAt(Instant.parse(ovAt)); } catch (Exception ignored) {}
            }
            r.setSeverity(s);
        }

        // Linked request IDs
        if (doc.containsKey("linkedRequestIds")) {
            r.setLinkedRequestIds(doc.getList("linkedRequestIds", String.class));
        }

        return r;
    }
}
