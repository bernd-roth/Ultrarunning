package at.co.netconsulting.runningtracker;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import at.co.netconsulting.runningtracker.general.BaseActivity;

public class StatisticsActivity extends BaseActivity {

    private TextView textViewDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        initObjects();
        readLastEntryFromDatabase();
    }

    private void readLastEntryFromDatabase() {
    }

    private void initObjects() {
        textViewDate = (findViewById(R.id.textViewDate));

    }
}