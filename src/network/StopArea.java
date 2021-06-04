package network;

import java.util.ArrayList;
import java.util.List;

public class StopArea implements Position {

    private String id;
    private int position;
    private String name;
    private double latitude;
    private double longitude;
    private List<StopPoint> children;

    public StopArea(String id, int position, String name, double latitude, double longitude) {
        this.id = id;
        this.position = position;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.children = new ArrayList<>();
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

    public List<StopPoint> getChildren() {
        return children;
    }
}
