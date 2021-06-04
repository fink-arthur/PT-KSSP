package network;

public interface Position {

    double getLatitude();

    double getLongitude();

    String getId();

    default double distanceTo(Position other) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(other.getLatitude() - getLatitude());
        double lonDistance = Math.toRadians(other.getLongitude() - getLongitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(getLatitude())) * Math.cos(Math.toRadians(other.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters


        distance = Math.pow(distance, 2);

        return Math.sqrt(distance);
    }
}
