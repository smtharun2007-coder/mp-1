package model;

public class RescueTeam {

    // Enum representing whether the team is available or currently deployed
    public enum TeamStatus {
        AVAILABLE, BUSY
    }

    private String teamId;
    private String teamName;
    private TeamStatus status;

    public RescueTeam(String teamId, String teamName, TeamStatus status) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.status = status;
    }

    public String getTeamId()       { return teamId; }
    public String getTeamName()     { return teamName; }
    public TeamStatus getStatus()   { return status; }

    public void setStatus(TeamStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "[" + teamId + "] " + teamName + " | Status: " + status;
    }
}
