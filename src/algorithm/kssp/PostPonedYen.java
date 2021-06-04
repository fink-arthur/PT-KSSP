package algorithm.kssp;

import algorithm.KsspResultAndMetrics;
import algorithm.ParetoCriteria;
import algorithm.Tools;
import algorithm.csa.CsaAlgorithmForKssp;
import algorithm.csa.ProfileCsaForKssp;
import com.google.common.base.Stopwatch;
import network.Connection;
import network.FootPath;
import network.PublicTransitNetwork;
import network.StopPoint;
import tools.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class PostPonedYen {

    private PublicTransitNetwork network;
    private CsaAlgorithmForKssp csaKssp;
    private ProfileCsaForKssp pcsaKssp;
    private StopPoint start;
    private StopPoint end;
    private int departureHour;
    private int departureMinute;
    private int counterUsedTrees = 0;
    private PriorityQueue<Journey> heapSortedPaths = new PriorityQueue<>(Comparator.comparingInt(Journey::getArrivalTime));
    private List<Journey> yieldedPaths = new ArrayList<>();
    private List<Integer> forbiddenEdges = new ArrayList<>();
    private Set<Pair<Integer, Integer>> forbiddenFootPaths = new HashSet<>();
    private List<Set<Pair<Integer, Integer>>> forbiddenFootPathsPerLeg = new ArrayList<>();
    private List<Set<Leg>> forbiddenConnectionsPerLeg = new ArrayList<>();
    private boolean[] forbiddenVertices;

    public PostPonedYen(PublicTransitNetwork network,
                        StopPoint start,
                        StopPoint end,
                        int departureHour,
                        int departureMinute) {

        this.network = network;
        this.csaKssp = new CsaAlgorithmForKssp(network, start, end, departureHour * 3600 + departureMinute * 60);
        this.pcsaKssp = new ProfileCsaForKssp(network, start, end, departureHour, departureMinute, 48, 0);
        this.start = start;
        this.end = end;
        this.departureHour = departureHour;
        this.departureMinute = departureMinute;
        this.forbiddenVertices = new boolean[network.getStopPoints().size()];
    }


    public boolean initialize() {
        if (this.start != this.end) {
            this.pcsaKssp.launch();
            Optional<Journey> journey = this.pcsaKssp.extractResult(this.start.getPosition(),
                    this.departureHour * 3600 + this.departureMinute * 60);
            if (journey.isPresent()) {
                journey.get().setDeviationIndex(0);
                this.heapSortedPaths.add(journey.get());
                return true;
            }
        }

        return false;
    }


    public Optional<Journey> nextPath() {

        while (!heapSortedPaths.isEmpty()) {

            Journey prevPath = heapSortedPaths.poll();

            if (isSimple(prevPath)) {
                yieldedPaths.add(prevPath);
                addMinExtensions(prevPath);

                return Optional.of(prevPath);
            } else {
                repair(prevPath);
            }
        }

        return Optional.empty();
    }


    public List<Journey> returnResults(int k) {

        List<Journey> result = new ArrayList<>();

        if (initialize()) {
            int counter = 0;
            while (counter < k) {
                Optional<Journey> res = nextPath();
                counter++;

                if (res.isPresent()) {
                    result.add(res.get());
                } else {
                    break;
                }
            }
        }

        return result;
    }


    public List<KsspResultAndMetrics> returnResultsForBenchmark(int k, List<Integer> values) {

        List<KsspResultAndMetrics> result = new ArrayList<>();

        Stopwatch stopwatch = Stopwatch.createStarted();
        int counter = 0;
        if (initialize()) {
            while (counter < k) {
                Optional<Journey> res = nextPath();

                if (res.isPresent()) {
                    counter++;
                } else {
                    break;
                }

                if (values.contains(counter)) {
                    int time = (int) stopwatch.elapsed(TimeUnit.MILLISECONDS);
                    KsspResultAndMetrics metric = new KsspResultAndMetrics(time, counter, counterUsedTrees);
                    result.add(metric);
                }
            }
        }
        stopwatch.stop();

        if (result.size() < values.size()) {
            int toAdd = values.size() - result.size();
            for (int i = 0; i < toAdd; i++) {
                int time = (int) stopwatch.elapsed(TimeUnit.MILLISECONDS);
                KsspResultAndMetrics metric = new KsspResultAndMetrics(time, counter, counterUsedTrees);
                result.add(metric);
            }
        }

        return result;
    }


    private boolean isSimple(Journey journey) {

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


    private void addMinExtensions(Journey journey) {

        List<Leg> previousPath = journey.getLegs();
        int deviationIndex = journey.getDeviationIndex();
        List<Leg> prefix = new ArrayList<>(previousPath.subList(0, deviationIndex));
        int legArrivalTime = deviationIndex != 0 ? previousPath.get(deviationIndex - 1).getArrivalTime() : departureHour * 3600 + departureMinute * 60;

        forbiddenFootPathsPerLeg.clear();
        forbiddenConnectionsPerLeg.clear();
        previousPath.forEach(e -> forbiddenFootPathsPerLeg.add(new HashSet<>()));
        previousPath.forEach(e -> forbiddenConnectionsPerLeg.add(new HashSet<>()));

        for (Journey yieldedPath : yieldedPaths) {

            int jMax = previousPath.size() < yieldedPath.getLegs().size() ? previousPath.size() : yieldedPath.getLegs().size() - 1;
            int j = 0;

            while (j < jMax &&
                    (previousPath.get(j).getDeparture() == yieldedPath.getLegs().get(j).getDeparture() &&
                            previousPath.get(j).getArrival() == yieldedPath.getLegs().get(j).getArrival() &&
                            previousPath.get(j).getTrip() == yieldedPath.getLegs().get(j).getTrip()
                    )) {
                j++;
            }

            if (deviationIndex <= j && j < yieldedPath.getLegs().size()) {
                // j is the first index at which the paths differ
                if (yieldedPath.getLegs().get(j).isFootpath()) {
                    forbiddenFootPathsPerLeg.get(j).add(new Pair<>(yieldedPath.getLegs().get(j).getDeparture(), yieldedPath.getLegs().get(j).getArrival()));
                } else {
                    forbiddenConnectionsPerLeg.get(j).add(yieldedPath.getLegs().get(j));
                }
            }
        }

        for (int i = deviationIndex; i < journey.getLegs().size() - 1; i++) {
            if (journey.getLegs().get(i).isFootpath()) {
                forbiddenFootPathsPerLeg.get(i).add(new Pair<>(previousPath.get(i).getDeparture(), previousPath.get(i).getArrival()));
            } else {
                forbiddenConnectionsPerLeg.get(i).add(previousPath.get(i));
            }
        }

        for (int i = deviationIndex; i < previousPath.size() - 1; i++) {
            Leg currentLeg = previousPath.get(i);
            int spurNode = currentLeg.getDeparture();
            Pair<Integer, Leg> earliestArrivalWalk = new Pair<>(Tools.MAX_ARRIVAL_TIME, new Leg(spurNode, currentLeg.getDepartureTime(), -1, Tools.MAX_ARRIVAL_TIME, -1, false));
            Pair<Integer, Leg> earliestArrivalConnection = new Pair<>(Tools.MAX_ARRIVAL_TIME, new Leg(spurNode, currentLeg.getDepartureTime(), -1, Tools.MAX_ARRIVAL_TIME, -1, false));
            Pair<Integer, Leg> bestEarliestArrival;

            if (i == 0 || !previousPath.get(i - 1).isFootpath()) {
                for (FootPath footPath : network.getFootPaths().get(spurNode)) {
                    Pair<Integer, Integer> pair = new Pair<>(footPath.getFrom().getPosition(), footPath.getTo().getPosition());
                    if (footPath.getTo().getPosition() != spurNode &&
                            !forbiddenFootPathsPerLeg.get(i).contains(pair)) {

                        ParetoCriteria earliestAfterTime;
                        if (footPath.getTo().getPosition() == this.end.getPosition()) {
                            Leg leg = new Leg(footPath.getFrom().getPosition(),
                                    legArrivalTime,
                                    footPath.getTo().getPosition(),
                                    legArrivalTime + footPath.getDuration(),
                                    -1,
                                    true);
                            earliestAfterTime = new ParetoCriteria(legArrivalTime, leg.getArrivalTime(), 0, 0, leg);
                        } else {
                            earliestAfterTime = pcsaKssp.earliestAfterTimeNotWalkPath(footPath.getTo().getPosition(),
                                    legArrivalTime + footPath.getDuration() + Tools.TRANSFER_WINDOW);
                        }

                        if (earliestAfterTime.getLeg() != null &&
                                earliestArrivalWalk.getLeft() > earliestAfterTime.getArrivalTime()) {
                            earliestArrivalWalk.setLeft(earliestAfterTime.getArrivalTime());
                            earliestArrivalWalk.getRight().update(spurNode,
                                    legArrivalTime,
                                    footPath.getTo().getPosition(),
                                    legArrivalTime + footPath.getDuration(),
                                    -1,
                                    true);
                        }
                    }
                }
            }


            for (int index : this.network.getConnectionIndicesFromStop().get(spurNode)) {

                Connection connection = this.network.getConnections().get(index);
                Leg connectionToLeg = new Leg(connection.getDeparturePosition(),
                        connection.getDepartureSeconds(),
                        connection.getArrivalPosition(),
                        connection.getArrivalSeconds(),
                        connection.getTripIndex(),
                        false);

                if (forbiddenConnectionsPerLeg.get(i).contains(connectionToLeg)) {
                    continue;
                }

                if (connection.getDepartureSeconds() > earliestArrivalConnection.getLeft()) {
                    break;
                }

                int currentPenalty = 0;
                if (deviationIndex != 0 && previousPath.get(i - 1).getTrip() != connection.getTripIndex()) {
                    currentPenalty = Tools.TRANSFER_WINDOW;
                }

                if (connection.getDepartureSeconds() >= legArrivalTime + currentPenalty &&
                        connection.getArrivalPosition() != spurNode) {

                    ParetoCriteria earliestAfterTime;
                    if (connection.getArrivalPosition() == this.end.getPosition()) {
                        Leg leg = new Leg(connection.getDeparturePosition(),
                                connection.getDepartureSeconds(),
                                connection.getArrivalPosition(),
                                connection.getArrivalSeconds(),
                                connection.getTripIndex(),
                                false);
                        earliestAfterTime = new ParetoCriteria(legArrivalTime, connection.getArrivalSeconds(), 0, 0, leg);
                    } else {
                        earliestAfterTime = pcsaKssp.earliestAfterTimeWithTrip(connection.getArrivalPosition(),
                                connection.getArrivalSeconds(),
                                connection.getTripIndex(),
                                Tools.TRANSFER_WINDOW);
                    }

                    if (earliestArrivalConnection.getLeft() > earliestAfterTime.getArrivalTime()) {
                        earliestArrivalConnection.setLeft(earliestAfterTime.getArrivalTime());
                        earliestArrivalConnection.getRight().update(connection.getDeparturePosition(),
                                connection.getDepartureSeconds(),
                                connection.getArrivalPosition(),
                                connection.getArrivalSeconds(),
                                connection.getTripIndex(),
                                false);
                    }
                }
            }

            if (earliestArrivalWalk.getLeft() == Tools.MAX_ARRIVAL_TIME && earliestArrivalConnection.getLeft() == Tools.MAX_ARRIVAL_TIME) {
                prefix.add(currentLeg);
                legArrivalTime = currentLeg.getArrivalTime();
                continue;
            }

            boolean noWalking = false;
            if (earliestArrivalConnection.getLeft() <= earliestArrivalWalk.getLeft()) {
                bestEarliestArrival = earliestArrivalConnection;
            } else {
                bestEarliestArrival = earliestArrivalWalk;
                noWalking = true;
            }

            Optional<Journey> result = pcsaKssp.extractResult(bestEarliestArrival.getRight().getArrival(),
                    bestEarliestArrival.getRight().getArrivalTime(),
                    noWalking,
                    bestEarliestArrival.getRight().getTrip());

            if (result.isPresent()) {

                // We shift the departure time of the walk paths so that it is the earliest possible
                if (i > 0 && !result.get().getLegs().isEmpty() && result.get().getLegs().get(0).isFootpath()) {
                    int newDepartureSeconds = bestEarliestArrival.getRight().getArrivalTime();
                    result.get().getLegs().get(0).update(result.get().getLegs().get(0).getDeparture(),
                            newDepartureSeconds,
                            result.get().getLegs().get(0).getArrival(),
                            newDepartureSeconds + (result.get().getLegs().get(0).getArrivalTime() - result.get().getLegs().get(0).getDepartureTime()),
                            result.get().getLegs().get(0).getTrip(),
                            result.get().getLegs().get(0).isFootpath());
                }

                result.get().getLegs().add(0, bestEarliestArrival.getRight());
                result.get().getLegs().addAll(0, prefix);
                result.get().setDeviationIndex(i);

                this.heapSortedPaths.add(result.get());
            }

            prefix.add(currentLeg);
            legArrivalTime = currentLeg.getArrivalTime();
        }
    }

    private void repair(Journey journey) {
        repair(journey, Collections.emptyList());
    }

    private void repair(Journey journey, List<Pair<Integer, Integer>> forbiddenPairs) {

        List<Leg> previousPath = journey.getLegs();
        int previousPathSize = journey.getLegs().size();
        int deviationIndex = journey.getDeviationIndex();

        forbiddenEdges.clear();
        forbiddenFootPaths.clear();

        forbiddenFootPaths.addAll(forbiddenPairs);

        for (Journey yieldedPath : yieldedPaths) {
            int jMax = previousPathSize < yieldedPath.getLegs().size() ? previousPathSize : yieldedPath.getLegs().size() - 1;
            int j = 0;

            while (j < jMax &&
                    (previousPath.get(j).getDeparture() == yieldedPath.getLegs().get(j).getDeparture() &&
                            previousPath.get(j).getArrival() == yieldedPath.getLegs().get(j).getArrival() &&
                            previousPath.get(j).getTrip() == yieldedPath.getLegs().get(j).getTrip())) {
                j++;
            }

            if (j == deviationIndex && j < yieldedPath.getLegs().size()) {
                // j is the first index at which the paths differ
                if (yieldedPath.getLegs().get(deviationIndex).isFootpath()) {
                    forbiddenFootPaths.add(new Pair<>(yieldedPath.getLegs().get(deviationIndex).getDeparture(), yieldedPath.getLegs().get(deviationIndex).getArrival()));
                } else {
                    removeEdges(yieldedPath.getLegs().get(j).getDeparture(), yieldedPath.getLegs().get(j).getArrival(), yieldedPath.getLegs().get(j).getTrip());
                }
            }
        }

        for (int i = 0; i < network.getStopPoints().size(); i++) {
            forbiddenVertices[i] = false;
        }

        for (int i = 1; i < deviationIndex; i++) {
            forbiddenVertices[previousPath.get(i - 1).getDeparture()] = true;
        }

        Collections.sort(forbiddenEdges);

        boolean previousWalk = deviationIndex != 0 && previousPath.get(deviationIndex - 1).isFootpath();
        int previousArrivalTime = deviationIndex != 0 ? previousPath.get(deviationIndex - 1).getArrivalTime() : departureHour * 3600 + departureMinute * 60;

        if (deviationIndex > 0) {
            forbiddenVertices[previousPath.get(deviationIndex - 1).getDeparture()] = true;
        }

        int spurNode = previousPath.get(deviationIndex).getDeparture();

        if (previousPath.get(deviationIndex).isFootpath()) {
            csaKssp.updateStartAndEnd(this.network.getStopPoints().get(spurNode), this.end, previousArrivalTime);
            csaKssp.launch(forbiddenVertices, forbiddenEdges, forbiddenFootPaths);
        } else if (previousWalk) {
            csaKssp.updateStartAndEnd(this.network.getStopPoints().get(spurNode), this.end, previousArrivalTime);
            network.getFootPaths().get(spurNode).forEach(e -> forbiddenFootPaths.add(new Pair<>(e.getFrom().getPosition(), e.getTo().getPosition())));
            csaKssp.launch(forbiddenVertices, forbiddenEdges, forbiddenFootPaths, true);
        } else {
            csaKssp.updateStartAndEnd(this.network.getStopPoints().get(spurNode), this.end, previousArrivalTime);
            csaKssp.launch(forbiddenVertices, forbiddenEdges, forbiddenFootPaths);
        }
        counterUsedTrees++;

        Optional<Journey> newJourney = csaKssp.extractResult();

        if (newJourney.isPresent() && correctJourney(newJourney.get())) {

            // We shift the departure time of the walk paths so that it is the earliest possible
            if (newJourney.get().getLegs().get(0).isFootpath()) {
                int newDepartureSeconds;
                if (deviationIndex == 0) {
                    newDepartureSeconds = departureHour * 3600 + departureMinute * 60;
                } else {
                    newDepartureSeconds = journey.getLegs().get(deviationIndex - 1).getArrivalTime();
                }
                newJourney.get().getLegs().get(0).update(newJourney.get().getLegs().get(0).getDeparture(),
                        newDepartureSeconds,
                        newJourney.get().getLegs().get(0).getArrival(),
                        newDepartureSeconds + (newJourney.get().getLegs().get(0).getArrivalTime() - newJourney.get().getLegs().get(0).getDepartureTime()),
                        newJourney.get().getLegs().get(0).getTrip(),
                        newJourney.get().getLegs().get(0).isFootpath());
            }

            newJourney.get().setDeviationIndex(deviationIndex);
            newJourney.get().getLegs().addAll(0, journey.getLegs().subList(0, deviationIndex));

            heapSortedPaths.add(newJourney.get());
        } else if (newJourney.isPresent() && !correctJourney(newJourney.get())) {

            // If the journey is not extracted properly, we forbid the first foot path and try to compute a deviation one more time
            if (newJourney.get().getLegs().size() > 1 &&
                    newJourney.get().getLegs().get(0).isFootpath() &&
                    newJourney.get().getLegs().get(1).isFootpath()) {
                List<Pair<Integer, Integer>> list = new ArrayList<>();
                list.addAll(forbiddenPairs);
                list.add(new Pair<>(newJourney.get().getLegs().get(1).getDeparture(), newJourney.get().getLegs().get(1).getArrival()));
                repair(journey, list);
            }
        }
    }


    private void removeEdges(int departure, int arrival, int tripIndex) {
        List<Integer> edges = network.getConnectionIndicesFromPair().getOrDefault(new Pair<>(departure, arrival), Collections.emptyList());

        for (Integer edge : edges) {
            Connection connection = network.getConnections().get(edge);

            if (connection.getTripIndex() == tripIndex && !forbiddenEdges.contains(edge)) {
                forbiddenEdges.add(edge);
            }
        }
    }


    public boolean correctJourney(Journey journey) {

        for (int i = 0; i < journey.getLegs().size() - 1; i++) {
            if (journey.getLegs().get(i).getArrival() != journey.getLegs().get(i + 1).getDeparture() ||
                    journey.getLegs().get(i).getArrivalTime() > journey.getLegs().get(i + 1).getDepartureTime() ||
                    (journey.getLegs().get(i).isFootpath() && journey.getLegs().get(i + 1).isFootpath())) {
                return false;
            }
        }

        return true;
    }

    public int getCounterUsedTrees() {
        return counterUsedTrees;
    }
}
