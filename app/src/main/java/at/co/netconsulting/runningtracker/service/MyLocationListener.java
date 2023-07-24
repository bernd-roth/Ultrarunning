package at.co.netconsulting.runningtracker.service;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import java.util.List;

public class MyLocationListener implements LocationListener {
    private LocationListener locationListener;

    @Override
    public void onLocationChanged(@NonNull Location location) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
//                latitude = location.getLatitude();
//                longitude = location.getLongitude();
//
//                //get the location name from latitude and longitude
//                latLng = new LatLng(latitude, longitude);
//                polylinePoints.add(latLng);
//
//                //get speed
//                speed = (location.getSpeed()/1000)*3600;
//                Log.d("SPEED", String.valueOf(location.getSpeed()));
//
//                //number of satellites
//                Log.d("NUMBER OF SATELLITES", String.valueOf(location.getExtras().getInt("satellites")));
                }
            };
        }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {
        LocationListener.super.onLocationChanged(locations);
    }

    @Override
    public void onFlushComplete(int requestCode) {
        LocationListener.super.onFlushComplete(requestCode);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }
}
