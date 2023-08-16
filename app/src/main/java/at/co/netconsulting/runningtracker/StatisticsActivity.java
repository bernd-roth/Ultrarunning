package at.co.netconsulting.runningtracker;

import static at.co.netconsulting.runningtracker.general.StaticFields.df;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.Run;

public class StatisticsActivity extends Activity {
    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeries mCurrentSeries;
    private XYSeriesRenderer mCurrentRenderer;
    private DatabaseHandler db;
    private List<Run> listOfRun;
    private TextView textViewMaxSpeed, textViewDistance, textViewAvgSpeed, textViewProgressbar;
    private float mSpeed, avgSpeed;
    private List<Float> maxSpeed;
    private float meters, speed;
    private ArrayList<Entry> values;
    private LinearLayout layout;
    private ProgressBar progressBar;

    private void initChart() {
        mCurrentSeries = new XYSeries("Distance / Speed Graph");
        mCurrentRenderer = new XYSeriesRenderer();
        mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
// here set the label and its size for all points in graph
//        mCurrentRenderer.setDisplayChartValues(true);
//        mCurrentRenderer.setChartValuesTextSize(45f);
        mCurrentRenderer.setFillPoints(true);
        mRenderer.addSeriesRenderer(mCurrentRenderer);
    }

    private void findHighestLowestValuesSpeed() {
        if(listOfRun.size()>0) {
            for (int i = 0; i < listOfRun.size(); i++) {
                meters = (float) listOfRun.get(i).getMeters_covered();
                meters /= 1000;
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

    private void callDatabase() {
//        for(int i = 1; i<50000; i++)
//            db.addSampleRun(i);

        int intLastEntry = db.getLastEntry();
        listOfRun = db.getSingleEntryOrderedByDateTime(intLastEntry);
        int sizeOfList = listOfRun.size();
        int counter = 0, result = 0, i = 0;

        for(Run run : listOfRun) {
            mCurrentSeries.add(run.getMeters_covered(), run.getSpeed());
            counter++;
            result = (counter*100)/sizeOfList;

            //findLowestHighestValues
            meters = (float) listOfRun.get(i).getMeters_covered();
            meters /= 1000;
            speed = (float) listOfRun.get(i).getSpeed();
            values.add(new Entry(meters, speed));
            float mSpeed = listOfRun.get(i).getSpeed();
            maxSpeed.add(mSpeed);
            avgSpeed += speed;

            i++;
            //set progress
            progressBar.setProgress(result);
        }

        mSpeed = Collections.max(maxSpeed);
        avgSpeed /= sizeOfList;

        textViewMaxSpeed.setText("Max. speed: " +  df.format(mSpeed) + " km/h");
        textViewDistance.setText("Distance: " + df.format(meters) + " Kilometer");
        textViewAvgSpeed.setText("Avg: speed: " + df.format(avgSpeed) + " km/h");
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        initializeObjects();
    }

    private void initializeObjects() {
        db = new DatabaseHandler(this);
        layout = (LinearLayout) findViewById(R.id.linechart);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        textViewMaxSpeed = findViewById(R.id.textViewMaxSpeed);
        textViewDistance = findViewById(R.id.textViewDistance);
        textViewAvgSpeed = findViewById(R.id.textViewAvgSpeed);
        textViewProgressbar = findViewById(R.id.textViewProgressbar);
        textViewProgressbar.setVisibility(View.VISIBLE);
        maxSpeed = new LinkedList<Float>();
        values = new ArrayList<>();
    }

    protected void onResume() {
        super.onResume();

        if (mChart == null) {
            initChart();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    //Background work here
                    callDatabase();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //UI Thread work here
                            mChart = ChartFactory.getCubeLineChartView(getApplicationContext(), mDataset, mRenderer, 0.3f);
                            layout.addView(mChart);
                            progressBar.setVisibility(View.INVISIBLE);
                            textViewProgressbar.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            });

            //callDatabase();
            mRenderer.setXTitle("Distance");
            mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
            mRenderer.setYTitle("Speed");
            mRenderer.setShowGrid(true);
            mRenderer.setGridColor(Color.GRAY);
            mRenderer.setLabelsTextSize(40f);
            mRenderer.setZoomButtonsVisible(true);
            mDataset.addSeries(mCurrentSeries);
        } else {
            mChart.repaint();
        }
    }
}