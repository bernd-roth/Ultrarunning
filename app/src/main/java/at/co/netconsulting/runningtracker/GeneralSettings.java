package at.co.netconsulting.runningtracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.Toast;

import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.BaseActivity;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;

public class GeneralSettings extends BaseActivity {
    private SharedPreferences sharedpreferences;
    private EditText editTextNumberSignedMinimumDistanceMeter, editTextPerson,
            editTextNumberSignedMinimumTimeMs, editTextNumberSignedThresholdSpeed;
    private Button buttonSave;
    private String mapType, recordingProfil;
    private RadioButton radioButtonNormal,
            radioButtonHybrid,
            radioButtonNone,
            radioButtonTerrain,
            radioButtonSatellite,
            radioButtonExact,
            radioButtonNormalBattery,
            radioButtonSavingBattery,
            radioButtonMaximumSavingBattery,
            radioButtonIndividual;
    private int minDistanceMeter;
    private long minTimeMs;
    private DatabaseHandler db;
    private ProgressDialog progressDialog;
    private String person;
    private boolean isBatteryOptimization, isDayNightModus, isTrafficEnabled, isVoiceMessage,
                    isAutomatedRecording, isTransmitDataToWebsocket;
    private Switch switchBatteryOptimization, switchDayNightModus, switchEnableTraffic, switchVoiceMessage,
                    switchAutomatedRecording, switchTransmitDataToWebsocket;
    private PendingIntent pendingIntent;
    private Long nextBackInMilliseconds;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);

        initObjects();
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_PERSON);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_SHOW_DISTANCE_COVERED);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_BATTERY_OPTIMIZATION);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_DAY_NIGHT_MODUS);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_ENABLE_TRAFFIC);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_AUTOMATED_RECORDING);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_THRESHOLD_SPEED);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_TRANSMIT_DATA_TO_WEBSOCKET);
    }

    private void loadSharedPreferences(String sharedPrefKey) {
        SharedPreferences sh;

        switch(sharedPrefKey) {
            case SharedPref.STATIC_SHARED_PREF_STRING_MAPTYPE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                mapType = sh.getString(sharedPrefKey, "MAP_TYPE_NORMAL");
                switch (mapType) {
                    case "MAP_TYPE_HYBRID":
                        radioButtonHybrid.setChecked(true);
                        break;
                    case "MAP_TYPE_NONE":
                        radioButtonNone.setChecked(true);
                        break;
                    case "MAP_TYPE_TERRAIN":
                        radioButtonTerrain.setChecked(true);
                        break;
                    case "MAP_TYPE_SATELLITE":
                        radioButtonSatellite.setChecked(true);
                        break;
                    default: //defaults to MAP_TYPE_NORMAL
                        radioButtonNormal.setChecked(true);
                        break;
                }
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
            case SharedPref.STATIC_SHARED_PREF_FLOAT_THRESHOLD_SPEED:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                float thresholdSpeed = sh.getFloat(sharedPrefKey, StaticFields.STATIC_FLOAT_THRESHOLD_SPEED);
                editTextNumberSignedThresholdSpeed.setText(String.valueOf(thresholdSpeed));
                break;
            case SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                recordingProfil = sh.getString(sharedPrefKey, "Individual");
                if(recordingProfil.equals("Exact")) {
                    radioButtonExact.setChecked(true);
                    editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                    editTextNumberSignedMinimumTimeMs.setEnabled(false);
                    break;
                } else if(recordingProfil.equals("Normal")) {
                    radioButtonNormalBattery.setChecked(true);
                    editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                    editTextNumberSignedMinimumTimeMs.setEnabled(false);
                    break;
                } else if(recordingProfil.equals("Saving_Battery")) {
                    radioButtonSavingBattery.setChecked(true);
                    editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                    editTextNumberSignedMinimumTimeMs.setEnabled(false);
                    break;
                } else if(recordingProfil.equals("Maximum_Saving_Battery")) {
                    radioButtonMaximumSavingBattery.setChecked(true);
                    editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                    editTextNumberSignedMinimumTimeMs.setEnabled(false);
                    break;
                } else if(recordingProfil.equals("Individual")) {
                    radioButtonIndividual.setChecked(true);
                    editTextNumberSignedMinimumDistanceMeter.setEnabled(true);
                    editTextNumberSignedMinimumTimeMs.setEnabled(true);
                    break;
                }
            case SharedPref.STATIC_SHARED_PREF_PERSON:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                person = sh.getString(sharedPrefKey, "Anonym");
                editTextPerson.setText(person);
                break;
            case SharedPref.STATIC_SHARED_PREF_BATTERY_OPTIMIZATION:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isBatteryOptimization = sh.getBoolean(sharedPrefKey, false);
                switchBatteryOptimization.setChecked(isBatteryOptimization);
                break;
            case SharedPref.STATIC_SHARED_PREF_DAY_NIGHT_MODUS:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isDayNightModus = sh.getBoolean(sharedPrefKey, false);
                switchDayNightModus.setChecked(isDayNightModus);
                break;
            case SharedPref.STATIC_SHARED_PREF_ENABLE_TRAFFIC:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isTrafficEnabled = sh.getBoolean(sharedPrefKey, false);
                switchEnableTraffic.setChecked(isTrafficEnabled);
                break;
            case SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isVoiceMessage = sh.getBoolean(sharedPrefKey, false);
                switchVoiceMessage.setChecked(isVoiceMessage);
                break;
            case SharedPref.STATIC_SHARED_PREF_AUTOMATED_RECORDING:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isAutomatedRecording = sh.getBoolean(sharedPrefKey, false);
                switchAutomatedRecording.setChecked(isAutomatedRecording);
                break;
            case SharedPref.STATIC_SHARED_PREF_TRANSMIT_DATA_TO_WEBSOCKET:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isTransmitDataToWebsocket = sh.getBoolean(sharedPrefKey, false);
                switchTransmitDataToWebsocket.setChecked(isTransmitDataToWebsocket);
                break;
        }
    }

    private void openBatterySettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        this.startActivity(intent);
    }

    private void initObjects() {
        editTextNumberSignedMinimumTimeMs = findViewById(R.id.editTextNumberSignedMinimumTimeMs);
        editTextNumberSignedMinimumDistanceMeter = findViewById(R.id.editTextNumberSignedMinimumDistanceMeter);
        editTextPerson = findViewById(R.id.editTextPerson);
        editTextNumberSignedThresholdSpeed = findViewById(R.id.editTextNumberSignedThresholdSpeed);

        buttonSave = findViewById(R.id.buttonSave);
        buttonSave.setTransformationMethod(null);
        buttonSave.setEnabled(true);

        radioButtonNormal = findViewById(R.id.radioButton_map_type_normal);
        radioButtonHybrid = findViewById(R.id.radioButton_map_type_hybrid);
        radioButtonNone = findViewById(R.id.radioButton_map_none);
        radioButtonTerrain = findViewById(R.id.radioButton_map_type_terrain);
        radioButtonSatellite = findViewById(R.id.radioButton_map_type_satellite);

        radioButtonExact = findViewById(R.id.radioButtonExact);
        radioButtonNormalBattery = findViewById(R.id.radioButtonNormalBattery);
        radioButtonSavingBattery = findViewById(R.id.radioButtonSavingBattery);
        radioButtonMaximumSavingBattery = findViewById(R.id.radioButtonMaximumSavingBattery);
        //radioButtonFast = findViewById(R.id.radioButtonFast);
        radioButtonIndividual = findViewById(R.id.radioButtonIndividual);

        switchBatteryOptimization = findViewById(R.id.switchBatteryOptimization);

        switchDayNightModus = findViewById(R.id.switchDayNightModus);
        switchEnableTraffic = findViewById(R.id.switchShowTraffic);
        switchVoiceMessage = findViewById(R.id.switchVoiceMessage);
        switchVoiceMessage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE);
            }
        });

        switchAutomatedRecording = findViewById(R.id.switchAutomatedRecording);
        switchAutomatedRecording.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_AUTOMATED_RECORDING);
            }
        });

        switchTransmitDataToWebsocket = findViewById(R.id.switchTransmitDataWebsocket);
        switchTransmitDataToWebsocket.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_TRANSMIT_DATA_TO_WEBSOCKET);
            }
        });

        db = new DatabaseHandler(this);
        //set screen orientation to portrait automatically
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
        } else if(sharedPreference.equals("Exact")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, "Exact");
            editor.commit();
        } else if(sharedPreference.equals("Normal")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, "Normal");
            editor.commit();
        } else if(sharedPreference.equals("Saving_Battery")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, "Saving_Battery");
            editor.commit();
        } else if(sharedPreference.equals("Maximum_Saving_Battery")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, "Maximum_Saving_Battery");
            editor.commit();
        } else if(sharedPreference.equals("Fast")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, "Fast");
            editor.commit();
        } else if(sharedPreference.equals("Individual")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL, "Individual");
            editor.commit();
        }  else if(sharedPreference.equals("PERSON")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_PERSON, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            String person = editTextPerson.getText().toString();
            editor.putString(SharedPref.STATIC_SHARED_PREF_PERSON, person);
            editor.commit();
        }  else if(sharedPreference.equals("BATTERY_OPTIMIZATION")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_BATTERY_OPTIMIZATION, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isBatteryOptimization = switchBatteryOptimization.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_BATTERY_OPTIMIZATION, isBatteryOptimization);
            editor.commit();
        }  else if(sharedPreference.equals("DAY_NIGHT_MODUS")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_DAY_NIGHT_MODUS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isDayNightModus = switchDayNightModus.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_DAY_NIGHT_MODUS, isDayNightModus);
            editor.commit();
        }  else if(sharedPreference.equals("ENABLE_TRAFFIC")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_ENABLE_TRAFFIC, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isTrafficEnabled = switchEnableTraffic.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_ENABLE_TRAFFIC, isTrafficEnabled);
            editor.commit();
        }  else if(sharedPreference.equals("VOICE_MESSAGE")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isVoiceMessage = switchVoiceMessage.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE, isVoiceMessage);
            editor.commit();
        } else if(sharedPreference.equals("AUTOMATED_RECORDING")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_AUTOMATED_RECORDING, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isAutomatedRecording = switchAutomatedRecording.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_AUTOMATED_RECORDING, isAutomatedRecording);
            editor.commit();
        } else if(sharedPreference.equals("THRESHOLD_SPEED")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_THRESHOLD_SPEED, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            float speed = Float.parseFloat(editTextNumberSignedThresholdSpeed.getText().toString());
            editor.putFloat(SharedPref.STATIC_SHARED_PREF_FLOAT_THRESHOLD_SPEED, speed);
            editor.commit();
        } else if(sharedPreference.equals("TRANSMIT_DATA_TO_WEBSOCKET")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_TRANSMIT_DATA_TO_WEBSOCKET, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isTransmitDataToWebsocket = switchTransmitDataToWebsocket.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_TRANSMIT_DATA_TO_WEBSOCKET, isTransmitDataToWebsocket);
            editor.commit();
        }
    }
    public void onClickRadioButtonBatteryGroup(View view) {
        switch (view.getResources().getResourceEntryName(view.getId())) {
            case "radioButtonExact":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(1));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(1));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Exact");
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonNormalBattery":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(10));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(1));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Normal");
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonSavingBattery":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(20));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(30));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Saving_Battery");
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonMaximumSavingBattery":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(100));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(1800));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Maximum_Saving_Battery");
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonFast":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(10));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(1));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Fast");
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonIndividual":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(1));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(1));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Individual");
                editTextNumberSignedMinimumTimeMs.setEnabled(true);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(true);
                break;
        }
    }

    public void switchEvent(View view) {
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_BATTERY_OPTIMIZATION);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            openBatterySettings();
        }
    }

    public void onClickDayNightModus(View view) {
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_DAY_NIGHT_MODUS);
    }

    public void onClickEnableTraffic(View view) {
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_ENABLE_TRAFFIC);
    }
    public void onclick_database(View view) {
        Intent intent = new Intent(this, DatabaseActivity.class);
        startActivity(intent);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
    public void save(View v) {
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_INTEGER_MIN_DISTANCE_METER);
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_MIN_TIME_MS);
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_PERSON);
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE);
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE);
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_FLOAT_THRESHOLD_SPEED);
        Toast.makeText(getApplicationContext(), R.string.save_settings_map_type_rec_profil_runners_name, Toast.LENGTH_LONG).show();
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
}