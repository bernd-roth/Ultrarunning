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
import android.os.Handler;
import android.os.Parcelable;
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
import at.co.netconsulting.runningtracker.pojo.Run;
import at.co.netconsulting.runningtracker.service.ForegroundService;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener {
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    //Polyline
    private Polyline polyline;
    private List<LatLng> polylinePoints, polylinePointsTemp;
    private boolean isDisableZoomCamera;
    private FloatingActionButton fabStartRecording, fabStopRecording, fabStatistics, fabPauseRecording;
    private double lastLat, lastLng;
    private String mapType;
    private SupportMapFragment mapFragment;
    private String[] permissions;
    private LocationManager locationManager;
    private boolean gps_enabled;
    private boolean startingPoint;
    private BroadcastReceiver receiver;
    private boolean isPauseRecordingClicked, isSwitchPausedActivated;

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

        sh = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE, Context.MODE_PRIVATE);
        isSwitchPausedActivated = sh.getBoolean(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE, false);
    }

    private void createListenerAndfillPolyPoints(double lastLat, double lastLng, List<LatLng> polylinePoints) {
        boolean isServiceRunning = isServiceRunning(getString(R.string.serviceName));

        if(!isServiceRunning) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLng), 0));
        } else {
            LatLng latLng = new LatLng(lastLat, lastLng);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

            if(startingPoint) {
                mMap.addMarker(new MarkerOptions().position(latLng).title(getResources().getString(R.string.current_location)));
                startingPoint = false;
            } else {
                polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
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
        polylinePoints = new ArrayList<>();
        polylinePointsTemp = new ArrayList<>();
        configureReceiver();
        permissions = new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS};
        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        isDisableZoomCamera = true;
        startingPoint = true;
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
        unregisterReceiver(receiver);
    }

    public void onClickRecording(View view) {
        switch(view.getId()) {
            case R.id.fabRecording:
                Context contextFabRecording = getApplicationContext();
                Intent intentForegroundService = new Intent(contextFabRecording, ForegroundService.class);
                intentForegroundService.setAction("ACTION_START");
                contextFabRecording.startForegroundService(intentForegroundService);
                //createListenerAndfillPolyPoints(lastLat, lastLng);
                LatLng latLng = new LatLng(lastLat, lastLng);
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
                //Intent intentStatistics = new Intent(MapsActivity.this, StatisticsActivity.class);
                //this.startActivity(intentStatistics);
                break;
            case R.id.fabSettings:
                Intent intentSettings = new Intent(MapsActivity.this, SettingsActivity.class);
                this.startActivity(intentSettings);
                break;
            case R.id.fabTracks:
                showAlertDialogWithTtracks();
        }
    }

    private void showAlertDialogWithTtracks() {
        DatabaseHandler db = new DatabaseHandler(this);
        List<Run> allEntries = db.getAllEntriesGroupedByRun();

        if(polylinePoints.size()>0) {
            polylinePoints.clear();
        }

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapsActivity.this);
        if(allEntries.size()<=0) {
            builderSingle.setTitle(getResources().getString(R.string.no_run_available));
        } else {
            builderSingle.setTitle(getResources().getString(R.string.select_one_run));
        }
        builderSingle.setIcon(R.drawable.icon_notification);

        // prevents closing alertdialog when clicking outside of it
        builderSingle.setCancelable(false);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.select_dialog_singlechoice);
        for(int i = 0; i<allEntries.size(); i++) {
            arrayAdapter.add(allEntries.get(i).getNumber_of_run() + "-DateTime: " + allEntries.get(i).getDateTime());
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
                String numberOfRun = arrayAdapter.getItem(which);
                String[] splittedString = numberOfRun.split("-");
                int intNumberOfRun = Integer.parseInt(splittedString[0]);

                List<Run> allEntries = db.getSingleEntryOrderedByDateTime(intNumberOfRun);
                LatLng latLng;

                for(int i = 0;i<allEntries.size(); i++) {
                    latLng = new LatLng(allEntries.get(i).getLat(), allEntries.get(i).getLng());
                    polylinePoints.add(latLng);
                }
                fillPolyPoints(polylinePoints);
            }
        });
        builderSingle.show();
    }

    private void fillPolyPoints(List<LatLng> polylinePoints) {
        double lat = polylinePoints.get(0).latitude;
        double lng = polylinePoints.get(0).longitude;

        LatLng latLng = new LatLng(lat, lng);

        mMap.clear();
        polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
        mMap.addMarker(new MarkerOptions().position(latLng).title("Current location"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));
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
                        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
                        int lastEntryOfRun = db.getLastEntry();
                        db.deleteLastRun(lastEntryOfRun);
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
                polylinePointsTemp.add(lastEntry);
            } else {
                LatLng lastEntry = (LatLng) polylinePoints.get(polylinePoints.size() - 2);
                lastLat = lastEntry.latitude;
                lastLng = lastEntry.longitude;
                polylinePointsTemp.add(lastEntry);
            }
            createListenerAndfillPolyPoints(lastLat, lastLng, polylinePointsTemp);
        }
    }
}