package at.co.netconsulting.runningtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.preference.PreferenceFragmentCompat;
import at.co.netconsulting.runningtracker.general.BaseActivity;
public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }
    public void onclick_database(View view) {
        Intent intent = new Intent(this, DatabaseActivity.class);
        startActivity(intent);
    }
    public void onclick_general_settings(View view) {
        Intent intent = new Intent(this, GeneralSettings.class);
        startActivity(intent);
    }
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
}