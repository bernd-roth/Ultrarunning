package at.co.netconsulting.runningtracker.pojo;

public class Run {
    private int id;
    private String dateTime;
    private double lat;
    private double lng;
    private double meters_covered;
    private float speed;
    private int heart_rate;
    private String comment;
    private int number_of_run;
    private long dateTimeInMs;
    private int laps;
    private double altitude;
    private String person;

    public Run() {
    }

    public Run(int id, String dateTime, double lat, double lng, double meters_covered,
               float speed, int heart_rate, String comment, int number_of_run,
               long dateTimeInMs, int laps, double altitude, String person) {
        this.id = id;
        this.dateTime = dateTime;
        this.lat = lat;
        this.lng = lng;
        this.meters_covered = meters_covered;
        this.speed = speed;
        this.heart_rate = heart_rate;
        this.comment = comment;
        this.number_of_run = number_of_run;
        this.dateTimeInMs = dateTimeInMs;
        this.laps = laps;
        this.altitude = altitude;
        this.person = person;
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

    public double getMeters_covered() {
        return meters_covered;
    }

    public void setMeters_covered(double meters_covered) {
        this.meters_covered = meters_covered;
    }

    public int getId() {
        return id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getNumber_of_run() {
        return number_of_run;
    }

    public void setNumber_of_run(int number_of_run) {
        this.number_of_run = number_of_run;
    }

    public void setLaps(int laps) {
        this.laps = laps;
    }

    public Run(int number_of_run) {
        this.number_of_run = number_of_run;
    }

    public long getDateTimeInMs() {
        return dateTimeInMs;
    }

    public void setDateTimeInMs(long dateTimeInMs) {
        this.dateTimeInMs = dateTimeInMs;
    }

    public int getLaps() {
        return laps;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }
}
