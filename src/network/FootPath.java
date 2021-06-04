package network;

public class FootPath {

    private StopPoint from;
    private StopPoint to;
    private int duration;

    public FootPath(StopPoint from, StopPoint to, int duration) {
        this.from = from;
        this.to = to;
        this.duration = duration;
    }

    public static FootPath getEmptyFootPath() {
        return new FootPath(null, null, -1);
    }

    public StopPoint getFrom() {
        return from;
    }

    public StopPoint getTo() {
        return to;
    }

    public int getDuration() {
        return duration;
    }
}
