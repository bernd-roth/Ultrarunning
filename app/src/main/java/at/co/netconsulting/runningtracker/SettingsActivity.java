package at.co.netconsulting.runningtracker;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.PreferenceFragmentCompat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.GregorianCalendar;
import java.util.List;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.BaseActivity;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;
import at.co.netconsulting.runningtracker.pojo.Run;
public class SettingsActivity extends BaseActivity {
    private SharedPreferences sharedpreferences;
    private EditText editTextNumberSignedMinimumDistanceMeter, editTextPerson,
            editTextNumberSignedMinimumTimeMs, editTextNumber;
    private Button buttonSave,
            buttonExport,
            buttonDelete,
            buttonImport;
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
    private int minDistanceMeter, numberInDays;
    private long minTimeMs;
    private DatabaseHandler db;
    private ProgressDialog progressDialog;
    private String person;
    private boolean isBatteryOptimization, isDayNightModus, isTrafficEnabled, isVoiceMessage;
    private Switch switchBatteryOptimization, switchDayNightModus, switchEnableTraffic, switchVoiceMessage;
    private PendingIntent pendingIntent;
    private TextView textViewExportDatabaseScheduled;
    private Long nextBackInMilliseconds;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_NEXT_BACKUP);
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
            case SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                numberInDays = sh.getInt(sharedPrefKey, 1);
                editTextNumber.setText("" + numberInDays);
                break;
            case SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isVoiceMessage = sh.getBoolean(sharedPrefKey, false);
                switchVoiceMessage.setChecked(isVoiceMessage);
                break;
            case SharedPref.STATIC_SHARED_PREF_NEXT_BACKUP:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                nextBackInMilliseconds = sh.getLong(sharedPrefKey, 0);
                textViewExportDatabaseScheduled.setText(getResources().getString(R.string.export_database_scheduled) + "\nNext backup at: " + calculateNextBackupDate(nextBackInMilliseconds) + " UTC");
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
        editTextNumber = findViewById(R.id.editTextNumber);

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

        buttonExport = findViewById(R.id.buttonExport);
        buttonExport.setTransformationMethod(null);

        buttonDelete = findViewById(R.id.buttonDelete);
        buttonDelete.setTransformationMethod(null);

        buttonImport = findViewById(R.id.buttonImport);
        buttonImport.setTransformationMethod(null);

        progressDialog = new ProgressDialog(SettingsActivity.this);
        progressDialog.setMessage("Exporting..."); // Setting Message
        progressDialog.setTitle("Exporting recorded runs"); // Setting Title
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
        progressDialog.setCancelable(false);

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

        textViewExportDatabaseScheduled = findViewById(R.id.textViewExportDatabaseScheduled);
        textViewExportDatabaseScheduled.setText(getResources().getString(R.string.export_database_scheduled) + "\nNext update ");

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
        }  else if(sharedPreference.equals("SCHEDULE_SAVE")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            int numberInDays = Integer.parseInt(editTextNumber.getText().toString());
            editor.putInt(SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE, numberInDays);
            editor.commit();
        }  else if(sharedPreference.equals("VOICE_MESSAGE")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isVoiceMessage = switchVoiceMessage.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_VOICE_MESSAGE, isVoiceMessage);
            editor.commit();
        }  else if(sharedPreference.equals("NEXT_BACKUP")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_NEXT_BACKUP, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putLong(SharedPref.STATIC_SHARED_PREF_NEXT_BACKUP, nextBackInMilliseconds);
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

    public void importGPX(View view) {
        Intent data = new Intent(Intent.ACTION_GET_CONTENT);
        data.setType("*/*");
        data = Intent.createChooser(data, "Choose a file");
        startActivityResultLauncher.launch(data);
    }
    ActivityResultLauncher<Intent> startActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    // Handle the returned Uri
                    Log.d("onActivityResult", "FileChooser works");

                    try {
                        InputStream is = getContentResolver().openInputStream(result.getData().getData());
                        Uri uri = Uri.fromFile(new File(getApplicationContext().getFilesDir().getParentFile().getPath() + "/databases/DATABASE_RUN"));
                        OutputStream out = getContentResolver().openOutputStream(uri);
                        copy(is, out);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
    private void copy(final InputStream in, final OutputStream out) throws IOException {
        final byte[] b = new byte[8192];
        for (int r; (r = in.read(b)) != -1;) {
            out.write(b, 0, r);
        }
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
        scheduleDatabaseBackup();
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_NEXT_BACKUP);
        Toast.makeText(getApplicationContext(), R.string.save_settings_map_type_rec_profil_runners_name, Toast.LENGTH_LONG).show();
    }
    private void scheduleDatabaseBackup() {
        int scheduledDays = Integer.parseInt(editTextNumber.getText().toString());

        if(scheduledDays > 0) {
            nextBackInMilliseconds = new GregorianCalendar().getTimeInMillis() + (scheduledDays * StaticFields.ONE_DAY_IN_MILLISECONDS);

            textViewExportDatabaseScheduled.setText(getResources().getString(R.string.export_database_scheduled) + "\nNext backup at: " + calculateNextBackupDate(nextBackInMilliseconds) + " UTC");

            Bundle bundle = new Bundle();
            bundle.putLong("scheduled_alarm", nextBackInMilliseconds);

            Intent intentAlarm = new Intent(this, AlarmReceiver.class);
            intentAlarm.putExtra("alarmmanager", bundle);

            pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextBackInMilliseconds, pendingIntent);
        } else {
            Intent intentAlarm = new Intent(this, AlarmReceiver.class);

            pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);

            textViewExportDatabaseScheduled.setText(getResources().getString(R.string.export_database_scheduled));
        }
    }

    private String calculateNextBackupDate(Long milliseconds) {
        DateTime dateTime = new DateTime( milliseconds, DateTimeZone.UTC );
        int day = dateTime.getDayOfMonth();
        int month = dateTime.getMonthOfYear();
        int year = dateTime.getYear();
        //time
        int hour = dateTime.getHourOfDay();
        int minute = dateTime.getMinuteOfHour();
        int seconds = dateTime.getSecondOfMinute();

        return String.format("%02d:%02d:%02d %02d-%02d-%d", hour, minute, seconds, day, month, year);
    }

    public void delete(View view) {
        Button buttonDelete = (Button)view;
        String buttonText = buttonDelete.getText().toString();
        if(buttonText.equals("Delete")) {
            createAlertDialog();
        }
    }
    private void createAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);

        builder.setMessage(getResources().getString(R.string.do_you_want_to_delete_all_entries))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.answer_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        db.delete();
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.all_entries_deleted), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.answer_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showAlertDialogForSelectingWhichEntryToDelete() {
        DatabaseHandler db = new DatabaseHandler(this);
        List<Run> allEntries = db.getAllEntriesGroupedByRun();

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(SettingsActivity.this);
        if(allEntries.size()<=0) {
            builderSingle.setTitle(getResources().getString(R.string.no_run_available));
        } else {
            builderSingle.setTitle(getResources().getString(R.string.select_one_run));
        }
        builderSingle.setIcon(R.drawable.icon_notification);

        // prevents closing alertdialog when clicking outside of it
        builderSingle.setCancelable(false);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(SettingsActivity.this, android.R.layout.select_dialog_singlechoice);
        for(int i = 0; i<allEntries.size(); i++) {
            arrayAdapter.add(allEntries.get(i).getNumber_of_run() + "-DateTime: " + allEntries.get(i).getDateTime());
        }

        builderSingle.setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String numberOfRun = arrayAdapter.getItem(which);
                String[] splittedString = numberOfRun.split("-");
                int intNumberOfRun = Integer.parseInt(splittedString[0]);

                db.deleteSingleEntry(intNumberOfRun);
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.single_entry_deleted), Toast.LENGTH_LONG).show();
            }
        });
        builderSingle.show();
    }

    public void export(View v) {
        progressDialog.show(); // Display Progress Dialog
        new Thread(new Runnable() {
            public void run() {
                try {
                    db.exportTableContent();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                progressDialog.dismiss();
            }
        }).start();
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