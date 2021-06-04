package core.benchmarking;

import algorithm.KsspResultAndMetrics;
import algorithm.kssp.PostPonedYen;
import algorithm.kssp.Yen;
import core.Algorithm;
import core.City;
import core.RoutingApplication;
import gtfs.GtfsImport;
import network.PublicTransitNetwork;
import network.StopPoint;
import tools.CsvReader;

import java.io.*;
import java.util.*;

public class BenchmarkingKsspApplication {

    private static final int REPEATS = 2;
    private static final int K = 100;
    private static final int SIZE = 1000;
    private static final List<Integer> VALUES = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100);

    public static KsspResultAndMetrics min(KsspResultAndMetrics v1, KsspResultAndMetrics v2) {
        if (v1.getTime() <= v2.getTime()) {
            return v1;
        } else {
            return v2;
        }
    }

    public static void main(String[] args) {

        City city = City.STOCKHOLM;
        BenchmarkingMethod method = BenchmarkingMethod.RANDOM;
        Algorithm algorithm = Algorithm.POSTPONEDKSSP;

        PublicTransitNetwork network = new PublicTransitNetwork();
        GtfsImport gtfsImport = new GtfsImport(city);

        gtfsImport.readFiles(RoutingApplication.LOAD_DATE, network);
        network.updatingNetwork();


        List<List<String>> runTimes = new ArrayList<>();
        VALUES.forEach(e -> runTimes.add(new ArrayList<>()));
        int counter = 0;

        System.out.println(String.format("> Benchmarking method %s for %s algorithm", method.toString().toLowerCase(), algorithm.toString().toLowerCase()));


        try (InputStream inputStream = BenchmarkingKsspApplication.class.getResourceAsStream("data/" + city.toString().toLowerCase() + "/" + method + "_" + SIZE)) {
            List<Map<String, String>> lines = CsvReader.readCsvWithHeader(inputStream);
            for (Map<String, String> line : lines) {

                String date = line.getOrDefault("date", "12:00");
                int hourOfDay = Integer.parseInt(date.split(":")[0]);
                int minuteOfHour = Integer.parseInt(date.split(":")[1]);
                StopPoint start = network.getStopPoints().get(Integer.parseInt(line.get("start")));
                StopPoint arrival = network.getStopPoints().get(Integer.parseInt(line.get("end")));
                String rank = line.getOrDefault("rank", "0");

                List<KsspResultAndMetrics> results = new ArrayList<>();
                VALUES.forEach(e -> results.add(new KsspResultAndMetrics()));

                for (int i = 0; i < REPEATS; i++) {

                    switch (algorithm) {
                        case KSSP:
                            Yen yen = new Yen(network, start, arrival, hourOfDay, minuteOfHour);
                            List<KsspResultAndMetrics> yenResult = yen.returnResultsForBenchmark(K, VALUES);

                            for (int j = 0; j < yenResult.size(); j++) {
                                results.set(j, min(results.get(j), yenResult.get(j)));
                            }

                            break;
                        case POSTPONEDKSSP:
                            PostPonedYen postPonedYen = new PostPonedYen(network, start, arrival, hourOfDay, minuteOfHour);
                            List<KsspResultAndMetrics> postponedYenResult = postPonedYen.returnResultsForBenchmark(K, VALUES);

                            for (int j = 0; j < postponedYenResult.size(); j++) {
                                results.set(j, min(results.get(j), postponedYenResult.get(j)));
                            }

                            break;
                    }
                }

                for (int i = 0; i < results.size(); i++) {
                    runTimes.get(i).add(start.getId() +
                            ";" + arrival.getId() +
                            ";" + results.get(i).getTime() +
                            ";" + (hourOfDay * 3600 + minuteOfHour * 60) +
                            ";" + results.get(i).getNumberOfResults() +
                            ";" + rank +
                            ";" + results.get(i).getNumberOfCsaCalls());
                }

                counter++;

                if (counter % 10 == 0) {
                    System.out.println(String.format("   > %s", counter));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < VALUES.size(); i++) {
            System.out.print("start;end;runtime;departureTime;numberOfResults;rank;numberOfCsaCalls\n");
            System.out.println(String.join("\n", runTimes.get(i)));
            System.out.println("\n");
        }

        System.out.println("> Benchmarking complete\n");
    }
}
