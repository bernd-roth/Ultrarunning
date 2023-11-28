package at.co.netconsulting.runningtracker.pojo;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LocationChangeEvent {
    public final List<LatLng> latLngs;
    public LocationChangeEvent(List<LatLng> latLngs) {
        this.latLngs = latLngs;
    }
}
