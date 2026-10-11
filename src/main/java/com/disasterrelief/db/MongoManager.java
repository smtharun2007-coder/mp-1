package com.disasterrelief.db;

import com.disasterrelief.config.AppConfig;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

/**
 * Singleton manager for MongoDB Atlas connectivity.
 * Configured with 5-second server selection timeout and idempotent index creation at startup.
 */
public class MongoManager {
    private static volatile MongoManager instance;
    private final MongoClient mongoClient;

    private MongoManager() {
        String uri = AppConfig.getMongoUri();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToClusterSettings(builder ->
                        builder.serverSelectionTimeout(5, TimeUnit.SECONDS))
                .build();
        this.mongoClient = MongoClients.create(settings);

        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "mongo-shutdown-hook"));
    }

    public static MongoManager getInstance() {
        if (instance == null) {
            synchronized (MongoManager.class) {
                if (instance == null) {
                    instance = new MongoManager();
                }
            }
        }
        return instance;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getDatabase() {
        return mongoClient.getDatabase(AppConfig.getDatabaseName());
    }

    public MongoDatabase getDatabase(String dbName) {
        return mongoClient.getDatabase(dbName);
    }

    public ClientSession startSession() {
        return mongoClient.startSession();
    }

    /**
     * Creates required indexes idempotently at application startup:
     * - users.username (unique)
     * - requests (compound: status, severity, population, arrivalNo)
     * - roads (compound: from, to)
     * - stock_ledger (timestamp)
     * - audit_log (timestamp)
     */
    public void initIndexes(MongoDatabase db) {
        try {
            // users.username unique
            db.getCollection("users").createIndex(
                    Indexes.ascending("username"),
                    new IndexOptions().unique(true).name("idx_users_username_unique")
            );

            // requests compound on (status, severity, population, arrivalNo)
            db.getCollection("requests").createIndex(
                    Indexes.compoundIndex(
                            Indexes.ascending("status"),
                            Indexes.descending("severity"),
                            Indexes.descending("affectedPopulation"),
                            Indexes.ascending("arrivalNo")
                    ),
                    new IndexOptions().name("idx_requests_triage")
            );

            // roads on (from, to)
            db.getCollection("roads").createIndex(
                    Indexes.compoundIndex(
                            Indexes.ascending("from"),
                            Indexes.ascending("to")
                    ),
                    new IndexOptions().name("idx_roads_endpoints")
            );

            // stock_ledger on timestamp
            db.getCollection("stock_ledger").createIndex(
                    Indexes.descending("timestamp"),
                    new IndexOptions().name("idx_stock_ledger_ts")
            );

            // audit_log on timestamp
            db.getCollection("audit_log").createIndex(
                    Indexes.descending("timestamp"),
                    new IndexOptions().name("idx_audit_log_ts")
            );
        } catch (Exception e) {
            System.err.println("  [WARN] Index creation encountered warning: " + e.getMessage());
        }
    }

    public synchronized void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
            } catch (Exception ignored) {}
        }
    }
}
