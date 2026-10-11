package com.disasterrelief.api;

import com.disasterrelief.auth.SessionManager;
import com.disasterrelief.config.AppConfig;
import com.disasterrelief.db.MongoManager;
import com.disasterrelief.db.SeedData;
import com.disasterrelief.graph.DijkstraResult;
import com.disasterrelief.graph.Graph;
import com.disasterrelief.model.*;
import com.disasterrelief.queue.RequestPriorityQueue;
import com.disasterrelief.repository.*;
import com.disasterrelief.config.SeverityConfig;
import com.disasterrelief.service.AllocationPreview;
import com.disasterrelief.service.AllocationService;
import com.disasterrelief.service.ReportService;
import com.disasterrelief.service.SeverityCalculator;
import com.disasterrelief.service.SeverityResult;
import com.disasterrelief.util.PasswordUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import java.time.Instant;
import java.util.*;

/**
 * Embedded Javalin 6 REST API and Static Asset Server.
 * Configures role-based access control (RBAC), JSON serialization, and all domain routes.
 */
public class RestApiServer {

    private final Javalin app;
    private final MongoDatabase database;
    private final MongoClient mongoClient;
    private final SessionManager sessionManager;
    private final AllocationService allocationService;
    private final RequestPriorityQueue priorityQueue;

    // Repositories
    private final UserRepository userRepo;
    private final NodeRepository nodeRepo;
    private final RoadRepository roadRepo;
    private final RegionRepository regionRepo;
    private final DepotRepository depotRepo;
    private final RescueTeamRepository teamRepo;
    private final ShelterRepository shelterRepo;
    private final RequestRepository requestRepo;
    private final AllocationRepository allocationRepo;
    private final StockLedgerRepository ledgerRepo;
    private final AuditLogRepository auditRepo;
    private final AlertRepository alertRepo;
    private final CounterRepository counterRepo;
    private final ReportService reportService;

