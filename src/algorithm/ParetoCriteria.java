package algorithm;

import algorithm.kssp.Leg;

public class ParetoCriteria {

    private int departureTime;
    private int arrivalTime;
    private int transfers;
    private int walkedDuration;
    private Leg leg;

    public ParetoCriteria() {
        this.departureTime = Integer.MIN_VALUE;
        this.arrivalTime = Tools.MAX_ARRIVAL_TIME;
        this.transfers = Tools.MAX_ARRIVAL_TIME;
        this.walkedDuration = Tools.MAX_ARRIVAL_TIME;
    }

    public ParetoCriteria(int departureTime, int arrivalTime, int transfers, int walkedDuration) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.transfers = transfers;
        this.walkedDuration = walkedDuration;
    }

    public ParetoCriteria(int departureTime, int arrivalTime, int transfers, int walkedDuration, Leg leg) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.transfers = transfers;
        this.walkedDuration = walkedDuration;
        this.leg = leg;
    }

    public void set(int departureTime, int arrivalTime, int transfers, int walkedDuration) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.transfers = transfers;
        this.walkedDuration = walkedDuration;
    }

    public void setLeg(Leg leg) {
        this.leg = leg;
    }

    public boolean dominate(ParetoCriteria other) {

        boolean dominate = false;

        if (this.getDepartureTime() >= other.getDepartureTime() &&
                this.getArrivalTime() <= other.getArrivalTime() &&
                this.getWalkedDuration() <= other.getWalkedDuration() + 60 &&
                this.getTransfers() <= other.getTransfers()) {

            dominate = true;

            if (this.getDepartureTime() == other.getDepartureTime() &&
                    this.getArrivalTime() == other.getArrivalTime() &&
                    this.getWalkedDuration() > other.getWalkedDuration() &&
                    this.getTransfers() == other.getTransfers()) {

                dominate = false;
            }
        }

        return dominate;
    }

    // A function to compare 2 profiles, the goal is to find the "better" one
    // We prefer connections over foot paths in the leg
    public boolean dominateProfile(ParetoCriteria other) {

        boolean dominate = false;

        if (this.getDepartureTime() >= other.getDepartureTime() &&
                this.getArrivalTime() <= other.getArrivalTime()) {

            dominate = true;

            if (this.getDepartureTime() == other.getDepartureTime() &&
                    this.getArrivalTime() == other.getArrivalTime() &&
                    this.getLeg().isFootpath()) {

                dominate = false;
            }
        }

        return dominate;
    }


    public boolean different(ParetoCriteria other) {
        return !(this.getDepartureTime() == other.getDepartureTime() &&
        this.getArrivalTime() == other.getArrivalTime() &&
        this.getWalkedDuration() == other.getWalkedDuration());
    }


    public boolean dominateVehicleJourney(ParetoCriteria other) {

        boolean dominate = false;

        if (this.getDepartureTime() >= other.getDepartureTime() &&
                this.getWalkedDuration() <= other.getWalkedDuration() && // + 60 &&
                this.getTransfers() <= other.getTransfers()) {
            dominate = true;
        }

        return dominate;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getTransfers() {
        return transfers;
    }

    public int getWalkedDuration() {
        return walkedDuration;
    }

    public Leg getLeg() {
        return leg;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    @Override
    public String toString() {
        return "ParetoCriteria{" +
                "departureTime=" + departureTime +
                ", arrivalTime=" + arrivalTime +
                ", transfers=" + transfers +
                ", walkedDuration=" + walkedDuration +
                '}';
    }
}
