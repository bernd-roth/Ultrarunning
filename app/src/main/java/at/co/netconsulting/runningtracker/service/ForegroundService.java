package at.co.netconsulting.runningtracker.service;

import static android.location.LocationManager.GPS_PROVIDER;

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
import android.graphics.BitmapFactory;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import at.co.netconsulting.runningtracker.MapsActivity;
import at.co.netconsulting.runningtracker.R;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;
import at.co.netconsulting.runningtracker.pojo.Run;
import timber.log.Timber;

public class ForegroundService extends Service implements LocationListener {
    private static final int NOTIFICATION_ID = 1;
    private String NOTIFICATION_CHANNEL_ID = "co.at.netconsulting.parkingticket";
    private Notification notification;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager manager;
    private DatabaseHandler db;
    private Run run;
    private double currentLatitude, currentLongitude;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private float calc;
    private float[] result;
    private ArrayList<LatLng> polylinePoints;
    private DateTimeFormatter formatDateTime;
    private LocalDateTime dateObj;
    private String formattedDateTime;
    private int lastRun;
    private LatLng latLng;
    private int minDistanceMeter;
    private long minTimeMs;
    private float speed;
    private double altitude;
    private float accuracy, currentSpeed;
    private long currentMilliseconds;
    private final long[] seconds = {0};
    private final long[] minutes = {0};
    private final long[] hours = {0};
    private Timer t;
    private int satelliteCount;
    private BroadcastReceiver broadcastReceiver;
    private String bundlePause;
    private boolean isCommentOnPause;
    private int laps;
    private int numberOfsatellitesInUse;

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseHandler(this);
        lastRun = db.getLastEntry();
        lastRun += 1;

