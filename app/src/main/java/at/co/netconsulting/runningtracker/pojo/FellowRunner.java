package at.co.netconsulting.runningtracker.pojo;

public class FellowRunner {
    private double latitude;
    private double longitude;
    private float distance;
    private double currentSpeed;
    private String sessionId;
    private String formattedTimestamp;

    public FellowRunner(String sessionId, double latitude, double longitude, float distance, double currentSpeed, String formattedTimestamp) {
        this.sessionId = sessionId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.currentSpeed = currentSpeed;
        this.formattedTimestamp = formattedTimestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getFormattedTimestamp() {
        return formattedTimestamp;
    }

    public void setFormattedTimestamp(String formattedTimestamp) {
        this.formattedTimestamp = formattedTimestamp;
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