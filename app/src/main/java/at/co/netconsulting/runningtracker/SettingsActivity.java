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
import android.widget.Switch;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        initObjects();
        loadSharedPreferences(StaticFields.STATIC_BATTERY_OPTIMIZATION);
        loadSharedPreferences(StaticFields.STATIC_STRING_MINIMUM_SPEED_LIMIT);
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
}