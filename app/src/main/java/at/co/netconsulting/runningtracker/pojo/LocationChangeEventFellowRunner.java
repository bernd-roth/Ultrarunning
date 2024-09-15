package at.co.netconsulting.runningtracker.pojo;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class LocationChangeEventFellowRunner {
    public final List<LatLng> latLngs;
    public LocationChangeEventFellowRunner(List<LatLng> latLngs) {
        this.latLngs = latLngs;
    }
}
