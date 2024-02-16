package at.co.netconsulting.runningtracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.Run;

public class DetailedGraphActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraIdleListener {
    private List<Run> run;
    private DatabaseHandler db;
    private GoogleMap mMap;
    private View mapView;
    private SupportMapFragment mapFragment;
    private LineChart mChart;
    private LatLng latLng;
    private Marker marker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_graph);

        initializeObjects();
        retrieveAllEntriesForSingleRunFromDatabase(savedInstanceState);
        renderData();
        setData();
    }

    private void setData() {
        ArrayList<Entry> values = new ArrayList<>();
        int sizeOfList = run.size();
        //int i = 0;

        if(sizeOfList!=0) {
            for (Run run : run) {
                //if(i%60==0) {
                float coveredMeter = (float) run.getMeters_covered();
                float speed = run.getSpeed();
                values.add(new Entry(coveredMeter, speed));
                //    i++;
                //}
                //i++;
            }
        }

        LineDataSet set1;
        mChart.getDescription().setEnabled(false);

        if (mChart.getData() != null &&
                mChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) mChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();
        } else {
            set1 = new LineDataSet(values, getString(R.string.distance_speed));
            set1.setDrawIcons(false);
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.DKGRAY);
            set1.setCircleColor(Color.DKGRAY);
            set1.setLineWidth(1f);
            set1.setCircleRadius(3f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setDrawFilled(true);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

            if (Utils.getSDKInt() >= 18) {
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_blue);
                set1.setFillDrawable(drawable);
            } else {
                set1.setFillColor(Color.DKGRAY);
            }
            LineData lineData = new LineData(set1);
            //ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            //dataSets.add(set1);
            //LineData data = new LineData(dataSets);
            mChart.setData(lineData);
            mChart.notifyDataSetChanged();
            mChart.invalidate();
        }
    }

    private void renderData() {
        LimitLine llXAxis = new LimitLine(10f, "Index 10");
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(10f);

        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setAxisMinimum(0f);
        xAxis.setDrawLimitLinesBehindData(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setAxisMaximum(run.size());

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        //Run maxSpeed = run.stream().max(Comparator.comparing(v -> v.getSpeed())).get();
        //leftAxis.setAxisMaximum(maxSpeed.getSpeed());
        leftAxis.setAxisMinimum(0);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        //show labels underneath chart
        mChart.getXAxis().setDrawLabels(true);
        mChart.getAxisRight().setEnabled(false);
    }

    private void retrieveAllEntriesForSingleRunFromDatabase(Bundle savedInstanceState) {
        Intent mIntent = getIntent();
        int intValue = mIntent.getIntExtra("numberOfRun", 0);
        run = db.getSingleEntryForStatistics(intValue);
    }
    private void initializeObjects() {
        run = new ArrayList<>();
        db = new DatabaseHandler(this);

        mChart = findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                double x = e.getX();
                double y = e.getY();
                double speed = 0;
                double distance = 0;
                long time = 0;

                for(int i = 0; i<run.size(); i++) {
                    if((run.get(i).getMeters_covered() == x) &&
                            (run.get(i).getSpeed() == y)) {
                        latLng = new LatLng(run.get(i).getLat(), run.get(i).getLng());
                        speed = run.get(i).getSpeed();
                        distance = run.get(i).getMeters_covered() / 1000;
                        time = run.get(i).getDateTimeInMs();
                    }
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//                mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

                if(marker != null) {
                    marker.remove();
                    marker = null;
                }
                Duration duration = Duration.ofMillis(time);

                String sTime = String.format("%02d:%02d:%02d", duration.toHours() % 24,
                        duration.toMinutes() % 60, duration.getSeconds() % 60);
                String sSpeed = String.format("Speed: %.2f km/h", speed);
                String sDistance = String.format("Distance: %.2f Km", distance);

                marker = mMap.addMarker(new MarkerOptions().position(latLng).title(sTime).snippet(sDistance + "\n" + sSpeed));
                marker.showInfoWindow();
            }

            @Override
            public void onNothingSelected() {

            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapDetailedGraph);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onCameraIdle() {

    }

    @Override
    public void onCameraMove() {

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        //if map is moved around, automatic camera movement is disabled
        mMap.setOnCameraMoveListener(this);
        mMap.setMapType(mMap.MAP_TYPE_NORMAL);

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(getApplicationContext());
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(getApplicationContext());
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(getApplicationContext());
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });

        // Get map views
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().
                findFragmentById(R.id.mapDetailedGraph);
        mapView = mapFragment.getView();
        View location_button = mapView.findViewWithTag("GoogleMapMyLocationButton");
        View zoom_in_button = mapView.findViewWithTag("GoogleMapZoomInButton");
        View zoom_layout = (View) zoom_in_button.getParent();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                location_button.getLayoutParams();
        // position on right bottom
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.setMargins(0, 5, 5, 255);

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
        mMap.setPadding(0, 0, 0, 90);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.setMapStyle(null);

        drawLine();
    }

    private void drawLine() {
        PolylineOptions lineOptions = new PolylineOptions().width(5).color(Color.RED);
        List<LatLng> polylinePoints = new ArrayList<>();

        for(int i = 0; i<run.size(); i++) {
            polylinePoints.add(new LatLng(run.get(i).getLat(), run.get(i).getLng()));
        }
        Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(Color.MAGENTA).jointType(JointType.ROUND).width(15.0f));
    }
}