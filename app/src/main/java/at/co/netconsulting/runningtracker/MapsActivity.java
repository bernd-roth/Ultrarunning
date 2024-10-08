package at.co.netconsulting.runningtracker;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.github.pengrad.mapscaleview.MapScaleView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.shredzone.commons.suncalc.SunTimes;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import at.co.netconsulting.runningtracker.databinding.ActivityMapsBinding;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.BaseActivity;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;
import at.co.netconsulting.runningtracker.logger.FileLogger;
import at.co.netconsulting.runningtracker.pojo.ColoredPoint;
import at.co.netconsulting.runningtracker.pojo.LocationChangeEvent;
import at.co.netconsulting.runningtracker.pojo.LocationChangeEventFellowRunner;
import at.co.netconsulting.runningtracker.pojo.Run;
import at.co.netconsulting.runningtracker.service.ForegroundService;
import at.co.netconsulting.runningtracker.view.DrawView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private Polyline polylineMain, polylineFellowRunner;
    private List<LatLng> mPolylinePoints, mPolylinePointsTemp, mPolylinePointsFellowRunner;
    private boolean isDayNightModusActive, isTrafficEnabled, isRecording,
            gps_enabled, startingPoint, isAutomatedRecording, isFirstStart,
            enabledAutomaticCamera, isOnMyLocationButtonClicked, startingPointFellowRunner;
    private FloatingActionButton fabStartRecording, fabStopRecording, fabStatistics, fabSettings, fabTracks, fabResetMap;
    private String mapType;
    private SupportMapFragment mapFragment;
    private String[] permissions;
    private LocationManager locationManager;
    private DrawView drawView;
    private View mapView;
    private Marker marker;
    private Intent intent;
    private TextView textViewFast, textViewSlow;
    private MapScaleView scaleView;
    private CameraPosition camPos;
    private Dialog dialog_data;
    private ArrayAdapter<String> arrayAdapter = null;
    private int positionOfTrack = -1;
    private List<Integer> intArray;
    private ListView alertdialog_Listview;
    private AlertDialog alert;
    private float zoomLevel;
    private OkHttpClient client;
    private WebSocket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        }

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        loadSharedPreferences();

        initObjects();
        permissionLauncherMultiple.launch(permissions);
        checkIfLocationIsEnabled();
        startAutomatedRecording();
        File logFile = new File(getApplicationContext().getExternalFilesDir(null), StaticFields.FILE_NAME);
    }

    private void startAutomatedRecording() {
        if (isAutomatedRecording) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    fabStartRecording.performClick();
                }
            }, 5000);
        }
    }

    private ActivityResultLauncher<String[]> permissionLauncherMultiple = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    boolean allAreGranted = true;
                    for (Boolean isGranted : result.values()) {
                        allAreGranted = allAreGranted && isGranted;
                    }

                    if (allAreGranted) {
                        Timber.d("MapsActivity: onActivityResult: All permissions were granted!");
                    } else {
                        Timber.d("MapsActivity: onActivityResult: All or some permission were denied!");
                    }
                }
            }
    );

    @Override
    protected void onResume() {
        super.onResume();
        loadSharedPreferences();
        //retrieving Bundle when returning
        if (this.getIntent().getExtras() != null) {
            Bundle bundle = this.getIntent().getExtras();
            boolean isStopButtonVisible = bundle.getBoolean("StopButtonIsVisible");
            if (isStopButtonVisible) {
                fabStartRecording.setVisibility(View.INVISIBLE);
                fabStopRecording.setVisibility(View.VISIBLE);
            }
        }
        //redraw Google Map, calling GoogleMap will fail due to NPE
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void loadSharedPreferences() {
        SharedPreferences sh;

        sh = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
        mapType = sh.getString(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_NORMAL");

        sh = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_DAY_NIGHT_MODUS, Context.MODE_PRIVATE);
        isDayNightModusActive = sh.getBoolean(SharedPref.STATIC_SHARED_PREF_DAY_NIGHT_MODUS, false);

        sh = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_ENABLE_TRAFFIC, Context.MODE_PRIVATE);
        isTrafficEnabled = sh.getBoolean(SharedPref.STATIC_SHARED_PREF_ENABLE_TRAFFIC, false);

        sh = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_AUTOMATED_RECORDING, Context.MODE_PRIVATE);
        isAutomatedRecording = sh.getBoolean(SharedPref.STATIC_SHARED_PREF_AUTOMATED_RECORDING, false);
    }

    private static List<LatLng> groupPoints(List<LatLng> polylinePoints, Projection projection) {
        ArrayList<LatLng> result = new ArrayList<LatLng>();
        Map<Point, LatLng> groupedPoints = new HashMap<Point, LatLng>();
        for (LatLng xlatLng : polylinePoints) {
            Point p = projection.toScreenLocation(xlatLng);
            if (!groupedPoints.containsKey(p)) {
                groupedPoints.put(p, xlatLng);
                result.add(xlatLng);
            }
        }
        Timber.d("result %s", result);
        return result;
    }

    private boolean isServiceRunning(String serviceName) {
        boolean serviceRunning = false;
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> l = am.getRunningServices(50);
        Iterator<ActivityManager.RunningServiceInfo> i = l.iterator();
        while (i.hasNext()) {
            ActivityManager.RunningServiceInfo runningServiceInfo = i
                    .next();

            if (runningServiceInfo.service.getClassName().equals(serviceName)) {
                serviceRunning = true;

                if (runningServiceInfo.foreground) {
                    //service run in foreground
                }
            }
        }
        return serviceRunning;
    }

    private void initObjects() {
        fabStartRecording = findViewById(R.id.fabRecording);
        fabStartRecording.setVisibility(View.VISIBLE);
        fabStopRecording = findViewById(R.id.fabStopRecording);
        fabStopRecording.setVisibility(View.INVISIBLE);
        fabStatistics = findViewById(R.id.fabStatistics);
        fabStatistics.setVisibility(View.VISIBLE);
        fabSettings = findViewById(R.id.fabSettings);
        fabSettings.setVisibility(View.VISIBLE);
        fabTracks = findViewById(R.id.fabTracks);
        fabTracks.setVisibility(View.VISIBLE);
        fabResetMap = findViewById(R.id.fabResetMap);
        fabResetMap.setVisibility(View.INVISIBLE);
        textViewFast = findViewById(R.id.textViewFast);
        textViewFast.setVisibility(View.INVISIBLE);
        textViewSlow = findViewById(R.id.textViewSlow);
        textViewSlow.setVisibility(View.INVISIBLE);
        scaleView = findViewById(R.id.scaleView);
        scaleView.metersOnly();

        mPolylinePoints = new ArrayList<>();
        mPolylinePointsTemp = new ArrayList<>();
        mPolylinePointsFellowRunner = new ArrayList<>();
        permissions = new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS};
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        startingPoint = true;
        startingPointFellowRunner = true;
        //draw speed scale and set visibility
        drawView = new DrawView(this);
        RelativeLayout myRelativeLayout = (RelativeLayout) findViewById(R.id.relLayout);
        myRelativeLayout.addView(drawView);
        drawView.setVisibility(View.INVISIBLE);
        //set screen orientaiton to potrait modus automatically
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void checkIfLocationIsEnabled() {
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled) {
            new AlertDialog.Builder(MapsActivity.this)
                    .setMessage(getResources().getString(R.string.location_service))
                    .setPositiveButton(getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            MapsActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.button_cancel), null)
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mapType.equals("MAP_TYPE_NORMAL"))
            mMap.setMapType(mMap.MAP_TYPE_NORMAL);
        else if (mapType.equals("MAP_TYPE_HYBRID"))
            mMap.setMapType(mMap.MAP_TYPE_HYBRID);
        else if (mapType.equals("MAP_TYPE_NONE"))
            mMap.setMapType(mMap.MAP_TYPE_NONE);
        else if (mapType.equals("MAP_TYPE_TERRAIN"))
            mMap.setMapType(mMap.MAP_TYPE_TERRAIN);
        else if (mapType.equals("MAP_TYPE_SATELLITE"))
            mMap.setMapType(mMap.MAP_TYPE_SATELLITE);
        else
            mMap.setMapType(mMap.MAP_TYPE_NORMAL);

        // Get map views
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().
                findFragmentById(R.id.map);
        mapView = mapFragment.getView();
        View location_button = mapView.findViewWithTag("GoogleMapMyLocationButton");
        View zoom_in_button = mapView.findViewWithTag("GoogleMapZoomInButton");
        View zomm_out_button = mapView.findViewWithTag("GoogleMapZoomOutButton");
        View zoom_layout = (View) zoom_in_button.getParent();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                location_button.getLayoutParams();
        // position on right bottom
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.setMargins(0, 0, 5, 255);

        //adjust location button layout params above the zoom layout
        RelativeLayout.LayoutParams location_layout = (RelativeLayout.LayoutParams) location_button.getLayoutParams();
        location_layout.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        location_layout.addRule(RelativeLayout.ABOVE, zoom_layout.getId());

        //Cross-hair is shown here, right lower corner
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.setPadding(0, 0, 0, 90);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        if (isTrafficEnabled) {
            mMap.setTrafficEnabled(true);
        } else {
            mMap.setTrafficEnabled(false);
        }
        camPos = mMap.getCameraPosition();
        scaleView.update(camPos.zoom, camPos.target.latitude);

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                enabledAutomaticCamera = true;

                updateZoomLevel(null, zoomLevel);

                isOnMyLocationButtonClicked = true;
                return enabledAutomaticCamera;
            }
        });

        zoom_in_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomLevel += 1;
                // Retrieve the details from the CameraPosition object
                CameraPosition camPos = mMap.getCameraPosition();
                LatLng targetLocation = camPos.target; // Latitude and Longitude
                updateZoomLevel(targetLocation, zoomLevel);
            }
        });

        zomm_out_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomLevel -= 1;
                // Retrieve the details from the CameraPosition object
                CameraPosition camPos = mMap.getCameraPosition();
                LatLng targetLocation = camPos.target; // Latitude and Longitude
                updateZoomLevel(targetLocation, zoomLevel);
            }
        });

        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    enabledAutomaticCamera = false;
                    isOnMyLocationButtonClicked = false;
                    // Retrieve the details from the CameraPosition object
                    CameraPosition camPos = mMap.getCameraPosition();
                    LatLng targetLocation = camPos.target; // Latitude and Longitude
                    zoomLevel = camPos.zoom;
                    updateZoomLevel(targetLocation, zoomLevel);
                }
            }
        });

        //if true, day/night modus switches automatically depending on sunrise/sunset
        if (isDayNightModusActive) {
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location location = new Location(loc);

            boolean isBetween = isCurrentTimeBetweenSunriseSunset(location);

            if (isBetween) {
                mMap.setMapStyle(null);
            } else {
                Timber.d(getResources().getString(R.string.isInBetween));
                boolean success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                this, R.raw.style_json));
                if (!success) {
                    Timber.d(getResources().getString(R.string.style_failed));
                }
            }
        } else {
            mMap.setMapStyle(null);
        }
    }

    private void updateZoomLevel(LatLng targetLocation, float zoomLevel) {
        if (ActivityCompat.checkSelfPermission(MapsActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MapsActivity.this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        Location loc, location;
        if(targetLocation==null) {
            loc = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            location = new Location(loc);
            targetLocation = new LatLng(location.getLatitude(), location.getLongitude());
        } else {
            camPos = mMap.getCameraPosition();
        }

        float maximumZoomLevel = mMap.getMaxZoomLevel();
        float minimumZoomLevel = mMap.getMinZoomLevel();

        if(zoomLevel <= minimumZoomLevel) {
            zoomLevel = minimumZoomLevel;
        }

        if(zoomLevel > maximumZoomLevel) {
            if(targetLocation==null) {
                CameraPosition camPos = mMap.getCameraPosition();
                scaleView.update(maximumZoomLevel, camPos.target.latitude);
            } else {
                scaleView.update(maximumZoomLevel, camPos.target.latitude);
            }
        } else if(zoomLevel == maximumZoomLevel) {
            if(targetLocation==null) {
                CameraPosition camPos = mMap.getCameraPosition();
                scaleView.update(maximumZoomLevel, camPos.target.latitude);
            } else {
                scaleView.update(maximumZoomLevel, camPos.target.latitude);
            }
        } else if(this.zoomLevel < maximumZoomLevel) {
            if(targetLocation==null) {
                CameraPosition camPos = mMap.getCameraPosition();
                scaleView.update(zoomLevel, camPos.target.latitude);
            } else {
                scaleView.update(zoomLevel, camPos.target.latitude);
            }
        }

        if(targetLocation!=null) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(targetLocation, this.zoomLevel);
            // Alternatively, to animate the camera to the new position smoothly
            mMap.animateCamera(cameraUpdate);
        }
    }

    private boolean isCurrentTimeBetweenSunriseSunset(Location location) {
        ZonedDateTime zdtNow = ZonedDateTime.now();

        SunTimes times = SunTimes.compute()
                .on(zdtNow)
                .at(location.getLatitude(), location.getLongitude())
                .execute();

        ZonedDateTime rise = times.getRise();
        ZonedDateTime set = times.getSet();

        LocalDateTime minusDaySet, minusDayRise;
        java.time.LocalDateTime localDateRise = rise.toLocalDateTime();
        java.time.LocalDateTime localDateSet = set.toLocalDateTime();

        int currentDayOfMonth = new org.joda.time.LocalDateTime().getDayOfMonth();

        //compare current date with sunrise date
        if(currentDayOfMonth!=localDateRise.getDayOfMonth()) {
            minusDayRise = localDateRise.minusDays(1);
        } else {
            minusDayRise = localDateRise.minusDays(0);
        }

        if(currentDayOfMonth!=localDateSet.getDayOfMonth()) {
            minusDaySet = localDateSet.minusDays(1);
        } else {
            minusDaySet = localDateSet.minusDays(0);
        }

        Log.d("Sunrise: %s", String.valueOf(minusDayRise));
        Log.d("Sunset: %s", String.valueOf(minusDaySet));

        return ChronoLocalDateTime.from(zdtNow).isAfter(minusDayRise) && ChronoLocalDateTime.from(zdtNow).isBefore(minusDaySet);
    }
    private void createCheckerFlag(List<LatLng> polylinePoints, boolean isColouredTrack, List<Run> allEntries) {
        int height = 100;
        int width = 100;
        int alreadyCovered = 0;
        boolean isStart = true;
        HashSet<Integer> hashArray = new HashSet<Integer>();
        List<Marker> lMarker = new ArrayList<>();

        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable. checkerflag);
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        BitmapDescriptor checkerFlag = BitmapDescriptorFactory.fromBitmap(smallMarker);

        if(allEntries != null && allEntries.size() > 0) {
            for(int i = 0; i<allEntries.size(); i++) {
                double meters_covered = allEntries.get(i).getMeters_covered() / 1000;
                alreadyCovered = (int) meters_covered;

                if (alreadyCovered > 0 && ! hashArray.contains(alreadyCovered)) {
                    marker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(polylinePoints.get(i).latitude,
                                    polylinePoints.get(i).longitude)).title("Kilometer: " + alreadyCovered));
                    //marker.setTitle(getResources().getString(R.string.finish));
                    marker.showInfoWindow();
                    lMarker.add(marker);
                    hashArray.add(alreadyCovered);
                } else if (isStart) {
                    marker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(polylinePoints.get(i).latitude,
                                    polylinePoints.get(i).longitude)));
                    marker.showInfoWindow();
                    lMarker.add(marker);
                    isStart = false;
                }
            }
        }

        if(polylinePoints.size()>1){
            marker = mMap.addMarker(new MarkerOptions().position(
                    new LatLng(polylinePoints.get(polylinePoints.size()-1).latitude,
                            polylinePoints.get(polylinePoints.size()-1).longitude)).icon(checkerFlag));
            marker.setTitle(getResources().getString(R.string.finish));
            marker.showInfoWindow();
            lMarker.add(marker);

            //move to last location when drawing the route
            double lat = polylinePoints.get(polylinePoints.size()-1).latitude;
            double lon = polylinePoints.get(polylinePoints.size()-1).longitude;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15f));
        }
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Subscribe
    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Subscribe
    @Override
    protected void onStop() {
        super.onStop();
        int isVisible = fabStopRecording.getVisibility();
        if(isVisible==0) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("StopButtonIsVisible", true);
            this.getIntent().putExtras(bundle);
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, ForegroundService.class));
    }

    public void onClickRecording(View view) {
        switch(view.getId()) {
            case R.id.fabRecording:
                Context contextFabRecording = getApplicationContext();
                Intent intentForegroundService = new Intent(contextFabRecording, ForegroundService.class);
                intentForegroundService.setAction("ACTION_START");
                contextFabRecording.startForegroundService(intentForegroundService);
                mMap.clear();

                fadingButtons(R.id.fabRecording);
                fabStartRecording.setEnabled(false);
                fabStopRecording.setEnabled(true);
                break;
            case R.id.fabStopRecording:
                stopService(new Intent(this, ForegroundService.class));

                fadingButtons(R.id.fabStopRecording);

                createCheckerFlag(mPolylinePointsTemp, false, null);
                showAlertDialogWithComment();
                startingPoint=true;
                isRecording = false;
                isFirstStart=false;
                fabStopRecording.setEnabled(false);
                fabStartRecording.setEnabled(true);
                break;
            case R.id.fabStatistics:
                Intent intentStatistics = new Intent(MapsActivity.this, StatisticsActivity.class);
                this.startActivity(intentStatistics);
                break;
            case R.id.fabSettings:
                Intent intentSettings = new Intent(MapsActivity.this, SettingsActivity.class);
                this.startActivity(intentSettings);
                break;
            case R.id.fabTracks:
                showAlertDialogWithTracks();
        }
    }

    private void showAlertDialogWithComment() {
        if(mPolylinePointsTemp.size()>0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            final EditText edittext = new EditText(getApplicationContext());
            builder.setMessage(getResources().getString(R.string.please_comment_your_run));
            builder.setCancelable(false);
            builder.setView(edittext);

            builder.setPositiveButton(
                    getResources().getString(R.string.buttonSave),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String comment = edittext.getText().toString();

                            DatabaseHandler db = new DatabaseHandler(getApplicationContext());
                            db.updateComment(comment);
                            dialog.cancel();
                        }
                    });

            builder.setNegativeButton(
                    getResources().getString(R.string.button_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            builder.setNeutralButton(getResources().getString(R.string.button_discard_run), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DatabaseHandler db = new DatabaseHandler(getApplicationContext());
                    int id = db.getLastEntry();
                    db.deleteSingleEntry(id);
                    dialog.cancel();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        }
    }
    private void showAlertDialogWithTracks() {
        DatabaseHandler db = new DatabaseHandler(this);
        List<Run> allEntries = db.getAllEntriesOrderedByRunNumber();
        TreeMap<Long, Run> orderedByDate = new TreeMap<>();
        long millis;
        Date date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        for(int i = 0; i<allEntries.size(); i++) {
            try {
                date = sdf.parse(allEntries.get(i).getDateTime());
            } catch(Exception e) {
                e.printStackTrace();
            }
            millis = date.getTime();

            Run runa = allEntries.get(i);
            orderedByDate.put(millis, runa);
        }

        dialog_data = new Dialog(MapsActivity.this);
        setDialogWithSpecificHeight(dialog_data);

        EditText filterText = (EditText) dialog_data.findViewById(R.id.alertdialog_edittext);
        alertdialog_Listview = (ListView) dialog_data.findViewById(R.id.alertdialog_Listview);
        alertdialog_Listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if(mPolylinePoints.size()>0) {
            mPolylinePoints.clear();
        }

        arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_text_view);
        intArray = new ArrayList<>();

        NavigableMap<Long, Run> reveresedTreeMap = orderedByDate.descendingMap();

        for(Map.Entry<Long, Run> entry : reveresedTreeMap.entrySet()) {
            arrayAdapter.add(
                (entry.getValue().getDateTime()+ "\n"
                + String.format("%.03f", entry.getValue().getMeters_covered()/1000) + " Km\n"
                + entry.getValue().getComment()));
            intArray.add(entry.getValue().getNumber_of_run());
        }

        alertdialog_Listview.setAdapter(arrayAdapter);
        alertdialog_Listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                positionOfTrack = intArray.get(position);
            }
        });

        filterText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                arrayAdapter.clear();
                intArray.clear();

                for(int i = 0; i<allEntries.size(); i++) {
                    String search = s.toString().toLowerCase(Locale.ENGLISH);
                    String searchableString = allEntries.get(i).getDateTime() + "\n"
                            + String.format("%.03f", allEntries.get(i).getMeters_covered() / 1000) + " Km\n"
                            + allEntries.get(i).getComment();
                    if(searchableString.toLowerCase(Locale.ENGLISH).contains(search)) {
                        arrayAdapter.add(
                                allEntries.get(i).getDateTime() + "\n"
                                        + String.format("%.03f", allEntries.get(i).getMeters_covered() / 1000) + " Km\n"
                                        + allEntries.get(i).getComment());
                        intArray.add(allEntries.get(i).getNumber_of_run());
                    }
                }
            }
        });
        dialog_data.show();
    }
    private void setAlertDialogWithSpecificHeight(AlertDialog.Builder builderSingle) {
        //set height to 50%
        int height = 2;

        AlertDialog alertDialog = builderSingle.create();
        alertDialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(alertDialog.getWindow().getAttributes());
        lp.width = this.getWindow().getDecorView().getWidth();
        lp.height = this.getWindow().getDecorView().getHeight()/height;

        alertDialog.getWindow().setAttributes(lp);
    }
    private void setDialogWithSpecificHeight(Dialog dialog) {
        //set height to 50%
        int height = 2;

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.setContentView(R.layout.custom_list);

        dialog.setContentView(R.layout.custom_list);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = this.getWindow().getDecorView().getWidth();
        lp.height = this.getWindow().getDecorView().getHeight()/height;
        lp.gravity = Gravity.CENTER;

        dialog.getWindow().setAttributes(lp);
    }
    private void showPolyline(List<ColoredPoint> points, List<Run> allEntries) {
        mMap.clear();
        if (points.size() < 2)
            return;

        int ix = 0;
        ColoredPoint currentPoint  = points.get(ix);
        int currentColor = currentPoint.color;
        List<LatLng> currentSegment = new ArrayList<>();
        currentSegment.add(currentPoint.coords);

        if(ix==0) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mPolylinePoints.get(0).latitude, mPolylinePoints.get(0).longitude))
                    .title(getString(R.string.starting_position))).showInfoWindow();
        }

        ix++;
        while (ix < points.size()) {
            currentPoint = points.get(ix);

            if (currentPoint.color == currentColor) {
                currentSegment.add(currentPoint.coords);
            } else {
                currentSegment.add(currentPoint.coords);
                mMap.addPolyline(new PolylineOptions()
                        .addAll(currentSegment)
                        .color(currentColor)
                        .width(20));
                currentColor = currentPoint.color;
                currentSegment.clear();
                currentSegment.add(currentPoint.coords);
            }
            ix++;
        }
        drawView.setVisibility(View.VISIBLE);
        textViewSlow.setVisibility(View.VISIBLE);
        textViewFast.setVisibility(View.VISIBLE);
        polylineMain = mMap.addPolyline(new PolylineOptions().addAll(currentSegment).color(currentColor).jointType(JointType.ROUND).width(15.0f));
        createCheckerFlag(mPolylinePoints, true, allEntries);
    }

    private void fadingButtons(int fabButton) {
        if(fabButton==R.id.fabRecording) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Define the animators
                    Animation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
                    Animation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
                    // Duration of animation
                    fadeInAnimation.setDuration(500);
                    fadeOutAnimation.setDuration(500);
                    // Keep stop button visible, start button invisible
                    fadeInAnimation.setFillAfter(true);
                    fadeOutAnimation.setFillAfter(true);

                    fabStopRecording.startAnimation(fadeInAnimation);
                    fabStartRecording.startAnimation(fadeOutAnimation);
                    //fabStatistics.startAnimation(fadeOutAnimation);
                    //fabSettings.startAnimation(fadeOutAnimation);
                    //fabTracks.startAnimation(fadeOutAnimation);

                    Bundle bundle = new Bundle();
                    bundle.putBoolean("StopButtonIsVisible", true);
                    getIntent().putExtras(bundle);

                    textViewFast.setVisibility(View.INVISIBLE);
                    textViewSlow.setVisibility(View.INVISIBLE);
                    drawView.setVisibility(View.INVISIBLE);
                }
            }, 500);// set time as per your requirement
        } else if(fabButton==R.id.fabStopRecording) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Define the animators
                    Animation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
                    Animation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
                    // Duration of animation
                    fadeInAnimation.setDuration(500);
                    fadeOutAnimation.setDuration(500);
                    // Keep stop button visible, start button invisible
                    fadeInAnimation.setFillAfter(true);
                    fadeOutAnimation.setFillAfter(true);

                    fabStopRecording.startAnimation(fadeOutAnimation);
                    fabStartRecording.startAnimation(fadeInAnimation);
                    //fabStatistics.startAnimation(fadeInAnimation);
                    //fabSettings.startAnimation(fadeInAnimation);
                    //fabTracks.startAnimation(fadeInAnimation);
                }
            }, 500);// set time as per your requirement
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            case R.id.action_maps:
                intent = new Intent(this, MapsActivity.class);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocationChangeEvent event) {
        List<LatLng> mPolylinePoints = new ArrayList<>(event.latLngs); // Use a new list to avoid references
        mPolylinePointsTemp = mPolylinePoints;
        Log.d("PolylinePointsMain", "Main Polyline Points: " + mPolylinePoints); // Log for debugging
        createPolypoints(mPolylinePoints);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocationChangeEventFellowRunner event) {
        List<LatLng> mPolylinePointsFellowRunner = new ArrayList<>(event.latLngs); // Use a new list to avoid references
        Log.d("PolylinePointsFellow", "Fellow Runner Polyline Points: " + mPolylinePointsFellowRunner); // Log for debugging
        createPolypointsForFellowRunner(mPolylinePointsFellowRunner);
    }

    private void createPolypoints(List<LatLng> polylinePoints) {
        boolean isServiceRunning = isServiceRunning(getString(R.string.serviceName));
        if (!isServiceRunning || polylinePoints == null || polylinePoints.isEmpty()) {
            return;
        }

        // Check for starting point condition
        if (startingPoint) {
            marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(polylinePoints.get(0).latitude, polylinePoints.get(0).longitude))
                    .title(getResources().getString(R.string.starting_position)));
            marker.showInfoWindow();
            startingPoint = false;
        } else {
            // Remove existing polyline if present
            if (polylineMain != null && !isFirstStart) {
                polylineMain.remove();
                isFirstStart = true;
            }
            // Update camera position if enabled
            if (enabledAutomaticCamera) {
                LatLng lastLocation = polylinePoints.get(polylinePoints.size() - 1);
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(lastLocation, zoomLevel);
                mMap.moveCamera(update);
            }
            // Draw the polyline on the map
            polylineMain = mMap.addPolyline(new PolylineOptions()
                    .addAll(new ArrayList<>(polylinePoints)) // Ensure a new list
                    .color(Color.MAGENTA)
                    .jointType(JointType.ROUND)
                    .width(15.0f));
        }
    }

    private void createPolypointsForFellowRunner(List<LatLng> mPolylinePoints) {
        if (mPolylinePoints == null || mPolylinePoints.isEmpty()) {
            return;
        }

        // Remove previous fellow runner polyline if it exists
        if (polylineFellowRunner != null) {
            polylineFellowRunner.remove();
        }

        if(startingPointFellowRunner) {
            marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mPolylinePoints.get(0).latitude, mPolylinePoints.get(0).longitude))
                    .title(getResources().getString(R.string.starting_fellow_runner_position)));
            marker.showInfoWindow();
            startingPointFellowRunner = false;
        } else {
            // Add a new polyline for fellow runner
            polylineFellowRunner = mMap.addPolyline(new PolylineOptions()
                    .addAll(new ArrayList<>(mPolylinePoints)) // Ensure a new list
                    .color(Color.BLUE)
                    .jointType(JointType.ROUND)
                    .width(15.0f));
        }
        Log.d("PolylinePointsFellow", "Fellow Runner Polyline Created with Points: " + mPolylinePoints);
    }

    public void showTrack(View view) {
        String fullName = getResources().getResourceName(view.getId());
        String name = fullName.substring(fullName.lastIndexOf("/") + 1);

        if (positionOfTrack!=-1 && name.equals("defaulttrack")) {
            DatabaseHandler db = new DatabaseHandler(this);
            List<Run> allEntries = db.getAllEntriesOrderedByRunNumber();
            int numberOfRun = 0;
            for(int i = 0; i<allEntries.size(); i++) {
                if(allEntries.get(i).getNumber_of_run() == positionOfTrack) {
                    numberOfRun = allEntries.get(i).getNumber_of_run();
                }
            }

            final LatLng[] latLng = new LatLng[1];

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            int finalNumberOfRun = numberOfRun;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    //Background work here
                    List<Run> allEntries = db.getSingleEntryOrderedByDateTime(finalNumberOfRun);
                    for (int i = 0; i < allEntries.size(); i++) {
                        latLng[0] = new LatLng(allEntries.get(i).getLat(), allEntries.get(i).getLng());
                        mPolylinePoints.add(latLng[0]);
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //UI Thread work here
                            mMap.clear();
                            drawView.setVisibility(View.INVISIBLE);
                            textViewSlow.setVisibility(View.INVISIBLE);
                            textViewFast.setVisibility(View.INVISIBLE);
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(mPolylinePoints.get(0).latitude, mPolylinePoints.get(0).longitude))
                                    .title(getString(R.string.starting_position)));
                            polylineMain = mMap.addPolyline(new PolylineOptions().addAll(mPolylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
                            createCheckerFlag(mPolylinePoints, false, null);
                            dialog_data.cancel();
                            fabResetMap.setVisibility(View.VISIBLE);
                        }
                    });
                    positionOfTrack=-1;
                }
            });
        } else if(positionOfTrack!=-1 && name.equals("colouredtrack")) {
            DatabaseHandler db = new DatabaseHandler(this);
            List<Run> allEntries = db.getAllEntriesOrderedByRunNumber();
            int numberOfRun = 0;
            for(int i = 0; i<allEntries.size(); i++) {
                if(allEntries.get(i).getNumber_of_run() == positionOfTrack) {
                    numberOfRun = allEntries.get(i).getNumber_of_run();
                }
            }
            int finalNumberOfRun = numberOfRun;
            final LatLng[] latLng = new LatLng[1];

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    List<Run> allEntries = db.getSingleEntryOrderedByDateTime(finalNumberOfRun);
                    List<ColoredPoint> sourcePoints = new ArrayList<>();

                    for(int i = 0; i<allEntries.size(); i++) {
                        latLng[0] = new LatLng(allEntries.get(i).getLat(), allEntries.get(i).getLng());
                        //FIXME make speed adjustable
                        if(allEntries.get(i).getSpeed()>8) { //running is over 8-10 km/h
                            sourcePoints.add(new ColoredPoint(latLng[0], Color.GREEN));
                        } else if(allEntries.get(i).getSpeed()>6 && // jogging is 6-8 km/h
                                allEntries.get(i).getSpeed()<8){
                            sourcePoints.add(new ColoredPoint(latLng[0], Color.YELLOW));
                        } else { // walking is around 5.5-6 km/h
                            sourcePoints.add(new ColoredPoint(latLng[0], Color.RED));
                        }
                        mPolylinePoints.add(latLng[0]);
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            showPolyline(sourcePoints, allEntries);
                            dialog_data.cancel();
                            fabResetMap.setVisibility(View.VISIBLE);
                        }
                    });
                    positionOfTrack=-1;
                }
            });
        } else if(positionOfTrack!=-1 && name.equals("updatetrack")) {
            DatabaseHandler db = new DatabaseHandler(this);
            List<Run> allEntries = db.getAllEntriesOrderedByRunNumber();
            int numberOfRun = 0;
            for(int i = 0; i<allEntries.size(); i++) {
                if(allEntries.get(i).getNumber_of_run() == positionOfTrack) {
                    numberOfRun = allEntries.get(i).getNumber_of_run();
                }
            }

            int finalNumberOfRun = numberOfRun;

            List<Run> run = db.getLastCommentEntryOfSelectedRun(numberOfRun);

            final EditText edittext = new EditText(MapsActivity.this);
            edittext.setText(run.get(0).getComment());

            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            builder.setTitle(getResources().getString(R.string.update_comment));
            builder.setView(edittext);
            builder.setPositiveButton(
                    getResources().getString(R.string.button_update),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            db.updateCommentById(finalNumberOfRun, edittext.getText().toString());

                            arrayAdapter.clear();
                            alert.dismiss();
                            intArray = new ArrayList<>();

                            refresh_list_view();
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.update_entry), Toast.LENGTH_LONG).show();
                        }
                    });
            builder.setNegativeButton(
                    getResources().getString(R.string.button_delete),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            db.deleteSingleEntry(finalNumberOfRun);

                            arrayAdapter.clear();
                            alert.dismiss();
                            intArray = new ArrayList<>();

                            refresh_list_view();
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.single_entry_deleted), Toast.LENGTH_LONG).show();
                        }
                    });
            builder.setNeutralButton(
                    getResources().getString(R.string.button_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            refresh_list_view();
                        }
                    });
            alert = builder.create();
            alert.show();
            positionOfTrack=-1;
        }
    }
    public void refresh_list_view() {
        DatabaseHandler db = new DatabaseHandler(this);

        alert.dismiss();
        alertdialog_Listview.clearChoices();
        intArray = new ArrayList<>();

        List<Run> allEntries = db.getAllEntriesOrderedByRunNumber();
        for(int i = 0; i<allEntries.size(); i++) {
            arrayAdapter.add(
                    allEntries.get(i).getDateTime() + "\n"
                            + String.format("%.03f", allEntries.get(i).getMeters_covered()/1000) + " Km\n"
                            + allEntries.get(i).getComment());
            intArray.add(allEntries.get(i).getNumber_of_run());
        }
        alertdialog_Listview.clearChoices();
        alertdialog_Listview.setAdapter(arrayAdapter);
    }
    public void onClickResetMap(View view) {
        mMap.clear();
        drawView.setVisibility(View.INVISIBLE);
        textViewFast.setVisibility(View.INVISIBLE);
        textViewSlow.setVisibility(View.INVISIBLE);
        fabResetMap.setVisibility(View.INVISIBLE);
    }
}