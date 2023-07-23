package at.co.netconsulting.runningtracker.service;

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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.location.LocationListener;
import com.google.android.gms.maps.model.LatLng;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private double latitude, longitude;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private Location location;
    private Timer timer;
    private float calc;
    private float speed;
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
        lastRun+=1;

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
        loadSharedPreferences(StaticFields.STATIC_SHARED_PREF_LONG_MIN_DISTANCE_METER);
        loadSharedPreferences(StaticFields.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
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
            Log.d("LOCATION: ", "Location is null");
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs*1000, (float) minDistanceMeter, locationListener);
    }

    private LocationManager getLocationManager() {
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setSpeedRequired(true);
        //TODO: take the best provider based on user input or device
        String bestProvider = locationManager.getBestProvider(criteria, true);
        bestProvider="gps";
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
            Log.i("GPS: ", "GPS is on");
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        return locationManager;
    }

    private void initObjects() {
        run = new Run();
        polylinePoints = new ArrayList<>();
        calc = 0;
        result = new float[1];
        speed = 0;
        timer = new Timer();
        dateObj = LocalDateTime.now();
        formatDateTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    }

    //Save input to database
    private void saveToDatabase() {
        //format date and time
        dateObj = LocalDateTime.now();
        formattedDateTime = dateObj.format(formatDateTime);

        run.setDateTime(dateObj.format(formatDateTime));
        run.setLat(latitude);
        run.setLng(longitude);
        run.setNumber_of_run(lastRun);
        run.setMeters_covered(calc);
        db.addRun(run);
    }

    private void startLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();

                //get the location name from latitude and longitude
                latLng = new LatLng(latitude, longitude);
                polylinePoints.add(latLng);

                //get speed
                speed = (location.getSpeed()/1000)*3600;
                Log.d("SPEED", String.valueOf(location.getSpeed()));

                //number of satellites
                Log.d("NUMBER OF SATELLITES", String.valueOf(location.getExtras().getInt("satellites")));
            }
        };
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
        waitForXMinutes*=60;

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(counter[0]>waitForXMinutes) {
                    timer.cancel();
                    stopSelfResult(NOTIFICATION_ID);
                } else {
                    saveToDatabase();
                    if(polylinePoints.size()>1) {
                        calculateDistance();
                    }
                    manager.notify(NOTIFICATION_ID /* ID of notification */,
                            notificationBuilder
                                    .setContentTitle("Distance covered: " + String.format("%.2f meter", calc))
                                    //.setContentText("Current speed: " + speed + " km/h | Number of satellites: " + location.getExtras().getInt("satellites"))
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText("Current speed: " + speed + " km/h"
                                            + "\nNumber of satellites: " + location.getExtras().getInt("satellites")
                                            + "\nLocation accuraccy: " + location.getExtras().getInt(String.valueOf(location.getAccuracy()))))
                                    .build());
                }
            }
        }, 0,100);
        return START_STICKY;
    }

    private void calculateDistance() {
        LatLng penultimatelastEntry = polylinePoints.get(polylinePoints.size()-2);
        double startLat = penultimatelastEntry.latitude;
        double startLng = penultimatelastEntry.longitude;

        Double oldDoubleLat = startLat;
        Double oldDoubleLng = startLng;

        LatLng lastEntry = polylinePoints.get(polylinePoints.size()-1);
        double newDoubleLat = lastEntry.latitude;
        double newDoubleLng = lastEntry.longitude;

//        if(speed>minimumSpeedLimit) {
        Location.distanceBetween(oldDoubleLat, oldDoubleLng, newDoubleLat, newDoubleLng, result);
        calc += result[0];
        saveToDatabase();
        sendBroadcastToMapsActivity(polylinePoints);
//        }
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