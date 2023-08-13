package at.co.netconsulting.runningtracker;

import static at.co.netconsulting.runningtracker.general.StaticFields.df;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
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
    private TextView textViewMaxSpeed, textViewDistance, textViewAvgSpeed;
    private float mSpeed, avgSpeed;
    private List<Float> maxSpeed;
    private float meters, speed;
    private ArrayList<Entry> values;

    private void initChart() {
        mCurrentSeries = new XYSeries("Distance / Speed Graph");
        mDataset.addSeries(mCurrentSeries);
        mCurrentRenderer = new XYSeriesRenderer();
        mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
        mRenderer.addSeriesRenderer(mCurrentRenderer);
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

    private void callDatabase() {
//        for(int i = 1; i<50000; i++)
//            db.addSampleRun(i);
        int intLastEntry = db.getLastEntry();
        listOfRun = db.getSingleEntryOrderedByDateTime(intLastEntry);
        for(Run run : listOfRun) {
            mCurrentSeries.add(run.getMeters_covered(), run.getSpeed());
        }
        findHighestLowestValuesSpeed();
        textViewMaxSpeed.setText("Max. speed: " +  df.format(mSpeed) + " km/h");
        textViewDistance.setText("Distance: " + df.format(meters) + " meter");
        textViewAvgSpeed.setText("Avg: speed: " + df.format(avgSpeed) + " km/h");
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        initializeObjects();
    }

    private void initializeObjects() {
        db = new DatabaseHandler(this);
        textViewMaxSpeed = findViewById(R.id.textViewMaxSpeed);
        textViewDistance = findViewById(R.id.textViewDistance);
        textViewAvgSpeed = findViewById(R.id.textViewAvgSpeed);
        maxSpeed = new LinkedList<Float>();
        values = new ArrayList<>();
    }

    protected void onResume() {
        super.onResume();
        LinearLayout layout = (LinearLayout) findViewById(R.id.linechart);
        if (mChart == null) {
            initChart();
            callDatabase();
//            mRenderer.setChartTitle("Distance/Speed graph");
            mRenderer.setXTitle("Distance");
//            mRenderer.setLegendTextSize(15f);
            mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
            mRenderer.setYTitle("Speed");
            mRenderer.setShowGrid(true);
            mRenderer.setGridColor(Color.GRAY);
            mChart = ChartFactory.getCubeLineChartView(this, mDataset, mRenderer, 0.3f);
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }
    }
}
