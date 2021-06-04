package algorithm.kssp;

import algorithm.KsspResultAndMetrics;
import algorithm.csa.CsaAlgorithmForKssp;
import com.google.common.base.Stopwatch;
import network.Connection;
import network.PublicTransitNetwork;
import network.StopPoint;
import tools.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Yen {

    private PublicTransitNetwork network;
    private CsaAlgorithmForKssp csaKssp;
    private StopPoint start;
    private StopPoint end;
    private int departureHour;
    private int departureMinute;
    private int counterUsedTrees = 0;
    private PriorityQueue<Journey> heapSortedPaths = new PriorityQueue<>(Comparator.comparingInt(Journey::getArrivalTime));
    private List<Journey> yieldedPaths = new ArrayList<>();
    private List<List<Integer>> forbiddenEdges = new ArrayList<>();
    private List<Set<Pair<Integer, Integer>>> forbiddenFootPaths = new ArrayList<>();
    private boolean[] forbiddenVertices;

    public Yen(PublicTransitNetwork network,
               StopPoint start,
               StopPoint end,
               int departureHour,
               int departureMinute) {

        this.network = network;
        this.csaKssp = new CsaAlgorithmForKssp(network, start, end, departureHour * 3600 + departureMinute * 60);
        this.start = start;
        this.end = end;
        this.departureHour = departureHour;
        this.departureMinute = departureMinute;
        this.forbiddenVertices = new boolean[network.getStopPoints().size()];
    }


    public boolean initialize() {
        if (this.start != this.end) {
            csaKssp.launch();
            counterUsedTrees++;

            Optional<Journey> journey = csaKssp.extractResult();
            if (journey.isPresent()) {
                journey.get().setDeviationIndex(0);
                heapSortedPaths.add(journey.get());
                return true;
            }
        }

        return false;
    }


    public Optional<Journey> nextPath() {

        if (heapSortedPaths.isEmpty()) {
            return Optional.empty();
        }

        Journey journey = heapSortedPaths.poll();

        computeDeviations(journey);

        yieldedPaths.add(journey);
        return Optional.of(journey);
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


    private void computeDeviations(Journey journey) {

        List<Leg> previousPath = journey.getLegs();
        int previousPathSize = journey.getLegs().size();
        int deviationIndex = journey.getDeviationIndex();

        forbiddenEdges.clear();
        forbiddenFootPaths.clear();
        previousPath.forEach(e -> forbiddenEdges.add(new ArrayList<>()));
        previousPath.forEach(e -> forbiddenFootPaths.add(new HashSet<>()));

        for (int i = deviationIndex; i < previousPathSize; i++) {
            if (previousPath.get(i).isFootpath()) {
                forbiddenFootPaths.get(i).add(new Pair<>(previousPath.get(i).getDeparture(), previousPath.get(i).getArrival()));
            } else {
                removeEdges(forbiddenEdges.get(i), previousPath.get(i).getDeparture(), previousPath.get(i).getArrival(), previousPath.get(i).getTrip());
            }
        }

        // We remove the first different edge of paths that share the same prefix
        for (Journey yieldedPath : yieldedPaths) {

            int jMax = previousPathSize < yieldedPath.getLegs().size() ? previousPathSize : yieldedPath.getLegs().size() - 1;
            int j = 0;

            while (j < jMax &&
                    (previousPath.get(j).getDeparture() == yieldedPath.getLegs().get(j).getDeparture() &&
                            previousPath.get(j).getArrival() == yieldedPath.getLegs().get(j).getArrival() &&
                            previousPath.get(j).getTrip() == yieldedPath.getLegs().get(j).getTrip())) {
                j++;
            }

            if (deviationIndex <= j && j < yieldedPath.getLegs().size()) {
                // j is the first index at which the paths differ
                if (yieldedPath.getLegs().get(j).isFootpath()) {
                    forbiddenFootPaths.get(j).add(new Pair<>(yieldedPath.getLegs().get(j).getDeparture(), yieldedPath.getLegs().get(j).getArrival()));
                } else {
                    removeEdges(forbiddenEdges.get(j), yieldedPath.getLegs().get(j).getDeparture(), yieldedPath.getLegs().get(j).getArrival(), yieldedPath.getLegs().get(j).getTrip());
                }
            }
        }

        for (int i = 0; i < network.getStopPoints().size(); i++) {
            forbiddenVertices[i] = false;
        }

        for (int i = 1; i < deviationIndex; i++) {
            forbiddenVertices[previousPath.get(i - 1).getDeparture()] = true;
        }

        forbiddenEdges.forEach(Collections::sort);

        boolean previousWalk = deviationIndex != 0 && previousPath.get(deviationIndex - 1).isFootpath();
        int previousArrivalTime = deviationIndex != 0 ? previousPath.get(deviationIndex - 1).getArrivalTime() : departureHour * 3600 + departureMinute * 60;

        for (int i = deviationIndex; i < previousPathSize - 1; i++) {

            if (i > 0) {
                forbiddenVertices[previousPath.get(i - 1).getDeparture()] = true;
            }

            int spurNode = previousPath.get(i).getDeparture();

            if (previousPath.get(i).isFootpath()) {
                csaKssp.updateStartAndEnd(this.network.getStopPoints().get(spurNode), this.end, previousArrivalTime);
                csaKssp.launch(forbiddenVertices, forbiddenEdges.get(i), forbiddenFootPaths.get(i));

                previousWalk = true;
            } else if (previousWalk) {
                csaKssp.updateStartAndEnd(this.network.getStopPoints().get(spurNode), this.end, previousArrivalTime);
                int index = i;
                network.getFootPaths().get(spurNode).forEach(e -> forbiddenFootPaths.get(index).add(new Pair<>(e.getFrom().getPosition(), e.getTo().getPosition())));
                csaKssp.launch(forbiddenVertices, forbiddenEdges.get(i), forbiddenFootPaths.get(i), true);

                previousWalk = false;
            } else {
                csaKssp.updateStartAndEnd(this.network.getStopPoints().get(spurNode), this.end, previousArrivalTime);
                csaKssp.launch(forbiddenVertices, forbiddenEdges.get(i), forbiddenFootPaths.get(i));

                previousWalk = false;
            }
            previousArrivalTime = previousPath.get(i).getArrivalTime();
            counterUsedTrees++;

            Optional<Journey> newJourney = csaKssp.extractResult();

            if (newJourney.isPresent() &&
                    newJourney.get().getLegs().size() == 1 &&
                    newJourney.get().getLegs().get(0).isFootpath() &&
                    forbiddenFootPaths.get(i).contains(new Pair<>(newJourney.get().getLegs().get(0).getDeparture(), newJourney.get().getLegs().get(0).getArrival()))) {
                continue;
            }

            if (newJourney.isPresent() && correctJourney(newJourney.get())) {

                // We shift the departure time of the walk paths so that it is the earliest possible
                if (newJourney.get().getLegs().get(0).isFootpath()) {
                    int newDepartureSeconds;
                    if (i == 0) {
                        newDepartureSeconds = departureHour * 3600 + departureMinute * 60;
                    } else {
                        newDepartureSeconds = journey.getLegs().get(i - 1).getArrivalTime();
                    }
                    newJourney.get().getLegs().get(0).update(newJourney.get().getLegs().get(0).getDeparture(),
                            newDepartureSeconds,
                            newJourney.get().getLegs().get(0).getArrival(),
                            newDepartureSeconds + (newJourney.get().getLegs().get(0).getArrivalTime() - newJourney.get().getLegs().get(0).getDepartureTime()),
                            newJourney.get().getLegs().get(0).getTrip(),
                            newJourney.get().getLegs().get(0).isFootpath());
                }

                newJourney.get().setDeviationIndex(i);
                newJourney.get().getLegs().addAll(0, journey.getLegs().subList(0, i));
                heapSortedPaths.add(newJourney.get());
            } else if (newJourney.isPresent() && !correctJourney(newJourney.get())) {

                // If the journey is not extracted properly, we forbid the first foot path and try to compute a deviation one more time
                if (newJourney.get().getLegs().size() > 1 &&
                        newJourney.get().getLegs().get(0).isFootpath() &&
                        newJourney.get().getLegs().get(1).isFootpath()) {
                    forbiddenFootPaths.get(i).add(new Pair<>(newJourney.get().getLegs().get(1).getDeparture(), newJourney.get().getLegs().get(1).getArrival()));
                    previousWalk = i != 0 && previousPath.get(i - 1).isFootpath();
                    previousArrivalTime = i != 0 ? previousPath.get(i - 1).getArrivalTime() : departureHour * 3600 + departureMinute * 60;
                    i--;
                }
            }
        }
    }


    private void removeEdges(List<Integer> forbiddenEdge, int departure, int arrival, int tripIndex) {
        List<Integer> edges = network.getConnectionIndicesFromPair().getOrDefault(new Pair<>(departure, arrival), Collections.emptyList());

        for (Integer edge : edges) {
            Connection connection = network.getConnections().get(edge);

            if (connection.getTripIndex() == tripIndex && !forbiddenEdge.contains(edge)) {
                forbiddenEdge.add(edge);
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

    public PriorityQueue<Journey> getHeapSortedPaths() {
        return heapSortedPaths;
    }

    public List<Journey> getYieldedPaths() {
        return yieldedPaths;
    }
}
