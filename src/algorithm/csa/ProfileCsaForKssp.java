package algorithm.csa;

import algorithm.ParetoCriteria;
import algorithm.Tools;
import algorithm.kssp.Journey;
import algorithm.kssp.Leg;
import network.*;

import java.util.*;

public class ProfileCsaForKssp {

    private PublicTransitNetwork network;
    private Map<Integer, Integer> arrivalWalkPaths = new HashMap<>();
    private int startIndex;
    private int endIndex;
    private StopPoint start;
    private StopPoint end;
    private Map<String, Integer> vehicleJourneyLabels = new HashMap<>();
    private Map<String, Connection> vehicleJourneyConnections = new HashMap<>();
    List<List<ParetoCriteria>> stopLabels = new ArrayList<>();
    List<List<ParetoCriteria>> footPathStopLabels = new ArrayList<>();

    public ProfileCsaForKssp(PublicTransitNetwork network,
                             StopPoint start,
                             StopPoint end,
                             int startHour,
                             int startMinute,
                             int endHour,
                             int endMinute) {

        this.network = network;
        this.startIndex = Tools.connectionBinarySearch(network.getConnections(), startHour, startMinute);
        this.endIndex = Tools.connectionBinarySearch(network.getConnections(), endHour, endMinute);
        this.start = start;
        this.end = end;

        for (StopPoint ignored : network.getStopPoints()) {
            stopLabels.add(new ArrayList<>());
            footPathStopLabels.add(new ArrayList<>());
        }

        network.getInboundFootPaths().get(end.getPosition()).forEach(e -> arrivalWalkPaths.put(e.getFrom().getPosition(), e.getDuration()));
        this.arrivalWalkPaths.put(end.getPosition(), 0);
    }


    public List<List<ParetoCriteria>> launch() {

        profileCsaLoop();

        return stopLabels;
    }


    private void profileCsaLoop() {

        for (int i = endIndex; i > startIndex; i--) {
            Connection connection = network.getConnections().get(i);

            int nextStopPointPosition = connection.getArrivalPosition();
            int currentStopPointPosition = connection.getDeparturePosition();

            int nearArrival = Tools.MAX_ARRIVAL_TIME;
            if (arrivalWalkPaths.containsKey(nextStopPointPosition)) {
                nearArrival = connection.getArrivalSeconds() + arrivalWalkPaths.get(nextStopPointPosition);
            }

            int sameVehicleJourneyArrival = vehicleJourneyLabels.getOrDefault(connection.getTripId(), Tools.MAX_ARRIVAL_TIME);

            int arrivalWithTransfer = Tools.MAX_ARRIVAL_TIME;
            int j = 0;

            while (j < stopLabels.get(nextStopPointPosition).size() &&
                    stopLabels.get(nextStopPointPosition).get(j).getDepartureTime() < connection.getArrivalSeconds()) {
                j++;
            }
            if (j < stopLabels.get(nextStopPointPosition).size()) {
                arrivalWithTransfer = stopLabels.get(nextStopPointPosition).get(j).getArrivalTime();
            }

            int arrivalWithFootPathTransfer = Tools.MAX_ARRIVAL_TIME;
            int k = 0;

            while (k < footPathStopLabels.get(nextStopPointPosition).size() &&
                    footPathStopLabels.get(nextStopPointPosition).get(k).getDepartureTime() < connection.getArrivalSeconds()) {
                k++;
            }
            if (k < footPathStopLabels.get(nextStopPointPosition).size()) {
                arrivalWithFootPathTransfer = footPathStopLabels.get(nextStopPointPosition).get(k).getArrivalTime();
            }

            int bestArrival = Math.min(nearArrival, Math.min(sameVehicleJourneyArrival, Math.min(arrivalWithTransfer, arrivalWithFootPathTransfer)));

            if (bestArrival != Tools.MAX_ARRIVAL_TIME) {

                if (sameVehicleJourneyArrival > bestArrival) {
                    vehicleJourneyLabels.put(connection.getTripId(), bestArrival);
                    vehicleJourneyConnections.put(connection.getTripId(), connection);
                }

                Leg leg = new Leg(connection.getDeparturePosition(),
                        connection.getDepartureSeconds(),
                        vehicleJourneyConnections.get(connection.getTripId()).getArrivalPosition(),
                        vehicleJourneyConnections.get(connection.getTripId()).getArrivalSeconds(),
                        connection.getTripIndex(),
                        false);

                ParetoCriteria currentObjectiveCriteria = new ParetoCriteria(connection.getDepartureSeconds() - Tools.TRANSFER_WINDOW,
                        bestArrival,
                        0,
                        0,
                        leg);

                if (!domination(currentObjectiveCriteria, stopLabels.get(currentStopPointPosition))) {

                    insertion(currentObjectiveCriteria, stopLabels.get(currentStopPointPosition));

                    // We look at the possible walk paths from the arrival of the timetable
                    for (FootPath destinationWalk : network.getInboundFootPaths().get(currentStopPointPosition)) {

                        if (currentStopPointPosition == destinationWalk.getFrom().getPosition()) {
                            continue;
                        }

                        Leg footPathLeg = new Leg(destinationWalk.getFrom().getPosition(),
                                connection.getDepartureSeconds() - destinationWalk.getDuration(),
                                destinationWalk.getTo().getPosition(),
                                connection.getDepartureSeconds(),
                                -1,
                                true);

                        ParetoCriteria currentWalkPathObjectiveCriteria = new ParetoCriteria(connection.getDepartureSeconds() - destinationWalk.getDuration() - Tools.TRANSFER_WINDOW,
                                bestArrival,
                                0,
                                0,
                                footPathLeg);

                        if (!domination(currentWalkPathObjectiveCriteria, footPathStopLabels.get(destinationWalk.getFrom().getPosition()))) {

                            insertion(currentWalkPathObjectiveCriteria, footPathStopLabels.get(destinationWalk.getFrom().getPosition()));
                        }
                    }
                }
            }
        }
    }


