package at.co.netconsulting.runningtracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.preference.PreferenceFragmentCompat;
import java.io.File;
import java.util.List;
import at.co.netconsulting.runningtracker.calculation.GPSDataFactory;
import at.co.netconsulting.runningtracker.calculation.KalmanFilter;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.BaseActivity;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;
import at.co.netconsulting.runningtracker.pojo.Run;

public class SettingsActivity extends BaseActivity {
    private SharedPreferences sharedpreferences;
    private EditText editTextNumberSignedMinimumTimeMs;
    private EditText editTextNumberSignedMinimumDistanceMeter, editTextPerson;
    private Button buttonSave,
//            buttonNormalizeWithKalmanFilter,
            buttonExport,
            buttonDelete,
            buttonDeleteSingleEntry;
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
//            radioButtonFast,
            radioButtonIndividual;
    private int minDistanceMeter;
    private long minTimeMs;
    private DatabaseHandler db;
    private Switch switchCommentPause, switchGoToLastLocation;
    private boolean isCommentOnPause, isCommentedOnPause, isGoToLastLocation;
    private ProgressDialog progressDialog;
    private String person;

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
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_RECORDING_PROFIL);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_GO_TO_LAST_LOCATION);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_PERSON);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_REDUCE_LAT_LANG);
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
//                }else if(recordingProfil.equals("Fast")) {
//                    radioButtonFast.setChecked(true);
//                    break;
                } else if(recordingProfil.equals("Individual")) {
                    radioButtonIndividual.setChecked(true);
//                    buttonSave.setEnabled(true);
                    editTextNumberSignedMinimumDistanceMeter.setEnabled(true);
                    editTextNumberSignedMinimumTimeMs.setEnabled(true);
                    break;
                }
            case SharedPref.STATIC_SHARED_PREF_GO_TO_LAST_LOCATION:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isGoToLastLocation = sh.getBoolean(sharedPrefKey, false);
                switchGoToLastLocation.setChecked(isGoToLastLocation);
                break;
            case SharedPref.STATIC_SHARED_PREF_PERSON:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                person = sh.getString(sharedPrefKey, "Anonym");
                editTextPerson.setText(person);
//                buttonSave.setEnabled(true);
                break;
        }
    }

    private void initObjects() {
        editTextNumberSignedMinimumTimeMs = findViewById(R.id.editTextNumberSignedMinimumTimeMs);
        editTextNumberSignedMinimumDistanceMeter = findViewById(R.id.editTextNumberSignedMinimumDistanceMeter);
        editTextPerson = findViewById(R.id.editTextPerson);

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

//        buttonNormalizeWithKalmanFilter = findViewById(R.id.buttonNormalizeWithKalmanFilter);
//        buttonNormalizeWithKalmanFilter.setTransformationMethod(null);

        buttonExport = findViewById(R.id.buttonExport);
        buttonExport.setTransformationMethod(null);

        buttonDelete = findViewById(R.id.buttonDelete);
        buttonDelete.setTransformationMethod(null);

        buttonDeleteSingleEntry = findViewById(R.id.buttonDeleteSingleEntry);
        buttonDeleteSingleEntry.setTransformationMethod(null);

        switchCommentPause = findViewById(R.id.switchCommentPause);
        switchGoToLastLocation = findViewById(R.id.switchGoToLastLocation);

        progressDialog = new ProgressDialog(SettingsActivity.this);
        progressDialog.setMessage("Exporting..."); // Setting Message
        progressDialog.setTitle("Exporting recorded runs"); // Setting Title
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
        progressDialog.setCancelable(false);

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
        } else if(sharedPreference.equals("SAVE_ON_COMMENT_PAUSE")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isCommentPauseChecked = switchCommentPause.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_SAVE_ON_COMMENT_PAUSE, isCommentPauseChecked);
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
        }  else if(sharedPreference.equals("GO_TO_LAST_LOCATION")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_GO_TO_LAST_LOCATION, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            boolean isGoToLastLocation = switchGoToLastLocation.isChecked();
            editor.putBoolean(SharedPref.STATIC_SHARED_PREF_GO_TO_LAST_LOCATION, isGoToLastLocation);
            editor.commit();
        }  else if(sharedPreference.equals("PERSON")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_PERSON, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            String person = editTextPerson.getText().toString();
            editor.putString(SharedPref.STATIC_SHARED_PREF_PERSON, person);
            editor.commit();
        }
    }

    public void saveAndCommentPause(View view) {
        if(switchCommentPause.isChecked()) {
            isCommentOnPause=true;
            saveSharedPreferences("SAVE_ON_COMMENT_PAUSE");
        } else {
            isCommentOnPause=false;
            saveSharedPreferences("SAVE_ON_COMMENT_PAUSE");
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
//                buttonSave.setEnabled(false);
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonNormalBattery":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(10));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(1));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Normal");
//                buttonSave.setEnabled(false);
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonSavingBattery":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(20));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(30));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Saving_Battery");
//                buttonSave.setEnabled(false);
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonMaximumSavingBattery":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(100));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(1800));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Maximum_Saving_Battery");
//                buttonSave.setEnabled(false);
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonFast":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(10));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(1));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Fast");
//                buttonSave.setEnabled(false);
                editTextNumberSignedMinimumTimeMs.setEnabled(false);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(false);
                break;
            case "radioButtonIndividual":
                editTextNumberSignedMinimumDistanceMeter.setText(String.valueOf(1));
                editTextNumberSignedMinimumTimeMs.setText(String.valueOf(1));
                saveSharedPreferences("MIN_DISTANCE_METER");
                saveSharedPreferences("MIN_TIME_MS");
                saveSharedPreferences("Individual");
//                buttonSave.setEnabled(true);
                editTextNumberSignedMinimumTimeMs.setEnabled(true);
                editTextNumberSignedMinimumDistanceMeter.setEnabled(true);
                break;
        }
    }

    public void goToLastLocation(View view) {
        if(switchGoToLastLocation.isChecked()) {
            isGoToLastLocation=true;
            saveSharedPreferences("GO_TO_LAST_LOCATION");
        } else {
            isGoToLastLocation=false;
            saveSharedPreferences("GO_TO_LAST_LOCATION");
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
        saveSharedPreferences(SharedPref.STATIC_SHARED_PREF_PERSON);
        Toast.makeText(getApplicationContext(), R.string.save_settings_map_type_rec_profil_runners_name, Toast.LENGTH_LONG).show();
    }

    public void delete(View view) {
        Button buttonDelete = (Button)view;
        String buttonText = buttonDelete.getText().toString();
        if(buttonText.equals("Delete")) {
            createAlertDialog();
        } else {
            showAlertDialogForSelectingWhichEntryToDelete();  
        }
    }

    private void createAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_message) .setTitle(R.string.dialog_title);

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

        builderSingle.setNegativeButton(getResources().getString(R.string.buttonCancel), new DialogInterface.OnClickListener() {
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

//    public void startKalmanFilter(View view) {
//        buttonNormalizeWithKalmanFilter.setEnabled(false);
//
//        File file = new File(getApplicationContext().getExternalFilesDir(null), "Kalman_filtered.csv");
//        try {
//            boolean fileExists = file.createNewFile();
//            if (fileExists) {
//                file.delete();
//            }
//        } catch(Exception e) {}
//
//        new KalmanFilter(getApplicationContext());
//        new GPSDataFactory(db);
//        buttonNormalizeWithKalmanFilter.setEnabled(true);
//    }
}