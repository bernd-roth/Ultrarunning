package at.co.netconsulting.runningtracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceFragmentCompat;

import at.co.netconsulting.runningtracker.general.BaseActivity;

public class SettingsActivity extends BaseActivity {

    private Switch switchBatteryOptimization;
    private boolean isSwitchBatteryOptimization;
    private float minimumSpeedLimit;
    private SharedPreferences sharedpreferences;
    private EditText editTextNumberDecimalMinimumSpeedLimit;
    private Button buttonSave;
    private String mapType;
    private RadioButton radioButtonNormal,
            radioButtonHybrid,
            radioButtonNone,
            radioButtonTerrain,
            radioButtonSatellite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        initObjects();
        loadSharedPreferences(StaticFields.STATIC_BATTERY_OPTIMIZATION);
        loadSharedPreferences(StaticFields.STATIC_STRING_MINIMUM_SPEED_LIMIT);
        loadSharedPreferences(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE);
        ignoreBatteryOptimization();
    }

    private void loadSharedPreferences(String sharedPrefKey) {
        SharedPreferences sh;

        switch(sharedPrefKey) {
            case StaticFields.STATIC_BATTERY_OPTIMIZATION:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                isSwitchBatteryOptimization = sh.getBoolean(sharedPrefKey, false);
                switchBatteryOptimization.setChecked(isSwitchBatteryOptimization);
                break;
            case StaticFields.STATIC_STRING_MINIMUM_SPEED_LIMIT:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                minimumSpeedLimit = sh.getFloat(sharedPrefKey, Float.valueOf(String.valueOf(StaticFields.STATIC_DOUBLE_MINIMUM_SPEED_LIMIT)));
                editTextNumberDecimalMinimumSpeedLimit.setText(String.valueOf(minimumSpeedLimit));
                break;
            case StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE:
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
        }
    }

    private void initObjects() {
        switchBatteryOptimization = findViewById(R.id.switchBatteryOptimization);
        switchBatteryOptimization.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v("Switch State=", ""+isChecked);
                if(switchBatteryOptimization.isChecked()) {
                    Intent intent = new Intent();
                    String packageName = getPackageName();
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        startActivity(intent);
                        saveSharedPreferences(StaticFields.STATIC_BATTERY_OPTIMIZATION);
                    }
                } else {
                    saveSharedPreferences(StaticFields.STATIC_BATTERY_OPTIMIZATION);
                }
            }
        });

        editTextNumberDecimalMinimumSpeedLimit = findViewById(R.id.editTextNumberSignedMinimumSpeedLimit);
        buttonSave = findViewById(R.id.buttonSave);

        radioButtonNormal = findViewById(R.id.radioButton_map_type_normal);
        radioButtonHybrid = findViewById(R.id.radioButton_map_type_hybrid);
        radioButtonNone = findViewById(R.id.radioButton_map_none);
        radioButtonTerrain = findViewById(R.id.radioButton_map_type_terrain);
        radioButtonSatellite = findViewById(R.id.radioButton_map_type_satellite);
    }

    private void saveSharedPreferences(String sharedPreference) {
        if(sharedPreference.equals(StaticFields.STATIC_BATTERY_OPTIMIZATION)) {
            sharedpreferences = getSharedPreferences(StaticFields.STATIC_BATTERY_OPTIMIZATION, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            if(switchBatteryOptimization.isChecked()) {
                editor.putBoolean(sharedPreference, true);
                editor.commit();
            }  else {
                editor.putBoolean(sharedPreference, false);
                editor.commit();
            }
        } else if(sharedPreference.equals(StaticFields.STATIC_STRING_MINIMUM_SPEED_LIMIT)) {
            sharedpreferences = getSharedPreferences(StaticFields.STATIC_STRING_MINIMUM_SPEED_LIMIT, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            minimumSpeedLimit = Float.parseFloat(editTextNumberDecimalMinimumSpeedLimit.getText().toString());
            editor.putFloat(sharedPreference, minimumSpeedLimit);
            editor.commit();
        } else if(sharedPreference.equals("MAP_TYPE_NORMAL")) {
            sharedpreferences = getSharedPreferences(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_NORMAL");
            editor.commit();
        } else if(sharedPreference.equals("MAP_TYPE_NONE")) {
            sharedpreferences = getSharedPreferences(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_NONE");
            editor.commit();
        } else if(sharedPreference.equals("MAP_TYPE_HYBRID")) {
            sharedpreferences = getSharedPreferences(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_HYBRID");
            editor.commit();
        } else if(sharedPreference.equals("MAP_TYPE_TERRAIN")) {
            sharedpreferences = getSharedPreferences(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_TERRAIN");
            editor.commit();
        } else if(sharedPreference.equals("MAP_TYPE_SATELLITE")) {
            sharedpreferences = getSharedPreferences(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(StaticFields.STATIC_SHARED_PREF_STRING_MAPTYPE, "MAP_TYPE_SATELLITE");
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

    public void save(View v)
    {
        saveSharedPreferences(StaticFields.STATIC_STRING_MINIMUM_SPEED_LIMIT);
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