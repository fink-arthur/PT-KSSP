package algorithm;

import network.*;
import tools.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Tools {

    public static final int TRANSFER_WINDOW = 0;
    public final static int MAX_ARRIVAL_TIME = 48 * 3600;

    public static int binarySearch(List<Timetable> timetables, int hour, int minute) {
        return binarySearch(timetables, hour * 3600 + minute * 60 - 1);
    }


    public static int binarySearch(List<Timetable> timetables, int departureTime) {
        int updatedDepartureTime = departureTime - 1;
        Timetable key = new Timetable(null, null, "", "", updatedDepartureTime, updatedDepartureTime, 0);

        int index = timetableBinarySearch(timetables, key);

        while (index < timetables.size() && timetables.get(index).getDepartureSeconds() == updatedDepartureTime) {
            index += 1;
        }

        return Math.min(index, timetables.size() - 1);
    }


    private static int timetableBinarySearch(List<Timetable> timetables, Timetable key) {
        int low = 0;
        int high = timetables.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Timetable midVal = timetables.get(mid);
            int cmp = PublicTransitNetwork.TIMETABLE_COMPARATOR.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return low;  // key not found
    }

    public static int connectionBinarySearch(List<Connection> connections, int hour, int minute) {
        return connectionBinarySearch(connections, hour * 3600 + minute * 60 - 1);
    }


    public static int connectionBinarySearch(List<Connection> connections, int departureTime) {
        int updatedDepartureTime = departureTime - 1;
        Connection connection = new Connection(updatedDepartureTime, updatedDepartureTime, 0, 0, "", 0, 0);

        int index = connectionBinarySearch(connections, connection);

        while (index < connections.size() && connections.get(index).getDepartureSeconds() == updatedDepartureTime) {
            index += 1;
        }

        return Math.min(index, connections.size() - 1);
    }


    private static int connectionBinarySearch(List<Connection> timetables, Connection key) {
        int low = 0;
        int high = timetables.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Connection midVal = timetables.get(mid);
            int cmp = PublicTransitNetwork.CONNECTION_COMPARATOR.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return low;  // key not found
    }


    public static boolean isPossible(PublicTransitNetwork network, StopPoint start, StopPoint end, int startHour, int startMinute) {

        int startSeconds = startHour * 3600 + startMinute * 60;
        boolean possibleAtStart = true;
        boolean possibleAtEnd = true;

        List<StopPoint> startStopPoints = network.getFootPaths().get(start.getPosition()).stream().map(FootPath::getTo).collect(Collectors.toList());
        startStopPoints.add(start);
        for (StopPoint stopPoint : startStopPoints) {
            for (Pair<Destination, Integer> destination : stopPoint.getDestinations()) {
                for (Trip trip : destination.getLeft().getTrips()) {
                    possibleAtStart = possibleAtStart && trip.getTimetables().get(destination.getRight()).getDepartureSeconds() < startSeconds;
                }
            }
        }

        List<StopPoint> endStopPoints = network.getFootPaths().get(end.getPosition()).stream().map(FootPath::getTo).collect(Collectors.toList());
        endStopPoints.add(end);
        for (StopPoint stopPoint : endStopPoints) {
            for (Pair<Destination, Integer> destination : stopPoint.getDestinations()) {
                for (Trip trip : destination.getLeft().getTrips()) {
                    possibleAtEnd = possibleAtEnd && trip.getTimetables().get(destination.getRight()).getDepartureSeconds() < startSeconds;
                }
            }
        }

        return !(possibleAtStart || possibleAtEnd);
    }
}
