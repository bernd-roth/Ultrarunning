package at.co.netconsulting.runningtracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceFragmentCompat;
import java.io.File;
import at.co.netconsulting.runningtracker.calculation.GPSDataFactory;
import at.co.netconsulting.runningtracker.calculation.KalmanFilter;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.BaseActivity;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;
import timber.log.Timber;

public class SettingsActivity extends BaseActivity {

    private Switch switchBatteryOptimization;
    private boolean isSwitchBatteryOptimization;
    private SharedPreferences sharedpreferences;
    private EditText editTextNumberSignedMinimumTimeMs;
    private EditText editTextNumberSignedMinimumDistanceMeter;
    private Button buttonSave, buttonNormalizeWithKalmanFilter;
    private String mapType;
    private RadioButton radioButtonNormal,
            radioButtonHybrid,
            radioButtonNone,
            radioButtonTerrain,
            radioButtonSatellite;
    private int minDistanceMeter;
    private long minTimeMs;
    private DatabaseHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        initObjects();
        loadSharedPreferences(SharedPref.STATIC_BATTERY_OPTIMIZATION);
        loadSharedPreferences(SharedPref.STATIC_STRING_MINIMUM_SPEED_LIMIT);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
        ignoreBatteryOptimization();
    }

    private void loadSharedPreferences(String sharedPrefKey) {
        SharedPreferences sh;

        switch(sharedPrefKey) {
            case SharedPref.STATIC_BATTERY_OPTIMIZATION:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isSwitchBatteryOptimization = sh.getBoolean(sharedPrefKey, false);
                switchBatteryOptimization.setChecked(isSwitchBatteryOptimization);
                break;
            case SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                mapType = sh.getString(sharedPrefKey, "MAP_TYPE_NORMAL");
                if(mapType.equals("MAP_TYPE_NORMAL"))
                    radioButtonNormal.setChecked(true);
                else if(mapType.equals("MAP_TYPE_HYBRID"))
                    radioButtonHybrid.setChecked(true);
                else if(mapType.equals("MAP_TYPE_NONE"))
                    radioButtonNone.setChecked(true);
                else if(mapType.equals("MAP_TYPE_TERRAIN"))
                    radioButtonTerrain.setChecked(true);
                else if(mapType.equals("MAP_TYPE_SATELLITE"))
                    radioButtonSatellite.setChecked(true);
                else
                    radioButtonNormal.setChecked(true);
                break;
            case SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                minDistanceMeter = sh.getInt(sharedPrefKey, StaticFields.STATIC_INTEGER_MIN_DISTANCE_METER);
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(minDistanceMeter));
                break;
            case SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                minTimeMs = sh.getLong(sharedPrefKey, StaticFields.STATIC_LONG_MIN_TIME_MS);
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(minTimeMs));
                break;
        }
    }

    private void initObjects() {
        switchBatteryOptimization = findViewById(R.id.switchBatteryOptimization);
        switchBatteryOptimization.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Timber.d("SettingsActivity: switchBatteryOptimization checked=%s", isChecked);
                if(switchBatteryOptimization.isChecked()) {
                    Intent intent = new Intent();
                    String packageName = getPackageName();
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        startActivity(intent);
                        saveSharedPreferences(SharedPref.STATIC_BATTERY_OPTIMIZATION);
                    }
                } else {
                    saveSharedPreferences(SharedPref.STATIC_BATTERY_OPTIMIZATION);
                }
            }
        });

        editTextNumberSignedMinimumTimeMs = findViewById(R.id.editTextNumberSignedMinimumTimeMs);
        editTextNumberSignedMinimumDistanceMeter = findViewById(R.id.editTextNumberSignedMinimumDistanceMeter);
        buttonSave = findViewById(R.id.buttonSave);

        radioButtonNormal = findViewById(R.id.radioButton_map_type_normal);
        radioButtonHybrid = findViewById(R.id.radioButton_map_type_hybrid);
        radioButtonNone = findViewById(R.id.radioButton_map_none);
        radioButtonTerrain = findViewById(R.id.radioButton_map_type_terrain);
        radioButtonSatellite = findViewById(R.id.radioButton_map_type_satellite);
        buttonNormalizeWithKalmanFilter = findViewById(R.id.buttonNormalizeWithKalmanFilter);

        db = new DatabaseHandler(this);
    }

    private void saveSharedPreferences(String sharedPreference) {
        if(sharedPreference.equals(SharedPref.STATIC_BATTERY_OPTIMIZATION)) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_BATTERY_OPTIMIZATION, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            if(switchBatteryOptimization.isChecked()) {
                editor.putBoolean(sharedPreference, true);
                editor.commit();
            }  else {
                editor.putBoolean(sharedPreference, false);
                editor.commit();
            }
        } else if(sharedPreference.equals("MAP_TYPE_NORMAL")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_NORMAL");
            editor.commit();
        } else if(sharedPreference.equals("MAP_TYPE_NONE")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_NONE");
            editor.commit();
        } else if(sharedPreference.equals("MAP_TYPE_HYBRID")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_HYBRID");
            editor.commit();
        } else if(sharedPreference.equals("MAP_TYPE_TERRAIN")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_TERRAIN");
            editor.commit();
        } else if(sharedPreference.equals("MAP_TYPE_SATELLITE")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_SATELLITE");
            editor.commit();
        } else if(sharedPreference.equals("MIN_DISTANCE_METER")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            minDistanceMeter = Integer.parseInt(editTextNumberSignedMinimumDistanceMeter.getText().toString());
            editor.putInt(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER, minDistanceMeter);
            editor.commit();
        } else if(sharedPreference.equals("MIN_TIME_MS")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            minTimeMs = Long.parseLong(editTextNumberSignedMinimumTimeMs.getText().toString());
            editor.putLong(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS, minTimeMs);
            editor.commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ignoreBatteryOptimization() {
        if(isSwitchBatteryOptimization) {
            switchBatteryOptimization.setChecked(isSwitchBatteryOptimization);
        }
    }

    public void save(View v) {
        saveSharedPreferences(SharedPref.STATIC_STRING_MINIMUM_SPEED_LIMIT);
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER);
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
    }

    public void delete(View v)
    {
        db.delete();
    }

    public void export(View v)
    {
        db.exportTableContent();
    }

    public void onClickRadioButtonNormal(View view) {
        saveSharedPreferences("MAP_TYPE_NORMAL");
    }

    public void onClickRadioButtonHybrid(View view) {
        saveSharedPreferences("MAP_TYPE_HYBRID");
    }

    public void onClickRadioButtonNone(View view) {
        saveSharedPreferences("MAP_TYPE_NONE");
    }

    public void onClickRadioButtonTerrain(View view) {
        saveSharedPreferences("MAP_TYPE_TERRAIN");
    }

    public void onClickRadioButtonSatellite(View view) {
        saveSharedPreferences("MAP_TYPE_SATELLITE");
    }

    public void startKalmanFilter(View view) {
        buttonNormalizeWithKalmanFilter.setEnabled(false);

        File file = new File(getApplicationContext().getExternalFilesDir(null), "Kalman_filtered.csv");
        try {
            boolean fileExists = file.createNewFile();
            if (fileExists) {
                file.delete();
            }
        } catch(Exception e) {}

        new KalmanFilter(getApplicationContext());
        new GPSDataFactory(db);
        buttonNormalizeWithKalmanFilter.setEnabled(true);
    }
}