package gtfs;

import com.opencsv.CSVReader;
import core.City;
import network.*;
import org.apache.commons.lang3.StringUtils;
import tools.CsvReader;
import tools.Pair;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GtfsImport {

    private static final String ROOT = "";//C:\\Source\\instantbenchmarking\\ressources\\gtfs\\";
    private static final String LOCAL_DATE_FORMAT = "yyyyMMdd";

    private City city;

    public GtfsImport(City city) {
        this.city = city;
    }

    public PublicTransitNetwork readFiles(LocalDate loadDate,
                                          PublicTransitNetwork network) {

        Map<String, Integer> positionToStopPointId = new HashMap<>();

        System.out.println(String.format("> Loading public transit network for %s", city));

        Set<String> services = new HashSet<>();
        if (getClass().getResource("data/" + city.toString().toLowerCase() + "/calendar.txt") != null) {
            services = readCalendar(loadDate);
        }

        readCalendarDates(loadDate, services);
        readStops(network, positionToStopPointId);

        if (getClass().getResource("data/" + city.toString().toLowerCase() + "/transfers.txt") != null) {
            readTransfers(network, positionToStopPointId);
        }

        readRoutes(network);
        readTrips(network, services);
        readStopTimes(network, positionToStopPointId);

        System.out.println("> Public transit network successfully loaded\n");

        return network;
    }

    private Set<String> readCalendar(LocalDate loadDate) {

        Set<String> services = new HashSet<>();

        try (InputStream file = getClass().getResourceAsStream("data/" + city.toString().toLowerCase() + "/calendar.txt")) {
            List<Map<String, String>> lines = CsvReader.readCsvWithHeader(file, ',');

            for (Map<String, String> line : lines) {

                String id = line.get("service_id");
                String startDateString = line.get("start_date");
                String endDateString = line.get("end_date");
                boolean isMonday = line.get("monday").equals("1");
                boolean isTuesday = line.get("tuesday").equals("1");
                boolean isWednesday = line.get("wednesday").equals("1");
                boolean isThursday = line.get("thursday").equals("1");
                boolean isFriday = line.get("friday").equals("1");
                boolean isSaturday = line.get("saturday").equals("1");
                boolean isSunday = line.get("sunday").equals("1");

                LocalDate startDate = LocalDate.parse(startDateString, DateTimeFormatter.ofPattern(LOCAL_DATE_FORMAT));
                LocalDate endDate = LocalDate.parse(endDateString, DateTimeFormatter.ofPattern(LOCAL_DATE_FORMAT));

                if (loadDate.isAfter(startDate) && loadDate.isBefore(endDate) || loadDate.isEqual(startDate) || loadDate.isEqual(endDate)) {

                    DayOfWeek dayOfWeek = DayOfWeek.from(loadDate);

                    switch (dayOfWeek) {
                        case MONDAY:
                            if (isMonday) {
                                services.add(id);
                            }
                            break;
                        case TUESDAY:
                            if (isTuesday) {
                                services.add(id);
                            }
                            break;
                        case WEDNESDAY:
                            if (isWednesday) {
                                services.add(id);
                            }
                            break;
                        case THURSDAY:
                            if (isThursday) {
                                services.add(id);
                            }
                            break;
                        case FRIDAY:
                            if (isFriday) {
                                services.add(id);
                            }
                            break;
                        case SATURDAY:
                            if (isSaturday) {
                                services.add(id);
                            }
                            break;
                        case SUNDAY:
                            if (isSunday) {
                                services.add(id);
                            }
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(String.format("      > Loaded %s services", services.size()));

        return services;
    }

    private void readCalendarDates(LocalDate loadDate, Set<String> services) {

        int counter = 0;

        try (InputStream file = getClass().getResourceAsStream("data/" + city.toString().toLowerCase() + "/calendar_dates.txt")) {
            List<Map<String, String>> lines = CsvReader.readCsvWithHeader(file, ',');

            for (Map<String, String> line : lines) {

                String id = line.get("service_id");
                String dateString = line.get("date");
                String exceptionType = line.get("exception_type");
                LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(LOCAL_DATE_FORMAT));

                if (date.isEqual(loadDate)) {
                    if (exceptionType.equals("1")) {
                        services.add(id);
                        counter++;
                    } else if (exceptionType.equals("2")) {
                        services.remove(id);
                        counter++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(String.format("      > Updated %s services", counter));
    }

    private void readStops(PublicTransitNetwork network,
                           Map<String, Integer> positionToStopPointId) {

        List<StopPoint> stopPoints = new ArrayList<>();
        List<StopArea> stopAreas = new ArrayList<>();
        int stopPointCounter = network.getStopPoints().size();
        int stopAreaCounter = network.getStopAreas().size();
        Map<String, Integer> positionToStopAreaId = new HashMap<>();

        try (InputStream file = getClass().getResourceAsStream("data/" + city.toString().toLowerCase() + "/stops.txt")) {
            List<Map<String, String>> lines = CsvReader.readCsvWithHeader(file, ',');
            List<Pair<String, String>> parents = new ArrayList<>();

            for (Map<String, String> line : lines) {

                String id = line.get("stop_id");
                String name = line.get("stop_name");
                double latitude = Double.parseDouble(line.get("stop_lat"));
                double longitude = Double.parseDouble(line.get("stop_lon"));
                String parentId = line.get("parent_station");
                String locationType = line.get("location_type");

                if (!StringUtils.isEmpty(locationType) && locationType.equals("1")) {
                    stopAreas.add(new StopArea(id, stopAreaCounter, name, latitude, longitude));
                    positionToStopAreaId.put(id, stopAreaCounter);
                    stopAreaCounter++;
                } else {
                    stopPoints.add(new StopPoint(id, stopPointCounter, name, latitude, longitude));
                    positionToStopPointId.put(id, stopPointCounter);
                    stopPointCounter++;
                }

                if (!StringUtils.isEmpty(parentId)) {
                    parents.add(new Pair<>(id, parentId));
                }
            }

            for (Pair<String, String> pair : parents) {
                StopPoint stopPoint = stopPoints.get(positionToStopPointId.get(pair.getLeft()) - network.getStopPoints().size());
                StopArea stopArea = stopAreas.get(positionToStopAreaId.get(pair.getRight()) - network.getStopAreas().size());

                stopPoint.setParent(stopArea);
                stopArea.getChildren().add(stopPoint);
            }

            if (!stopPoints.isEmpty() && stopAreas.isEmpty()) {
                stopPoints.forEach(e -> stopAreas.add(new StopArea(e.getId(), e.getPosition(), e.getName(), e.getLatitude(), e.getLongitude())));
            }

            System.out.println(String.format("      > Loaded %s stop points", stopPoints.size()));
            System.out.println(String.format("      > Loaded %s stop areas", stopAreas.size()));

            network.getStopPoints().addAll(stopPoints);
            network.getStopAreas().addAll(stopAreas);
        } catch (IOException e) {
            System.out.println("Error while loading stops.txt file");
            e.printStackTrace();
        }
    }

    private void readTransfers(PublicTransitNetwork network,
                               Map<String, Integer> positionToStopPointId) {

        List<List<FootPath>> footPaths = new ArrayList<>();
        network.getStopPoints().forEach(e -> footPaths.add(new ArrayList<>()));

        try (InputStream file = getClass().getResourceAsStream("data/" + city.toString().toLowerCase() + "/transfers.txt")) {
            List<Map<String, String>> lines = CsvReader.readCsvWithHeader(file, ',');

            for (Map<String, String> line : lines) {

                String fromId = line.get("from_stop_id");
                String toId = line.get("to_stop_id");
                String transferTime = line.get("min_transfer_time");

                if (fromId.equals(toId)) {
                    continue;
                }

                if (!StringUtils.isEmpty(transferTime) && positionToStopPointId.containsKey(fromId) && positionToStopPointId.containsKey(toId)) {
                    if (footPaths.get(positionToStopPointId.get(fromId)).stream().noneMatch(e -> e.getTo().getPosition() == positionToStopPointId.get(toId))) {
                        footPaths.get(positionToStopPointId.get(fromId)).add(new FootPath(network.getStopPoints().get(positionToStopPointId.get(fromId)), network.getStopPoints().get(positionToStopPointId.get(toId)), Integer.parseInt(transferTime)));
                    }
//                    footPaths.get(positionToStopPointId.get(fromId)).add(new FootPath(network.getStopPoints().get(positionToStopPointId.get(fromId)), network.getStopPoints().get(positionToStopPointId.get(toId)), (int) network.getStopPoints().get(positionToStopPointId.get(fromId)).distanceTo(network.getStopPoints().get(positionToStopPointId.get(toId)))));
                }
            }

            System.out.println(String.format("      > Loaded %s foot paths", footPaths.stream().mapToInt(List::size).sum()));

            network.getFootPaths().addAll(footPaths);
        } catch (IOException e) {
            System.out.println("Error while loading transfers.txt file");
            e.printStackTrace();
        }
    }

    private void readRoutes(PublicTransitNetwork network) {

        Map<String, Route> routes = new HashMap<>();

        try (InputStream file = getClass().getResourceAsStream("data/" + city.toString().toLowerCase() + "/routes.txt")) {
            List<Map<String, String>> lines = CsvReader.readCsvWithHeader(file, ',');

            for (Map<String, String> line : lines) {

                String id = line.get("route_id");
                String shortName = line.get("route_short_name");
                String longName = line.get("route_long_name");
                int type = Integer.parseInt(line.get("route_type"));
                PublicTransitMode mode = PublicTransitMode.BUS;

                switch (type) {
                    case 0:
                        mode = PublicTransitMode.TRAM;
                        break;
                    case 1:
                        mode = PublicTransitMode.METRO;
                        break;
                    case 2:
                        mode = PublicTransitMode.RAIL;
                        break;
                    case 3:
                        mode = PublicTransitMode.BUS;
                        break;
                    case 4:
                        mode = PublicTransitMode.FERRY;
                        break;
                    case 5:
                        mode = PublicTransitMode.CABLECAR;
                        break;
                    case 6:
                        mode = PublicTransitMode.SUSPENDEDCABLECAR;
                        break;
                    case 7:
                        mode = PublicTransitMode.FUNICULAR;
                        break;
                    default:
                }

                routes.put(id, new Route(id, StringUtils.isEmpty(shortName) ? longName : shortName, mode));
            }

            System.out.println(String.format("      > Loaded %s routes", routes.size()));

            network.getRoutes().putAll(routes);
        } catch (IOException e) {
            System.out.println("Error while loading routes.txt file");
            e.printStackTrace();
        }
    }

    private void readTrips(PublicTransitNetwork network, Set<String> services) {

        Map<String, Trip> trips = new HashMap<>();

        try (InputStream file = getClass().getResourceAsStream("data/" + city.toString().toLowerCase() + "/trips.txt")) {
            List<Map<String, String>> lines = CsvReader.readCsvWithHeader(file, ',');

            for (Map<String, String> line : lines) {

                String id = line.get("trip_id");
                String name = line.get("trip_short_name");
                String headsign = line.get("trip_headsign");
                String serviceId = line.get("service_id");
                int directionId = Integer.parseInt(line.get("direction_id"));
                String routeId = line.get("route_id");

                if (services.contains(serviceId)) {
                    trips.put(id, new Trip(id, name, headsign, directionId, network.getRoutes().get(routeId)));
                }
            }

            System.out.println(String.format("      > Loaded %s trips", trips.size()));

            network.getTrips().putAll(trips);
        } catch (IOException e) {
            System.out.println("Error while loading trips.txt file");
            e.printStackTrace();
        }
    }

    private void readStopTimes(PublicTransitNetwork network,
                               Map<String, Integer> positionToStopPointId) {

        List<Timetable> timetables = new ArrayList<>();

        try (FileInputStream file = new FileInputStream(getClass().getResource("data/" + city.toString().toLowerCase() + "/stop_times.txt").getPath())) {

            try (CSVReader reader = new CSVReader(new InputStreamReader(file, Charset.defaultCharset()))) {
                String[] columnNames = reader.readNext();

                if (columnNames == null) {
                    return;
                }

                CsvReader.readColumnNames(columnNames);

                String[] line;
                while ((line = reader.readNext()) != null) {

                    String tripId = "";
                    String stopId = "";
                    String arrivalTime = "";
                    String departureTime = "";
                    int stopSequence = 0;
                    int pickupType = 0;
                    int dropOffType = 0;

                    for (int ci = 0; ci < line.length; ++ci) {
                        switch (columnNames[ci]) {
                            case "trip_id":
                                tripId = line[ci];
                                break;
                            case "stop_id":
                                stopId = line[ci];
                                break;
                            case "stop_sequence":
                                stopSequence = Integer.parseInt(line[ci]);
                                break;
                            case "departure_time":
                                departureTime = line[ci];
                                break;
                            case "arrival_time":
                                arrivalTime = line[ci];
                                break;
                            // TODO add drop off and pick up type
//                            case "pickup_type":
//                                pickupType = Integer.parseInt(line[ci]);
//                                break;
//                            case "drop_off_type":
//                                dropOffType = Integer.parseInt(line[ci]);
//                                break;
                        }
                    }

                    String[] splitDeparture = departureTime.split(":");
                    int departureSeconds = Integer.parseInt(splitDeparture[2]) + Integer.parseInt(splitDeparture[1]) * 60 + Integer.parseInt(splitDeparture[0]) * 3600;
                    String[] splitArrival = arrivalTime.split(":");
                    int arrivalSeconds = Integer.parseInt(splitArrival[2]) + Integer.parseInt(splitArrival[1]) * 60 + Integer.parseInt(splitArrival[0]) * 3600;

                    if (network.getTrips().containsKey(tripId)) {
                        Trip trip = network.getTrips().get(tripId);
                        Timetable timetable = new Timetable(network.getStopPoints().get(positionToStopPointId.get(stopId)), trip, departureTime, arrivalTime, departureSeconds, arrivalSeconds, stopSequence);
                        timetables.add(timetable);
                        trip.getTimetables().add(timetable);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            System.out.println(String.format("      > Loaded %s timetables", timetables.size()));

            network.getTimetables().addAll(timetables);
        } catch (IOException e) {
            System.out.println("Error while loading stop_times.txt file");
            e.printStackTrace();
        }
    }
}
