package at.co.netconsulting.runningtracker.service;

import static android.location.LocationManager.GPS_PROVIDER;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.location.LocationListener;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import at.co.netconsulting.runningtracker.MapsActivity;
import at.co.netconsulting.runningtracker.R;
import at.co.netconsulting.runningtracker.StaticFields;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.Run;

public class ForegroundService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private String NOTIFICATION_CHANNEL_ID = "co.at.netconsulting.parkingticket";
    private Notification notification;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager manager;
    private int waitForXMinutes = 10;
    private DatabaseHandler db;
    private Run run;
    private double currentLatitude, currentLongitude;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private Location currentLocation;
    private Timer timer;
    private float calc;
    private float[] result;
    private float minimumSpeedLimit;
    //Polyline
    private ArrayList<LatLng> polylinePoints;
    private DateTimeFormatter formatDateTime;
    private LocalDateTime dateObj;
    private String formattedDateTime;
    private int lastRun;
    private LatLng latLng;
    private int minDistanceMeter;
    private long minTimeMs;

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseHandler(this);
        lastRun = db.getLastEntry();
        lastRun += 1;
        locationListener = new MyLocationListener();

        loadSharedPreferences(StaticFields.STATIC_STRING_MINIMUM_SPEED_LIMIT);
        loadSharedPreferences(StaticFields.STATIC_SHARED_PREF_LONG_MIN_DISTANCE_METER);
        loadSharedPreferences(StaticFields.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
        //startLocationListener();
        locationManager = getLocationManager();
        getLastKnownLocation(locationManager);
    }

    private void getLastKnownLocation(LocationManager locationManager) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(GPS_PROVIDER, minTimeMs * 1000, (float) minDistanceMeter, locationListener, Looper.getMainLooper());
        currentLocation = locationManager.getLastKnownLocation(GPS_PROVIDER);
    }

    private LocationManager getLocationManager() {
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        return locationManager;
    }

    private void initObjects() {
        run = new Run();
        polylinePoints = new ArrayList<>();
        calc = 0;
        result = new float[1];
        timer = new Timer();
        dateObj = LocalDateTime.now();
        formatDateTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    }

    //Save input to database
    private void saveToDatabase() {
        //format date and time
        dateObj = LocalDateTime.now();
        formattedDateTime = dateObj.format(formatDateTime);

        //save all entries from polyline to table now
        for(int i = 0; i<polylinePoints.size(); i++) {
            run.setDateTime(dateObj.format(formatDateTime));
            run.setLat(currentLatitude);
            run.setLng(currentLongitude);
            run.setNumber_of_run(lastRun);
            run.setMeters_covered(calc);
            db.addRun(run);
        }

        //List<Run> l = db.getAllEntries();
        //for(int i = 0;i<l.size();i++)
        //    Log.d("Entries", "ID: " + l.get(i).getId() + " Lat: " + l.get(i).getLat() + " Lon: " + l.get(i).getLng() + " Speed: " + l.get(i).getSpeed() + " Date: " + l.get(i).getDateTime());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initObjects();
        createNotificationChannel();
        //PendingIntent stopPendingIntent = createPendingIntent();
        createPendingIntent();

        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.notificationBuilder_title))
                //.setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                //show notification on home screen to everyone
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //without FOREGROUND_SERVICE_IMMEDIATE, notification can take up to 10 secs to be shown
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        notification = notificationBuilder.build();

        startForeground(NOTIFICATION_ID, notification);

        final int[] counter = {1};
        waitForXMinutes *= 60;

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                currentLatitude = currentLocation.getLatitude();
                currentLongitude = currentLocation.getLongitude();
                polylinePoints.add(new LatLng(currentLatitude, currentLongitude));
                getLastKnownLocation(locationManager);
                if (polylinePoints.size() > 1) {
                    calculateDistance();
                }
                manager.notify(NOTIFICATION_ID /* ID of notification */,
                        notificationBuilder
                                .setContentTitle("Distance covered: " + String.format("%.2f meter", calc))
                                .setStyle(new NotificationCompat.BigTextStyle()
                                        .bigText("Current speed: " + (currentLocation.getSpeed()/1000)*3600 + " km/h"
                                                + "\nNumber of satellites: " + currentLocation.getExtras().getInt("satellites")
                                                + "\nLocation accuraccy: " + currentLocation.getExtras().getInt(String.valueOf(currentLocation.getAccuracy()))))
                                .build());
            }
        }, 0, 100);
        return START_STICKY;
    }

    private void calculateDistance() {
        //one location older than current location
        LatLng penultimatelastEntry = polylinePoints.get(polylinePoints.size()-2);
        double oldDoubleLat = penultimatelastEntry.latitude;
        double oldDoubleLng = penultimatelastEntry.longitude;

        //current location
        LatLng lastEntry = polylinePoints.get(polylinePoints.size()-1);
        double newDoubleLat = lastEntry.latitude;
        double newDoubleLng = lastEntry.longitude;

        Location.distanceBetween(oldDoubleLat, oldDoubleLng, newDoubleLat, newDoubleLng, result);
        calc += result[0];
        sendBroadcastToMapsActivity(polylinePoints);
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
        timer.cancel();
        cancelNotification();
        saveToDatabase();
        polylinePoints.clear();
        latLng = null;
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
                minimumSpeedLimit = sh.getFloat(sharedPrefKey, Double.valueOf(StaticFields.STATIC_DOUBLE_MINIMUM_SPEED_LIMIT).floatValue());
                break;
            case StaticFields.STATIC_SHARED_PREF_LONG_MIN_DISTANCE_METER:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                minDistanceMeter = sh.getInt(sharedPrefKey, StaticFields.STATIC_SHARED_PREF_FLOAT_MIN_DISTANCE_METER_DEFAULT);
                break;
            case StaticFields.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                minTimeMs = sh.getLong(sharedPrefKey, StaticFields.STATIC_SHARED_PREF_LONG_MIN_TIME_MS_DEFAULT);
                break;
        }
    }
}