    private void insertion(ParetoCriteria currentObjectiveCriteria,
                           List<ParetoCriteria> objectiveCriteriaList) {

        for (int k = 0; k < objectiveCriteriaList.size(); k++) {
            if (objectiveCriteriaList.get(k).getDepartureTime() <= currentObjectiveCriteria.getDepartureTime()) {
                if (currentObjectiveCriteria.dominateProfile(objectiveCriteriaList.get(k))) {
                    objectiveCriteriaList.remove(k);
                    k--;
                } else if (k == objectiveCriteriaList.size() - 1) {
                    objectiveCriteriaList.add(currentObjectiveCriteria);
                    break;
                }
            } else {
                objectiveCriteriaList.add(k, currentObjectiveCriteria);
                break;
            }
        }

        if (objectiveCriteriaList.size() == 0) {
            objectiveCriteriaList.add(currentObjectiveCriteria);
        }
    }


    private boolean domination(ParetoCriteria currentObjectiveCriteria,
                               List<ParetoCriteria> objectiveCriteriaList) {

        for (ParetoCriteria objectiveCriteria : objectiveCriteriaList) {
            if (objectiveCriteria.getDepartureTime() >= currentObjectiveCriteria.getDepartureTime() || objectiveCriteria.getDepartureTime() == Integer.MIN_VALUE) {
                if (objectiveCriteria.dominateProfile(currentObjectiveCriteria)) {
                    return true;
                }
            }
        }

        return false;
    }


    public Optional<Journey> extractResult(int startPosition, int departureSeconds) {
        return extractResult(startPosition, departureSeconds, false, -1);
    }


