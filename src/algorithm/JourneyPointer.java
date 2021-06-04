package algorithm;

import network.FootPath;

public class JourneyPointer {

    private int enterConnection;
    private int exitConnection;
    private FootPath footPath;
    private String tripId;

    public JourneyPointer(int enterConnection, int exitConnection, FootPath footPath, String tripId) {
        this.enterConnection = enterConnection;
        this.exitConnection = exitConnection;
        this.footPath = footPath;
        this.tripId = tripId;
    }

    public void update(int enterConnection, int exitConnection, FootPath footPath, String tripId) {
        this.enterConnection = enterConnection;
        this.exitConnection = exitConnection;
        this.footPath = footPath;
        this.tripId = tripId;
    }

    public void update(int enterConnection, int exitConnection, String tripId) {
        this.enterConnection = enterConnection;
        this.exitConnection = exitConnection;
        this.footPath = FootPath.getEmptyFootPath();
        this.tripId = tripId;
    }

    public int getEnterConnection() {
        return enterConnection;
    }

    public int getExitConnection() {
        return exitConnection;
    }

    public FootPath getFootPath() {
        return footPath;
    }

    public String getTripId() {
        return tripId;
    }
}
