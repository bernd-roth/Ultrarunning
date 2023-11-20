package at.co.netconsulting.runningtracker;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.Run;

public class StatisticsActivity extends AppCompatActivity {
    private LineChart mChart;
    private DatabaseHandler db;
    private List<Run> listOfRun;
    private TextView textViewMaxSpeed, textViewDistance, textViewAvgSpeed;
    private float maxSpeed, avgSpeed, totalDistance;
    private List<Float> listSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        initializeObjects();
        callDatabase();
        calcAvgSpeedMaxSpeedTotalDistance();
        renderData();
        setData();
        setTextView();
    }

    private void setTextView() {
        //average speed
        textViewAvgSpeed.setText("Average speed: " + avgSpeed + " Km/h");
        //max speed
        textViewMaxSpeed.setText("Max. speed: " + maxSpeed + " Km/h");
        //distance
        textViewDistance.setText("Total distance: " + totalDistance + " Km");
    }

    private void initializeObjects() {
        mChart = findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);

        textViewMaxSpeed = findViewById(R.id.textViewMaxSpeed);
        textViewDistance = findViewById(R.id.textViewDistance);
        textViewAvgSpeed = findViewById(R.id.textViewAvgSpeed);

        listOfRun = new ArrayList<>();
        listSpeed = new ArrayList<>();
        db = new DatabaseHandler(this);
    }

    public void renderData() {
        LimitLine llXAxis = new LimitLine(10f, "Index 10");
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(10f);

        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
//        xAxis.setAxisMaximum(10f);
        xAxis.setAxisMinimum(0f);
//        xAxis.setAxisMaximum(totalDistance);
        xAxis.setDrawLimitLinesBehindData(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

//        LimitLine ll1 = new LimitLine(215f, "Maximum Limit");
//        ll1.setLineWidth(4f);
//        ll1.enableDashedLine(10f, 10f, 0f);
//        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
//        ll1.setTextSize(10f);
//
//        LimitLine ll2 = new LimitLine(70f, "Minimum Limit");
//        ll2.setLineWidth(4f);
//        ll2.enableDashedLine(10f, 10f, 0f);
//        ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
//        ll2.setTextSize(10f);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
//        leftAxis.addLimitLine(ll1);
//        leftAxis.addLimitLine(ll2);
        leftAxis.setAxisMaximum(maxSpeed);
        leftAxis.setAxisMinimum(0);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        mChart.getAxisRight().setEnabled(false);
    }

    private void callDatabase() {
        int intLastEntry = db.getLastEntry();

        if(intLastEntry!=0) {
            listOfRun = db.getSingleEntryForStatistics(intLastEntry);
        }
    }

    private void setData() {
        ArrayList<Entry> values = new ArrayList<>();
        int sizeOfList = listOfRun.size();

        if(sizeOfList!=0) {
            for (Run run : listOfRun) {
                float coveredMeter = (float) run.getMeters_covered();
                float speed = run.getSpeed();
                values.add(new Entry(coveredMeter, speed));
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
            set1 = new LineDataSet(values, "Time/Velocity");
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

    private void calcAvgSpeedMaxSpeedTotalDistance() {
        if(listOfRun.size()>0) {
            for (int i = 0; i < listOfRun.size(); i++) {
                float mSpeed = listOfRun.get(i).getSpeed();
                avgSpeed += mSpeed;
                listSpeed.add(mSpeed);
            }
            avgSpeed/=listOfRun.size();
            maxSpeed = Collections.max(listSpeed);
            totalDistance = (float) listOfRun.get(listOfRun.size()-1).getMeters_covered();
            totalDistance/=1000;
        } else {
            //create test data for textview
            avgSpeed=8;
            avgSpeed/=3;
            listSpeed.add(8f);
            listSpeed.add(8f);
            listSpeed.add(9f);
            listSpeed.add(14f);
            listSpeed.add(6f);
            listSpeed.add(7f);
            maxSpeed = Collections.max(listSpeed);
            totalDistance = 25195;
            totalDistance/=1000;
        }
    }
}