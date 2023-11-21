package at.co.netconsulting.runningtracker;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.PolyUtil;
import org.joda.time.Duration;
import org.joda.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import at.co.netconsulting.runningtracker.databinding.ActivityMapsBinding;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.BaseActivity;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.pojo.ColoredPoint;
import at.co.netconsulting.runningtracker.pojo.Run;
import at.co.netconsulting.runningtracker.service.ForegroundService;
import at.co.netconsulting.runningtracker.view.DrawView;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener {
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    //Polyline
    private Polyline polyline, polylineKalmanFiltered, polylineOtherPerson;
    private List<LatLng> polylinePoints, polylinePointsTemp, polylinePointsOtherPerson;
    private boolean isDisableZoomCamera;
    private FloatingActionButton fabStartRecording, fabStopRecording, fabStatistics, fabPauseRecording;
    private double lastLat, lastLng;
    private String mapType, speed;
    private SupportMapFragment mapFragment;
    private String[] permissions;
    private LocationManager locationManager;
    private boolean gps_enabled;
    private boolean startingPoint, startingPointJulia;
    private BroadcastReceiver receiver;
    private boolean isPauseRecordingClicked, isSwitchPausedActivated, isSwitchGoToLastLocation;
    private DatabaseHandler db;
    private Toolbar toolbar;
    private TextView toolbar_title, textViewSlow, textViewFast;
    private float coveredDistance;
    private PolyUtil polyUtil;
    private DrawView drawView;

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

        //initialize objects
        initObjects();
        permissionLauncherMultiple.launch(permissions);
        checkIfLocationIsEnabled();
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
            if(isStopButtonVisible) {
                fabStartRecording.setVisibility(View.INVISIBLE);
                fabStopRecording.setVisibility(View.VISIBLE);
                fabPauseRecording.setVisibility(View.VISIBLE);
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

        sh = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE, Context.MODE_PRIVATE);
        isSwitchPausedActivated = sh.getBoolean(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE, false);

        sh = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_GO_TO_LAST_LOCATION, Context.MODE_PRIVATE);
        isSwitchGoToLastLocation = sh.getBoolean(SharedPref.STATIC_SHARED_PREF_GO_TO_LAST_LOCATION, false);
    }

//    private void createPolypoints(double lastLat, double lastLng, List<LatLng> polylinePoints) {
//        boolean isServiceRunning = isServiceRunning(getString(R.string.serviceName));
//        //isServiceRunning = true; // FIXME
//
//        if(!isServiceRunning) {
//            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLng), 0));
//            toolbar_title.setText("Distance: 0.0 Km" + "\nSpeed: 0.0");
//        } else {
//            LatLng latLng = new LatLng(lastLat, lastLng);
//            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
//            Projection projection = mMap.getProjection();
//
//            if(startingPoint) {
//                mMap.addMarker(new MarkerOptions().position(latLng).title(getResources().getString(R.string.current_location))).showInfoWindow();
//                startingPoint = false;
//            } else {
//                //polylinePoints = groupPoints(polylinePoints, projection);
//
//                polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
//                toolbar_title.setText("Distance: " + String.format("%.2f", coveredDistance) + "\nSpeed: " + speed);
//
//                //check if firebase has some values left and draw it
//                //getFirebaseDatabase(polylinePoints);
//            }
//        }
//    }

    private void createPolypoints(String speed, float coveredDistance, double lastLat, double lastLng) {
        boolean isServiceRunning = isServiceRunning(getString(R.string.serviceName));

        if(!isServiceRunning) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLng), 0));
            toolbar_title.setText("Distance: 0.0 Km" + "\nSpeed: 0.0");
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLng), 16));

            if(startingPoint) {
                mMap.addMarker(new MarkerOptions().position(new LatLng(lastLat, lastLng)).title(getResources().getString(R.string.current_location))).showInfoWindow();
                startingPoint = false;
            } else {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLng), 16));
                toolbar_title.setText("Distance: " + String.format("%.2f", coveredDistance) + "\nSpeed: " + speed);
            }
        }
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

