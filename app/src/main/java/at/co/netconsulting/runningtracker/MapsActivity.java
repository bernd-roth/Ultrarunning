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
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import at.co.netconsulting.runningtracker.databinding.ActivityMapsBinding;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.BaseActivity;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.service.ForegroundService;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener {
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    //Polyline
    private Polyline polyline;
    private List<LatLng> polylinePoints;
    private boolean isDisableZoomCamera = true;
    private FloatingActionButton fabStartRecording, fabStopRecording;
    private double lastLat, lastLng;
    private String mapType;
    private SupportMapFragment mapFragment;
    private String[] permissions;
    private LocationManager locationManager;
    private boolean gps_enabled;

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
        //redraw Google Map, calling GoogleMap will fail du to NPE
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void loadSharedPreferences() {
        SharedPreferences sh;

        sh = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
        mapType = sh.getString(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_NORMAL");
    }

    private void createListenerAndfillPolyPoints(double lastLat, double lastLng) {
        boolean isServiceRunning = isServiceRunning("at.co.netconsulting.runningtracker.service.ForegroundService");

        if(!isServiceRunning) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLng), 0));
        } else {
            if(lastLat!=0 && lastLng!=0) {
                LatLng latLng = new LatLng(lastLat, lastLng);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

                if (polyline != null) {
                    polyline.setPoints(polylinePoints);
                    Marker markerName = mMap.addMarker(new MarkerOptions().position(latLng).title("Current location"));
                    markerName.remove();
                } else {
                    polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
                    mMap.addMarker(new MarkerOptions().position(latLng).title("Current location"));
                }
            }
        }
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

    private void configureReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SharedPref.STATIC_BROADCAST_ACTION);
        BroadcastReceiver receiver = new DataBroadcastReceiver();
        registerReceiver(receiver, filter);
    }

    private void initObjects() {
        fabStartRecording = findViewById(R.id.fabRecording);
        fabStartRecording.setVisibility(View.VISIBLE);
        fabStopRecording = findViewById(R.id.fabStopRecording);
        fabStopRecording.setVisibility(View.INVISIBLE);
        polylinePoints = new ArrayList<>();
        configureReceiver();
        permissions = new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS};
        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    }

    private void checkIfLocationIsEnabled() {
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled) {
            new AlertDialog.Builder(MapsActivity.this)
                    .setMessage("Please, turn location service on!")
                    .setPositiveButton("Open location settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            MapsActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("Cancel",null)
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

        createListenerAndfillPolyPoints(0, 0);

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
    }

    @Override
    protected void onStop() {
        super.onStop();
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
    }

    public void onClickRecording(View view) {
        switch(view.getId()) {
            case R.id.fabRecording:
                Context contextFabRecording = getApplicationContext();
                Intent intentForegroundService = new Intent(contextFabRecording, ForegroundService.class);
                intentForegroundService.setAction("ACTION_START");
                contextFabRecording.startForegroundService(intentForegroundService);
                createListenerAndfillPolyPoints(lastLat, lastLng);
                fabStopRecording.setVisibility(View.VISIBLE);
                fabStartRecording.setVisibility(View.INVISIBLE);
                break;
            case R.id.fabStopRecording:
                stopService(new Intent(this, ForegroundService.class));
                fabStopRecording.setVisibility(View.INVISIBLE);
                fabStartRecording.setVisibility(View.VISIBLE);
                createAlertDialog();
                break;
            case R.id.fabStatistics:
                Intent intentStatistics = new Intent(MapsActivity.this, StatisticsActivity.class);
                this.startActivity(intentStatistics);
                break;
            case R.id.fabSettings:
                Intent intentSettings = new Intent(MapsActivity.this, SettingsActivity.class);
                this.startActivity(intentSettings);
                break;
        }
    }

    private void createAlertDialog() {
        final EditText taskEditText = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Save exercise and comment it?")
                .setMessage("Comments on your last exercise")
                .setView(taskEditText)
                // prevents closing alertdialog when clicking outside of it
                .setCancelable(false)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String task = String.valueOf(taskEditText.getText());
                        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
                        int lastEntryOfRun = db.getLastEntry();
                        db.updateComment(task, lastEntryOfRun);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
                        int lastEntryOfRun = db.getLastEntry();
                        db.deleteLastRun(lastEntryOfRun);
                    }
                })
                .create();
        dialog.show();
    }

    private class DataBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Timber.d("DataBroadcastReceiver %s", action);
            ArrayList<Parcelable> polylinePoints = intent.getExtras().getParcelableArrayList(SharedPref.STATIC_BROADCAST_ACTION);

            int size = polylinePoints.size();

            if(size==0) {
                lastLat = 0;
                lastLng = 0;
            } else if(size==1) {
                LatLng lastEntry = (LatLng) polylinePoints.get(polylinePoints.size());
                lastLat = lastEntry.latitude;
                lastLng = lastEntry.longitude;
            } else {
                LatLng lastEntry = (LatLng) polylinePoints.get(polylinePoints.size() - 2);
                lastLat = lastEntry.latitude;
                lastLng = lastEntry.longitude;
            }
            createListenerAndfillPolyPoints(lastLat, lastLng);
        }
    }
}