package algorithm;

import com.google.common.base.Stopwatch;

public class KsspResultAndMetrics {

    private int time = Integer.MAX_VALUE;
    private int numberOfResults = 0;
    private int numberOfCsaCalls = 0;

    public KsspResultAndMetrics() {
    }

    public KsspResultAndMetrics(int time, int numberOfResults, int numberOfCsaCalls) {
        this.time = time;
        this.numberOfResults = numberOfResults;
        this.numberOfCsaCalls = numberOfCsaCalls;
    }

    public int getTime() {
        return time;
    }

    public int getNumberOfResults() {
        return numberOfResults;
    }

    public int getNumberOfCsaCalls() {
        return numberOfCsaCalls;
    }
}