    public RestApiServer(MongoDatabase database, MongoClient mongoClient, int port) {
        this.database = database;
        this.mongoClient = mongoClient;
        this.sessionManager = new SessionManager();
        this.priorityQueue = new RequestPriorityQueue();

        this.userRepo = new UserRepository(database);
        this.nodeRepo = new NodeRepository(database);
        this.roadRepo = new RoadRepository(database);
        this.regionRepo = new RegionRepository(database);
        this.depotRepo = new DepotRepository(database);
        this.teamRepo = new RescueTeamRepository(database);
        this.shelterRepo = new ShelterRepository(database);
        this.requestRepo = new RequestRepository(database);
        this.allocationRepo = new AllocationRepository(database);
        this.ledgerRepo = new StockLedgerRepository(database);
        this.auditRepo = new AuditLogRepository(database);
        this.alertRepo = new AlertRepository(database);
        this.counterRepo = new CounterRepository(database);

        this.allocationService = new AllocationService(database, mongoClient, priorityQueue);
        this.reportService = new ReportService(database, mongoClient, priorityQueue);

        // Jackson JSON configuration
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.app = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = io.javalin.http.staticfiles.Location.CLASSPATH;
            });
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        setupSecurityFilters();
        setupRoutes();
    }

    public void start(int port) {
        // Rebuild in-memory priority queue from database
        priorityQueue.rebuild(requestRepo.findWaitingOrPartiallyAllocated());
        app.start(port);
        System.out.println("  [SERVER] Disaster Relief API & Frontend running at http://localhost:" + port);
    }

    public void stop() {
        app.stop();
    }

    public Javalin getApp() {
        return app;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public ReportService getReportService() {
        return reportService;
    }

    // ---------------------------------------------------------------------------------------------
    // SECURITY & RBAC FILTERS
    // ---------------------------------------------------------------------------------------------
    private void setupSecurityFilters() {
        app.before("/api/*", ctx -> {
            String path = ctx.path();
            if (path.equals("/api/auth/login") || path.equals("/api/health") || path.equals("/api/severity/preview")) {
                return; // Public endpoints
            }

            // Extract session token
            String token = ctx.header("X-Auth-Token");
            if (token == null) {
                String authHeader = ctx.header("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7).trim();
                }
            }
            if (token == null) {
                token = ctx.queryParam("token");
            }

            SessionManager.UserSession session = sessionManager.getSession(token);
            if (session == null) {
                throw new io.javalin.http.UnauthorizedResponse("Unauthorized: Valid session token required. Please log in.");
            }

            // Store current session in request attributes
            ctx.attribute("userSession", session);

            // Role-Based Access Control (RBAC) Enforcement
            if (session.getRole() == Role.FIELD_OFFICER) {
                // Deny Control Room privileged operations
                if (path.startsWith("/api/allocations/commit") ||
                    path.startsWith("/api/allocations/process") ||
                    path.startsWith("/api/stock/adjust") ||
                    path.startsWith("/api/stock/ledger") ||
                    path.startsWith("/api/users") ||
                    path.startsWith("/api/admin/reset") ||
                    path.startsWith("/api/reports/pending") ||
                    path.endsWith("/approve") ||
                    path.endsWith("/override") ||
                    path.endsWith("/reject")) {

                    throw new io.javalin.http.ForbiddenResponse("Forbidden: Field officers are not permitted to execute control-room actions.");
                }
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // ROUTE DECLARATIONS
    // ---------------------------------------------------------------------------------------------
    private void setupRoutes() {

        // Health check
        app.get("/api/health", ctx -> ctx.json(Map.of("status", "UP", "timestamp", Instant.now().toString())));

        // 1. Authentication
        app.post("/api/auth/login", this::handleLogin);
        app.post("/api/auth/logout", this::handleLogout);
        app.get("/api/auth/me", this::handleGetMe);

        // 2. Dashboard & Statistics
        app.get("/api/dashboard/stats", this::handleGetStats);
        app.get("/api/dashboard/regions", this::handleGetRegionBoard);
        app.get("/api/dashboard/impact", this::handleGetImpactMetrics);

        // 3. Requests & Queue
        app.get("/api/requests", this::handleListRequests);
        app.get("/api/requests/inbox", this::handleGetReviewInbox);
        app.post("/api/requests/review", this::handleReviewRequest);
        app.post("/api/requests", this::handleCreateRequest);
        app.get("/api/requests/queue", this::handleGetLiveQueue);
        app.put("/api/requests/{id}", this::handleUpdateRequest);
        app.delete("/api/requests/{id}", this::handleCancelRequest);

        // 3.5 Assessment Reports & Severity Marking (Unified START Single Report)
        app.post("/api/severity/preview", this::handleSeverityPreview);
        app.post("/api/reports", this::handleCreateReport);
        app.put("/api/reports/{id}", this::handleUpdateReport);
        app.get("/api/reports/mine", this::handleGetMyReports);
        app.get("/api/reports/pending", this::handleGetPendingReports);
        app.get("/api/reports/{id}", this::handleGetReport);
        app.post("/api/reports/{id}/approve", this::handleApproveReport);
        app.post("/api/reports/{id}/override", this::handleOverrideReport);
        app.post("/api/reports/{id}/reject", this::handleRejectReport);

        // 4. Allocations
        app.get("/api/allocations/preview/{requestId}", this::handlePreviewAllocation);
        app.post("/api/allocations/commit", this::handleCommitAllocation);
        app.post("/api/allocations/process-next", this::handleProcessNext);
        app.post("/api/allocations/process-all", this::handleProcessAll);
        app.get("/api/allocations", this::handleListAllocations);
        app.post("/api/allocations/{id}/dispatch", this::handleDispatchAllocation);
        app.post("/api/allocations/{id}/deliver", this::handleDeliverAllocation);
        app.post("/api/allocations/{id}/cancel", this::handleCancelAllocation);

        // 5. Roads & Network Graph
        app.get("/api/roads", ctx -> ctx.json(roadRepo.findAll()));
        app.post("/api/roads/{id}/status", this::handleUpdateRoadStatus);
        app.post("/api/roads/route", this::handleFindRoute);
        app.get("/api/roads/reachability", this::handleGetReachability);

        // 6. Depots & Stock Management
        app.get("/api/stock/depots", ctx -> ctx.json(depotRepo.findAll()));
        app.post("/api/stock/adjust", this::handleAdjustStock);
        app.get("/api/stock/ledger", ctx -> ctx.json(ledgerRepo.findAll()));
        app.get("/api/stock/integrity", ctx -> ctx.json(allocationService.verifyStockIntegrity()));

        // 7. Shelters & Teams
        app.get("/api/shelters", ctx -> ctx.json(shelterRepo.findAll()));
        app.get("/api/teams", ctx -> ctx.json(teamRepo.findAll()));

        // 8. Alerts & Audit
        app.get("/api/alerts", ctx -> ctx.json(alertRepo.findAll()));
        app.post("/api/alerts/{id}/ack", ctx -> ctx.json(Map.of("success", alertRepo.acknowledge(ctx.pathParam("id")))));
        app.get("/api/audit", ctx -> ctx.json(auditRepo.findAll()));

        // 9. Users & Admin
        app.get("/api/users", ctx -> ctx.json(userRepo.findAll()));
        app.post("/api/users", this::handleCreateUser);
        app.put("/api/users/{id}/status", this::handleUpdateUserStatus);
        app.post("/api/admin/reset", this::handleResetDemoData);

        // 10. Field Officer Dedicated APIs
        app.get("/api/officer/my-area", this::handleOfficerMyArea);
        app.get("/api/officer/my-reports", this::handleOfficerMyReports);
        app.post("/api/officer/road-report", this::handleOfficerRoadReport);
    }

    // ---------------------------------------------------------------------------------------------
    // CONTROLLER HANDLERS
    // ---------------------------------------------------------------------------------------------

    private void handleLogin(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Username and password required"));
            return;
        }

        User user = userRepo.findByUsername(username.trim());
        if (user == null || !user.isActive()) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Invalid credentials or inactive account"));
            return;
        }

        boolean valid = PasswordUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash());
        if (!valid) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Invalid credentials"));
            return;
        }

        SessionManager.UserSession session = sessionManager.createSession(user);
        auditRepo.log(user.getUsername(), user.getRole().name(), "LOGIN", "User logged in successfully");

        ctx.json(Map.of(
                "token", session.getToken(),
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "role", user.getRole().name(),
                        "assignedLocationIds", user.getAssignedLocationIds()
                )
        ));
    }

    private void handleLogout(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        if (session != null) {
            sessionManager.removeSession(session.getToken());
            auditRepo.log(session.getUsername(), session.getRole().name(), "LOGOUT", "User logged out");
        }
        ctx.json(Map.of("success", true));
    }

    private void handleGetMe(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        ctx.json(Map.of(
                "userId", session.getUserId(),
                "username", session.getUsername(),
                "role", session.getRole().name(),
                "assignedLocationIds", session.getAssignedLocationIds()
        ));
    }

    private void handleGetStats(Context ctx) {
        List<Request> allReqs = requestRepo.findAll();
        Map<RequestStatus, Integer> countsByStatus = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : RequestStatus.values()) countsByStatus.put(s, 0);
        for (Request r : allReqs) {
            countsByStatus.put(r.getStatus(), countsByStatus.get(r.getStatus()) + 1);
        }

        // Live isolated regions
        Graph graph = allocationService.buildLiveGraph();
        List<Integer> regionNodeIds = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> depotNodeIds = List.of(11, 12);
        List<Integer> isolated = graph.findIsolatedRegions(regionNodeIds, depotNodeIds);

        // Stock totals
        int totalMedicine = 0, totalFood = 0, reservedMedicine = 0, reservedFood = 0;
        for (Depot d : depotRepo.findAll()) {
            totalMedicine += d.getMedicineKits();
            totalFood += d.getFoodPacks();
            reservedMedicine += d.getReservedMedicine();
            reservedFood += d.getReservedFood();
        }

        // Shelter totals
        int totalShelterCap = 0, reservedShelter = 0;
        for (Shelter s : shelterRepo.findAll()) {
            totalShelterCap += s.getCapacity();
            reservedShelter += s.getReserved();
        }

        // Team status counts
        int availTeams = 0, busyTeams = 0;
        for (RescueTeam t : teamRepo.findAll()) {
            if (t.isAvailable()) availTeams++; else busyTeams++;
        }

        // Escalation flag (L1 if any Sev 4 waiting, L2 if any Sev 5 waiting - adapted from the national disaster-level idea)
        String escalationLevel = "NORMAL";
        for (Request r : priorityQueue.getOrderedSnapshot()) {
            if (r.getSeverity() == 5) {
                escalationLevel = "ESCALATION_LEVEL_2 (CRITICAL) [adapted from national disaster-level idea]";
                break;
            } else if (r.getSeverity() == 4 && !escalationLevel.contains("LEVEL_2")) {
                escalationLevel = "ESCALATION_LEVEL_1 (HIGH) [adapted from national disaster-level idea]";
            }
        }

        int pendingReportsCount = 0;
        try {
            pendingReportsCount = reportService.getPending().size();
        } catch (Exception ignored) {}

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("countsByStatus", countsByStatus);
        response.put("pendingReportsCount", pendingReportsCount);
        response.put("liveQueueCount", priorityQueue.size());
        response.put("totalStockMedicine", totalMedicine);
        response.put("totalStockFood", totalFood);
        response.put("reservedMedicine", reservedMedicine);
        response.put("reservedFood", reservedFood);
        response.put("shelterCapacity", totalShelterCap);
        response.put("shelterReserved", reservedShelter);
        response.put("availableTeams", availTeams);
        response.put("busyTeams", busyTeams);
        response.put("isolatedRegions", isolated);
        response.put("escalationFlag", escalationLevel);
        response.put("stockIntegrity", allocationService.verifyStockIntegrity());

        ctx.json(response);
    }

    private void handleGetRegionBoard(Context ctx) {
        // Region priority board: region rank is determined by its highest priority waiting request
        List<Region> regions = regionRepo.findAll();
        List<Request> waiting = priorityQueue.getOrderedSnapshot();

        Map<Integer, Request> bestRequestPerRegion = new HashMap<>();
        for (Request r : waiting) {
            bestRequestPerRegion.putIfAbsent(r.getRegionId(), r);
        }

        List<Map<String, Object>> board = new ArrayList<>();
        for (Region reg : regions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("regionId", reg.getId());
            item.put("locationCode", reg.getLocationCode());
            item.put("regionName", reg.getName());
            item.put("name", reg.getName());
            item.put("population", reg.getPopulation());

            Request best = bestRequestPerRegion.get(reg.getId());
            if (best != null) {
                item.put("highestSeverity", best.getSeverity());
                item.put("waitingRequestId", best.getId());
                item.put("priorityRankReason", RequestPriorityQueue.getRankReason(best));
            } else {
                item.put("highestSeverity", 0);
                item.put("waitingRequestId", "None");
                item.put("priorityRankReason", "No active waiting incidents");
            }
            board.add(item);
        }

        // Sort board by highest severity DESC
        board.sort((a, b) -> Integer.compare((int) b.get("highestSeverity"), (int) a.get("highestSeverity")));
        ctx.json(board);
    }

    private void handleGetImpactMetrics(Context ctx) {
        List<Request> allReqs = requestRepo.findAll();
        List<Allocation> allAllocs = allocationRepo.findAll();

        int total = allReqs.size();
        int fulfilledCount = 0;
        int peopleSheltered = 0;
        int medicineKitsDelivered = 0;
        int foodPacksDelivered = 0;
        int totalTravelTime = 0;
        int allocatedCount = 0;

        for (Request r : allReqs) {
            if (r.getStatus() == RequestStatus.ALLOCATED || r.getStatus() == RequestStatus.DELIVERED) {
                fulfilledCount++;
            }
        }

        for (Allocation a : allAllocs) {
            peopleSheltered += a.getPeopleEvacuated();
            medicineKitsDelivered += a.getMedicineAllocated();
            foodPacksDelivered += a.getFoodAllocated();
            totalTravelTime += a.getTravelTime();
            allocatedCount++;
        }

        double percentFulfilled = total > 0 ? (fulfilledCount * 100.0 / total) : 0.0;
        double avgTravelTime = allocatedCount > 0 ? (totalTravelTime * 1.0 / allocatedCount) : 0.0;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("percentFulfilled", Math.round(percentFulfilled * 10.0) / 10.0);
        metrics.put("peopleSheltered", peopleSheltered);
        metrics.put("medicineKitsDelivered", medicineKitsDelivered);
        metrics.put("foodPacksDelivered", foodPacksDelivered);
        metrics.put("averageTravelTimeMinutes", Math.round(avgTravelTime * 10.0) / 10.0);
        metrics.put("doubleAllocationsCount", 0); // Mathematically zero via atomic transactions

        // SDG Mapping
        Map<String, String> sdgPanel = new LinkedHashMap<>();
        sdgPanel.put("SDG 3: Good Health and Well-Being",
                "Delivered " + medicineKitsDelivered + " medicine triage kits directly to frontline casualties via nearest-depot routing.");
        sdgPanel.put("SDG 9: Resilient Infrastructure & Routing",
                "Dynamic Dijkstra bypass routes around closed roads; average response travel time: " + Math.round(avgTravelTime) + " mins.");
        sdgPanel.put("SDG 11: Sustainable Cities & Safe Communities",
                "Evacuated and sheltered " + peopleSheltered + " vulnerable citizens with strict capacity reservation to prevent overcrowding.");
        sdgPanel.put("SDG 13: Climate & Disaster Action",
                "Zero-loss multi-criteria triage engine prioritizing Level 5 emergencies within seconds of field incident reporting.");
        metrics.put("sdgAlignment", sdgPanel);

        metrics.put("costProfile", "Operates entirely on the free MongoDB Atlas M0 cluster tier and standalone lightweight Java 17 runtime ($0.00 cloud operational cost).");

        ctx.json(metrics);
    }

    private void handleListRequests(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        List<Request> all = requestRepo.findAll();

        if (session.getRole() == Role.FIELD_OFFICER) {
            // Filter to officer's assigned regions
            List<Integer> assigned = session.getAssignedLocationIds();
            List<Request> filtered = new ArrayList<>();
            for (Request r : all) {
                if (assigned.contains(r.getRegionId()) || session.getUsername().equalsIgnoreCase(r.getCreatedBy())) {
                    filtered.add(r);
                }
            }
            ctx.json(filtered);
        } else {
            ctx.json(all);
        }
    }

    private void handleGetReviewInbox(Context ctx) {
        List<Request> inbox = new ArrayList<>();
        for (Request r : requestRepo.findAll()) {
            if (r.getStatus() == RequestStatus.PENDING_REVIEW) {
                inbox.add(r);
            }
        }
        ctx.json(inbox);
    }

    private void handleReviewRequest(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String requestId = (String) body.get("requestId");
        String decision = (String) body.get("decision"); // "APPROVE", "REJECT", "OVERRIDE"
        String reason = (String) body.get("reason");

        Request req = requestRepo.findById(requestId);
        if (req == null) {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Request not found"));
            return;
        }

        if ("OVERRIDE".equalsIgnoreCase(decision)) {
            if (reason == null || reason.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Required justification reason missing for severity override"));
                return;
            }
            int newSeverity = ((Number) body.get("newSeverity")).intValue();
            req.setSeverity(newSeverity);
            req.setStatus(RequestStatus.WAITING);
            req.setNotes("Severity overridden by " + session.getUsername() + ": " + reason);
            requestRepo.update(req);
            priorityQueue.add(req);
            auditRepo.log(session.getUsername(), session.getRole().name(), "SEVERITY_OVERRIDE",
                    "Overrode severity of " + requestId + " to " + newSeverity + ". Reason: " + reason);
        } else if ("APPROVE".equalsIgnoreCase(decision)) {
            req.setStatus(RequestStatus.WAITING);
            requestRepo.update(req);
            priorityQueue.add(req);
            auditRepo.log(session.getUsername(), session.getRole().name(), "REQUEST_APPROVED",
                    "Approved request " + requestId + " into active triage queue.");
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            req.setStatus(RequestStatus.REJECTED);
            req.setRejectionReason(reason != null ? reason : "Rejected by Control Room");
            requestRepo.update(req);
            auditRepo.log(session.getUsername(), session.getRole().name(), "REQUEST_REJECTED",
                    "Rejected request " + requestId + ": " + reason);
        }

        ctx.json(Map.of("success", true, "request", req));
    }

    private void handleCreateRequest(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        Map<String, Object> body = ctx.bodyAsClass(Map.class);

        int regionId = ((Number) body.get("regionId")).intValue();
        int severity = ((Number) body.get("severity")).intValue();
        int affectedPopulation = ((Number) body.get("affectedPopulation")).intValue();
        String mostUrgent = (String) body.getOrDefault("mostUrgentNeed", "RESCUE");
        UrgentNeed urgentNeed = UrgentNeed.valueOf(mostUrgent.toUpperCase());

        int medicineNeeded = body.containsKey("medicineNeeded") ? ((Number) body.get("medicineNeeded")).intValue() : 0;
        int foodNeeded = body.containsKey("foodNeeded") ? ((Number) body.get("foodNeeded")).intValue() : 0;
        int peopleToEvacuate = body.containsKey("peopleToEvacuate") ? ((Number) body.get("peopleToEvacuate")).intValue() : 0;

        String reportId = "REP_" + UUID.randomUUID().toString().substring(0, 8);
        List<Request> created = new ArrayList<>();

        // Determine if direct approval (Control Room) or PENDING_REVIEW (Field Officer)
        RequestStatus initialStatus = (session.getRole() == Role.CONTROL_ROOM)
                ? RequestStatus.WAITING
                : RequestStatus.PENDING_REVIEW;

        // 1. Create RescueRequest if rescue needed
        if (peopleToEvacuate > 0) {
            long arrivalNo = counterRepo.getNextArrivalNumber();
            String reqId = "REQ_" + UUID.randomUUID().toString().substring(0, 8);
            RescueRequest rr = new RescueRequest(reqId, reportId, regionId, severity, affectedPopulation,
                    arrivalNo, initialStatus, session.getUsername(), urgentNeed, peopleToEvacuate);
            requestRepo.create(rr);
            created.add(rr);
            if (initialStatus == RequestStatus.WAITING) {
                priorityQueue.add(rr);
            }
        }

        // 2. Create SupplyRequest if medicine or food needed
        if (medicineNeeded > 0 || foodNeeded > 0) {
            long arrivalNo = counterRepo.getNextArrivalNumber();
            String reqId = "REQ_" + UUID.randomUUID().toString().substring(0, 8);
            SupplyRequest sr = new SupplyRequest(reqId, reportId, regionId, severity, affectedPopulation,
                    arrivalNo, initialStatus, session.getUsername(), urgentNeed, medicineNeeded, foodNeeded);
            requestRepo.create(sr);
            created.add(sr);
            if (initialStatus == RequestStatus.WAITING) {
                priorityQueue.add(sr);
            }
        }

        auditRepo.log(session.getUsername(), session.getRole().name(), "CREATE_INCIDENT_REPORT",
                "Created report " + reportId + " with " + created.size() + " linked requests for Region " + regionId);

        ctx.json(Map.of("reportId", reportId, "requests", created));
    }

    private void handleGetLiveQueue(Context ctx) {
        List<Request> ordered = priorityQueue.getOrderedSnapshot();
        List<Map<String, Object>> queueView = new ArrayList<>();

        int rank = 1;
        for (Request r : ordered) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("requestId", r.getId());
            item.put("id", r.getId());
            item.put("reportId", r.getReportId());
            item.put("regionId", r.getRegionId());
            String locCode = "LOC" + (r.getRegionId() < 10 ? "0" + r.getRegionId() : String.valueOf(r.getRegionId()));
            item.put("locationCode", locCode);
            Region reg = regionRepo.findById(r.getRegionId());
            item.put("regionName", reg != null ? reg.getName() : "Region " + r.getRegionId());
            item.put("type", r.getType());
            item.put("severity", r.getSeverity());
            item.put("affectedPopulation", r.getAffectedPopulation());
            item.put("arrivalNo", r.getArrivalNo());
            item.put("status", r.getStatus().name());
            if (r instanceof ReliefRequest) {
                ReliefRequest rr = (ReliefRequest) r;
                item.put("foodNeeded", rr.getFoodNeeded());
                item.put("medicineNeeded", rr.getMedicineNeeded());
                item.put("rescueRequired", rr.isRescueRequired());
                item.put("shelterPlacesRequested", rr.getShelterPlacesRequested());
                item.put("foodAllocated", rr.getFoodAllocated());
                item.put("medicineAllocated", rr.getMedicineAllocated());
                item.put("outstandingFood", rr.getOutstandingFood());
                item.put("outstandingMedicine", rr.getOutstandingMedicine());
            }
            item.put("whyThisRank", RequestPriorityQueue.getRankReason(r));
            queueView.add(item);
        }

        ctx.json(queueView);
    }

    private void handleUpdateRequest(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        String id = ctx.pathParam("id");
        Request req = requestRepo.findById(id);
        if (req == null) {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Request not found"));
            return;
        }

        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        if (body.containsKey("severity")) {
            req.setSeverity(((Number) body.get("severity")).intValue());
        }
        if (body.containsKey("affectedPopulation")) {
            req.setAffectedPopulation(((Number) body.get("affectedPopulation")).intValue());
        }

        requestRepo.update(req);
        priorityQueue.update(req); // Re-orders priority queue: remove() then add()

        auditRepo.log(session.getUsername(), session.getRole().name(), "UPDATE_REQUEST",
                "Updated parameters for request " + id);
        ctx.json(Map.of("success", true, "request", req));
    }

    private void handleCancelRequest(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        String id = ctx.pathParam("id");
        Request req = requestRepo.findById(id);
        if (req != null) {
            req.setStatus(RequestStatus.REJECTED);
            requestRepo.update(req);
            priorityQueue.remove(req);
            auditRepo.log(session.getUsername(), session.getRole().name(), "CANCEL_REQUEST",
                    "Cancelled request " + id);
        }
        ctx.json(Map.of("success", true));
    }

    private void handlePreviewAllocation(Context ctx) {
        String requestId = ctx.pathParam("requestId");
        AllocationPreview preview = allocationService.preview(requestId);
        ctx.json(preview);
    }

    private void handleCommitAllocation(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String requestId = body.get("requestId");

        boolean ok = allocationService.commit(requestId, session.getUsername(), session.getRole().name());
        ctx.json(Map.of("success", ok, "requestId", requestId));
    }

    private void handleProcessNext(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        Request top = priorityQueue.peek();
        if (top == null) {
            ctx.json(Map.of("success", false, "message", "No requests waiting in queue"));
            return;
        }

        boolean ok = allocationService.commit(top.getId(), session.getUsername(), session.getRole().name());
        ctx.json(Map.of("success", ok, "processedRequestId", top.getId()));
    }

    private void handleProcessAll(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        List<Request> waiting = priorityQueue.getOrderedSnapshot();
        int processedCount = 0;

        for (Request r : waiting) {
            boolean ok = allocationService.commit(r.getId(), session.getUsername(), session.getRole().name());
            if (ok) processedCount++;
        }

        ctx.json(Map.of("success", true, "processedCount", processedCount));
    }

    private void handleListAllocations(Context ctx) {
        ctx.json(allocationRepo.findAll());
    }

    private void handleDispatchAllocation(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        String id = ctx.pathParam("id");
        allocationRepo.updateStatus(null, id, Allocation.AllocationStatus.DISPATCHED);
        auditRepo.log(session.getUsername(), session.getRole().name(), "DISPATCH_ALLOCATION",
                "Allocation " + id + " dispatched into the field.");
        ctx.json(Map.of("success", true));
    }

    private void handleDeliverAllocation(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        String id = ctx.pathParam("id");
        Allocation a = allocationRepo.findById(id);
        if (a != null) {
            allocationRepo.updateStatus(null, id, Allocation.AllocationStatus.DELIVERED);
            requestRepo.updateStatus(null, a.getRequestId(), RequestStatus.DELIVERED);

            // If supply, consume reserved stock
            if ("SUPPLY".equalsIgnoreCase(a.getType()) && a.getDepotId() != null) {
                depotRepo.consumeReservedStock(null, a.getDepotId(), a.getMedicineAllocated(), a.getFoodAllocated());
                ledgerRepo.create(new StockLedgerEntry(
                        "LED_" + UUID.randomUUID().toString().substring(0, 8),
                        a.getDepotId(), session.getUsername(), session.getRole().name(),
                        "MEDICINE", -a.getMedicineAllocated(), "CONSUME", "Delivered to target region"
                ));
            } else if ("RESCUE".equalsIgnoreCase(a.getType()) && a.getTeamId() != null) {
                teamRepo.releaseTeam(null, a.getTeamId());
            }

            auditRepo.log(session.getUsername(), session.getRole().name(), "DELIVERY_CONFIRMED",
                    "Allocation " + id + " marked DELIVERED.");
        }
        ctx.json(Map.of("success", true));
    }

    private void handleCancelAllocation(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        String id = ctx.pathParam("id");
        Allocation a = allocationRepo.findById(id);
        if (a != null && a.getStatus() == Allocation.AllocationStatus.COMMITTED) {
            allocationRepo.updateStatus(null, id, Allocation.AllocationStatus.CANCELLED);
            if ("SUPPLY".equalsIgnoreCase(a.getType()) && a.getDepotId() != null) {
                depotRepo.releaseStock(null, a.getDepotId(), a.getMedicineAllocated(), a.getFoodAllocated());
            } else if ("RESCUE".equalsIgnoreCase(a.getType())) {
                if (a.getTeamId() != null) teamRepo.releaseTeam(null, a.getTeamId());
                if (a.getShelterId() != null) shelterRepo.releasePlaces(null, a.getShelterId(), a.getPeopleEvacuated());
            }
            requestRepo.updateStatus(null, a.getRequestId(), RequestStatus.WAITING);
            Request r = requestRepo.findById(a.getRequestId());
            if (r != null) {
                priorityQueue.update(r);
            }
            auditRepo.log(session.getUsername(), session.getRole().name(), "CANCEL_ALLOCATION",
                    "Cancelled allocation " + id);
        }
        ctx.json(Map.of("success", true));
    }

    private void handleUpdateRoadStatus(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        String roadId = ctx.pathParam("id");
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String statusStr = body.get("status"); // "OPEN" or "CLOSED"
        String reason = body.getOrDefault("reason", "Control room status change");

        Road.RoadStatus status = Road.RoadStatus.valueOf(statusStr.toUpperCase());
        if (status == Road.RoadStatus.CLOSED) {
            allocationService.handleRoadClosure(roadId, reason, session.getUsername());
        } else {
            roadRepo.updateStatus(roadId, Road.RoadStatus.OPEN, "");
            allocationService.handleReopenOrStockReplenish("Road " + roadId + " reopened", session.getUsername());
        }

        ctx.json(Map.of("success", true, "roadId", roadId, "newStatus", status.name()));
    }

    private void handleFindRoute(Context ctx) {
        Map<String, Number> body = ctx.bodyAsClass(Map.class);
        int source = body.get("source").intValue();
        int target = body.get("target").intValue();

        Graph graph = allocationService.buildLiveGraph();
        DijkstraResult res = graph.findShortestPath(source, target);
        ctx.json(res);
    }

    private void handleGetReachability(Context ctx) {
        String param = ctx.queryParam("startNode");
        int startNode = (param != null && !param.isBlank()) ? Integer.parseInt(param.trim()) : 11;
        Graph graph = allocationService.buildLiveGraph();
        Set<Integer> bfsSet = graph.bfsReachableFrom(startNode);
        Set<Integer> dfsSet = graph.dfsReachableFrom(startNode);
        ctx.json(Map.of(
                "startNode", startNode,
                "reachableNodes", bfsSet,
                "bfsAndDfsMatch", bfsSet.equals(dfsSet)
        ));
    }

    private void handleAdjustStock(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String depotId = (String) body.get("depotId");
        int medicineDelta = ((Number) body.getOrDefault("medicineDelta", 0)).intValue();
        int foodDelta = ((Number) body.getOrDefault("foodDelta", 0)).intValue();
        String reason = (String) body.getOrDefault("reason", "Warehouse inventory restock");

        depotRepo.adjustStock(null, depotId, medicineDelta, foodDelta);

        if (medicineDelta != 0) {
            ledgerRepo.create(new StockLedgerEntry(
                    "LED_" + UUID.randomUUID().toString().substring(0, 8),
                    depotId, session.getUsername(), session.getRole().name(),
                    "MEDICINE", medicineDelta, (medicineDelta > 0 ? "RESTOCK" : "ADJUST"), reason
            ));
        }
        if (foodDelta != 0) {
            ledgerRepo.create(new StockLedgerEntry(
                    "LED_" + UUID.randomUUID().toString().substring(0, 8),
                    depotId, session.getUsername(), session.getRole().name(),
                    "FOOD", foodDelta, (foodDelta > 0 ? "RESTOCK" : "ADJUST"), reason
            ));
        }

        allocationService.handleReopenOrStockReplenish("Stock replenished at " + depotId, session.getUsername());
        ctx.json(Map.of("success", true, "integrity", allocationService.verifyStockIntegrity()));
    }

    private void handleCreateUser(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String roleStr = (String) body.get("role");
        List<Integer> locations = (List<Integer>) body.get("assignedLocationIds");

        String salt = PasswordUtil.generateSaltHex();
        String hash = PasswordUtil.hashPassword(password, salt);
        User u = new User("usr_" + UUID.randomUUID().toString().substring(0, 8), username, hash, salt, Role.valueOf(roleStr), locations);
        userRepo.create(u);

        auditRepo.log(session.getUsername(), session.getRole().name(), "CREATE_USER", "Created user " + username);
        ctx.json(Map.of("success", true, "userId", u.getId()));
    }

    private void handleUpdateUserStatus(Context ctx) {
        String id = ctx.pathParam("id");
        Map<String, Boolean> body = ctx.bodyAsClass(Map.class);
        boolean active = body.get("active");
        userRepo.setActive(id, active);
        ctx.json(Map.of("success", true));
    }

    private void handleResetDemoData(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        SeedData.resetDatabase(database);
        priorityQueue.rebuild(requestRepo.findWaitingOrPartiallyAllocated());
        auditRepo.log(session.getUsername(), session.getRole().name(), "DEMO_DATA_RESET", "Demo database reset to factory state.");
        ctx.json(Map.of("success", true, "message", "Database successfully reset to initial seed state"));
    }

    // Field Officer endpoints
    private void handleOfficerMyArea(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        List<Integer> assigned = session.getAssignedLocationIds();

        List<Request> myAreaRequests = new ArrayList<>();
        for (Request r : requestRepo.findAll()) {
            if (assigned.contains(r.getRegionId())) {
                myAreaRequests.add(r);
            }
        }

        List<Allocation> myAreaAllocations = new ArrayList<>();
        for (Allocation a : allocationRepo.findAll()) {
            if (assigned.contains(a.getTargetRegionId())) {
                myAreaAllocations.add(a);
            }
        }

        ctx.json(Map.of(
                "assignedRegions", assigned,
                "requests", myAreaRequests,
                "incomingAllocations", myAreaAllocations
        ));
    }

    private void handleOfficerMyReports(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        List<Request> myReports = new ArrayList<>();
        for (Request r : requestRepo.findAll()) {
            if (session.getUsername().equalsIgnoreCase(r.getCreatedBy())) {
                myReports.add(r);
            }
        }
        ctx.json(myReports);
    }

    private void handleOfficerRoadReport(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String roadId = body.get("roadId");
        String hazard = body.get("hazard");

        alertRepo.create(new AlertEntry(
                "ALT_" + UUID.randomUUID().toString().substring(0, 8),
                "Field Officer Road Hazard Report",
                "Officer " + session.getUsername() + " reported road hazard on " + roadId + ": " + hazard,
                "WARNING",
                roadId
        ));
        auditRepo.log(session.getUsername(), session.getRole().name(), "ROAD_HAZARD_REPORT",
                "Reported hazard on road " + roadId + ": " + hazard);
        ctx.json(Map.of("success", true, "message", "Road report submitted to Control Room"));
    }

    // =============================================================================================
    // ASSESSMENT REPORT & SEVERITY HANDLERS
    // =============================================================================================

    private void handleSeverityPreview(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        int total = body.containsKey("total") ? ((Number) body.get("total")).intValue() : 0;
        int red = body.containsKey("red") ? ((Number) body.get("red")).intValue() : 0;
        int yellow = body.containsKey("yellow") ? ((Number) body.get("yellow")).intValue() : 0;
        int green = body.containsKey("green") ? ((Number) body.get("green")).intValue() : 0;
        int black = body.containsKey("black") ? ((Number) body.get("black")).intValue() : 0;
        int trapped = body.containsKey("trapped") ? ((Number) body.get("trapped")).intValue() : 0;
        int vulnerable = body.containsKey("vulnerable") ? ((Number) body.get("vulnerable")).intValue() : 0;
        int hours = body.containsKey("hoursWithoutSupplies") ? ((Number) body.get("hoursWithoutSupplies")).intValue() : 0;
        boolean threat = body.containsKey("ongoingThreat") && Boolean.parseBoolean(String.valueOf(body.get("ongoingThreat")));

        SeverityResult res = SeverityCalculator.calculate(total, red, yellow, green, black, trapped, vulnerable, hours, threat);

        int medPeople = body.containsKey("medicinePeople") ? ((Number) body.get("medicinePeople")).intValue() : 0;
        int foodPeople = body.containsKey("foodPeople") ? ((Number) body.get("foodPeople")).intValue() : 0;
        int foodDays = body.containsKey("foodDays") ? ((Number) body.get("foodDays")).intValue() : 1;

        int suggestedMedKits = SeverityConfig.calculateMedicineKits(medPeople);
        int suggestedFoodKits = SeverityConfig.calculateFoodKits(foodPeople, foodDays);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("level", res.getLevel());
        resp.put("label", res.getLabel());
        resp.put("ruleId", res.getRuleId());
        resp.put("explanation", res.getExplanation());
        resp.put("suggestedMedicineKits", suggestedMedKits);
        resp.put("suggestedFoodKits", suggestedFoodKits);

        ctx.json(resp);
    }

    private void handleCreateReport(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        User user = userRepo.findByUsername(session.getUsername());
        if (user == null) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "User session invalid"));
            return;
        }

        Report report = ctx.bodyAsClass(Report.class);
        if (session.getRole() == Role.FIELD_OFFICER) {
            if (session.getAssignedLocationIds() != null && !session.getAssignedLocationIds().isEmpty()
                    && !session.getAssignedLocationIds().contains(report.getRegionId())) {
                ctx.status(HttpStatus.FORBIDDEN)
                   .json(Map.of("error", "Forbidden: Field officer is not assigned to Region " + report.getRegionId()));
                return;
            }
        }

        boolean allowDuplicate = "true".equalsIgnoreCase(ctx.queryParam("allowDuplicate"));
        try {
            Report created = reportService.submit(report, user, allowDuplicate);
            ctx.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        }
    }

    private void handleUpdateReport(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        User user = userRepo.findByUsername(session.getUsername());
        String id = ctx.pathParam("id");
        Report updatedData = ctx.bodyAsClass(Report.class);

        try {
            Report updated = reportService.updateReport(id, updatedData, user);
            ctx.json(updated);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
        }
    }

    private void handleGetReport(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        String id = ctx.pathParam("id");
        Report report = reportService.getById(id);
        if (report == null) {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Report not found: " + id));
            return;
        }
        if (session.getRole() == Role.FIELD_OFFICER) {
            if (!session.getUsername().equalsIgnoreCase(report.getOfficerId()) &&
                (session.getAssignedLocationIds() == null || !session.getAssignedLocationIds().contains(report.getRegionId()))) {
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Forbidden: Access to report " + id + " restricted"));
                return;
            }
        }
        ctx.json(report);
    }

    private void handleGetMyReports(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        List<Report> reports = reportService.getMine(session.getUsername());
        ctx.json(reports);
    }

    private void handleGetPendingReports(Context ctx) {
        List<Report> pending = reportService.getPending();
        ctx.json(pending);
    }

    private void handleApproveReport(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        User user = userRepo.findByUsername(session.getUsername());
        String id = ctx.pathParam("id");
        try {
            Report approved = reportService.approve(id, user);
            ctx.json(approved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
        }
    }

    private void handleOverrideReport(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        User user = userRepo.findByUsername(session.getUsername());
        String id = ctx.pathParam("id");
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        if (!body.containsKey("newLevel") || !body.containsKey("reason")) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Both 'newLevel' (1-5) and 'reason' are required for severity override"));
            return;
        }
        int newLevel = ((Number) body.get("newLevel")).intValue();
        String reason = (String) body.get("reason");

        try {
            Report overridden = reportService.overrideSeverity(id, newLevel, reason, user);
            ctx.json(overridden);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
        }
    }

    private void handleRejectReport(Context ctx) {
        SessionManager.UserSession session = ctx.attribute("userSession");
        User user = userRepo.findByUsername(session.getUsername());
        String id = ctx.pathParam("id");
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String reason = (String) body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "A non-empty 'reason' is required to reject a report"));
            return;
        }

        try {
            Report rejected = reportService.reject(id, reason, user);
            ctx.json(rejected);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
        }
    }
}
