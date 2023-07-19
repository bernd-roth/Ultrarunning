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
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceFragmentCompat;

import at.co.netconsulting.runningtracker.general.BaseActivity;

public class SettingsActivity extends BaseActivity {

    private Switch switchBatteryOptimization;
    private boolean isSwitchBatteryOptimization;
    private SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        initObjects();
        loadSharedPreferences(StaticFields.STATIC_BATTERY_OPTIMIZATION);
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
    }

    private void saveSharedPreferences(String sharedPreference) {
        sharedpreferences = getSharedPreferences(StaticFields.STATIC_BATTERY_OPTIMIZATION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();

        if(switchBatteryOptimization.isChecked()) {
            editor.putBoolean(sharedPreference, true);
            editor.commit();
        } else {
            editor.putBoolean(sharedPreference, false);
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
}