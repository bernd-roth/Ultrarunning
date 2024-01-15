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
import android.speech.tts.TextToSpeech;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.maps.model.LatLng;
import org.greenrobot.eventbus.EventBus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import at.co.netconsulting.runningtracker.MapsActivity;
import at.co.netconsulting.runningtracker.R;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;
import at.co.netconsulting.runningtracker.pojo.LocationChangeEvent;
import at.co.netconsulting.runningtracker.pojo.Run;

public class ForegroundService extends Service implements LocationListener {
    private static final int NOTIFICATION_ID = 1;
    private String NOTIFICATION_CHANNEL_ID = "co.at.netconsulting.runningtracker",
            person;
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
    private DateTimeFormatter formatDateTime;
    private LocalDateTime dateObj;
    private long currentMilliseconds, oldCurrentMilliseconds = 0, currentSeconds, minTimeMs;
    private Timer timer;
    private boolean isFirstEntry, hasEnoughTimePassed, isVoiceMessage;
    private int laps, satelliteCount, minDistanceMeter, numberOfsatellitesInUse, lastRun;
    private float lapCounter, coveredDistance, accuracy, currentSpeed;
    private String notificationService;
    private NotificationManager nMgr;
    private NotificationChannel serviceChannel;
    private GnssStatus.Callback gnssCallback;
    protected WatchDogRunner mWatchdogRunner;
    protected Thread mWatchdogThread = null;
    private Location mLocation;
    private Instant starts, ends;
    private TextToSpeech tts;

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

        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_PERSON);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_SHOW_DISTANCE_COVERED);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE);
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
        coveredDistance = 0;
        result = new float[1];
        dateObj = LocalDateTime.now();
        formatDateTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        timer = new Timer();
        laps=0;
        lapCounter=0;
        db = new DatabaseHandler(this);
        locationManager = getLocationManager();
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });
    }
    private void saveToDatabase(double latitude, double longitude) {
        //format date and time
        dateObj = LocalDateTime.now();
        currentMilliseconds = System.currentTimeMillis();

        run.setDateTime(dateObj.format(formatDateTime));
        run.setLat(latitude);
        run.setLng(longitude);
        run.setNumber_of_run(lastRun);
        run.setMeters_covered(coveredDistance);
        run.setSpeed(currentSpeed);
        run.setDateTimeInMs(currentMilliseconds);
        run.setLaps(laps);
        run.setAltitude(altitude);
        run.setPerson(person);
        db.addRun(run);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createPendingIntent();

        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.gather_information))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.icon_notification))
                //.setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.icon_notification)
                //show notification on home screen to everyone
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //without FOREGROUND_SERVICE_IMMEDIATE, notification can take up to 10 secs to be shown
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        notification = notificationBuilder.build();

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
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.registerGnssStatusCallback(gnssCallback, new Handler(Looper.myLooper()));
    }

    public void deinitCallbacks() {
        locationManager.unregisterGnssStatusCallback(gnssCallback);
    }

    private float calculateDistance(double oldLatitude, double oldLongitude, double currentLatitude, double currentLongitude) {
        Location.distanceBetween(
                //older location
                oldLatitude,
                oldLongitude,
                //current location
                currentLatitude,
                currentLongitude,
                result);
        coveredDistance += result[0];
        return result[0];
    }

    private void calculateLaps(float pResult) {
        lapCounter += pResult;
        if(lapCounter>=1000) {
            laps+=1;
            lapCounter=0;
        }
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
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        timer.cancel();
        deinitCallbacks();
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
            case SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isVoiceMessage = sh.getBoolean(SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE, false);
                break;
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        mLocation = location;
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
    private class WatchDogRunner implements Runnable {
        boolean running = true;
        int hours, minutes, seconds = 0;
        List<LatLng> latLngs = new ArrayList<>();
        @Override
        public void run() {
            running = true;
            starts = Instant.now();

            try {
                while (running) {
                    if(mLocation!=null) {
                        hasEnoughTimePassed = hasEnoughTimePassed();
                        if(hasEnoughTimePassed) {
                            int alreadyCoveredDistance = (int) (coveredDistance / 1000) % 10;
                            if(isVoiceMessage && (int) (coveredDistance / 1000) > 0 && alreadyCoveredDistance == 0) {
                                tts.speak(alreadyCoveredDistance + " Kilometers have already passed by!",TextToSpeech.QUEUE_FLUSH,null,null);
                            }
                            latLngs.add(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
                            saveToDatabase(mLocation.getLatitude(), mLocation.getLongitude());
                            EventBus.getDefault().post(new LocationChangeEvent(latLngs));
                            hasEnoughTimePassed = false;
                        }
                    }

                    ends = Instant.now();
                    Duration diffBetweenStartEnd = Duration.between(starts, ends);

                    long secl = diffBetweenStartEnd.getSeconds();
                    seconds = Math.toIntExact(Long.valueOf(secl));

                    if (seconds >= 60) {
                        minutes += 1;
                        seconds = 0;
                        if (minutes >= 60) {
                            hours += 1;
                            minutes = 0;
                            seconds = 0;
                        }
                        starts = Instant.now();
                    }

                    updateNotification(hours, minutes, seconds);
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private int getExactStopWatch(Instant starts) {

            return seconds;
        }

        public void stop() {
            running = false;
        }
    }
    public boolean hasEnoughTimePassed() {
        currentMilliseconds = System.currentTimeMillis();
        currentSeconds = TimeUnit.MILLISECONDS.toSeconds(currentMilliseconds);

        if(isFirstEntry) {
            oldLatitude = mLocation.getLatitude();
            oldLongitude = mLocation.getLongitude();
        } else {
            currentLatitude = mLocation.getLatitude();
            currentLongitude = mLocation.getLongitude();
        }

        //number of satellites
        numberOfsatellitesInUse = mLocation.getExtras().getInt("satellites");

        altitude = mLocation.getAltitude();
        accuracy = mLocation.getAccuracy();
        currentSpeed = (mLocation.getSpeed() / 1000) * 3600;

        if(minDistanceMeter==1 && minTimeMs==1) {
            //currentSeconds must be checked because requestLocationUpdates is not always exactly 1 second
            //so it might lead to almost doubled entries in database
            if ((currentMilliseconds != oldCurrentMilliseconds) && (currentSeconds>=StaticFields.TIME_INTERVAL)) {
//                    if(currentSpeed>0) {
                    if (isFirstEntry) {
                        isFirstEntry = false;
                    } else {
                        result[0] = calculateDistance(oldLatitude, oldLongitude, currentLatitude, currentLongitude);
                        calculateLaps(result[0]);
                        oldLatitude = currentLatitude;
                        oldLongitude = currentLongitude;
                        return hasEnoughTimePassed = true;
                    }
                    oldCurrentMilliseconds = currentMilliseconds;
//                   }
            }
        } else {
            calculateDistance(oldLatitude, oldLongitude, currentLatitude, currentLongitude);
            oldLatitude = currentLatitude;
            oldLongitude = currentLongitude;
            return hasEnoughTimePassed = true;
        }
        return false;
    }
    private void updateNotification(int hours, int minutes, int seconds) {
        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(String.format("%02d:%02d:%02d", hours, minutes, seconds))
                .setContentText("D: " + String.format("%.2f Km", coveredDistance / 1000)
                        + " P: " + String.format("%.2f Km/h", currentSpeed)
                        + " S: " + numberOfsatellitesInUse + "/" + satelliteCount
                        + " Alt: " + String.format("%.2f Meter", altitude))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.icon_notification))
                //.setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.icon_notification)
                //show notification on home screen to everyone
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //without FOREGROUND_SERVICE_IMMEDIATE, notification can take up to 10 secs to be shown
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        notification = notificationBuilder.build();

        manager.notify(NOTIFICATION_ID, notificationBuilder
                .setContentTitle(String.format("%02d:%02d:%02d", hours, minutes, seconds))
                .setContentText("D: " + String.format("%.2f Km", coveredDistance / 1000)
                                + " P: " + String.format("%.2f Km/h", currentSpeed)
                                + " S: " + numberOfsatellitesInUse + "/" + satelliteCount
                                + " Alt: " + String.format("%.2f Meter", altitude))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_notification))
                .build());
    }
}