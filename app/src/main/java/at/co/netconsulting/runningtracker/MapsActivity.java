package at.co.netconsulting.runningtracker;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.PolyUtil;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import at.co.netconsulting.runningtracker.databinding.ActivityMapsBinding;
import at.co.netconsulting.runningtracker.general.BaseActivity;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.pojo.ColoredPoint;
import at.co.netconsulting.runningtracker.pojo.LocationChangeEvent;
import at.co.netconsulting.runningtracker.service.ForegroundService;
import at.co.netconsulting.runningtracker.view.DrawView;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener {
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    //Polyline
    private Polyline polyline;
    private List<LatLng> polylinePoints;
    private boolean isDisableZoomCamera;
    private FloatingActionButton fabStartRecording, fabStopRecording, fabStatistics;
    private String mapType;
    private SupportMapFragment mapFragment;
    private String[] permissions;
    private LocationManager locationManager;
    private boolean gps_enabled;
    private boolean startingPoint;
    private Toolbar toolbar;
    private TextView toolbar_title, textViewSlow, textViewFast;
    private float coveredDistance;
    private PolyUtil polyUtil;
    private DrawView drawView;
    private View mapView;
    private Marker marker;
    private LatLng latLng;
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
    }

    private void createPolypoints(List<LatLng> polylinePoints) {
        boolean isServiceRunning = isServiceRunning(getString(R.string.serviceName));
        //isServiceRunning = true; // FIXME

        if (!isServiceRunning) {
            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLng), 0));
            toolbar_title.setText("Distance: 0.0 Km" + "\nSpeed: 0.0");
        } else {
            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLng), 16));
            //Projection projection = mMap.getProjection();

            //if (startingPoint) {
                //marker = mMap.addMarker(new MarkerOptions().position(new LatLng(lastLat, lastLng)).title(getResources().getString(R.string.current_location)));
                //marker.showInfoWindow();
                //startingPoint = false;
            //} else {
                //polylinePoints = groupPoints(polylinePoints, projection);

                if(polyline!=null) {
                    polyline.remove();
                    mMap.clear();
                    //mMap.addMarker(new MarkerOptions().position(new LatLng(polylinePoints.get(0).latitude, polylinePoints.get(0).longitude)).title(getResources().getString(R.string.current_location)));
                    //marker.showInfoWindow();
                }
                polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
            //}
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
        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.LTGRAY);
        toolbar_title = findViewById(R.id.toolbar_title);
        textViewSlow = findViewById(R.id.textViewSlow);
        //textViewFast = findViewById(R.id.textViewFast);

        polylinePoints = new ArrayList<>();
        permissions = new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS};
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        isDisableZoomCamera = true;
        startingPoint = true;
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
        mapView = mapFragment.getView();
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
                }
            }
        });
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

            //move to last location when drawing the route
            double lat = polylinePoints.get(polylinePoints.size()-1).latitude;
            double lon = polylinePoints.get(polylinePoints.size()-1).longitude;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 16f));
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
        //unregisterReceiver(receiver);
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
                break;
            case R.id.fabStopRecording:
                stopService(new Intent(this, ForegroundService.class));

                fadingButtons(R.id.fabStopRecording);

                createCheckerFlag(polylinePoints);

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

        PatternItem DOT = new Dot();
        PatternItem GAP = new Gap(20);
        List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);

        mMap.clear();
        polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
        polyline.setClickable(true);

        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(getString(R.string.starting_position)));
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
                    fabStartRecording.startAnimation(fadeInAnimation);
                }
            }, 2000);// set time as per your requirement
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocationChangeEvent event) {
        latLng = new LatLng(event.location.getLatitude(), event.location.getLongitude());

        polylinePoints.add(latLng);

        createPolypoints(polylinePoints);
    };
}