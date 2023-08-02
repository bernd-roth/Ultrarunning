package at.co.netconsulting.runningtracker;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;

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

    private boolean isSwitchBatteryOptimization;
    private SharedPreferences sharedpreferences;
    private EditText editTextNumberSignedMinimumTimeMs;
    private EditText editTextNumberSignedMinimumDistanceMeter;
    private Button buttonSave, buttonNormalizeWithKalmanFilter, buttonExport, buttonDelete;
    private String mapType;
    private RadioButton radioButtonNormal,
            radioButtonHybrid,
            radioButtonNone,
            radioButtonTerrain,
            radioButtonSatellite;
    private int minDistanceMeter;
    private long minTimeMs;
    private DatabaseHandler db;
    private static final int IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1002;
    private ProgressDialog dialog;
    private Switch switchCommentPause;
    private boolean isCommentOnPause, isCommentedOnPause;
    private boolean editTextDistance, editTextTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initObjects();
        loadSharedPreferences(SharedPref.STATIC_STRING_MINIMUM_SPEED_LIMIT);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE);
    }

    private void loadSharedPreferences(String sharedPrefKey) {
        SharedPreferences sh;

        switch(sharedPrefKey) {
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
            case SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isCommentedOnPause = sh.getBoolean(sharedPrefKey, Boolean.parseBoolean(StaticFields.STATIC_SAVE_ON_COMMENT_PAUSE));
                switchCommentPause.setChecked(isCommentedOnPause);
                break;
        }
    }

    private void initObjects() {
        editTextNumberSignedMinimumTimeMs = findViewById(R.id.editTextNumberSignedMinimumTimeMs);
        editTextNumberSignedMinimumTimeMs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();

                if(s.length()==0) {
                    buttonSave.setEnabled(false);
                } else {
                    int number = Integer.parseInt(s.toString());
                    if (length > 0 && length < 3) {
                        Timber.d("Length: %s", length);
                        if (number > 0 && number < 11) {
                            Timber.d("Number: %s", number);
                            buttonSave.setEnabled(true);
                            editTextTime=true;
                        } else {
                            buttonSave.setEnabled(false);
                            editTextTime=false;
                        }
                    } else {
                        buttonSave.setEnabled(false);
                        editTextTime=false;
                    }
                }
                if(editTextDistance&&editTextTime) {
                    buttonSave.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        editTextNumberSignedMinimumDistanceMeter = findViewById(R.id.editTextNumberSignedMinimumDistanceMeter);
        editTextNumberSignedMinimumDistanceMeter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();

                if(s.length()==0) {
                    buttonSave.setEnabled(false);
                    editTextDistance=false;
                } else {
                    int number = Integer.parseInt(s.toString());
                    if (length > 0 && length < 3) {
                        Timber.d("Length: %s", length);
                        if (number > 0 && number < 11) {
                            Timber.d("Number: %s", number);
                            editTextDistance=true;
                        } else {
                            buttonSave.setEnabled(false);
                            editTextDistance=false;
                        }
                    } else {
                        buttonSave.setEnabled(false);
                        editTextDistance=false;
                    }
                }
                if(editTextDistance&&editTextTime) {
                    buttonSave.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        buttonSave = findViewById(R.id.buttonSave);
        buttonSave.setTransformationMethod(null);
        buttonSave.setEnabled(false);

        radioButtonNormal = findViewById(R.id.radioButton_map_type_normal);
        radioButtonHybrid = findViewById(R.id.radioButton_map_type_hybrid);
        radioButtonNone = findViewById(R.id.radioButton_map_none);
        radioButtonTerrain = findViewById(R.id.radioButton_map_type_terrain);
        radioButtonSatellite = findViewById(R.id.radioButton_map_type_satellite);

        buttonNormalizeWithKalmanFilter = findViewById(R.id.buttonNormalizeWithKalmanFilter);
        buttonNormalizeWithKalmanFilter.setTransformationMethod(null);

        buttonExport = findViewById(R.id.buttonExport);
        buttonExport.setTransformationMethod(null);

        buttonDelete = findViewById(R.id.buttonDelete);
        buttonDelete.setTransformationMethod(null);

        switchCommentPause = findViewById(R.id.switchCommentPause);

        dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setTitle("Loading");
            dialog.setMessage("Loading. Please wait...");
            dialog.setIndeterminate(true);
            dialog.setCanceledOnTouchOutside(false);

        db = new DatabaseHandler(this);
    }

    private void saveSharedPreferences(String sharedPreference) {
        if(sharedPreference.equals("MAP_TYPE_NORMAL")) {
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
        } else if(sharedPreference.equals("COMMENT_AND_PAUSE")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isCommentPauseChecked = switchCommentPause.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE, isCommentPauseChecked);
            editor.commit();
        }
    }

    public void saveAndCommentPause(View view) {
        if(switchCommentPause.isChecked()) {
            isCommentOnPause=true;
            saveSharedPreferences("COMMENT_AND_PAUSE");
        } else {
            isCommentOnPause=false;
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
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
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle(getResources().getString(R.string.working_with_kalman_filter));
        dialog.setMessage(getResources().getString(R.string.work_in_progress));
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

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
        dialog.cancel();
    }
}