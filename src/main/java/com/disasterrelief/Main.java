package com.disasterrelief;

import com.disasterrelief.api.RestApiServer;
import com.disasterrelief.config.AppConfig;
import com.disasterrelief.db.MongoManager;
import com.disasterrelief.db.SeedData;
import com.mongodb.client.MongoDatabase;

/**
 * Main application launcher for the Disaster Relief Resource Allocation System.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  DISASTER RELIEF RESOURCE ALLOCATION AND PRIORITISATION SYSTEM");
        System.out.println("==========================================================================");

        // 1. Fail-fast configuration validation
        String mongoUri;
        try {
            mongoUri = AppConfig.getMongoUri();
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        int port = AppConfig.getPort();
        String dbName = AppConfig.getDatabaseName();

        System.out.println("  [CONFIG] Database Name : " + dbName);
        System.out.println("  [CONFIG] Server Port   : " + port);

        // 2. Connect to MongoDB Atlas
        MongoManager mongoManager = MongoManager.getInstance();
        MongoDatabase db = mongoManager.getDatabase();

        // 3. Create idempotent indexes
        System.out.println("  [INIT] Ensuring MongoDB collection indexes...");
        mongoManager.initIndexes(db);

        // 4. Seed initial dataset if database is empty
        long userCount = db.getCollection("users").countDocuments();
        if (userCount == 0) {
            System.out.println("  [SEED] Populating initial demo dataset...");
            SeedData.resetDatabase(db);
        }

        // 5. Start Javalin REST API & Static Web Server
        RestApiServer server = new RestApiServer(db, mongoManager.getMongoClient(), port);
        server.start(port);

        // 6. Print login credentials to console
        System.out.println("==========================================================================");
        System.out.println("  DEMO ACCESS CREDENTIALS:");
        System.out.println("  ------------------------------------------------------------------------");
        System.out.println("  Control Room : admin    | Password : " + AppConfig.getDemoControlPassword());
        System.out.println("  Field Officer: officer1 | Password : " + AppConfig.getDemoOfficerPassword());
        System.out.println("  ------------------------------------------------------------------------");
        System.out.println("  Web UI URL   : http://localhost:" + port);
        System.out.println("==========================================================================");
    }
}
