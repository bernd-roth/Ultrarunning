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
    private String NOTIFICATION_CHANNEL_ID = "co.at.netconsulting.parkingticket",
            person, bundlePause, formattedDateTime;
    private Notification notification;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager manager;
    private DatabaseHandler db;
    private Run run;
    private double currentLatitude,
            currentLongitude,
            altitude,
            oldLatitude,
            oldLongitude;
    private LocationManager locationManager;
    private float[] result;
    //private ArrayList<LatLng> polylinePoints;
    private DateTimeFormatter formatDateTime;
    private LocalDateTime dateObj;
    private long currentMilliseconds, oldCurrentMilliseconds = 0, minTimeMs;
    private final long[] seconds = {0}, minutes = {0}, hours = {0};
    private Timer timer;
    private BroadcastReceiver broadcastReceiver;
    private boolean isFirstEntry;
    private int laps, satelliteCount, minDistanceMeter, numberOfsatellitesInUse, lastRun;
    private float lapCounter, calc, accuracy, currentSpeed;
    private IntentFilter filter;
    private Intent intent;
    private Bundle bundle;
    private String notificationService;
    private NotificationManager nMgr;
    private NotificationChannel serviceChannel;
    private GnssStatus.Callback gnssCallback;
    protected WatchDogRunner mWatchdogRunner;
    protected Thread mWatchdogThread = null;

    @Override
    public void onCreate() {
        super.onCreate();
        initObjects();
        createNotificationChannel();
        initializeWatchdog();
        getLastKnownLocation(locationManager);
        initCallbacks();

        lastRun = db.getLastEntry();
        lastRun += 1;

        loadSharedPreferences(SharedPref.STATIC_STRING_MINIMUM_SPEED_LIMIT);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_PERSON);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_SHOW_DISTANCE_COVERED);

        configureBroadcastReceiver();
    }

    private void initializeWatchdog() {
        if (mWatchdogThread == null || !mWatchdogThread.isAlive()) {
            mWatchdogRunner = new WatchDogRunner();
            mWatchdogThread = new Thread(mWatchdogRunner, "WorkoutWatchdog");
        }
        if (!mWatchdogThread.isAlive()) {
            mWatchdogThread.start();
        }
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
        isFirstEntry = true;
        run = new Run();
        //polylinePoints = new ArrayList<>();
        calc = 0;
        result = new float[1];
        dateObj = LocalDateTime.now();
        formatDateTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        timer = new Timer();
        laps=0;
        lapCounter=0;
        filter = new IntentFilter();
        broadcastReceiver = new DataBroadcastReceiver();
        db = new DatabaseHandler(this);
        intent=new Intent();
        bundle = new Bundle();
        locationManager = getLocationManager();
    }

    private void configureBroadcastReceiver() {
        filter.addAction(SharedPref.STATIC_BROADCAST_PAUSE_ACTION);
        registerReceiver(broadcastReceiver, filter);
    }
    private void saveToDatabase(double latitude, double longitude) {
        //format date and time
        dateObj = LocalDateTime.now();
        formattedDateTime = dateObj.format(formatDateTime);
        currentMilliseconds = System.currentTimeMillis();

        run.setDateTime(dateObj.format(formatDateTime));
        run.setLat(latitude);
        run.setLng(longitude);
        run.setNumber_of_run(lastRun);
        run.setMeters_covered(calc);
        run.setSpeed(currentSpeed);
        run.setDateTimeInMs(currentMilliseconds);
        run.setLaps(laps);
        run.setAltitude(altitude);
        run.setPerson(person);
        db.addRun(run);
        //saveToFirebase(run);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createPendingIntent();

        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    public void initCallbacks() {
        gnssCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                satelliteCount = status.getSatelliteCount();
                int usedSatellites = 0;
                for (int i = 0; i < satelliteCount; ++i) {
                    if (status.usedInFix(i)) {
                        ++usedSatellites;
                    }
                }
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

    private void calculateDistance(double oldLatitude, double oldLongitude, double currentLatitude, double currentLongitude) {
        Location.distanceBetween(
                //older location
                oldLatitude,
                oldLongitude,
                //current location
                currentLatitude,
                currentLongitude,
                result);
        calc += result[0];
        lapCounter += result[0];
        calculateLaps();
    }

    private void calculateLaps() {
        if(lapCounter>=1000) {
            laps+=1;
            lapCounter=0;
        }
    }

    private void sendBroadcastToMapsActivity(double oldLatitude, double oldLongitude, double currentLatitude, double currentLongitude) {
        intent.setAction(SharedPref.STATIC_BROADCAST_ACTION);
        bundle.putString("SPEED", String.valueOf(currentSpeed));
        bundle.putFloat("DISTANCE", calc);
        //old
        bundle.putDouble("OLD-LAT", oldLatitude);
        bundle.putDouble("OLD-LON", oldLongitude);
        //new
        bundle.putDouble("CURRENT-LAT", currentLatitude);
        bundle.putDouble("CURRENT-LON", currentLongitude);

        bundle.putString("SPEED", String.valueOf(currentSpeed));
        bundle.putFloat("DISTANCE", calc);

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
        mWatchdogRunner.stop();
        cancelNotification();
        locationManager.removeUpdates(this);
//        polylinePoints.clear();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        timer.cancel();
        deinitCallbacks();
        unregisterReceiver(broadcastReceiver);
    }

    private void cancelNotification() {
        notificationService = Context.NOTIFICATION_SERVICE;
        nMgr = (NotificationManager) getApplicationContext().getSystemService(notificationService);
        nMgr.cancel(NOTIFICATION_ID);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannel = new NotificationChannel(
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
                minTimeMs = sh.getLong(sharedPrefKey, (long) StaticFields.STATIC_LONG_MIN_TIME_MS);
                break;
            case SharedPref.STATIC_SHARED_PREF_PERSON:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                person = sh.getString(sharedPrefKey, StaticFields.STATIC_STRING_PERSON);
                break;
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentMilliseconds = System.currentTimeMillis();

        if(isFirstEntry) {
            oldLatitude = location.getLatitude();
            oldLongitude = location.getLongitude();
        } else {
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
        }

        //number of satellites
        numberOfsatellitesInUse = location.getExtras().getInt("satellites");

        altitude = location.getAltitude();
        accuracy = location.getAccuracy();
        currentSpeed = (location.getSpeed() / 1000) * 3600;

        //pause button was not pressed yet
        if(bundlePause==null) {
            if(minDistanceMeter==1 && minTimeMs==1) {
                if (currentMilliseconds != oldCurrentMilliseconds) {
//                    if(currentSpeed>0) {
                        if(isFirstEntry) {
                            //saveToDatabase(oldLatitude, oldLongitude);
                            isFirstEntry = false;
                        } else {
                            calculateDistance(oldLatitude, oldLongitude, currentLatitude, currentLongitude);
                            //saveToDatabase(currentLatitude, currentLongitude);
                            oldLatitude = currentLatitude;
                            oldLongitude = currentLongitude;
                            sendBroadcastToMapsActivity(oldLatitude, oldLongitude, currentLatitude, currentLongitude);
                        }
                        oldCurrentMilliseconds = currentMilliseconds;
                }
            } else {
                calculateDistance(oldLatitude, oldLongitude, currentLatitude, currentLongitude);
                //saveToDatabase(currentLatitude, currentLongitude);
                oldLatitude = currentLatitude;
                oldLongitude = currentLongitude;
            }
        }
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
            bundlePause = intent.getExtras().getString("Pausing");
            Timber.d("Foregrundservice: DataBroadcastReceiver: %s", bundlePause);
        }
    }

    private class WatchDogRunner implements Runnable {
            boolean running = true;
            @Override
            public void run() {
                running = true;
                try {
                    while (running) {
                        updateNotification();
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            public void stop() {
                running = false;
            }
        }
    private void updateNotification() {
        long hour = hours[0];
        long minute = minutes[0];
        long second = seconds[0];

        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Still trying to gather information!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Distance covered: 0.00 Km"
                                + "\nCurrent speed: 0 Km/h"
                                + "\nNumber of satellites: 0/" + satelliteCount
                                + "\nLocation accuracy: 0 Meter"
                                + "\nAltitude: 0 Meter"
                                + "\nLaps: " + String.format("%03d", laps)
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

        seconds[0] += 1;
        if (seconds[0] == 60) {
            minutes[0] += 1;
            seconds[0] = 0;
            if (minutes[0] == 60) {
                hours[0] += 1;
                minutes[0] = 0;
            }
        }

        manager.notify(NOTIFICATION_ID, notificationBuilder
                .setContentTitle("Distance covered: " + String.format("%.2f Km", calc / 1000))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Current speed: " + String.format("%.2f", currentSpeed) + " Km/h"
                                + "\nNumber of satellites: " + numberOfsatellitesInUse + "/" + satelliteCount
                                + "\nLocation accuracy: " + String.format("%.2f Meter", accuracy)
                                + "\nAltitude: " + String.format("%.2f Meter", altitude)
                                + "\nLaps: " + String.format("%03d", laps)
                                + "\nTime: " + String.format("%02d:%02d:%02d", hours[0], minutes[0], seconds[0])))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_notification))
                .build());
    }
}