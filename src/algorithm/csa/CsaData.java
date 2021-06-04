package algorithm.csa;

import algorithm.JourneyPointer;
import algorithm.Tools;
import network.FootPath;
import network.PublicTransitNetwork;
import network.StopPoint;

import java.util.*;

public class CsaData {

    private int[] earliestArrivalArray;
    private int[] usedTripsWithPointersArray;
    private List<JourneyPointer> journeyPointers;
    private List<Integer> departureStops;
    private int startIndex;
    private int departureTime;

    public CsaData(PublicTransitNetwork network) {
        this.earliestArrivalArray = new int[network.getStopPoints().size()];
        this.usedTripsWithPointersArray = new int[network.getTrips().size()];
        Arrays.fill(this.usedTripsWithPointersArray, -1);
        this.journeyPointers = new ArrayList<>(network.getStopPoints().size());
        this.departureStops = new ArrayList<>();
    }

    public int[] getEarliestArrivalArray() {
        return earliestArrivalArray;
    }

    public int[] getUsedTripsWithPointersArray() {
        return usedTripsWithPointersArray;
    }

    public List<JourneyPointer> getJourneyPointers() {
        return journeyPointers;
    }

    public List<Integer> getDepartureStops() {
        return departureStops;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }


    public static CsaData initializeCsa(PublicTransitNetwork network,
                                        StopPoint start,
                                        int departureHour,
                                        int departureMinute) {

        return initializeCsa(network, start, departureHour, departureMinute, false);
    }


    public static CsaData initializeCsa(PublicTransitNetwork network,
                                        StopPoint start,
                                        int departureHour,
                                        int departureMinute,
                                        boolean noFootPaths) {

        return initializeCsa(network, start, departureHour * 3600 + departureMinute * 60, noFootPaths);
    }


    public static CsaData initializeCsa(PublicTransitNetwork network,
                                        StopPoint start,
                                        int departureSeconds,
                                        boolean noFootPaths) {

        CsaData data = new CsaData(network);

        data.setDepartureTime(departureSeconds);

        for (StopPoint stopPoint : network.getStopPoints()) {
            data.getEarliestArrivalArray()[stopPoint.getPosition()] = Tools.MAX_ARRIVAL_TIME;
            data.getJourneyPointers().add(new JourneyPointer(-1, -1, FootPath.getEmptyFootPath(), ""));
        }

        List<String> departureStops = new ArrayList<>();
        data.getEarliestArrivalArray()[start.getPosition()] = departureSeconds;
        departureStops.add(start.getId());
        data.getDepartureStops().add(start.getPosition());

        if (!noFootPaths) {
            for (FootPath footPath : network.getFootPaths().get(start.getPosition())) {
                if (!departureStops.contains(footPath.getTo().getId())) {
                    data.getEarliestArrivalArray()[footPath.getTo().getPosition()] = departureSeconds + footPath.getDuration();
                    data.getJourneyPointers().get(footPath.getTo().getPosition()).update(-1, -1, footPath, "");

                    departureStops.add(footPath.getTo().getId());
                    data.getDepartureStops().add(footPath.getTo().getPosition());
                }
            }
        }

        return data;
    }
}