        loadSharedPreferences(SharedPref.STATIC_STRING_MINIMUM_SPEED_LIMIT);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE);

        locationManager = getLocationManager();
        getLastKnownLocation(locationManager);
        configureBroadcastReceiver();
    }

    private void getLastKnownLocation(LocationManager locationManager) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(GPS_PROVIDER, minTimeMs * 1000, (float) minDistanceMeter, this);
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
        dateObj = LocalDateTime.now();
        formatDateTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        t = new Timer();
        laps=0;
        initCallbacks();
    }

    private void configureBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SharedPref.STATIC_BROADCAST_PAUSE_ACTION);
        broadcastReceiver = new DataBroadcastReceiver();
        registerReceiver(broadcastReceiver, filter);
    }

    //Save input to database
    private void saveToDatabase() {
        //format date and time
        dateObj = LocalDateTime.now();
        formattedDateTime = dateObj.format(formatDateTime);
        currentMilliseconds = System.currentTimeMillis();

        //save all entries from polyline to table now
        for (int i = 0; i < polylinePoints.size(); i++) {
            run.setDateTime(dateObj.format(formatDateTime));
            run.setLat(currentLatitude);
            run.setLng(currentLongitude);
            run.setNumber_of_run(lastRun);
            run.setMeters_covered(calc);
            run.setSpeed(speed);
            run.setDateTimeInMs(currentMilliseconds);
            run.setLaps(laps);
            db.addRun(run);
        }
    }

    //Save input to database
    private void saveToDatabaseWithComment(String commentOnPause) {
        //format date and time
        dateObj = LocalDateTime.now();
        formattedDateTime = dateObj.format(formatDateTime);
        currentMilliseconds = System.currentTimeMillis();

        //save all entries from polyline to table now
        for (int i = 0; i < polylinePoints.size(); i++) {
            run.setDateTime(dateObj.format(formatDateTime));
            run.setLat(currentLatitude);
            run.setLng(currentLongitude);
            run.setNumber_of_run(lastRun);
            run.setMeters_covered(calc);
            run.setSpeed(speed);
            run.setDateTimeInMs(currentMilliseconds);
            run.setComment(commentOnPause);
            run.setLaps(laps);
            db.addRun(run);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initObjects();
        createNotificationChannel();
        createPendingIntent();

        long hour = hours[0];
        long minute = minutes[0];
        long second = seconds[0];

        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Still trying to gather information!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Distance covered: 0.00 Km"
                                + "\nCurrent speed: 0 Km/h"
                                + "\nNumber of satellites: 0/" + satelliteCount
                                + "\nLocation accuracy: 0 m"
                                + "\nAltitude: 0 Meter"
                                + "\nTime: " + String.format("%02d:%02d:%02d", hour, minute, second)))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.icon_notification))
                //.setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.icon_notification)
                //show notification on home screen to everyone
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //without FOREGROUND_SERVICE_IMMEDIATE, notification can take up to 10 secs to be shown
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        notification = notificationBuilder.build();

        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                seconds[0] += 1;
                if (seconds[0] == 60) {
                    minutes[0] += 1;
                    seconds[0] = 0;
                    if (minutes[0] == 60) {
                        hours[0] += 1;
                        minutes[0] = 0;
                    }
                }

                manager.notify(NOTIFICATION_ID /* ID of notification */, notificationBuilder
                        .setContentTitle("Distance covered: " + String.format("%.2f Km", calc/1000))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Current speed: " + String.format("%.2f", currentSpeed) + " Km/h"
                                        + "\nNumber of satellites: " + numberOfsatellitesInUse + "/" + satelliteCount
                                        + "\nLocation accuracy: " + String.format("%.2f", accuracy)
                                        + "\nAltitude: " + String.format("%.2f Meter", altitude)
                                        + "\nLaps: " + String.format("%03d Meter", laps)
                                        + "\nTime: " + String.format("%02d:%02d:%02d", hours[0], minutes[0], seconds[0])))
                        .setLargeIcon(BitmapFactory. decodeResource (getResources() , R.drawable. icon_notification ))
                        .build());

            }
        }, 0, 1000);

        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    private GnssStatus.Callback gnssCallback;

    public void initCallbacks() {
        gnssCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                satelliteCount = status.getSatelliteCount();
                int usedCount = 0;
                for (int i = 0; i < satelliteCount; ++i)
                    if (status.usedInFix(i))
                        ++usedCount;
                Timber.d("Total number of satellites: %s", satelliteCount);
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.registerGnssStatusCallback(gnssCallback, new Handler(Looper.myLooper()));
    }

    public void deinitCallbacks() {
        locationManager.unregisterGnssStatusCallback(gnssCallback);
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
        if(calc>1000) {
            laps+=1;
        }
        sendBroadcastToMapsActivity(polylinePoints);
    }

    private void sendBroadcastToMapsActivity(ArrayList<LatLng> polylinePoints) {
        Intent intent=new Intent();
        intent.setAction(SharedPref.STATIC_BROADCAST_ACTION);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(SharedPref.STATIC_BROADCAST_ACTION, polylinePoints);
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
        locationManager.removeUpdates(this);
        polylinePoints.clear();
        latLng = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        t.cancel();
        deinitCallbacks();
        unregisterReceiver(broadcastReceiver);
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
            case SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                minDistanceMeter = sh.getInt(sharedPrefKey, StaticFields.STATIC_INTEGER_MIN_DISTANCE_METER);
                break;
            case SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                minTimeMs =  sh.getLong(sharedPrefKey, (long) StaticFields.STATIC_LONG_MIN_TIME_MS);
                break;
            case SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isCommentOnPause =  sh.getBoolean(sharedPrefKey, Boolean.parseBoolean(StaticFields.STATIC_SAVE_ON_COMMENT_PAUSE));
                break;
        }
    }

    //methods to implement
    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        //get the location name from latitude and longitude
        latLng = new LatLng(currentLatitude, currentLongitude);

        polylinePoints.add(latLng);

        //get speed
        speed = (location.getSpeed() / 1000) * 3600;

        //number of satellites
        numberOfsatellitesInUse = location.getExtras().getInt("satellites");

        if (polylinePoints.size() > 1) {
            calculateDistance();
        }

        altitude = location.getAltitude();
        accuracy = location.getAccuracy();
        currentSpeed = (location.getSpeed() / 1000) * 3600;

        //pause button was not pressed yet
        if(bundlePause==null) {
            saveToDatabase();
        } else {
            if(isCommentOnPause) {
                saveToDatabaseWithComment("Paused");
            }
        }
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

    private class DataBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            bundlePause = intent.getExtras().getString("Pausing");
            Timber.d("Foregrundservice: DataBroadcastReceiver: %s", bundlePause);
        }
    }
}