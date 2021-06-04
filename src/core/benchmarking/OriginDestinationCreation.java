package core.benchmarking;

import algorithm.Tools;
import core.City;
import core.RoutingApplication;
import gtfs.GtfsImport;
import network.PublicTransitNetwork;
import network.StopPoint;
import tools.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class OriginDestinationCreation {

    public static final int SIZE = 1000;
    public static final int MIN_TIME_HOUR = 6;

    public static List<String> getRandomOriginDestination(PublicTransitNetwork network) {

        Random random = new Random();
        List<String> coordinates = new ArrayList<>();
        Set<Pair<StopPoint, StopPoint>> already = new HashSet<>();

        while (coordinates.size() < SIZE) {
            StopPoint start = network.getStopPoints().get(random.nextInt(network.getStopPoints().size()));
            StopPoint arrival = network.getStopPoints().get(random.nextInt(network.getStopPoints().size()));

            int departureHour = random.nextInt(23 - MIN_TIME_HOUR) + MIN_TIME_HOUR;
            int departureMinute = random.nextInt(60);

            if (start.getLatitude() == 0.0 && start.getLongitude() == 0.0) {
                continue;
            }

            if (arrival.getLatitude() == 0.0 && arrival.getLongitude() == 0.0) {
                continue;
            }

            if (!Tools.isPossible(network, start, arrival, departureHour, departureMinute)) {
                continue;
            }

            Pair<StopPoint, StopPoint> pair = new Pair<>(start, arrival);
            if (already.contains(pair)) {
                continue;
            }
            already.add(pair);

            String date = departureHour + ":" + departureMinute;

            coordinates.add(start.getPosition() + ";" + arrival.getPosition() + ";" + date + ";0");

            if (coordinates.size() % 10 == 0) {
                System.out.println(String.format("> %s", coordinates.size()));
            }
        }

        return coordinates;
    }

    private static List<String> getGeoRankOriginDestination(PublicTransitNetwork network) {

        Random random = new Random();
        List<String> coordinates = new ArrayList<>();
        Set<Pair<StopPoint, StopPoint>> already = new HashSet<>();

        while (coordinates.size() < SIZE) {
            int index = 4;
            int errorCounter = 0;
            StopPoint start = network.getStopPoints().get(random.nextInt(network.getStopPoints().size()));

            if (start.getLatitude() == 0.0 && start.getLongitude() == 0.0) {
                continue;
            }

            List<StopPoint> stopPoints = network.getStopPoints().stream().sorted(Comparator.comparingDouble(start::distanceTo)).collect(Collectors.toList());

            while (Math.pow(2, index) < stopPoints.size() - 1) {

                if (errorCounter > 50) {
                    break;
                }

                int secondIndex = random.nextInt((int) Math.pow(2, index) - 1);

                if (secondIndex + Math.pow(2, index) >= stopPoints.size()) {
                    continue;
                }

                StopPoint arrival = stopPoints.get((int) Math.pow(2, index) + secondIndex);

                int departureHour = random.nextInt(23 - MIN_TIME_HOUR) + MIN_TIME_HOUR;
                int departureMinute = random.nextInt(60);

                if (arrival.getLatitude() == 0.0 && arrival.getLongitude() == 0.0) {
                    continue;
                }

                if (!Tools.isPossible(network, start, arrival, departureHour, departureMinute)) {
                    errorCounter++;
                    continue;
                }

                Pair<StopPoint, StopPoint> pair = new Pair<>(start, arrival);
                if (already.contains(pair)) {
                    continue;
                }
                already.add(pair);

                String date = departureHour + ":" + departureMinute;

                coordinates.add(start.getPosition() + ";" + arrival.getPosition() + ";" + date + ";" + index);
                index += 1;

                if (coordinates.size() % 10 == 0) {
                    System.out.println(String.format("> %s", coordinates.size()));
                }
            }
        }

        return coordinates;
    }


    public static void main(String[] args) {

        City city = City.SWITZERLAND;
        BenchmarkingMethod method = BenchmarkingMethod.RANDOM;

        PublicTransitNetwork network = new PublicTransitNetwork();
        GtfsImport gtfsImport = new GtfsImport(city);

        gtfsImport.readFiles(RoutingApplication.LOAD_DATE, network);
        network.updatingNetwork();

        List<String> coordinates;

        switch (method) {
            case RANDOM:
                coordinates = getRandomOriginDestination(network);
                break;
            case GEORANK:
                coordinates = getGeoRankOriginDestination(network);
                break;
            default:
                coordinates = getRandomOriginDestination(network);
                break;
        }

        System.out.println("start;end;date;rank\n");
        System.out.println(String.join("\n", coordinates));
    }
}
