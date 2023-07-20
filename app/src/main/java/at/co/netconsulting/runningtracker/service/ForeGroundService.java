package at.co.netconsulting.runningtracker.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import at.co.netconsulting.runningtracker.MapsActivity;
import at.co.netconsulting.runningtracker.R;
import at.co.netconsulting.runningtracker.StaticFields;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.Run;

public class ForeGroundService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private String NOTIFICATION_CHANNEL_ID = "com.netconsulting.parkingticket";
    private Notification notification;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager manager;
    private IntentFilter filter;
    private int waitForXMinutes = 10;
    private DatabaseHandler db;
    private Run run;
    private double latitude, longitude;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private Location location;
    private Timer timer;
    float calc;
    float[] result;
    private long minimumSpeedLimit;

    //Polyline
    private ArrayList<LatLng> polylinePoints;

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseHandler(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        loadSharedPreferences(StaticFields.STATIC_STRING_MINIMUM_SPEED_LIMIT);
        startLocationListener();
        LocationManager locationManager = getLocationManager();
        setGPSProviderAsLocationManager(locationManager);
    }

    private void setGPSProviderAsLocationManager(LocationManager locationManager) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            Log.d("TAG: ", "BLA");
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
    }

    private LocationManager getLocationManager() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        Location location = locationManager.getLastKnownLocation(bestProvider);

        if (location != null) {
            Log.e("TAG", "GPS is on");
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        return locationManager;
    }

    private void initObjects() {
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        run = new Run();
        polylinePoints = new ArrayList<>();
        calc = 0;
        result = new float[1];
    }

    private void saveToDatabase() {
//Save input to database
//                run.setLat(latitude);
//                run.setLng(longitude);
//                db.addRun(run);
//                polylinePoints.add(latLng);
    }

    private void startLocationListener() {
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(@NonNull Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();

                //get the location name from latitude and longitude
                LatLng latLng = new LatLng(latitude, longitude);
                polylinePoints.add(latLng);

                //get the speed
                if (location.hasSpeed())
                    Log.d("ForeGroundService: calculateDistance: ", String.valueOf(location.getSpeed()));
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
            public void onProviderEnabled(@NonNull String provider) {
                LocationListener.super.onProviderEnabled(provider);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                LocationListener.super.onProviderDisabled(provider);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initObjects();
        createNotificationChannel();
        PendingIntent stopPendingIntent = createPendingIntent();

        Intent notificationIntent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.notificationBuilder_title))
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_launcher_background);

        notification = notificationBuilder.build();

        startForeground(NOTIFICATION_ID, notification);

        final int[] counter = {1};
        waitForXMinutes*=60;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(counter[0]>waitForXMinutes) {
                    timer.cancel();
                    stopSelfResult(NOTIFICATION_ID);
                } else {
                    if(polylinePoints.size()>1) {
                        calculateDistance();
                    }
                    manager.notify(NOTIFICATION_ID /* ID of notification */,
                            notificationBuilder.setContentTitle("Distance covered: " + calc + " meter").build());
                }
            }
        }, 0,1000);
        return super.onStartCommand(intent, flags, startId);
    }

    private void calculateDistance() {
        LatLng lastEntry = polylinePoints.get(polylinePoints.size()-2);
        double startLat = lastEntry.latitude;
        double startLng = lastEntry.longitude;

//        String oLat = (String) String.format(Locale.ENGLISH, "%.8f", startLat);
//        Double oldDoubleLat = Double.parseDouble(oLat);
        Double oldDoubleLat = startLat;

//        String oLng = (String) String.format(Locale.ENGLISH,"%.8f", startLng);
//        Double oldDoubleLng = Double.parseDouble(oLng);
        Double oldDoubleLng = startLng;

//        String newLat = (String) String.format(Locale.ENGLISH,"%.8f", location.getLatitude());
//        Double newDoubleLat = Double.parseDouble(newLat);
        Double newDoubleLat = location.getLatitude();

//        String newLng = (String) String.format(Locale.ENGLISH,"%.8f", location.getLongitude());
//        Double newDoubleLng = Double.parseDouble(newLng);
        Double newDoubleLng = location.getLongitude();

        if(location.getSpeed()>minimumSpeedLimit) {
            Location.distanceBetween(oldDoubleLat, oldDoubleLng, newDoubleLat, newDoubleLng, result);
            calc += result[0];
            sendBroadcastToMapsActivity(polylinePoints);
        }
    }

    private void sendBroadcastToMapsActivity(ArrayList<LatLng> polylinePoints) {
        Intent intent=new Intent();
        intent.setAction(StaticFields.STATIC_BROADCAST_ACTION);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(StaticFields.STATIC_BROADCAST_ACTION, polylinePoints);
        intent.putExtras(bundle);
        getApplicationContext().sendBroadcast(intent);
    }

    private PendingIntent createPendingIntent() {
        Intent stopIntent = new Intent(this, MapsActivity.class);
        stopIntent.setAction("ACTION.STOPFOREGROUND_ACTION");
        stopIntent.putExtra("EXTRA_NOTIFICATION_ID", 0);
        PendingIntent stopPendingIntent = PendingIntent.getActivity(this,
                0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        return stopPendingIntent;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelNotification();
        timer.cancel();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void cancelNotification() {
        String notificationService = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(notificationService);
        nMgr.cancel(NOTIFICATION_ID);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void loadSharedPreferences(String sharedPrefKey) {
        SharedPreferences sh;

        switch(sharedPrefKey) {
            case StaticFields.STATIC_STRING_MINIMUM_SPEED_LIMIT:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                minimumSpeedLimit = sh.getLong(sharedPrefKey, Double.valueOf(StaticFields.STATIC_DOUBLE_MINIMUM_SPEED_LIMIT).longValue());
                break;
        }
    }
}