package at.co.netconsulting.runningtracker;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.Run;

public class StatisticsActivity extends AppCompatActivity {
    private LineChart mChart, mChartTimeSpeed;
    private DatabaseHandler db;
    private List<Run> listOfRun;
    private TextView textViewMaxSpeed, textViewDistance, textViewAvgSpeed,
            textViewSlowestLap, textViewFastestLap, textViewStartingElevation,
            textViewEndingElevation, textViewHighestElevation, textViewTotalElevation,
            textViewMovementTime, textViewLowestElevation;
    private float maxSpeed, avgSpeed, totalDistance;
    private double  totalElevation, highestElevation, lastElevationPoint, sumElevation,lowestElevation;
    private String totalMovementTime;
    private List<Float> listSpeed;
    private TableLayout tableSection, tableHeader, tableYearSection, tableHeaderYear;
    private TextView txtGeneric, txtGenericYear;
    private TableRow tr, trYear;
    private long HH, MM, SS;
    private Spinner spinnerRunChoose;
    private LinearLayout linearLayout;
    private View mView, viewSeperator, viewSeperator1;
    private TextView textView;
    private ArrayList<Integer> intNumberOfRun;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        initializeObjects();
        callDatabaseForSpinner();
        callDatabase();
        calcAvgSpeedMaxSpeedTotalDistance();
        calcElevation();
        calcMovementTime();
        renderData();
        renderDataTimeSpeed();
        setData();
        setDataRenderDataTimeSpeed();
        //setTextView();
    }

    private void calcMovementTime() {
        if(listOfRun.size()>0) {
            long firstElement = listOfRun.get(0).getDateTimeInMs();
            long lastElement = listOfRun.get(listOfRun.size()-1).getDateTimeInMs();

            long calcResult = lastElement-firstElement;

            Duration duration = Duration.ofMillis(calcResult);

            totalMovementTime = String.format("%02d:%02d:%02d:%02d", duration.toDays(), duration.toHours() % 24,
                    duration.toMinutes() % 60, duration.getSeconds() % 60);
        }
    }

    private void calcElevation() {
        lastElevationPoint = 0;
        sumElevation = 0;
        totalElevation = 0;
        lastElevationPoint = 0;
        highestElevation = 0;
        lowestElevation = 0;

        if(listOfRun.size()>0) {
            for (int i = 0; i < listOfRun.size(); i++) {
                if(i==0) {
                    lastElevationPoint=listOfRun.get(i).getAltitude();
                } else {
                    sumElevation = listOfRun.get(i).getAltitude()-lastElevationPoint;
                    totalElevation += sumElevation;
                    lastElevationPoint = listOfRun.get(i).getAltitude();
                }
                //highest elevation
                if(listOfRun.get(i).getAltitude()>=highestElevation) {
                    highestElevation = listOfRun.get(i).getAltitude();
                }
                //lowest elevation
                if(listOfRun.get(i).getAltitude()!=0) {
                    lowestElevation = listOfRun.get(i).getAltitude();
                }
            }
        }
    }
    private void callDatabaseForSpinner() {
        ArrayList parsedToStringRun = new ArrayList<String>();
        intNumberOfRun = new ArrayList<Integer>();

        for(Run run : db.getAllEntriesOrderedByRunNumber()) {
            String meters_covered = String.format("%.2f", run.getMeters_covered()/1000);
            parsedToStringRun.add(
                    "Date: " + run.getDateTime()
                    + "\nDistance: " + meters_covered + " Km"
                    + "\nComment: " + run.getComment());
            intNumberOfRun.add(run.getNumber_of_run());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, parsedToStringRun);
        adapter.setDropDownViewResource(android.R.layout.simple_expandable_list_item_1);

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
    private List<Integer> showTableLayout(List<Long> groupedSectionList) {
        List<Integer> slowestFastestLap = new ArrayList<>();
        int rows = groupedSectionList.size();
        int colums  = 1;
        int counter = 1;
        TreeMap<String, Integer> fastestSlowestLap = new TreeMap<>();
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

        for(int i = 0; i < rows; i++){
            fastestSlowestLap.put(String.valueOf(groupedSectionList.get(i)), counter);
            counter++;
        }

        int fastestLap = chooseFastestLap(fastestSlowestLap);
        int slowestLap = chooseSlowestLap(fastestSlowestLap);
        slowestFastestLap.add(fastestLap);
        slowestFastestLap.add(slowestLap);

        counter=1;
        //Content of rows
        for(int i = 0; i < rows; i++){
            tr =  new TableRow(this);
            for(int j = 0; j < colums; j++) {
                txtGeneric = new TextView(this);
                txtGeneric.setTextSize(18);

                HH = TimeUnit.MILLISECONDS.toHours(groupedSectionList.get(i));
                MM = TimeUnit.MILLISECONDS.toMinutes(groupedSectionList.get(i)) % 60;
                SS = TimeUnit.MILLISECONDS.toSeconds(groupedSectionList.get(i)) % 60;

                if(fastestLap==counter) {
                    txtGeneric.setBackgroundResource(R.color.green);
                    txtGeneric.setText("\t\t\t\t" + counter++ + "\t\t\t\t"
                            + HH + " " + getResources().getString(R.string.hours) + " "
                            + MM + " " + getResources().getString(R.string.minutes) + " "
                            + SS + " " + getResources().getString(R.string.seconds));
                    tr.addView(txtGeneric);
                } else if(slowestLap==counter) {
                    txtGeneric.setBackgroundResource(R.color.red);
                    txtGeneric.setText("\t\t\t\t" + counter++ + "\t\t\t\t"
                            + HH + " " + getResources().getString(R.string.hours) + " "
                            + MM + " " + getResources().getString(R.string.minutes) + " "
                            + SS + " " + getResources().getString(R.string.seconds));
                    tr.addView(txtGeneric);
                } else {
                    txtGeneric.setText("\t\t\t\t" + counter++ + "\t\t\t\t"
                            + HH + " " + getResources().getString(R.string.hours) + " "
                            + MM + " " + getResources().getString(R.string.minutes) + " "
                            + SS + " " + getResources().getString(R.string.seconds));
                    tr.addView(txtGeneric);
                }
            }
            tableSection.addView(tr);
        }
        return slowestFastestLap;
    }

    private int chooseSlowestLap(TreeMap<String, Integer> fastestSlowestLap) {
        int size = fastestSlowestLap.size();
        int slowestLap = fastestSlowestLap.lastEntry().getValue();

        if((slowestLap==size) && size != 1) {
            size -= 2;
            slowestLap = fastestSlowestLap.entrySet().stream().skip(size).findFirst().get().getValue();
        } else {
            slowestLap = fastestSlowestLap.descendingMap().entrySet().stream().skip(0).findFirst().get().getValue();
        }
        return slowestLap;
    }

    private int chooseFastestLap(TreeMap<String, Integer> fastestSlowestLap) {
        int size = fastestSlowestLap.size();
        int fastestLap = fastestSlowestLap.entrySet().stream().skip(0).findFirst().get().getValue();

        if((fastestLap==size) && size != 1) {
            fastestLap = fastestSlowestLap.entrySet().stream().skip(1).findFirst().get().getValue();
        } else {
            fastestLap = fastestSlowestLap.entrySet().stream().skip(0).findFirst().get().getValue();
        }
        return fastestLap;
    }

    private void setTextView(List<Integer> fastestSlowestLap) {
        //average speed
        textViewAvgSpeed.setText(String.format("Average speed: %.2f km/h", avgSpeed));
        //max speed
        textViewMaxSpeed.setText(String.format("Max. speed: %.2f km/h", maxSpeed));
        //distance
        textViewDistance.setText(String.format("Total distance: %.3f km", totalDistance));
        //fastest lap
        textViewFastestLap.setText(String.format("Fastest lap: %03d", fastestSlowestLap.get(0)));
        //slowest lap
        textViewSlowestLap.setText(String.format("Slowest lap: %03d", fastestSlowestLap.get(1)));
        //starting elevation
        textViewStartingElevation.setText(String.format("Starting elevation: %.3f meter", listOfRun.get(0).getAltitude()));
        //ending elevation
        textViewEndingElevation.setText(String.format("Ending elevation: %.3f meter", listOfRun.get(listOfRun.size() - 1).getAltitude()));
        //highest elevation
        textViewHighestElevation.setText(String.format("Highest elevation: %.3f meter", highestElevation));
        //Lowest elevation
        textViewLowestElevation.setText(String.format("Lowest elevation: %.3f meter", lowestElevation));
        //Total elevation
        textViewTotalElevation.setText(String.format("Total elevation: %.3f meter", totalElevation));
        //Movement time
        textViewMovementTime.setText(String.format("Total movement time: %s", totalMovementTime));
    }

    private void initializeObjects() {
        mChart = findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);

        mChartTimeSpeed = findViewById(R.id.chartTimeSpeed);
        mChartTimeSpeed.setTouchEnabled(true);
        mChartTimeSpeed.setPinchZoom(true);

        textViewMaxSpeed = findViewById(R.id.textViewMaxSpeed);
        textViewDistance = findViewById(R.id.textViewDistance);
        textViewAvgSpeed = findViewById(R.id.textViewAvgSpeed);
        textViewSlowestLap = findViewById(R.id.textViewSlowestLap);
        textViewFastestLap = findViewById(R.id.textViewFastestLap);
        textViewStartingElevation = findViewById(R.id.textViewStartingElevation);
        textViewEndingElevation = findViewById(R.id.textViewEndingElevation);
        textViewHighestElevation = findViewById(R.id.textViewHighestElevation);
        textViewLowestElevation = findViewById(R.id.textViewLowestElevation);
        textViewTotalElevation = findViewById(R.id.textViewTotalElevation);
        textViewMovementTime = findViewById(R.id.textViewMovementTime);

        tableHeader = (TableLayout)findViewById(R.id.tableHeader);
        tableSection = (TableLayout)findViewById(R.id.tableSection);

        tableHeaderYear = (TableLayout)findViewById(R.id.tableHeaderYearSection);
        tableYearSection = (TableLayout)findViewById(R.id.tableYearSection);

        listOfRun = new ArrayList<>();
        listSpeed = new ArrayList<>();

        spinnerRunChoose = findViewById(R.id.spinner);
        spinnerRunChoose.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int spinnerPosition = spinnerRunChoose.getSelectedItemPosition();
                int arrayPosition = intNumberOfRun.get(spinnerPosition);

                listOfRun.clear();
                listOfRun = db.getSingleEntryForStatistics(arrayPosition);

                //first graph
                calcAvgSpeedMaxSpeedTotalDistance();
                renderData();
                setData();

                calcElevation();
                calcMovementTime();

                //second graph
                renderDataTimeSpeed();
                setDataRenderDataTimeSpeed();
                List<Long> groupedSectionList = calculateSections();
                List<Integer> fastestSlowestLap = showTableLayout(groupedSectionList);
                setTextView(fastestSlowestLap);

                if(textView == null) {
                    float dp = convertDpToPx(2);
                    createViewSeparator(dp);
                } else if(textView.getVisibility() == TextView.VISIBLE){
                    linearLayout.removeView(textView);
                    linearLayout.removeView(viewSeperator);
                    linearLayout.removeView(viewSeperator1);
                    float dp = convertDpToPx(2);
                    createViewSeparator(dp);
                }

                //table section year
                TreeMap<Integer, Double> year = calculateSectionsYear();
                showTableLayoutYear(year);

                mChart.notifyDataSetChanged();
                mChart.invalidate();
                //Time/Speed chart
                mChartTimeSpeed.notifyDataSetChanged();
                mChartTimeSpeed.invalidate();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });

        linearLayout = findViewById(R.id.ll);

        db = new DatabaseHandler(this);
    }

    private void showTableLayoutYear(TreeMap<Integer, Double> year) {
        int colums = 1;
        int rows = year.size();

        // Convert keys to a List
        List<Integer> keyList = new ArrayList<>(year.keySet());
        // Convert values to a List
        List<Double> valueList = new ArrayList<>(year.values());

        tableHeaderYear.setStretchAllColumns(true);
        tableHeaderYear.bringToFront();

        tableYearSection.setStretchAllColumns(true);
        tableYearSection.bringToFront();

        //Header
        for (int i = 0; i < 1; i++) {
            trYear = new TableRow(this);
            for (int j = 0; j < colums; j++) {
                if (txtGenericYear == null) {
                    txtGenericYear = new TextView(this);
                    txtGenericYear.setTextSize(18);

                    txtGenericYear.setText("\t\t\t\t" + "Year" + "\t\t\t\t\t\t\t\t\t" + "Total Km");
                    txtGenericYear.setBackgroundColor(Color.LTGRAY);
                    trYear.addView(txtGenericYear);
                }
            }
            tableHeaderYear.addView(trYear);
        }

        // remove previous tablelayout if necessary
        if(tableYearSection.getChildCount()>0) {
            tableYearSection.removeViews(0, Math.max(0, tableYearSection.getChildCount()));
        }

        //Content of rows
        for (int i = 0; i < rows; i++) {
            trYear = new TableRow(this);
            for (int j = 0; j < colums; j++) {
                txtGenericYear = new TextView(this);
                txtGenericYear.setTextSize(18);

                String convertedKm = String.format("%.2f", valueList.get(i)/1000);
                txtGenericYear.setText("\t\t\t\t" + keyList.get(i) + "\t\t\t\t\t\t\t\t\t\t" + convertedKm);

                trYear.addView(txtGenericYear);
            }
            tableYearSection.addView(trYear);
        }
    }

    private TreeMap<Integer, Double> calculateSectionsYear() {
        TreeMap<Integer, Double> groupedTreeMap = new TreeMap<>();
        List<Run> listOfRun = db.getAllEntriesForYearCalculation();
        double meters = 0;
        String yearTemp = "0";



        for(int i = 0; i<listOfRun.size(); i++) {
            String[] dateSplit= listOfRun.get(i).getDateTime().split("-");
            String[] yearSplit = dateSplit[2].split(" ");
            String year = yearSplit[0];
            if(year.equals(yearTemp)) {
                meters += listOfRun.get(i).getMeters_covered();
                updateTreeMap(groupedTreeMap, year, meters);
            } else {
                meters = listOfRun.get(i).getMeters_covered();
                yearTemp = year;
                groupedTreeMap.put(Integer.valueOf(year), meters);
            }
        }
        return groupedTreeMap;
    }
    public float convertDpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
    private void createViewSeparator(float dp) {
        mView = new View(StatisticsActivity.this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mView.setBackgroundResource(android.R.color.black);
        linearLayout.addView(mView, params);

        viewSeperator = new View(this);
        viewSeperator.setBackgroundResource(R.color.lightgrey);
        viewSeperator.setActivated(true);
        viewSeperator.setMinimumHeight((int) dp);
        linearLayout.addView(viewSeperator);

        textView = new TextView(this);
        textView.setText("Aggregation");
        textView.setActivated(true);
        textView.setGravity(Gravity.CENTER);
        linearLayout.addView(textView);

        viewSeperator1 = new View(this);
        viewSeperator1.setBackgroundResource(R.color.lightgrey);
        viewSeperator1.setActivated(true);
        viewSeperator1.setMinimumHeight((int) dp);
        linearLayout.addView(viewSeperator1);
    }

    private void updateTreeMap(TreeMap<Integer, Double> groupedTreeMap, String year, double meters) {
        int lastKey = groupedTreeMap.lastKey();
        groupedTreeMap.put(lastKey, meters);
    }
    public void renderData() {
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
        xAxis.setAxisMaximum(listOfRun.size());

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.setAxisMaximum(maxSpeed);
        leftAxis.setAxisMinimum(0);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        mChart.getAxisRight().setEnabled(false);
    }

    private void renderDataTimeSpeed() {
        LimitLine llXAxis = new LimitLine(10f, "Index 10");
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(10f);

        XAxis xAxis = mChartTimeSpeed.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setAxisMinimum(0f);
        xAxis.setDrawLimitLinesBehindData(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = mChartTimeSpeed.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.setAxisMaximum(maxSpeed);
        leftAxis.setAxisMinimum(0);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        mChartTimeSpeed.getAxisRight().setEnabled(false);
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
        //int i = 0;

        if(sizeOfList!=0) {
            for (Run run : listOfRun) {
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
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);
            mChart.setVisibleXRangeMaximum(50);
        }
    }

    private void setDataRenderDataTimeSpeed() {
        ArrayList<Entry> values = new ArrayList<>();
        int sizeOfList = listOfRun.size();
        int i = 0;

        if(sizeOfList!=0) {
            for (Run run : listOfRun) {
                //if(i%60==0) {
                    //float coveredMeter = (float) run.getMeters_covered();
                    long time = i;
                    float speed = run.getSpeed();
                    values.add(new Entry(time, speed));
                //}
                i++;
            }
        }

        LineDataSet set1;
        mChartTimeSpeed.getDescription().setEnabled(false);

        if (mChartTimeSpeed.getData() != null &&
                mChartTimeSpeed.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) mChartTimeSpeed.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mChartTimeSpeed.getData().notifyDataChanged();
            mChartTimeSpeed.notifyDataSetChanged();
        } else {
            set1 = new LineDataSet(values, getString(R.string.time_speed));
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
            mChartTimeSpeed.setData(data);
            mChartTimeSpeed.setVisibleXRangeMaximum(50);
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