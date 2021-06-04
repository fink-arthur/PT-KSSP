package core;

import algorithm.kssp.Journey;
import algorithm.kssp.PostPonedYen;
import algorithm.kssp.Yen;
import com.google.common.base.Stopwatch;
import gtfs.GtfsImport;
import network.PublicTransitNetwork;
import network.StopPoint;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RoutingApplication {

    public static final LocalDate LOAD_DATE = LocalDate.of(2019, 10, 10);

    public static void main(String[] args) {

        PublicTransitNetwork network = new PublicTransitNetwork();
        GtfsImport gtfsImportParis = new GtfsImport(City.BERLIN);

        gtfsImportParis.readFiles(LOAD_DATE, network);

        network.updatingNetwork();

        Random random = new Random();
        List<StopPoint> stopPoints = network.getStopPoints();

        while (true) {
            StopPoint start = stopPoints.get(random.nextInt(stopPoints.size()));
            StopPoint end = stopPoints.get(random.nextInt(stopPoints.size()));

            if (start.equals(end)) {
                continue;
            }

            int departureHour = random.nextInt(17) + 6;
            int departureMinute = random.nextInt(60);

            int K = 50;

            for (int i = 0; i < 1; i++) {
                System.out.println(String.format("Start stop : %s, end stop : %s, departure date : %sh%s, distance between start and end : %s", start.getId(), end.getId(), departureHour, departureMinute, start.distanceTo(end)));

                Yen yen = new Yen(network, start, end, departureHour, departureMinute);

                Stopwatch stopwatch = Stopwatch.createStarted();
                List<Journey> yenResult = yen.returnResults(K);
                stopwatch.stop();

                System.out.println("Elapsed time : " + (int) stopwatch.elapsed(TimeUnit.MILLISECONDS));
                System.out.println("Number of CSA called : " + yen.getCounterUsedTrees());
                System.out.println("[" + yenResult.stream().map(e -> String.valueOf(e.getArrivalTime())).collect(Collectors.joining(",")) + "]");

                stopwatch = Stopwatch.createStarted();
                PostPonedYen postPonedYen = new PostPonedYen(network, start, end, departureHour, departureMinute);
                List<Journey> postponedYenResult = postPonedYen.returnResults(K);
                stopwatch.stop();
                System.out.println("Elapsed time : " + (int) stopwatch.elapsed(TimeUnit.MILLISECONDS));
                System.out.println("Number of CSA called : " + postPonedYen.getCounterUsedTrees());
                System.out.println("[" + postponedYenResult.stream().map(e -> String.valueOf(e.getArrivalTime())).collect(Collectors.joining(",")) + "]");

                System.out.println();
            }
        }
    }
}