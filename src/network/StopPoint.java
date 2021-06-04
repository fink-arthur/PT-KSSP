package network;

import tools.Pair;

import java.util.ArrayList;
import java.util.List;

public class StopPoint implements Position {

    private String id;
    private int position;
    private String name;
    private double latitude;
    private double longitude;
    private StopArea parent;
    private List<Pair<Destination, Integer>> destinations;

    public StopPoint(String id, int position, String name, double latitude, double longitude) {
        this.id = id;
        this.position = position;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.destinations = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public StopArea getParent() {
        return parent;
    }

    public void setParent(StopArea parent) {
        this.parent = parent;
    }

    public List<Pair<Destination, Integer>> getDestinations() {
        return destinations;
    }
}
