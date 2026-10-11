package com.disasterrelief.repository;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;

/**
 * Atomic sequence generator for sequential identifiers.
 * Guarantees monotonic, non-reusable arrival order sequences using findOneAndUpdate with $inc.
 */
public class CounterRepository {
    private final MongoCollection<Document> collection;

    public CounterRepository(MongoDatabase database) {
        this.collection = database.getCollection("counters");
    }

    /**
     * Atomically increments and returns the next arrival number sequence.
     */
    public long getNextArrivalNumber() {
        return getNextArrivalNumber(null);
    }

    public long getNextArrivalNumber(ClientSession session) {
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER);

        Document result;
        if (session != null) {
            result = collection.findOneAndUpdate(session,
                    Filters.eq("_id", "arrival"),
                    Updates.inc("seq", 1L),
                    options);
        } else {
            result = collection.findOneAndUpdate(
                    Filters.eq("_id", "arrival"),
                    Updates.inc("seq", 1L),
                    options);
        }

        if (result != null && result.containsKey("seq")) {
            Number seq = (Number) result.get("seq");
            return seq.longValue();
        }
        return 1L;
    }

    public void resetArrivalCounter(long startValue) {
        collection.updateOne(
                Filters.eq("_id", "arrival"),
                Updates.set("seq", startValue),
                new com.mongodb.client.model.UpdateOptions().upsert(true)
        );
    }
}