    public Optional<Journey> extractResult(int startPosition, int departureSeconds, boolean noWalking, int tripIndex) {

        ParetoCriteria currentLeg;
        if (noWalking) {
            currentLeg = earliestAfterTimeNotWalkPath(startPosition, departureSeconds);
        } else {
            currentLeg = earliestAfterTimeWithTrip(startPosition, departureSeconds, tripIndex, Tools.TRANSFER_WINDOW);
        }

        Journey result = new Journey();

        if (startPosition == this.end.getPosition()) {
            result.setArrivalTime(departureSeconds);
            return Optional.of(result);
        }

        if (currentLeg.getArrivalTime() == Tools.MAX_ARRIVAL_TIME) {
            return Optional.empty();
        }

        while (true) {

            if (currentLeg.getLeg().isFootpath()) {
                if (result.getLegs().isEmpty()) {
                    Leg currentWalkPathLeg = new Leg(currentLeg.getLeg().getDeparture(),
                            departureSeconds,
                            currentLeg.getLeg().getArrival(),
                            departureSeconds + (currentLeg.getLeg().getArrivalTime() - currentLeg.getLeg().getDepartureTime()),
                            -1,
                            true);
                    result.getLegs().add(currentWalkPathLeg);
                } else {
                    int currentDepartureSeconds = result.getLegs().get(result.getLegs().size() - 1).getArrivalTime();
                    Leg currentWalkPathLeg = new Leg(currentLeg.getLeg().getDeparture(),
                            currentDepartureSeconds,
                            currentLeg.getLeg().getArrival(),
                            currentDepartureSeconds + (currentLeg.getLeg().getArrivalTime() - currentLeg.getLeg().getDepartureTime()),
                            -1,
                            true);
                    result.getLegs().add(currentWalkPathLeg);
                }
            } else {

                Trip trip = network.getTrips().get(network.getTripLights().get(currentLeg.getLeg().getTrip()).getId());
                boolean printing = false;

                for (Connection connection : trip.getConnections()) {

                    if (connection.getDeparturePosition() == currentLeg.getLeg().getDeparture() &&
                            connection.getDepartureSeconds() >= currentLeg.getLeg().getDepartureTime()) {
                        printing = true;
                    }

                    if (printing) {
                        Leg leg = new Leg(connection.getDeparturePosition(),
                                connection.getDepartureSeconds(),
                                connection.getArrivalPosition(),
                                connection.getArrivalSeconds(),
                                connection.getTripIndex(),
                                false);
                        result.getLegs().add(leg);
                    }

                    if (connection.getArrivalPosition() == currentLeg.getLeg().getArrival() &&
                            connection.getDepartureSeconds() >= currentLeg.getLeg().getDepartureTime()) {
                        break;
                    }
                }
            }

            if (this.end.getPosition() == currentLeg.getLeg().getArrival()) {
                break;
            }

            if (currentLeg.getLeg().getArrivalTime() + this.arrivalWalkPaths.getOrDefault(currentLeg.getLeg().getArrival(), Tools.MAX_ARRIVAL_TIME) == currentLeg.getArrivalTime()) {
                Leg leg = new Leg(currentLeg.getLeg().getArrival(),
                        currentLeg.getLeg().getArrivalTime(),
                        this.end.getPosition(),
                        currentLeg.getArrivalTime(),
                        -1,
                        false);
                result.getLegs().add(leg);
                break;
            }

            if (currentLeg.getLeg().isFootpath()) {
                currentLeg = earliestAfterTimeNotWalkPath(currentLeg.getLeg().getArrival(), currentLeg.getLeg().getArrivalTime() + Tools.TRANSFER_WINDOW);
            } else {
                currentLeg = earliestAfterTime(currentLeg.getLeg().getArrival(), currentLeg.getLeg().getArrivalTime() + Tools.TRANSFER_WINDOW);
            }
        }

        result.setArrivalTime(result.getLegs().get(result.getLegs().size() - 1).getArrivalTime());

        return Optional.of(result);
    }


