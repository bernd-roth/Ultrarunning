package at.co.netconsulting.runningtracker;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.Run;

public class StatisticsActivity extends AppCompatActivity {
    private LineChart mChart;
    private DatabaseHandler db;
    private List<Run> listOfRun;
    private TextView textViewMaxSpeed, textViewDistance, textViewAvgSpeed;
    private float maxSpeed, avgSpeed, totalDistance;
    private List<Float> listSpeed;
    private TableLayout tableSection, tableHeader;
    private TextView txtGeneric;
    private TableRow tr;
    private long HH, MM, SS;
    private Spinner spinnerRunChoose;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        initializeObjects();
        callDatabaseForSpinner();
        callDatabase();
        calcAvgSpeedMaxSpeedTotalDistance();
        renderData();
        setData();
        setTextView();
        //List<Long> groupedSectionList = calculateSections();
        //showTableLayout(groupedSectionList);
    }

    private void callDatabaseForSpinner() {
        ArrayList parsedToStringRun = new ArrayList<String>();

        for(Run run : db.getAllEntriesOrderedByDate()) {
            parsedToStringRun.add(run.getNumber_of_run() + ":" + run.getDateTime());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, parsedToStringRun);
        spinnerRunChoose.setAdapter(adapter);
    }

    private List<Long> calculateSections() {
        double lap, oldLap = 0;
        List<Long> groupedList = new ArrayList<>();

        long oldMilliSeconds, currentMilliSeconds = 0, calculatedMilliSeconds;
        oldMilliSeconds = listOfRun.get(0).getDateTimeInMs();

        for(int i = 0; i<listOfRun.size(); i++) {
            lap = listOfRun.get(i).getLaps();

            if(lap!=oldLap || i==listOfRun.size()-1) {
                currentMilliSeconds=listOfRun.get(i).getDateTimeInMs();
                calculatedMilliSeconds=currentMilliSeconds-oldMilliSeconds;

                groupedList.add(calculatedMilliSeconds);
                oldMilliSeconds=currentMilliSeconds;
                oldLap=lap;
            }
        }
        return groupedList;
    }
    private void showTableLayout(List<Long> groupedSectionList) {
        int rows = groupedSectionList.size();
        int colums  = 1;
        int counter = 1;
        tableHeader.setStretchAllColumns(true);
        tableHeader.bringToFront();

        tableSection.setStretchAllColumns(true);
        tableSection.bringToFront();

        //Header
        for(int i = 0; i < 1; i++) {
            tr = new TableRow(this);
            for (int j = 0; j < colums; j++) {
                if(txtGeneric==null) {
                    txtGeneric = new TextView(this);
                    txtGeneric.setTextSize(18);

                    txtGeneric.setText("\t\t\t\t" + getResources().getString(R.string.lap) + "\t\t\t\t\t\t\t\t\t" + getResources().getString(R.string.time));
                    txtGeneric.setBackgroundColor(Color.LTGRAY);
                    tr.addView(txtGeneric);
                }
            }
            tableHeader.addView(tr);
        }

        // remove previous tablelayout if necessary
        if(tableSection.getChildCount()>0) {
            tableSection.removeViews(0, Math.max(0, tableSection.getChildCount()));
        }

        //Content of rows
        for(int i = 0; i < rows; i++){
            tr =  new TableRow(this);
            for(int j = 0; j < colums; j++) {
                txtGeneric = new TextView(this);
                txtGeneric.setTextSize(18);

                HH = TimeUnit.MILLISECONDS.toHours(groupedSectionList.get(i));
                MM = TimeUnit.MILLISECONDS.toMinutes(groupedSectionList.get(i)) % 60;
                SS = TimeUnit.MILLISECONDS.toSeconds(groupedSectionList.get(i)) % 60;

                txtGeneric.setText("\t\t\t\t" + counter++ + "\t\t\t\t"
                        + HH + " " + getResources().getString(R.string.hours) + " "
                        + MM + " " + getResources().getString(R.string.minutes) + " "
                        + SS + " " + getResources().getString(R.string.seconds));
                tr.addView(txtGeneric);
            }
            tableSection.addView(tr);
        }
    }
    private void setTextView() {
        //average speed
        textViewAvgSpeed.setText(String.format("Average speed: %.2f", avgSpeed) + " Km/h");
        //max speed
        textViewMaxSpeed.setText(String.format("Max. speed: %.2f", maxSpeed) + " Km/h");
        //distance
        textViewDistance.setText(String.format("Total distance: %.3f", totalDistance) + " Km");
    }

    private void initializeObjects() {
        mChart = findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);

        textViewMaxSpeed = findViewById(R.id.textViewMaxSpeed);
        textViewDistance = findViewById(R.id.textViewDistance);
        textViewAvgSpeed = findViewById(R.id.textViewAvgSpeed);

        tableHeader = (TableLayout)findViewById(R.id.tableHeader);
        tableSection = (TableLayout)findViewById(R.id.tableSection);

        listOfRun = new ArrayList<>();
        listSpeed = new ArrayList<>();

        spinnerRunChoose = findViewById(R.id.spinner);
        spinnerRunChoose.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String numberOfRun = spinnerRunChoose.getSelectedItem().toString();
                String[] splittedString = numberOfRun.split(":");
                int intNumberOfRun = Integer.parseInt(splittedString[0]);

                listOfRun.clear();
                listOfRun = db.getSingleEntryForStatistics(intNumberOfRun);

                calcAvgSpeedMaxSpeedTotalDistance();
                renderData();
                setData();
                setTextView();
                List<Long> groupedSectionList = calculateSections();
                showTableLayout(groupedSectionList);
                mChart.notifyDataSetChanged();
                mChart.invalidate();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });

        linearLayout = findViewById(R.id.ll);

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
            set1 = new LineDataSet(values, getString(R.string.time_velocity));
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