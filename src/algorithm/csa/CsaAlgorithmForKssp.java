package algorithm.csa;

import algorithm.JourneyPointer;
import algorithm.Tools;
import algorithm.kssp.Journey;
import algorithm.kssp.Leg;
import com.google.common.base.Stopwatch;
import network.*;
import tools.Pair;

import java.util.*;

public class CsaAlgorithmForKssp {

    private PublicTransitNetwork network;
    private CsaData csaData;
    private StopPoint start;
    private StopPoint end;
    private int departureSeconds;


    public CsaAlgorithmForKssp(PublicTransitNetwork network, StopPoint start, StopPoint end, int departureSeconds) {
        this.network = network;
        this.start = start;
        this.end = end;
        this.departureSeconds = departureSeconds;
    }


    public void launch() {
        boolean[] ignored = new boolean[this.network.getStopPoints().size()];
        launch(ignored, Collections.emptyList(), Collections.emptySet(), false);
    }

    public void launch(boolean[] forbiddenVertices, List<Integer> forbiddenEdges) {
        launch(forbiddenVertices, forbiddenEdges, Collections.emptySet(), false);
    }

    public void launch(boolean[] forbiddenVertices, List<Integer> forbiddenEdges, boolean noFootPaths) {
        launch(forbiddenVertices, forbiddenEdges, Collections.emptySet(), noFootPaths);
    }

    public void launch(boolean[] forbiddenVertices, List<Integer> forbiddenEdges, Set<Pair<Integer, Integer>> forbiddenFootPaths) {
        launch(forbiddenVertices, forbiddenEdges, forbiddenFootPaths, false);
    }


    public void launch(boolean[] forbiddenVertices,
                       List<Integer> forbiddenEdges,
                       Set<Pair<Integer, Integer>> forbiddenFootPaths,
                       boolean noFootPaths) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        this.csaData = CsaData.initializeCsa(network, start, departureSeconds, noFootPaths);

        for (Integer departureStop : this.csaData.getDepartureStops()) {
            Pair<Integer, Integer> pair = new Pair<>(start.getPosition(), departureStop);
            if (forbiddenFootPaths.contains(pair)) {
                this.csaData.getEarliestArrivalArray()[departureStop] = Tools.MAX_ARRIVAL_TIME;
                this.csaData.getJourneyPointers().set(departureStop, new JourneyPointer(-1, -1, FootPath.getEmptyFootPath(), ""));
            }
        }

        this.csaData.setStartIndex(Tools.connectionBinarySearch(network.getConnections(), departureSeconds - 1));

        csa(forbiddenVertices, forbiddenEdges, forbiddenFootPaths);

