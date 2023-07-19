package at.co.netconsulting.runningtracker.pojo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class Run {
    private int id;
    private String dateTime;
    private double lat;
    private double lng;
    private float speed;
    private int heart_rate;

    public Run() {
    }

    public Run(int id, String dateTime, double lat, double lng, float speed, int heart_rate) {
        this.id = id;
        this.dateTime = dateTime;
        this.lat = lat;
        this.lng = lng;
        this.speed = speed;
        this.heart_rate = heart_rate;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public int getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getHeart_rate() {
        return heart_rate;
    }

    public void setHeart_rate(int heart_rate) {
        this.heart_rate = heart_rate;
    }
}
