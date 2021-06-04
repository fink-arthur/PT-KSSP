package network;

public class Connection {

    private int departureSeconds;
    private int arrivalSeconds;
    private int departurePosition;
    private int arrivalPosition;
    private String tripId;
    private int tripIndex;
    private int sequence;

    public Connection(int departureSeconds, int arrivalSeconds, int departurePosition, int arrivalPosition, String tripId, int tripIndex, int sequence) {
        this.departureSeconds = departureSeconds;
        this.arrivalSeconds = arrivalSeconds;
        this.departurePosition = departurePosition;
        this.arrivalPosition = arrivalPosition;
        this.tripId = tripId;
        this.tripIndex = tripIndex;
        this.sequence = sequence;
    }

    public int getArrivalSeconds() {
        return arrivalSeconds;
    }

    public int getDeparturePosition() {
        return departurePosition;
    }

    public int getArrivalPosition() {
        return arrivalPosition;
    }

    public String getTripId() {
        return tripId;
    }

    public int getTripIndex() {
        return tripIndex;
    }

    public int getDepartureSeconds() {
        return this.departureSeconds;
    }

    public int getSequence() {
        return this.sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }
}
