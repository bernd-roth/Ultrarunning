package at.co.netconsulting.runningtracker.pojo;

import android.location.Location;

public class LocationChangeEvent {
    public final Location location;
    public final float coveredDistance;
    public LocationChangeEvent(Location location, float coveredDistance) {
        this.location = location;
        this.coveredDistance = coveredDistance;
    }
}
