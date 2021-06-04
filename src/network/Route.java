package network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Route {

    private String id;
    private String name;
    private PublicTransitMode mode;
    private Map<Integer, List<Trip>> directions;
    private List<Destination> destinations;

    public Route(String id, String name, PublicTransitMode mode) {
        this.id = id;
        this.name = name;
        this.mode = mode;
        this.directions = new HashMap<>();
        this.directions.put(0, new ArrayList<>());
        this.directions.put(1, new ArrayList<>());
        this.destinations = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PublicTransitMode getMode() {
        return mode;
    }

    public Map<Integer, List<Trip>> getDirections() {
        return directions;
    }

    public List<Destination> getDestinations() {
        return destinations;
    }
}
