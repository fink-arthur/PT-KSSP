package network;

import java.util.ArrayList;
import java.util.List;

public class Destination {

    private String id;
    private int position;
    private Route route;
    private int direction;
    private List<Trip> trips;
    private List<StopPoint> stopPoints;
    private List<Timetable> timetables;
    private List<Connection> connections;
    private List<Integer> departureTimes;
    private List<Integer> arrivalTimes;

    public Destination(String id, Route route, int direction) {
        this.id = id;
        this.route = route;
        this.direction = direction;
        this.trips = new ArrayList<>();
        this.stopPoints = new ArrayList<>();
        this.timetables = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.departureTimes = new ArrayList<>();
        this.arrivalTimes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Route getRoute() {
        return route;
    }

    public int getDirection() {
        return direction;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public List<StopPoint> getStopPoints() {
        return stopPoints;
    }

    public List<Timetable> getTimetables() {
        return timetables;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public List<Integer> getDepartureTimes() {
        return departureTimes;
    }

    public List<Integer> getArrivalTimes() {
        return arrivalTimes;
    }
}