/*    private void getFirebaseDatabase(List<LatLng> polylinePoints) {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference().child("Users");
        DatabaseReference user1 = myRef.child("Bernd");
        DatabaseReference user2 = myRef.child("Julia");

        //get value
        user1.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();

                    Double lat = snapshot.child("lat").getValue(Double.class);
                    Double lon = snapshot.child("lon").getValue(Double.class);

                    //User Bernd
                    polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
                } else {
                    Timber.d("Exception addOnCompleteListener: %s", task.getException().getMessage());
                }
            }
        });

        user2.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();

                    Double lat = snapshot.child("lat").getValue(Double.class);
                    Double lon = snapshot.child("lon").getValue(Double.class);

                    //User Julia
                    if(startingPointJulia) {
                        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Julia")).showInfoWindow();
                        startingPointJulia=false;
                    }
                    polylinePointsOtherPerson.add(new LatLng(lat, lon));
                    polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePointsOtherPerson).color(Color.BLACK).jointType(JointType.ROUND).width(15.0f));
                } else {
                    Timber.d("Exception addOnCompleteListener: %s", task.getException().getMessage());
                }
            }
        });
    }*/

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

    private void configureReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SharedPref.STATIC_BROADCAST_ACTION);
        receiver = new DataBroadcastReceiver();
        registerReceiver(receiver, filter);
    }

    private void initObjects() {
        fabStartRecording = findViewById(R.id.fabRecording);
        fabStartRecording.setVisibility(View.VISIBLE);
        fabStopRecording = findViewById(R.id.fabStopRecording);
        fabStopRecording.setVisibility(View.INVISIBLE);
        fabStatistics = findViewById(R.id.fabStatistics);
        fabPauseRecording = findViewById(R.id.fabPauseRecording);
        fabPauseRecording.setVisibility(View.INVISIBLE);
        fabStatistics.setVisibility(View.VISIBLE);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.LTGRAY);
        toolbar_title = findViewById(R.id.toolbar_title);
        textViewSlow = findViewById(R.id.textViewSlow);
        //textViewFast = findViewById(R.id.textViewFast);

        polylinePoints = new ArrayList<>();
        polylinePointsTemp = new ArrayList<>();
        polylinePointsOtherPerson = new ArrayList<>();
        configureReceiver();
        permissions = new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS};
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        isDisableZoomCamera = true;
        startingPoint = true;
        startingPointJulia = true;
        db = new DatabaseHandler(this);
        drawView = new DrawView(this);
        RelativeLayout myRelativeLayout = (RelativeLayout) findViewById(R.id.relLayout);
        myRelativeLayout.addView(drawView);
    }

    private void checkIfLocationIsEnabled() {
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled) {
            new AlertDialog.Builder(MapsActivity.this)
                    .setMessage(getResources().getString(R.string.location_service))
                    .setPositiveButton(getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            MapsActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.buttonCancel),null)
                    .show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //if map is moved around, automatic camera movement is disabled
        mMap.setOnCameraMoveListener(this);
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

        //createListenerAndfillPolyPoints(0, 0);

        // Get map views
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().
                findFragmentById(R.id.map);
        View mapView = mapFragment.getView();
        View location_button = mapView.findViewWithTag("GoogleMapMyLocationButton");
        View zoom_in_button = mapView.findViewWithTag("GoogleMapZoomInButton");
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

        //Cross-hair is shown here, right upper corner
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setPadding(0,0,0,90);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.setTrafficEnabled(true);
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                int mCameraMoveReason = reason;
                if (mCameraMoveReason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    mMap.stopAnimation();
                } else if(mCameraMoveReason == GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION) {
                    goToLastLocation();
                }
            }
        });
        goToLastLocation();
    }

    private void createCheckerFlag(List<LatLng> polylinePoints) {
        int height = 100;
        int width = 100;

        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable. checkerflag);
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        BitmapDescriptor checkerFlag = BitmapDescriptorFactory.fromBitmap(smallMarker);

        if(polylinePoints.size()>1){
            mMap.addMarker(new MarkerOptions().position(
                    new LatLng(polylinePoints.get(polylinePoints.size()-1).latitude,
                            polylinePoints.get(polylinePoints.size()-1).longitude)).icon(checkerFlag));
        }
    }

    private void goToLastLocation() {
        if(isSwitchGoToLastLocation) {
            int lastEntry = db.getLastEntry();
            if(lastEntry!=0) {
                if(db.getLastEntryOrderedById(lastEntry)!=null) {
                    Run run = db.getLastEntryOrderedById(lastEntry);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(run.getLat(), run.getLng()), 16.0f));
                    toolbar_title.setText("Distance: 0.0 Km" + "\nSpeed: 0.0 Km/h");
                } else {
                    toolbar_title.setText("Distance: 0.0 Km" + "\nSpeed: 0.0 Km/h");
                }
            } else {
                toolbar_title.setText("Distance: 0.0 Km" + "\nSpeed: 0.0 Km/h");
            }
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

    @Override
    protected void onStop() {
        super.onStop();
        int isVisible = fabStopRecording.getVisibility();
        if(isVisible==0) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("StopButtonIsVisible", true);
            this.getIntent().putExtras(bundle);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    public void onCameraMove() {
        isDisableZoomCamera=false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, ForegroundService.class));
        unregisterReceiver(receiver);
    }

    public void onClickRecording(View view) {
        switch(view.getId()) {
            case R.id.fabRecording:
                Context contextFabRecording = getApplicationContext();
                Intent intentForegroundService = new Intent(contextFabRecording, ForegroundService.class);
                intentForegroundService.setAction("ACTION_START");
                contextFabRecording.startForegroundService(intentForegroundService);

                //LatLng latLng = new LatLng(lastLat, lastLng);
                //markerName = mMap.addMarker(new MarkerOptions().position(latLng).title(getResources().getString(R.string.current_location)));
                mMap.clear();

                fadingButtons(R.id.fabRecording);

                if(isSwitchPausedActivated) {
                    fadingButtons(R.id.fabPauseRecording);
                }
                break;
            case R.id.fabStopRecording:
                stopService(new Intent(this, ForegroundService.class));

                // since paused switch is activated and comment is automatically filled
                // we do not provide the alertDialog
                if(!isSwitchPausedActivated) {
                    createAlertDialog();
                }

                fadingButtons(R.id.fabStopRecording);
                fadingButtons(R.id.fabPauseRecording);

                isPauseRecordingClicked = false;

                createCheckerFlag(polylinePointsTemp);

                break;
            case R.id.fabPauseRecording:
                if(isPauseRecordingClicked==false) {
                    //pause button was pressed
                    sendBroadcastToForegroundService("Pausing");
                    isPauseRecordingClicked = true;
                } else {
                    //pause button was not pressed
                    sendBroadcastToForegroundService(null);
                    isPauseRecordingClicked = false;
                }
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

    private void showAlertDialogWithTracks() {
        DatabaseHandler db = new DatabaseHandler(this);
        List<Run> allEntries = db.getAllEntriesGroupedByRun();

        if(polylinePoints.size()>0) {
            polylinePoints.clear();
        }

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapsActivity.this);
        if(allEntries.size() == 0) {
            builderSingle.setTitle(getResources().getString(R.string.no_run_available));
        } else {
            builderSingle.setTitle(getResources().getString(R.string.select_one_run));
        }
        builderSingle.setIcon(R.drawable.icon_notification);

        // prevents closing alertdialog when clicking outside of it
        builderSingle.setCancelable(false);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.select_dialog_singlechoice);
        Duration duration;
        Period period;
        int days, hours, minutes, count;

        for(int i = 0; i<allEntries.size(); i++) {
            duration = new Duration(allEntries.get(i).getDateTimeInMs());
            period = duration.toPeriod();
            days = period.getDays();
            hours = period.getHours();
            minutes = period.getMinutes();
            count = db.countDataOfRun(allEntries.get(i).getNumber_of_run());

            arrayAdapter.add(allEntries.get(i).getDateTime()
                    + "\n" + String.format("Meters covered: %.2f", allEntries.get(i).getMeters_covered()/1000) + " Km"
                    + "\n" + "Duration: " + days + " days " + hours + " hours " + minutes + " minutes"
                    + "\n" + count + " points to load");
        }

        builderSingle.setNegativeButton(getResources().getString(R.string.buttonCancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int numberOfRun = allEntries.get(which).getNumber_of_run();

                final LatLng[] latLng = new LatLng[1];

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        //Background work here
                        List<Run> allEntries = db.getSingleEntryOrderedByDateTime(numberOfRun);
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
                            polylinePoints.add(latLng[0]);
                        }

                        if(allEntries.size()>=50000) {
                            polylinePoints = PolyUtil.simplify(polylinePoints, 40);
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //UI Thread work here
                                if(allEntries.size()>=50000) {
                                    fillPolyPoints(polylinePoints);
                                } else {
                                    showPolyline(sourcePoints);
                                }
                            }
                        });
                    }
                });
            }
        });
        builderSingle.show();
    }

    private void showPolyline(List<ColoredPoint> points) {
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
                    .position(new LatLng(polylinePoints.get(0).latitude, polylinePoints.get(0).longitude))
                    .title(getString(R.string.starting_position)));
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

        polyline = mMap.addPolyline(new PolylineOptions().addAll(currentSegment).color(currentColor).jointType(JointType.ROUND).width(15.0f));
        createCheckerFlag(polylinePoints);
    }

    private void fillPolyPoints(List<LatLng> polylinePoints) {
        double lat = polylinePoints.get(0).latitude;
        double lng = polylinePoints.get(0).longitude;

        LatLng latLng = new LatLng(lat, lng);
        PatternItem DOT = new Dot();
        PatternItem GAP = new Gap(20);
        List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);

        mMap.clear();
        polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
        polyline.setClickable(true);

        mMap.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.starting_position)));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 16));
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(@NonNull Polyline polyline) {
                polyline.setPattern(PATTERN_POLYLINE_DOTTED);
            }
        });
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
                    fadeInAnimation.setDuration(1500);
                    fadeOutAnimation.setDuration(1500);
                    // Keep stop button visible, start button invisible
                    fadeInAnimation.setFillAfter(true);
                    fadeOutAnimation.setFillAfter(true);

                    fabStopRecording.startAnimation(fadeInAnimation);
                    fabPauseRecording.startAnimation(fadeInAnimation);
                    fabStartRecording.startAnimation(fadeOutAnimation);

                    Bundle bundle = new Bundle();
                    bundle.putBoolean("StopButtonIsVisible", true);
                    getIntent().putExtras(bundle);
                }
            }, 2000);// set time as per your requirement
        } else if(fabButton==R.id.fabStopRecording) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Define the animators
                    Animation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
                    Animation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
                    // Duration of animation
                    fadeInAnimation.setDuration(1500);
                    fadeOutAnimation.setDuration(1500);
                    // Keep stop button visible, start button invisible
                    fadeInAnimation.setFillAfter(true);
                    fadeOutAnimation.setFillAfter(true);

                    fabStopRecording.startAnimation(fadeOutAnimation);
                    fabPauseRecording.startAnimation(fadeOutAnimation);
                    fabStartRecording.startAnimation(fadeInAnimation);
                }
            }, 2000);// set time as per your requirement
        }
    }

    private void createAlertDialog() {
        final EditText taskEditText = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle(getResources().getString(R.string.save_exercise));
        builder.setMessage(getResources().getString(R.string.comment_exercise));
        builder.setView(taskEditText);
        // prevents closing alertdialog when clicking outside of it
        builder.setCancelable(false);
        builder.setPositiveButton(getResources().getString(R.string.buttonSave), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String task = String.valueOf(taskEditText.getText());
                        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
                        int lastEntryOfRun = db.getLastEntry();
                        db.updateComment(task, lastEntryOfRun);
                    }
                });
        builder.setNegativeButton(getResources().getString(R.string.buttonCancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

//                        TODO: if recording is running but nothing is saved to database because no fix is available,
//                              last entry will be fetched and deleted. leading to the problem that with every recording and no fix
//                              one dataset after the other will be deleted
//                        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
//                        int lastEntryOfRun = db.getLastEntry();
//                        db.deleteLastRun(lastEntryOfRun);
                    }
                })
                .create();

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if(button!=null) {
                    button.setEnabled(false);
                }
            }
        });
        alertDialog.show();

        TextWatcher mTextEditorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count>0) {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        taskEditText.addTextChangedListener(mTextEditorWatcher);
    }

    private void sendBroadcastToForegroundService(String value) {
        Intent intent=new Intent();
        intent.setAction(SharedPref.STATIC_BROADCAST_PAUSE_ACTION);
        Bundle bundle = new Bundle();
        bundle.putString("Pausing", value);
        intent.putExtras(bundle);
        getApplicationContext().sendBroadcast(intent);
    }

    public void onClickShowRecordedPlusKalmanfilter(View view) {
        mMap.clear();

        //start with fetching and then filling polyline
        int lastRun = db.getLastEntry();
        List<Run> allEntries = db.getSingleEntryOrderedByDateTime(lastRun);
        LatLng latLng;

        for(int i = 0;i<allEntries.size(); i++) {
            latLng = new LatLng(allEntries.get(i).getLat(), allEntries.get(i).getLng());
            polylinePoints.add(latLng);
        }
        polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));

        //start with fetching kalman filtered values and then filling polyline
        lastRun = db.getLastEntry();
        allEntries = db.getSingleEntryOrderedByDateTime(lastRun);

        for(int i = 0;i<allEntries.size(); i++) {
            latLng = new LatLng(allEntries.get(i).getLat(), allEntries.get(i).getLng());
            polylinePoints.add(latLng);
        }
        polylineKalmanFiltered = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
    }

