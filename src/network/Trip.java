package network;

import java.util.ArrayList;
import java.util.List;

public class Trip {

    private String id;
    private String name;
    private String headsign;
    private int directionId;
    private List<Timetable> timetables;
    private List<Connection> connections;
    private List<Integer> departureTimes;
    private List<Integer> arrivalTimes;
    private Timetable[] arrayTimetables;
    private Route route;
    private Destination destination;
    private int position;
    private int index;

    public Trip(String id, String name, String headsign, int directionId, Route route) {
        this.id = id;
        this.name = name;
        this.headsign = headsign;
        this.directionId = directionId;
        this.timetables = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.departureTimes = new ArrayList<>();
        this.arrivalTimes = new ArrayList<>();
        this.route = route;
        this.position = -1;
    }

    public void updateTimetables() {
        this.arrayTimetables = new Timetable[this.timetables.size()];

        for (int i = 0; i < this.timetables.size(); i++) {
            this.arrayTimetables[i] = this.timetables.get(i);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHeadsign() {
        return headsign;
    }

    public int getDirectionId() {
        return directionId;
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

    public Timetable[] getArrayTimetables() {
        return arrayTimetables;
    }

    public Route getRoute() {
        return route;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
