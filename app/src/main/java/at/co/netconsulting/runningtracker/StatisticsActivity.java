package at.co.netconsulting.runningtracker;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Year;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.BaseActivity;
import at.co.netconsulting.runningtracker.pojo.Run;
import at.co.netconsulting.runningtracker.util.StaticVariables;

public class StatisticsActivity extends BaseActivity {
    private LineChart mChart, mChartTimeSpeed, mAltitudeChart;
    private DatabaseHandler db;
    private List<Run> listOfRun;
    private TextView textViewMaxSpeed, textViewDistance, textViewAvgSpeed,
            textViewSlowestLap, textViewFastestLap, textViewStartingElevation,
            textViewEndingElevation, textViewHighestElevation, textViewTotalElevation,
            textViewMovementTime, textViewLowestElevation, textViewStartTime, textViewEndTime,
            textViewPace;
    private float maxSpeed, avgSpeed, totalDistance;
    private double totalElevation, highestElevation, lastElevationPoint,
            sumElevation,lowestElevation;
    private String totalMovementTime, startTime, endTime, sPace;
    private List<Float> listSpeed;
    private TableLayout tableSection, tableHeader, tableYearSection, tableHeaderYear;
    private TextView txtGeneric, txtGenericYear, textView;
    private TableRow tr, trYear;
    private long HH, MM, SS;
    private Spinner spinnerRunChoose;
    private LinearLayout linearLayout;
    private View mView, viewSeperator, viewSeperator1;
    private ArrayList<Integer> intNumberOfRun;
    private int positionInArray, positionOfTrack = -1;
    private Dialog dialog_data;
    private FloatingActionButton fabStatistics;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        initializeObjects();
        showAlertDialogWithTracks();
    }
    private void showAlertDialogWithTracks(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DatabaseHandler db = new DatabaseHandler(StatisticsActivity.this);
                List<Run> allEntries = db.getAllEntriesOrderedByRunNumber();
                TreeMap<Long, Run> orderedByDate = new TreeMap<>();
                long millis;
                Date date = null;
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

                dialog_data = new Dialog(StatisticsActivity.this);
                setDialogWithSpecificHeight(dialog_data);

                EditText filterText = (EditText) dialog_data.findViewById(R.id.alertdialog_edittext);
                ListView alertdialog_Listview = (ListView) dialog_data.findViewById(R.id.alertdialog_Listview);
                alertdialog_Listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_text_view);
                List<Integer> intArray = new ArrayList<>();

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
                        //int arrayPosition = intNumberOfRun.get(positionOfTrack);
                        positionInArray = positionOfTrack;
                        callDatabase();
                        calcAvgSpeedMaxSpeedTotalDistance();
                        calcElevation();
                        calcMovementTime();
                        calcStartTime();
                        calcEndTime();
                        calcPace();
                        renderData();
                        renderDataAltitude();
                        //renderDataTimeSpeed();
                        setData();
                        setDataAltitude();
                        //setDataRenderDataTimeSpeed();

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

                        mAltitudeChart.notifyDataSetChanged();
                        mAltitudeChart.invalidate();

                        dialog_data.dismiss();
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

                        for (int i = 0; i < allEntries.size(); i++) {
                            String search = s.toString().toLowerCase(Locale.ENGLISH);
                            String searchableString = allEntries.get(i).getDateTime() + "\n"
                                    + String.format("%.03f", allEntries.get(i).getMeters_covered() / 1000) + " Km\n"
                                    + allEntries.get(i).getComment();
                            if (searchableString.toLowerCase(Locale.ENGLISH).contains(search)) {
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
        }, 500);
    }
    private void setDialogWithSpecificHeight(Dialog dialog) {
        //set height to 50%
        int height = 2;

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.setContentView(R.layout.custom_list_statistics);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = this.getWindow().getDecorView().getWidth();
        lp.height = this.getWindow().getDecorView().getHeight()/height;
        lp.gravity = Gravity.CENTER;

        dialog.getWindow().setAttributes(lp);
    }

    private void calcPace() {
        if(listOfRun.size()>0) {
            long firstElement = listOfRun.get(0).getDateTimeInMs();
            long lastElement = listOfRun.get(listOfRun.size()-1).getDateTimeInMs();

            long calcResult = lastElement-firstElement;

            Duration duration = Duration.ofMillis(calcResult);
            double tPace = duration.toMinutes() / totalDistance;
            long iPart = (long) tPace; //iPart stands for integer part
            double fPart = tPace - iPart; //fPart stands for fractional part
            fPart *= 60;

            sPace = String.format("%2d:%02d min/km", iPart, (int) fPart);
        }
    }

    private void calcEndTime() {
        if(listOfRun.size()>0) {
            long lastElement = listOfRun.get(listOfRun.size()-1).getDateTimeInMs();

            Date date = new Date(lastElement);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int seconds = calendar.get(Calendar.SECOND);

            endTime = String.format("%02d-%02d-%02d %02d:%02d:%02d", day, month, year, hour, minute, seconds);
        }
    }

    private void calcStartTime() {
        if(listOfRun.size()>0) {
            long firstElement = listOfRun.get(0).getDateTimeInMs();

            Date date = new Date(firstElement);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int seconds = calendar.get(Calendar.SECOND);

            startTime = String.format("%02d-%02d-%02d %02d:%02d:%02d", day, month, year, hour, minute, seconds);
        }
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
        TreeMap<Long, Integer> fastestSlowestLap = new TreeMap<>();
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
            fastestSlowestLap.put(groupedSectionList.get(i), counter);
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

    private int chooseSlowestLap(TreeMap<Long, Integer> fastestSlowestLap) {
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

    private int chooseFastestLap(TreeMap<Long, Integer> fastestSlowestLap) {
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
        //StartTime
        textViewStartTime.setText(String.format("Start time: %s", startTime));
        //EndTime
        textViewEndTime.setText(String.format("End time: %s", endTime));
        //Pace
        textViewPace.setText(String.format("Total pace: %s", sPace));
    }

    private void initializeObjects() {
        mChart = findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(false);
        mChart.setScaleEnabled(false);
        mChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDetailedGraph(view);
            }
        });

        mAltitudeChart = findViewById(R.id.altitudeChart);
        mAltitudeChart.setTouchEnabled(true);
        mAltitudeChart.setPinchZoom(false);
        mAltitudeChart.setScaleEnabled(false);
        mAltitudeChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDetailedGraph(view);
            }
        });

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
        textViewStartTime = findViewById(R.id.textViewStartTime);
        textViewEndTime = findViewById(R.id.textViewEndTime);
        textViewPace = findViewById(R.id.textViewPace);

        tableHeader = (TableLayout)findViewById(R.id.tableHeader);
        tableSection = (TableLayout)findViewById(R.id.tableSection);

        tableHeaderYear = (TableLayout)findViewById(R.id.tableHeaderYearSection);
        tableYearSection = (TableLayout)findViewById(R.id.tableYearSection);

        listOfRun = new ArrayList<>();
        listSpeed = new ArrayList<>();

        fabStatistics = new FloatingActionButton(this);

        linearLayout = findViewById(R.id.ll);
        db = new DatabaseHandler(this);
    }

    private void showTableLayoutYear(TreeMap<Integer, Double> year) {
        int colums = 2;
        int rows = year.size();
        float totalKm = 0;

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

                    txtGenericYear.setText("\t\t\t\t" + "Year" + "\t\t\t\t\t\t\t\t\t" + "Total Km" + "\t\t\t\t\t\t\t\t" + "Ø / day");
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

        TextView textViewOverview = null;
        int numberOfDayOfCurrentYear = getNumberOfDayOfCurrentYear();

        //Content of rows
        for (int i = 0; i < rows; i++) {
            trYear = new TableRow(this);
            textViewOverview = new TextView(this);
            for (int j = 0; j < 1; j++) {
                txtGenericYear = new TextView(this);
                txtGenericYear.setTextSize(18);

                if(Year.now().getValue()!=keyList.get(i)) {
                    String convertedKm = String.format("%.2f", valueList.get(i) / 1000);
                    String convertedAvg = String.format("%.2f", (valueList.get(i) / 1000)/ StaticVariables.DAYS_PER_YEAR);

                    //TODO how to show comma for each year in one vertical line
                    if(convertedKm.length()<6) {
                        txtGenericYear.setText("\t\t\t\t" + keyList.get(i) + "\t\t\t\t\t\t\t\t\t\t\t" + convertedKm + "\t\t\t\t\t\t\t\t\t" + convertedAvg);
                    } else {
                        txtGenericYear.setText("\t\t\t\t" + keyList.get(i) + "\t\t\t\t\t\t\t\t\t\t" + convertedKm + "\t\t\t\t\t\t\t\t" + convertedAvg);
                    }
                } else {
                    String convertedKm = String.format("%.2f", valueList.get(i) / 1000);
                    String convertedAvg = String.format("%.2f", (valueList.get(i) / 1000)/numberOfDayOfCurrentYear);
                    txtGenericYear.setText("\t\t\t\t" + keyList.get(i) + "\t\t\t\t\t\t\t\t\t\t" + convertedKm + "\t\t\t\t\t\t\t\t" + convertedAvg);
                }

                trYear.addView(txtGenericYear);

                totalKm += valueList.get(i)/1000;
            }
            tableYearSection.addView(trYear);
        }

        int pixels = calculateViewHeight();

        View view = new View(this);
        view.setMinimumHeight(pixels);

        int parseColor = Color.parseColor("#c0c0c0");
        view.setBackgroundColor(parseColor);

        textViewOverview.setTextSize(18);
        String sTotalKm = String.format("%.2f", totalKm);
        textViewOverview.setText("\t\t\t\tTotal:" + "\t\t\t\t\t\t\t\t\t" + sTotalKm);

        tableYearSection.addView(view);
        tableYearSection.addView(textViewOverview);
    }

    private int getNumberOfDayOfCurrentYear() {
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        return dayOfYear;
    }

    private int calculateViewHeight() {
        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        int dp = 2;
        int pixels = (int) (dp * scale + 0.5f);
        return pixels;
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

            if(groupedTreeMap.containsKey(Integer.valueOf(year))) {
                meters = groupedTreeMap.get(Integer.valueOf(year));
                meters += listOfRun.get(i).getMeters_covered();
                updateTreeMap(groupedTreeMap, year, meters);
            } else {
                meters = listOfRun.get(i).getMeters_covered();
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
        groupedTreeMap.put(Integer.valueOf(year), meters);
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
        //xAxis.setAxisMaximum(listOfRun.size());

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        //leftAxis.setAxisMaximum(maxSpeed);
        leftAxis.setAxisMinimum(0);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        //show labels underneath chart
        mChart.getXAxis().setDrawLabels(true);
        mChart.getAxisRight().setEnabled(false);
    }

    public void renderDataAltitude() {
        LimitLine llXAxis = new LimitLine(10f, "Index 10");
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(10f);

        XAxis xAxis = mAltitudeChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setAxisMinimum(0f);
        xAxis.setDrawLimitLinesBehindData(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setAxisMaximum(listOfRun.size());

        YAxis leftAxis = mAltitudeChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        //leftAxis.setAxisMaximum(maxSpeed);
        leftAxis.setAxisMinimum(0);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        //show labels underneath chart
        mAltitudeChart.getXAxis().setDrawLabels(true);
        mAltitudeChart.getAxisRight().setEnabled(false);
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
        //leftAxis.setAxisMaximum(maxSpeed);
        leftAxis.setAxisMinimum(0);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        //show labels underneath chart
        mChartTimeSpeed.getXAxis().setDrawLabels(true);
        mChartTimeSpeed.getAxisRight().setEnabled(false);
    }

    private void callDatabase() {
        listOfRun = db.getSingleEntryForStatistics(positionOfTrack);
    }

    private void setData() {
        List<Entry> values = new ArrayList<>();
        int sizeOfList = listOfRun.size();
        int counter = 0;

        if(sizeOfList!=0) {
            if(sizeOfList>5000) {
                for (Run run : listOfRun) {
                    if(counter%100==0) {
                        float coveredMeter = (float) run.getMeters_covered();
                        float speed = run.getSpeed();
                        values.add(new Entry(coveredMeter, speed));
                    }
                }
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
            //mChart.setVisibleXRangeMaximum(50);
            mChart.setData(data);
            mChart.notifyDataSetChanged();
            mChart.invalidate();
        }
    }

    private void setDataAltitude() {
        List<Entry> values = new ArrayList<>();
        int sizeOfList = listOfRun.size();
        int counter = 0;

        if(sizeOfList!=0) {
            if(sizeOfList>5000) {
                for (Run run : listOfRun) {
                    if (counter % 100 == 0) {
                        float coveredMeter = (float) run.getMeters_covered();
                        double altitude = run.getAltitude();
                        values.add(new Entry(coveredMeter, (float) altitude));
                    }
                }
            }
        }

        LineDataSet set1;
        mAltitudeChart.getDescription().setEnabled(false);

        if (mAltitudeChart.getData() != null &&
                mAltitudeChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) mAltitudeChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mAltitudeChart.getData().notifyDataChanged();
            mAltitudeChart.notifyDataSetChanged();
        } else {
            set1 = new LineDataSet(values, getString(R.string.distance_altitude));
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
            mAltitudeChart.setData(data);
            mAltitudeChart.notifyDataSetChanged();
            mAltitudeChart.invalidate();
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
    public void showDetailedGraph(View view) {
        View viewSpeed = view.findViewById(R.id.chart);
        View viewAltitude = view.findViewById(R.id.altitudeChart);

        if(viewSpeed == view) {
            Intent detailedDistanceSpeedChart = new Intent(StatisticsActivity.this, DetailedGraphActivity.class);
            detailedDistanceSpeedChart.putExtra("numberOfRun", positionInArray);
            StatisticsActivity.this.startActivity(detailedDistanceSpeedChart);
        } else if(viewAltitude == view) {
            Intent detailedAltitudeChart = new Intent(StatisticsActivity.this, DetailedAltitudeGraphActivity.class);
            detailedAltitudeChart.putExtra("numberOfRun", positionInArray);
            StatisticsActivity.this.startActivity(detailedAltitudeChart);
        }
    }
    public void showTrack(View view) {
        showAlertDialogWithTracks();
    }
}