//    private class DataBroadcastReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            Timber.d("DataBroadcastReceiver %s", action);
//            ArrayList<Parcelable> polylinePoints = intent.getExtras().getParcelableArrayList(SharedPref.STATIC_BROADCAST_ACTION);
//            speed = intent.getExtras().getString("SPEED");
//            coveredDistance = intent.getExtras().getFloat("DISTANCE");
//
//            int size = polylinePoints.size();
//
//            if (size == 0) {
//                lastLat = 0;
//                lastLng = 0;
//            } else if (size == 1) {
//                LatLng lastEntry = (LatLng) polylinePoints.get(polylinePoints.size());
//                lastLat = lastEntry.latitude;
//                lastLng = lastEntry.longitude;
//                polylinePointsTemp.add(lastEntry);
//            } else {
//                LatLng lastEntry = (LatLng) polylinePoints.get(polylinePoints.size() - 2);
//                lastLat = lastEntry.latitude;
//                lastLng = lastEntry.longitude;
//                polylinePointsTemp.add(lastEntry);
//            }
//            createPolypoints(lastLat, lastLng, polylinePointsTemp);
//        }
//    }

    private class DataBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Timber.d("DataBroadcastReceiver %s", action);
            //ArrayList<Parcelable> polylinePoints = intent.getExtras().getParcelableArrayList(SharedPref.STATIC_BROADCAST_ACTION);
            speed = intent.getExtras().getString("SPEED");
            coveredDistance = intent.getExtras().getFloat("DISTANCE");
            lastLat = intent.getExtras().getDouble("LAT");
            lastLng = intent.getExtras().getDouble("LON");

            createPolypoints(speed, coveredDistance, lastLat, lastLng);
        }
    }
}