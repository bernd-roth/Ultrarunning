package at.co.netconsulting.runningtracker;

import static at.co.netconsulting.runningtracker.general.StaticFields.df;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.Run;

public class StatisticsActivity extends AppCompatActivity {
    private LineChart mChart;
    private DatabaseHandler db;
    private ArrayList<Entry> values;
    private List<Run> listOfRun;
    private List<Float> maxSpeed;
    private float meters, speed;
    private TextView textViewMaxSpeed, textViewDistance, textViewAvgSpeed;
    private float mSpeed, avgSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        getSupportActionBar().hide();

        initObjects();
        callDatabase();

        Description desc = new Description();
        desc.setText("Velocity");

        mChart = findViewById(R.id.linechart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setDescription(desc);
        MyMarkerView mv = new MyMarkerView(getApplicationContext(), R.layout.custom_marker_view);
        mv.setChartView(mChart);
        mChart.setMarker(mv);
        findHighestLowestValuesSpeed();
        renderData();
        textViewMaxSpeed.setText("Max. speed: " +  df.format(mSpeed) + " km/h");
        textViewDistance.setText("Distance: " + df.format(meters) + " meter");
        textViewAvgSpeed.setText("Avg: speed: " + df.format(avgSpeed) + " meter");
    }

    private void callDatabase() {
        int intLastEntry = db.getLastEntry();
        listOfRun = db.getSingleEntryOrderedByDateTime(intLastEntry);
    }

    private void initObjects() {
        db = new DatabaseHandler(this);
        values = new ArrayList<>();
        maxSpeed = new LinkedList<Float>();
        textViewMaxSpeed = findViewById(R.id.textViewMaxSpeed);
        textViewDistance = findViewById(R.id.textViewDistance);
        textViewAvgSpeed = findViewById(R.id.textViewAvgSpeed);
    }

    private void findHighestLowestValuesSpeed() {
        if(listOfRun.size()>0) {
            for (int i = 0; i < listOfRun.size(); i++) {
                meters = (float) listOfRun.get(i).getMeters_covered();
                speed = (float) listOfRun.get(i).getSpeed();
                values.add(new Entry(meters, speed));
                float mSpeed = listOfRun.get(i).getSpeed();
                maxSpeed.add(mSpeed);
                avgSpeed += speed;
            }
            mSpeed = Collections.max(maxSpeed);
            avgSpeed /= listOfRun.size();
        }
    }

    private void showAlertDialog() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(StatisticsActivity.this);
        if(listOfRun.size()==0) {
            builderSingle.setTitle(getResources().getString(R.string.no_run_available));
        }
        builderSingle.setIcon(R.drawable.icon_notification);

        // prevents closing alertdialog when clicking outside of it
        builderSingle.setCancelable(false);

        builderSingle.setPositiveButton(getResources().getString(R.string.buttonOk), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builderSingle.show();
    }

    public void renderData() {
        LimitLine llXAxis = new LimitLine(10f, "Index 10");
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(10f);

        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.resetAxisMaximum();
        xAxis.setAxisMinimum(0f);
        xAxis.setDrawLimitLinesBehindData(true);

        LimitLine ll1 = new LimitLine(215f, "Maximum Limit");
        ll1.setLineWidth(4f);
        ll1.enableDashedLine(10f, 10f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setTextSize(10f);

        LimitLine ll2 = new LimitLine(70f, "Minimum Limit");
        ll2.setLineWidth(4f);
        ll2.enableDashedLine(10f, 10f, 0f);
        ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        ll2.setTextSize(10f);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        //leftAxis.addLimitLine(ll1);
        //leftAxis.addLimitLine(ll2);
        leftAxis.resetAxisMaximum();
        leftAxis.setAxisMinimum(0f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        mChart.getAxisRight().setEnabled(false);
        setData();
    }

    private void setData() {
        LineDataSet set1;
        if (mChart.getData() != null &&
                mChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) mChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();
        } else {
            set1 = new LineDataSet(values, "Velocity");
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
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);
        }
    }
}