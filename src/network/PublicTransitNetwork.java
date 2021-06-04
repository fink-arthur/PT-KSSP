package network;

import tools.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class PublicTransitNetwork {

    public static final Comparator<Timetable> TIMETABLE_COMPARATOR = Comparator.comparingInt(Timetable::getDepartureSeconds).thenComparingInt(Timetable::getSequence);
    public static final Comparator<Connection> CONNECTION_COMPARATOR = Comparator.comparingInt(Connection::getDepartureSeconds).thenComparingInt(Connection::getSequence);

    private List<StopPoint> stopPoints;
    private List<StopArea> stopAreas;
    private List<List<FootPath>> footPaths;
    private List<List<FootPath>> inboundFootPaths;
    private Map<String, Route> routes;
    private Map<String, Trip> trips;
    private List<TripLight> tripLights;
    private List<Destination> destinations;
    private List<Timetable> timetables;
    private List<Connection> connections;
    private List<List<Integer>> connectionIndicesFromStop;
    private Map<Pair<Integer, Integer>, List<Integer>> connectionIndicesFromPair;
    private Connection[] connectionsArray;

    public PublicTransitNetwork() {
        this.stopPoints = new ArrayList<>();
        this.stopAreas = new ArrayList<>();
        this.inboundFootPaths = new ArrayList<>();
        this.footPaths = new ArrayList<>();
        this.routes = new HashMap<>();
        this.trips = new HashMap<>();
        this.tripLights = new ArrayList<>();
        this.destinations = new ArrayList<>();
        this.timetables = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.connectionIndicesFromStop = new ArrayList<>();
    }

    public List<StopPoint> getStopPoints() {
        return stopPoints;
    }

    public List<StopArea> getStopAreas() {
        return stopAreas;
    }

    public List<List<FootPath>> getInboundFootPaths() {
        return inboundFootPaths;
    }

    public List<List<FootPath>> getFootPaths() {
        return footPaths;
    }

    public Map<String, Route> getRoutes() {
        return routes;
    }

    public Map<String, Trip> getTrips() {
        return trips;
    }

    public List<TripLight> getTripLights() {
        return tripLights;
    }

    public List<Destination> getDestinations() {
        return destinations;
    }

    public Map<Pair<Integer, Integer>, List<Integer>> getConnectionIndicesFromPair() {
        return connectionIndicesFromPair;
    }

    public List<Timetable> getTimetables() {
        return timetables;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public List<List<Integer>> getConnectionIndicesFromStop() {
        return connectionIndicesFromStop;
    }

    public Connection[] getConnectionsArray() {
        return connectionsArray;
    }

    public void updatingNetwork() {

        System.out.println("> Updating network");

        int counter = 0;
        int intermediateCounter = 1;

        while (intermediateCounter != 0) {
            intermediateCounter = 0;

            for (StopPoint stopPoint : this.getStopPoints()) {

                Set<StopPoint> existingFootPaths = this.getFootPaths().get(stopPoint.getPosition()).stream().map(FootPath::getTo).collect(Collectors.toSet());
                Set<StopPoint> alreadyAdded = new HashSet<>();
                alreadyAdded.add(stopPoint);

                for (StopPoint intermediateStopPoint : existingFootPaths) {

                    List<FootPath> toAdd = new ArrayList<>();

                    for (FootPath footPath : this.getFootPaths().get(intermediateStopPoint.getPosition())) {
                        if (!existingFootPaths.contains(footPath.getTo()) && !alreadyAdded.contains(footPath.getTo())) {
                            toAdd.add(new FootPath(stopPoint, footPath.getTo(), (int) stopPoint.distanceTo(footPath.getTo())));
                            alreadyAdded.add(footPath.getTo());
                            intermediateCounter++;
                        }
                    }

                    this.getFootPaths().get(stopPoint.getPosition()).addAll(toAdd);
                }
            }

            counter += intermediateCounter;
        }

        this.getStopPoints().forEach(e -> this.getInboundFootPaths().add(new ArrayList<>()));

        for (List<FootPath> footPath : this.getFootPaths()) {
            for (FootPath path : footPath) {
                this.getInboundFootPaths().get(path.getTo().getPosition()).add(path);
            }
        }

        System.out.println(String.format("    > Added %s missing foot paths", counter));

        this.getTimetables().sort(TIMETABLE_COMPARATOR);
        this.getTrips().values().forEach(e -> e.getTimetables().sort(Comparator.comparingInt(Timetable::getSequence)));

        System.out.println("    > Sorted timetables");

        counter = 0;
        Set<String> trips = new HashSet<>();

        for (int i = 0; i < this.getTimetables().size(); i++) {
            if (this.getTimetables().get(i).getStopPoint() == null) {
                counter++;
                trips.add(this.getTimetables().get(i).getTrip().getId());
            }
        }

        for (String trip : trips) {
            this.getTrips().get(trip).getTimetables().forEach(e -> this.getTimetables().remove(e));
        }

        System.out.println(String.format("    > Removed %s incorrect timetables", counter));

        for (Trip trip : this.getTrips().values()) {
            for (int i = 1; i < trip.getTimetables().size(); i++) {
                Timetable currentTimetable = trip.getTimetables().get(i);
                Timetable previousTimetable = trip.getTimetables().get(i - 1);

                currentTimetable.setPreviousTimetable(previousTimetable);
                previousTimetable.setNextTimetable(currentTimetable);
            }
        }

        System.out.println("    > Chained timetables");

        for (Trip trip : this.getTrips().values()) {
            for (int i = 0; i < trip.getTimetables().size() - 1; i++) {
                if (trip.getTimetables().get(i).getStopPoint().getPosition() == trip.getTimetables().get(i + 1).getStopPoint().getPosition()) {
                    Timetable toSquashStart = trip.getTimetables().get(i);
                    Timetable toSquashEnd = trip.getTimetables().get(i + 1);
                    Timetable replacement = new Timetable(toSquashStart.getStopPoint(), trip, toSquashEnd.getDepartureDate(), toSquashStart.getArrivalDate(), toSquashEnd.getDepartureSeconds(), toSquashStart.getArrivalSeconds(), toSquashStart.getSequence());
                    replacement.setPreviousTimetable(toSquashStart.getPreviousTimetable());
                    replacement.setNextTimetable(toSquashEnd.getNextTimetable());
                    trip.getTimetables().remove(i);
                    trip.getTimetables().add(i, replacement);
                    trip.getTimetables().remove(i + 1);
                }
            }
        }

        for (Trip trip : this.getTrips().values()) {
            this.getRoutes().get(trip.getRoute().getId()).getDirections().get(trip.getDirectionId()).add(trip);
            trip.updateTimetables();
        }

        for (Route route : this.getRoutes().values()) {
            for (Integer direction : route.getDirections().keySet()) {
                Map<String, List<Trip>> destinations = new HashMap<>();

                for (Trip trip : route.getDirections().get(direction)) {
                    String positions = trip.getTimetables().stream().map(e -> Integer.toString(e.getStopPoint().getPosition())).collect(Collectors.joining());
                    String destinationId = route.getId() + "#" + trip.getHeadsign() + "#" + trip.getTimetables().size() + "#" + positions;

                    if (destinations.containsKey(destinationId)) {
                        destinations.get(destinationId).add(trip);
                    } else {
                        List<Trip> currentTrips = new ArrayList<>();
                        currentTrips.add(trip);
                        destinations.put(destinationId, currentTrips);
                    }
                }

                // remove trips that overtake each other
                List<Pair<String, Trip>> toAdd = new ArrayList<>();
                int cpt = 0;
                for (String destinationId : destinations.keySet()) {
                    List<Trip> tripList = destinations.get(destinationId);
                    tripList.sort(Comparator.comparingInt(e -> e.getTimetables().get(0).getDepartureSeconds()));

                    for (int i = 0; i < tripList.size() - 1; i++) {
                        for (int j = 0; j < tripList.get(0).getTimetables().size(); j++) {
                            if (tripList.get(i).getTimetables().get(j).getDepartureSeconds() > tripList.get(i + 1).getTimetables().get(j).getDepartureSeconds()) {
                                Trip removedTrip = tripList.remove(i + 1);
                                toAdd.add(new Pair<>(destinationId + "_" + cpt, removedTrip));
                                cpt++;
                                i--;
                                break;
                            }

                            if (tripList.get(i).getTimetables().get(j).getArrivalSeconds() > tripList.get(i + 1).getTimetables().get(j).getArrivalSeconds()) {
                                Trip removedTrip = tripList.remove(i + 1);
                                toAdd.add(new Pair<>(destinationId + "_" + cpt, removedTrip));
                                cpt++;
                                i--;
                                break;
                            }
                        }
                    }
                }

                for (Pair<String, Trip> trip : toAdd) {
                    destinations.put(trip.getLeft(), Collections.singletonList(trip.getRight()));
                }

                for (String destinationId : destinations.keySet()) {
                    Destination destination = new Destination(destinationId, route, direction);
                    destination.getTrips().addAll(destinations.get(destinationId));
                    destination.getStopPoints().addAll(destination.getTrips().get(0).getTimetables().stream().map(Timetable::getStopPoint).collect(Collectors.toList()));
                    destination.getTrips().forEach(e -> e.setDestination(destination));
                    destination.setPosition(this.getDestinations().size());
                    this.getDestinations().add(destination);
                    route.getDestinations().add(destination);
                }
            }
        }

        System.out.println(String.format("    > Created %s destinations", this.destinations.size()));

        for (Destination destination : this.getDestinations()) {
            List<Timetable> timetableList = destination.getTrips().get(0).getTimetables();
            for (int i = 0; i < timetableList.size(); i++) {
                Timetable timetable = timetableList.get(i);
                timetable.getStopPoint().getDestinations().add(new Pair<>(destination, i));
            }
        }

        System.out.println("    > Added destinations to stop points");

        for (Destination destination : this.getDestinations()) {
            List<Timetable> timetables = destination.getTrips().stream().sorted(Comparator.comparingInt(e -> e.getTimetables().get(0).getDepartureSeconds())).flatMap(f -> f.getTimetables().stream()).collect(Collectors.toList());
            destination.getTimetables().addAll(timetables);

            List<String> updatedTrips = new ArrayList<>();
            for (Timetable timetable : timetables) {
                if (!updatedTrips.contains(timetable.getTrip().getId())) {
                    timetable.getTrip().setPosition(updatedTrips.size());
                    updatedTrips.add(timetable.getTrip().getId());
                }
            }

            destination.getTrips().sort(Comparator.comparingInt(Trip::getPosition));
        }

        for (Trip trip : this.getTrips().values()) {
            for (Timetable timetable : trip.getTimetables()) {
                trip.getDepartureTimes().add(timetable.getDepartureSeconds());
                trip.getArrivalTimes().add(timetable.getArrivalSeconds());
            }
        }

        int index = 0;
        for (Trip trip : this.getTrips().values()) {
            trip.setIndex(index);
            index += 1;
        }

        for (Timetable timetable : this.getTimetables()) {
            if (timetable.getNextTimetable() != null) {
                Connection connection = new Connection(timetable.getDepartureSeconds(),
                        timetable.getNextTimetable().getArrivalSeconds(),
                        timetable.getStopPoint().getPosition(),
                        timetable.getNextTimetable().getStopPoint().getPosition(),
                        timetable.getTrip().getId(),
                        timetable.getTrip().getIndex(),
                        timetable.getSequence());
                this.getConnections().add(connection);
                this.getTrips().get(timetable.getTrip().getId()).getConnections().add(connection);
            }
        }

        this.connectionsArray = new Connection[this.getConnections().size()];
        List<Connection> connectionList = this.getConnections();
        for (int i = 0; i < connectionList.size(); i++) {
            Connection connection = connectionList.get(i);
            this.connectionsArray[i] = connection;
        }

        for (Destination destination : this.getDestinations()) {
            List<Connection> connections = destination.getTrips().stream().sorted(Comparator.comparingInt(e -> e.getConnections().get(0).getDepartureSeconds())).flatMap(f -> f.getConnections().stream()).collect(Collectors.toList());
            destination.getConnections().addAll(connections);

            destination.getDepartureTimes().addAll(destination.getTrips().stream().sorted(Comparator.comparingInt(e -> e.getTimetables().get(0).getDepartureSeconds())).flatMap(f -> f.getTimetables().stream()).map(Timetable::getDepartureSeconds).collect(Collectors.toList()));
            destination.getArrivalTimes().addAll(destination.getTrips().stream().sorted(Comparator.comparingInt(e -> e.getTimetables().get(0).getDepartureSeconds())).flatMap(f -> f.getTimetables().stream()).map(Timetable::getArrivalSeconds).collect(Collectors.toList()));
        }

        System.out.println("    > Create data structure for CSA");

        for (Trip trip : this.getTrips().values().stream().sorted(Comparator.comparingInt(Trip::getIndex)).collect(Collectors.toList())) {
            TripLight tripLight = new TripLight(trip.getId(), trip.getPosition());
            tripLight.getDepartureTimes().addAll(trip.getDepartureTimes());
            tripLight.getArrivalTimes().addAll(trip.getArrivalTimes());
            this.getTripLights().add(tripLight);
        }

        this.connectionIndicesFromPair = new HashMap<>();
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            Pair<Integer, Integer> pair = new Pair<>(connection.getDeparturePosition(), connection.getArrivalPosition());

            if (connectionIndicesFromPair.containsKey(pair)) {
                connectionIndicesFromPair.get(pair).add(i);
            } else {
                List<Integer> newList = new ArrayList<>();
                newList.add(i);
                connectionIndicesFromPair.put(pair, newList);
            }
        }

        stopPoints.forEach(e -> connectionIndicesFromStop.add(new ArrayList<>()));
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            connectionIndicesFromStop.get(connection.getDeparturePosition()).add(i);
        }

        for (Trip trip : getTrips().values()) {
            for (int i = 0; i < trip.getConnections().size(); i++) {
                Connection connection = trip.getConnections().get(i);
                connection.setSequence(i);
            }
        }

        System.out.println("> Updated network successfully\n");
    }
}
