package algorithm.kssp;

import java.util.Objects;

public class Leg {

    private int departure;
    private int departureTime;
    private int arrival;
    private int arrivalTime;
    private int trip;
    private boolean isFootpath;

    public Leg(int departure, int departureTime, int arrival, int arrivalTime, int trip, boolean isFootpath) {
        this.departure = departure;
        this.departureTime = departureTime;
        this.arrival = arrival;
        this.arrivalTime = arrivalTime;
        this.trip = trip;
        this.isFootpath = isFootpath;
    }

    public void update(int departure, int departureTime, int arrival, int arrivalTime, int trip, boolean isFootpath) {
        this.departure = departure;
        this.departureTime = departureTime;
        this.arrival = arrival;
        this.arrivalTime = arrivalTime;
        this.trip = trip;
        this.isFootpath = isFootpath;
    }

    public int getDeparture() {
        return departure;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public int getArrival() {
        return arrival;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getTrip() {
        return trip;
    }

    public boolean isFootpath() {
        return isFootpath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Leg leg = (Leg) o;
        return departure == leg.departure && arrival == leg.arrival && trip == leg.trip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(departure, arrival, trip);
    }
}
