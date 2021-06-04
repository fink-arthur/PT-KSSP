package network;

public class Timetable {

    private StopPoint stopPoint;
    private Trip trip;
    private String departureDate;
    private String arrivalDate;
    private int departureSeconds;
    private int arrivalSeconds;
    private int sequence;
    private Timetable previousTimetable;
    private Timetable nextTimetable;
    private int dropOffType;
    private int pickIpType;

    // TODO use drop off and pick up type

    public Timetable(StopPoint stopPoint, Trip trip, String departureDate, String arrivalDate, int departureSeconds, int arrivalSeconds, int sequence) {
        this.stopPoint = stopPoint;
        this.trip = trip;
        this.departureDate = departureDate;
        this.arrivalDate = arrivalDate;
        this.departureSeconds = departureSeconds;
        this.arrivalSeconds = arrivalSeconds;
        this.sequence = sequence;
    }

    public StopPoint getStopPoint() {
        return stopPoint;
    }

    public Trip getTrip() {
        return trip;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public String getArrivalDate() {
        return arrivalDate;
    }

    public int getDepartureSeconds() {
        return departureSeconds;
    }

    public int getArrivalSeconds() {
        return arrivalSeconds;
    }

    public int getSequence() {
        return sequence;
    }

    public Timetable getPreviousTimetable() {
        return previousTimetable;
    }

    public void setPreviousTimetable(Timetable previousTimetable) {
        this.previousTimetable = previousTimetable;
    }

    public Timetable getNextTimetable() {
        return nextTimetable;
    }

    public void setNextTimetable(Timetable nextTimetable) {
        this.nextTimetable = nextTimetable;
    }

    public int getDropOffType() {
        return dropOffType;
    }

    public int getPickIpType() {
        return pickIpType;
    }
}
