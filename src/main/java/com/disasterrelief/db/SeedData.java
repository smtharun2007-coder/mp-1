package com.disasterrelief.db;

import com.disasterrelief.config.AppConfig;
import com.disasterrelief.config.SeverityConfig;
import com.disasterrelief.model.*;
import com.disasterrelief.repository.*;
import com.disasterrelief.service.SeverityCalculator;
import com.disasterrelief.service.SeverityResult;
import com.disasterrelief.util.PasswordUtil;
import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Initializes and resets the District Aruvi disaster management dataset.
 * Seeds:
 * - 10 Locations LOC01 to LOC10
 * - 2 Depots DEP01, DEP02 (opening stock: 300 food kits, 150 medicine kits)
 * - 3 Rescue Teams TEAM01 to TEAM03 (AVAILABLE)
 * - 2 Shelters SH01, SH02 (available places: 80 and 50)
 * - 20 Undirected Roads RD01 to RD20 (all OPEN)
 * - 20 Relief Requests REQ01 to REQ20 (operator supplied, WAITING)
 */
public class SeedData {

    public static void resetDatabase(MongoDatabase db) {
        UserRepository userRepo = new UserRepository(db);
        NodeRepository nodeRepo = new NodeRepository(db);
        RoadRepository roadRepo = new RoadRepository(db);
        RegionRepository regionRepo = new RegionRepository(db);
        DepotRepository depotRepo = new DepotRepository(db);
        RescueTeamRepository teamRepo = new RescueTeamRepository(db);
        ShelterRepository shelterRepo = new ShelterRepository(db);
        ReportRepository reportRepo = new ReportRepository(db);
        RequestRepository reqRepo = new RequestRepository(db);
        AllocationRepository allocRepo = new AllocationRepository(db);
        StockLedgerRepository ledgerRepo = new StockLedgerRepository(db);
        AuditLogRepository auditRepo = new AuditLogRepository(db);
        AlertRepository alertRepo = new AlertRepository(db);
        CounterRepository counterRepo = new CounterRepository(db);

        // 1. Delete all existing records
        userRepo.deleteAll();
        nodeRepo.deleteAll();
        roadRepo.deleteAll();
        regionRepo.deleteAll();
        depotRepo.deleteAll();
        teamRepo.deleteAll();
        shelterRepo.deleteAll();
        reportRepo.deleteAll();
        reqRepo.deleteAll();
        allocRepo.deleteAll();
        ledgerRepo.deleteAll();
        auditRepo.deleteAll();
        alertRepo.deleteAll();
        counterRepo.resetArrivalCounter(20L); // Next operator request starts at 21

        // 2. Seed Users
        String controlPw = AppConfig.getDemoControlPassword();
        String officerPw = AppConfig.getDemoOfficerPassword();

        String salt1 = PasswordUtil.generateSaltHex();
        String hash1 = PasswordUtil.hashPassword(controlPw, salt1);
        User controlUser = new User("usr_admin", "admin", hash1, salt1, Role.CONTROL_ROOM, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        userRepo.create(controlUser);

        String salt2 = PasswordUtil.generateSaltHex();
        String hash2 = PasswordUtil.hashPassword(officerPw, salt2);
        User officerUser1 = new User("usr_officer1", "officer1", hash2, salt2, Role.FIELD_OFFICER, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        userRepo.create(officerUser1);

        String salt3 = PasswordUtil.generateSaltHex();
        String hash3 = PasswordUtil.hashPassword(officerPw, salt3);
        User officerUser2 = new User("usr_officer2", "officer2", hash3, salt3, Role.FIELD_OFFICER, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        userRepo.create(officerUser2);

        // 3. Seed Nodes (10 locations LOC01 to LOC10)
        List<Node> nodes = List.of(
                new Node(1, "Aruvi Central (LOC01)", Node.NodeType.REGION), // Control Room
                new Node(2, "Northbank (LOC02)", Node.NodeType.REGION),
                new Node(3, "Hillview (LOC03)", Node.NodeType.REGION),
                new Node(4, "Riverside (LOC04)", Node.NodeType.REGION),
                new Node(5, "Greenfield (LOC05)", Node.NodeType.REGION),
                new Node(6, "Eastwood (LOC06)", Node.NodeType.REGION),
                new Node(7, "Lakeview (LOC07)", Node.NodeType.REGION),
                new Node(8, "Pinebridge (LOC08)", Node.NodeType.REGION),
                new Node(9, "Meadowpark (LOC09)", Node.NodeType.REGION),
                new Node(10, "Stonepass (LOC10)", Node.NodeType.REGION)
        );
        nodeRepo.createMany(nodes);

        // 4. Seed Affected Regions
        List<Region> regions = List.of(
                new Region("LOC01", 1, "Aruvi Central", 5000, true),
                new Region("LOC02", 2, "Northbank", 3000, false),
                new Region("LOC03", 3, "Hillview", 2500, false),
                new Region("LOC04", 4, "Riverside", 3500, false),
                new Region("LOC05", 5, "Greenfield", 2000, false),
                new Region("LOC06", 6, "Eastwood", 2800, false),
                new Region("LOC07", 7, "Lakeview", 4000, false),
                new Region("LOC08", 8, "Pinebridge", 3200, false),
                new Region("LOC09", 9, "Meadowpark", 1800, false),
                new Region("LOC10", 10, "Stonepass", 1500, false)
        );
        regionRepo.createMany(regions);

        // 5. Seed Depots (DEP01 at LOC01, DEP02 at LOC02)
        // Opening stock: DEP01: 180 food, 90 medicine; DEP02: 120 food, 60 medicine. Total: 300 food, 150 medicine.
        List<Depot> depots = List.of(
                new Depot("DEP01", "Aruvi Central Depot", "LOC01", 1, 90, 180),
                new Depot("DEP02", "Northbank Regional Depot", "LOC02", 2, 60, 120)
        );
        depotRepo.createMany(depots);

        // Stock ledger opening records
        ledgerRepo.create(new StockLedgerEntry("LEDGER_INIT_1", "DEP01", "SYSTEM", "CONTROL_ROOM", "FOOD", 180, "RESTOCK", "Initial opening stock at Aruvi Central"));
        ledgerRepo.create(new StockLedgerEntry("LEDGER_INIT_2", "DEP01", "SYSTEM", "CONTROL_ROOM", "MEDICINE", 90, "RESTOCK", "Initial opening stock at Aruvi Central"));
        ledgerRepo.create(new StockLedgerEntry("LEDGER_INIT_3", "DEP02", "SYSTEM", "CONTROL_ROOM", "FOOD", 120, "RESTOCK", "Initial opening stock at Northbank"));
        ledgerRepo.create(new StockLedgerEntry("LEDGER_INIT_4", "DEP02", "SYSTEM", "CONTROL_ROOM", "MEDICINE", 60, "RESTOCK", "Initial opening stock at Northbank"));

        // 6. Seed Shelters (SH01 at LOC01, SH02 at LOC02)
        // SH01: cap 100, occupied 20 (avail 80); SH02: cap 60, occupied 10 (avail 50)
        Shelter sh01 = new Shelter("SH01", "Aruvi Relief Centre", "LOC01", 1, 100);
        sh01.hydrate(100, 20);
        Shelter sh02 = new Shelter("SH02", "Northbank Community Shelter", "LOC02", 2, 60);
        sh02.hydrate(60, 10);
        shelterRepo.createMany(List.of(sh01, sh02));

        // 7. Seed Rescue Teams (TEAM01 at LOC01, TEAM02 at LOC02, TEAM03 at LOC08; all AVAILABLE)
        List<RescueTeam> teams = List.of(
                new RescueTeam("TEAM01", "Rapid Response Alpha", "LOC01", 1, RescueTeam.TeamStatus.AVAILABLE),
                new RescueTeam("TEAM02", "Rapid Response Bravo", "LOC02", 2, RescueTeam.TeamStatus.AVAILABLE),
                new RescueTeam("TEAM03", "Rescue Unit Charlie", "LOC08", 8, RescueTeam.TeamStatus.AVAILABLE)
        );
        teamRepo.createMany(teams);

        // 8. Seed 20 Undirected Roads (all open, positive travel times)
        List<Road> roads = List.of(
                new Road("RD01", 1, 2, 10, Road.RoadStatus.OPEN, ""),
                new Road("RD02", 1, 4, 12, Road.RoadStatus.OPEN, ""),
                new Road("RD03", 1, 7, 15, Road.RoadStatus.OPEN, ""),
                new Road("RD04", 2, 3, 8, Road.RoadStatus.OPEN, ""),
                new Road("RD05", 2, 5, 14, Road.RoadStatus.OPEN, ""),
                new Road("RD06", 2, 4, 7, Road.RoadStatus.OPEN, ""),
                new Road("RD07", 3, 6, 11, Road.RoadStatus.OPEN, ""),
                new Road("RD08", 3, 8, 16, Road.RoadStatus.OPEN, ""),
                new Road("RD09", 4, 5, 9, Road.RoadStatus.OPEN, ""),
                new Road("RD10", 4, 7, 8, Road.RoadStatus.OPEN, ""),
                new Road("RD11", 5, 6, 10, Road.RoadStatus.OPEN, ""),
                new Road("RD12", 5, 9, 13, Road.RoadStatus.OPEN, ""),
                new Road("RD13", 6, 8, 9, Road.RoadStatus.OPEN, ""),
                new Road("RD14", 6, 10, 18, Road.RoadStatus.OPEN, ""),
                new Road("RD15", 7, 9, 10, Road.RoadStatus.OPEN, ""),
                new Road("RD16", 7, 8, 14, Road.RoadStatus.OPEN, ""),
                new Road("RD17", 8, 10, 12, Road.RoadStatus.OPEN, ""),
                new Road("RD18", 9, 10, 15, Road.RoadStatus.OPEN, ""),
                new Road("RD19", 2, 7, 18, Road.RoadStatus.OPEN, ""),
                new Road("RD20", 4, 8, 20, Road.RoadStatus.OPEN, "")
        );
        roadRepo.createMany(roads);

        // 9. Seed 20 Operator-Entered Relief Requests (REQ01-REQ20) and linked Reports (REP01-REP20)
        List<Report> reportList = new ArrayList<>();
        List<Request> requestList = new ArrayList<>();

        // Raw table records:
        // ID, Loc, SevName, SevInt, Pop, Arr, Food, Med, Rescue(bool), Shelter
        Object[][] rawRequests = new Object[][]{
                {"REQ01", "LOC02", 2, "High", 3, 180, 1L, 40, 20, true, 25},
                {"REQ02", "LOC04", 4, "Critical", 4, 320, 2L, 60, 35, true, 40},
                {"REQ03", "LOC03", 3, "High", 3, 240, 3L, 35, 18, true, 20},
                {"REQ04", "LOC05", 5, "Medium", 2, 150, 4L, 30, 10, false, 0},
                {"REQ05", "LOC07", 7, "Critical", 4, 400, 5L, 70, 40, true, 45},
                {"REQ06", "LOC06", 6, "Medium", 2, 110, 6L, 25, 12, false, 10},
                {"REQ07", "LOC08", 8, "High", 3, 260, 7L, 45, 20, true, 30},
                {"REQ08", "LOC09", 9, "Low", 1, 70, 8L, 15, 5, false, 0},
                {"REQ09", "LOC10", 10, "High", 3, 290, 9L, 50, 25, true, 35},
                {"REQ10", "LOC02", 2, "Medium", 2, 130, 10L, 20, 8, false, 0},
                {"REQ11", "LOC04", 4, "Critical", 4, 350, 11L, 65, 30, true, 50},
                {"REQ12", "LOC05", 5, "Low", 1, 60, 12L, 12, 4, false, 0},
                {"REQ13", "LOC07", 7, "High", 3, 210, 13L, 35, 15, true, 20},
                {"REQ14", "LOC03", 3, "Medium", 2, 140, 14L, 28, 10, false, 0},
                {"REQ15", "LOC06", 6, "Critical", 4, 300, 15L, 55, 28, true, 30},
                {"REQ16", "LOC08", 8, "Medium", 2, 125, 16L, 22, 9, false, 0},
                {"REQ17", "LOC09", 9, "High", 3, 230, 17L, 38, 18, true, 25},
                {"REQ18", "LOC10", 10, "Low", 1, 80, 18L, 18, 6, false, 0},
                {"REQ19", "LOC02", 2, "High", 3, 240, 19L, 42, 22, true, 20},
                {"REQ20", "LOC04", 4, "Medium", 2, 160, 20L, 30, 12, false, 0}
        };

        for (Object[] raw : rawRequests) {
            String reqId = (String) raw[0];
            String locCode = (String) raw[1];
            int regionId = (int) raw[2];
            String sevName = (String) raw[3];
            int sevLevel = (int) raw[4];
            int pop = (int) raw[5];
            long arrivalNo = (long) raw[6];
            int foodKits = (int) raw[7];
            int medKits = (int) raw[8];
            boolean rescueReq = (boolean) raw[9];
            int shelterPlaces = (int) raw[10];

            String repId = "REP" + reqId.substring(3);
            String officer = (regionId <= 5) ? "officer1" : "officer2";

            // Linked Report for comprehensive UI visibility
            Report rep = new Report(repId, officer, regionId);
            rep.setStatus("APPROVED");
            rep.setNotes("Operator-entered assessment for " + locCode);
            rep.setNeedOrder(rescueReq ? List.of("RESCUE", "SHELTER", "FOOD", "MEDICINE") : List.of("FOOD", "MEDICINE"));

            ReportCounts counts = new ReportCounts(pop, (sevLevel >= 4 ? 4 : 0), (sevLevel == 3 ? 5 : 0), pop - 10, 0, rescueReq ? shelterPlaces : 0, 10);
            rep.setCounts(counts);

            ReportNeeds needs = new ReportNeeds();
            if (rescueReq) needs.setRescue(new ReportNeeds.RescueNeed(shelterPlaces > 0 ? shelterPlaces : pop));
            if (shelterPlaces > 0) needs.setShelter(new ReportNeeds.ShelterNeed(shelterPlaces));
            if (foodKits > 0) needs.setFood(new ReportNeeds.FoodNeed(pop, 5, foodKits, foodKits, "Operator specified"));
            if (medKits > 0) needs.setMedicine(new ReportNeeds.MedicineNeed(pop, medKits, medKits, "Operator specified"));
            rep.setNeeds(needs);

            rep.getSeverity().setCalculated(sevLevel);
            rep.getSeverity().setRuleId("RULE_" + sevName.toUpperCase());
            rep.getSeverity().setExplanation("Operator-supplied severity: " + sevName);
            rep.setLinkedRequestIds(List.of(reqId));
            rep.getHistory().add(new ReportHistoryEntry(1, officer, "INITIAL_SEED", counts, rep.getSeverity(), "Operator request seeded"));
            reportList.add(rep);

            // Create concrete Request (RescueRequest or SupplyRequest extending ReliefRequest)
            ReliefRequest req;
            if (rescueReq) {
                RescueRequest rr = new RescueRequest(reqId, locCode, regionId, sevLevel, pop, arrivalNo,
                        RequestStatus.WAITING, foodKits, medKits, true, shelterPlaces);
                rr.setReportId(repId);
                rr.setCreatedBy("operator");
                req = rr;
            } else {
                SupplyRequest sr = new SupplyRequest(reqId, locCode, regionId, sevLevel, pop, arrivalNo,
                        RequestStatus.WAITING, foodKits, medKits);
                sr.setReportId(repId);
                sr.setCreatedBy("operator");
                if (shelterPlaces > 0) {
                    sr.setShelterPlacesRequested(shelterPlaces);
                }
                req = sr;
            }

            req.setSeverityLevel(sevLevel);
            req.setSeverityRuleId("RULE_" + sevName.toUpperCase());
            req.setSeverityExplanation("Operator-supplied severity: " + sevName);
            req.setNeedOrder(rep.getNeedOrder());

            // Build request items
            if (rescueReq) req.getItems().add(new RequestItem("RESCUE", shelterPlaces > 0 ? shelterPlaces : pop));
            if (shelterPlaces > 0) req.getItems().add(new RequestItem("SHELTER", shelterPlaces));
            if (foodKits > 0) req.getItems().add(new RequestItem("FOOD", foodKits));
            if (medKits > 0) req.getItems().add(new RequestItem("MEDICINE", medKits));

            requestList.add(req);
        }

        reportRepo.createMany(reportList);
        reqRepo.createMany(requestList);

        // Audit log initial system seeding
        auditRepo.log("SYSTEM", "CONTROL_ROOM", "SEED_DATA_RESET",
                "District Aruvi database initialized with 10 locations, 2 depots, 3 rescue teams, 2 shelters, 20 roads, and 20 relief requests.");
    }
}
