package network;

import java.util.ArrayList;
import java.util.List;

public class TripLight {

    private String id;
    private int position;
    private List<Integer> departureTimes;
    private List<Integer> arrivalTimes;

    public TripLight(String id, int position) {
        this.id = id;
        this.position = position;
        this.departureTimes = new ArrayList<>();
        this.arrivalTimes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public List<Integer> getDepartureTimes() {
        return departureTimes;
    }

    public List<Integer> getArrivalTimes() {
        return arrivalTimes;
    }
}