        stopwatch.stop();
    }


    public void updateStartAndEnd(StopPoint start, StopPoint end, int departureSeconds) {
        this.start = start;
        this.end = end;
        this.departureSeconds = departureSeconds;
    }


    public void csa(boolean[] forbiddenVertices,
                    List<Integer> forbiddenEdges,
                    Set<Pair<Integer, Integer>> forbiddenFootPaths) {

        // Using an array instead of an array list for connections, using an array instead of a set for used trips
        // Using only earliest arrival time instead of stop labels for each stop
        final int size = network.getConnections().size();

        int j = forbiddenEdges.isEmpty() ? -1 : Collections.binarySearch(forbiddenEdges, this.csaData.getStartIndex());
        if (j < 0 && !forbiddenEdges.isEmpty()) {
            j = (-j) - 1;
        }

        for (int i = this.csaData.getStartIndex(); i < size; i++) {
            Connection connection = network.getConnectionsArray()[i];

            // We avoid certain connections if their vertices are forbidden or their edges
            if (forbiddenVertices[connection.getDeparturePosition()] || forbiddenVertices[connection.getArrivalPosition()]) {
                this.csaData.getUsedTripsWithPointersArray()[connection.getTripIndex()] = -1;
                continue;
            }

            if (j != -1 && j < forbiddenEdges.size() && forbiddenEdges.get(j) == i) {
                j++;
                this.csaData.getUsedTripsWithPointersArray()[connection.getTripIndex()] = -1;
                continue;
            }

            int earliestArrival = this.csaData.getEarliestArrivalArray()[connection.getDeparturePosition()];

            if (this.csaData.getUsedTripsWithPointersArray()[connection.getTripIndex()] != -1 ||
                    connection.getDepartureSeconds() >= earliestArrival + Tools.TRANSFER_WINDOW) {

                if (this.csaData.getUsedTripsWithPointersArray()[connection.getTripIndex()] == -1) {
                    this.csaData.getUsedTripsWithPointersArray()[connection.getTripIndex()] = connection.getSequence();
                }

                int arrivalStopPointPosition = connection.getArrivalPosition();
                int arrivalSeconds = connection.getArrivalSeconds();

                if (arrivalSeconds < this.csaData.getEarliestArrivalArray()[arrivalStopPointPosition]) {

                    this.csaData.getEarliestArrivalArray()[arrivalStopPointPosition] = arrivalSeconds;
                    this.csaData.getJourneyPointers().get(arrivalStopPointPosition).
                            update(this.csaData.getUsedTripsWithPointersArray()[connection.getTripIndex()],
                                    connection.getSequence(),
                                    connection.getTripId());
                }

                for (FootPath footPath : network.getFootPaths().get(arrivalStopPointPosition)) {

                    if (forbiddenVertices[footPath.getTo().getPosition()]) {
                        continue;
                    }

                    Pair<Integer, Integer> pair = new Pair<>(footPath.getFrom().getPosition(), footPath.getTo().getPosition());
                    if (forbiddenFootPaths.contains(pair)) {
                        continue;
                    }

                    if (arrivalSeconds + footPath.getDuration() < this.csaData.getEarliestArrivalArray()[footPath.getTo().getPosition()]) {

                        this.csaData.getEarliestArrivalArray()[footPath.getTo().getPosition()] = arrivalSeconds + footPath.getDuration();
                        this.csaData.getJourneyPointers().get(footPath.getTo().getPosition()).
                                update(this.csaData.getUsedTripsWithPointersArray()[connection.getTripIndex()],
                                        connection.getSequence(),
                                        footPath,
                                        connection.getTripId());
                    }
                }
            }

            if (this.csaData.getEarliestArrivalArray()[this.end.getPosition()] != Tools.MAX_ARRIVAL_TIME &&
                    connection.getDepartureSeconds() > this.csaData.getEarliestArrivalArray()[this.end.getPosition()]) {
                return;
            }
        }
    }


    public Optional<Journey> extractResult() {

        JourneyPointer currentJourneyPointer = this.csaData.getJourneyPointers().get(this.end.getPosition());
        Journey result = new Journey();

        if (currentJourneyPointer.getEnterConnection() == -1) {
            Optional<FootPath> endFootPath = this.network.getFootPaths().get(this.start.getPosition()).stream().
                    filter(e -> e.getTo().getPosition() == this.end.getPosition()).findAny();
            if (endFootPath.isPresent()) {
                Leg walkingLeg = new Leg(endFootPath.get().getFrom().getPosition(),
                        this.departureSeconds,
                        endFootPath.get().getTo().getPosition(),
                        this.departureSeconds + endFootPath.get().getDuration(),
                        -1,
                        true);
                result.getLegs().add(walkingLeg);
                result.setArrivalTime(result.getLegs().get(result.getLegs().size() - 1).getArrivalTime());
                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        }

        while (currentJourneyPointer.getEnterConnection() != -1) {

            createLeg(result, currentJourneyPointer);

            int offset = this.network.getTrips().get(currentJourneyPointer.getTripId()).getConnections().get(0).getSequence() == 1 ? 1 : 0;
            Connection connection = this.network.getTrips().get(currentJourneyPointer.getTripId()).getConnections().get(currentJourneyPointer.getEnterConnection() - offset);

            currentJourneyPointer = this.csaData.getJourneyPointers().get(connection.getDeparturePosition());
        }

        if (currentJourneyPointer.getFootPath().getDuration() != -1) {
            int departureTime = result.getLegs().get(result.getLegs().size() - 1).getDepartureTime();
            Leg walkingLeg = new Leg(currentJourneyPointer.getFootPath().getFrom().getPosition(),
                    departureTime - currentJourneyPointer.getFootPath().getDuration() - Tools.TRANSFER_WINDOW,
                    currentJourneyPointer.getFootPath().getTo().getPosition(),
                    departureTime - Tools.TRANSFER_WINDOW,
                    -1,
                    true);
            result.getLegs().add(walkingLeg);
        }

        Collections.reverse(result.getLegs());
        result.setArrivalTime(result.getLegs().get(result.getLegs().size() - 1).getArrivalTime());
        makeSimple(result);
        return Optional.of(result);
    }


    private void createLeg(Journey journey, JourneyPointer journeyPointer) {

        int offset = this.network.getTrips().get(journeyPointer.getTripId()).getConnections().get(0).getSequence() == 1 ? 1 : 0;
        Connection currentConnection = this.network.getTrips().get(journeyPointer.getTripId()).getConnections().get(journeyPointer.getExitConnection() - offset);

        if (journeyPointer.getFootPath().getDuration() != -1) {
            Leg walkingLeg = new Leg(journeyPointer.getFootPath().getFrom().getPosition(),
                    currentConnection.getArrivalSeconds(),
                    journeyPointer.getFootPath().getTo().getPosition(),
                    currentConnection.getArrivalSeconds() + journeyPointer.getFootPath().getDuration(),
                    -1,
                    true);

            journey.getLegs().add(walkingLeg);
        }

        for (int i = journeyPointer.getExitConnection(); i >= journeyPointer.getEnterConnection(); i--) {
            currentConnection = this.network.getTrips().get(journeyPointer.getTripId()).getConnections().get(i - offset);
            Leg currentLeg = new Leg(currentConnection.getDeparturePosition(),
                    currentConnection.getDepartureSeconds(),
                    currentConnection.getArrivalPosition(),
                    currentConnection.getArrivalSeconds(),
                    currentConnection.getTripIndex(),
                    false);
            journey.getLegs().add(currentLeg);
        }
    }


    private void makeSimple(Journey journey) {

        while (!isSimple(journey)) {

            Set<Integer> stops = new HashSet<>();
            Map<Integer, Integer> stopsToLegPositions = new HashMap<>();

            for (int i = 0; i < journey.getLegs().size(); i++) {

                Leg leg = journey.getLegs().get(i);

                if (stops.contains(leg.getDeparture())) {
//                    journey.getLegs().removeAll(journey.getLegs().subList(stopsToLegPositions.get(leg.getDeparture()), i));
                    removeBetweenIndexes(journey, stopsToLegPositions.get(leg.getDeparture()), i);
                    break;
                } else {
                    stops.add(leg.getDeparture());
                    stopsToLegPositions.put(leg.getDeparture(), i);
                }
            }
        }
    }


    private void removeBetweenIndexes(Journey journey, int fromIndex, int toIndex) {

        int i = 0;

        for (Iterator<Leg> iterator = journey.getLegs().iterator(); iterator.hasNext(); ) {

            Leg leg = iterator.next();

            if (i >= fromIndex && i < toIndex) {
                iterator.remove();
            }

            if (i >= toIndex) {
                break;
            }

            i++;
        }
    }


    public boolean isSimple(Journey journey) {

        Set<Integer> stops = new HashSet<>();

        for (Leg leg : journey.getLegs()) {
            if (stops.contains(leg.getDeparture())) {
                return false;
            } else {
                stops.add(leg.getDeparture());
            }
        }

        return true;
    }
}
