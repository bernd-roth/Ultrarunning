package at.co.netconsulting.runningtracker.pojo;

import com.google.android.gms.maps.model.LatLng;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ColoredPoint {
    public LatLng coords;
    public int color;
}