    public ParetoCriteria earliestAfterTime(int stopPointPosition,
                                            int departureSeconds) {

        ParetoCriteria earliestAfterTime = new ParetoCriteria();
        for (ParetoCriteria paretoCriteria : this.stopLabels.get(stopPointPosition)) {

            if (paretoCriteria.getDepartureTime() >= departureSeconds) {
                earliestAfterTime = paretoCriteria;
                break;
            }
        }

        ParetoCriteria earliestAfterFootPathTime = new ParetoCriteria();
        for (ParetoCriteria paretoCriteria : this.footPathStopLabels.get(stopPointPosition)) {

            if (paretoCriteria.getDepartureTime() >= departureSeconds) {
                earliestAfterFootPathTime = paretoCriteria;
                break;
            }
        }

        ParetoCriteria earliestAfterNearArrival = new ParetoCriteria();
        int walkDistanceToArrival = this.arrivalWalkPaths.getOrDefault(stopPointPosition, Tools.MAX_ARRIVAL_TIME);
        if (walkDistanceToArrival != Tools.MAX_ARRIVAL_TIME) {
            Leg leg = new Leg(stopPointPosition,
                    departureSeconds,
                    this.end.getPosition(),
                    departureSeconds + walkDistanceToArrival,
                    -1,
                    true);
            earliestAfterNearArrival.set(departureSeconds, departureSeconds + walkDistanceToArrival, 0, 0);
            earliestAfterNearArrival.setLeg(leg);
        }

        if (earliestAfterNearArrival.getArrivalTime() < earliestAfterFootPathTime.getArrivalTime() &&
                earliestAfterNearArrival.getArrivalTime() < earliestAfterTime.getArrivalTime()) {
            return earliestAfterNearArrival;
        } else if (earliestAfterFootPathTime.getArrivalTime() < earliestAfterTime.getArrivalTime()) {
            return earliestAfterFootPathTime;
        } else {
            return earliestAfterTime;
        }
    }


    public ParetoCriteria earliestAfterTimeWithTrip(int stopPointPosition,
                                                    int departureSeconds,
                                                    int tripIndex,
                                                    int transferPenalty) {

        ParetoCriteria earliestAfterTime = new ParetoCriteria();
        for (ParetoCriteria paretoCriteria : this.stopLabels.get(stopPointPosition)) {

            if (paretoCriteria.getDepartureTime() >= departureSeconds + (paretoCriteria.getLeg().getTrip() == tripIndex ? 0 : transferPenalty)) {
                earliestAfterTime = paretoCriteria;
                break;
            }
        }

        ParetoCriteria earliestAfterFootPathTime = new ParetoCriteria();
        for (ParetoCriteria paretoCriteria : this.footPathStopLabels.get(stopPointPosition)) {

            if (paretoCriteria.getDepartureTime() >= departureSeconds + transferPenalty) {
                earliestAfterFootPathTime = paretoCriteria;
                break;
            }
        }

        ParetoCriteria earliestAfterNearArrival = new ParetoCriteria();
        int walkDistanceToArrival = this.arrivalWalkPaths.getOrDefault(stopPointPosition, Tools.MAX_ARRIVAL_TIME);
        if (walkDistanceToArrival != Tools.MAX_ARRIVAL_TIME) {
            Leg leg = new Leg(stopPointPosition,
                    departureSeconds,
                    this.end.getPosition(),
                    departureSeconds + walkDistanceToArrival,
                    -1,
                    true);
            earliestAfterNearArrival.set(departureSeconds, departureSeconds + walkDistanceToArrival, 0, 0);
            earliestAfterNearArrival.setLeg(leg);
        }

        if (earliestAfterNearArrival.getArrivalTime() < earliestAfterFootPathTime.getArrivalTime() &&
                earliestAfterNearArrival.getArrivalTime() < earliestAfterTime.getArrivalTime()) {
            return earliestAfterNearArrival;
        } else if (earliestAfterFootPathTime.getArrivalTime() < earliestAfterTime.getArrivalTime()) {
            return earliestAfterFootPathTime;
        } else {
            return earliestAfterTime;
        }
    }


    public ParetoCriteria earliestAfterTimeNotWalkPath(int stopPointPosition,
                                                       int departureSeconds) {

        for (ParetoCriteria paretoCriteria : this.stopLabels.get(stopPointPosition)) {

            if (paretoCriteria.getDepartureTime() >= departureSeconds) {
                return paretoCriteria;
            }
        }

        return new ParetoCriteria();
    }
}
