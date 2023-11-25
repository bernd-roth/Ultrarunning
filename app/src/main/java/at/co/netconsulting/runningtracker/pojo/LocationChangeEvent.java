package at.co.netconsulting.runningtracker.pojo;

import android.location.Location;

public class LocationChangeEvent {
    public final Location location;
    public LocationChangeEvent(Location location) {
        this.location = location;
    }
}
