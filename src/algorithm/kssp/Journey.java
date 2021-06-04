package algorithm.kssp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Journey {

    private List<Leg> legs = new ArrayList<>();
    private int arrivalTime = 0;
    private int deviationIndex = 0;

    public Journey() {

    }

    public List<Leg> getLegs() {
        return legs;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getDeviationIndex() {
        return deviationIndex;
    }

    public void setDeviationIndex(int deviationIndex) {
        this.deviationIndex = deviationIndex;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Journey journey = (Journey) o;
        return arrivalTime == journey.arrivalTime && Objects.equals(legs, journey.legs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(legs, arrivalTime);
    }
}
