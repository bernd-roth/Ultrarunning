package at.co.netconsulting.runningtracker.pojo;

public class FellowRunner {
    private double latitude;
    private double longitude;
    private float distance;
    private double currentSpeed;

    public FellowRunner(double latitude, double longitude, float distance, double currentSpeed) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.currentSpeed = currentSpeed;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }
}