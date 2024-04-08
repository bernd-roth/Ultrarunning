package at.co.netconsulting.runningtracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.GregorianCalendar;

import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;

public class AlarmReceiver extends BroadcastReceiver {
    private DatabaseHandler db;
    private SharedPreferences sh;
    private int scheduledDays;
    @Override
    public void onReceive(Context context, Intent intent) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    db = new DatabaseHandler(context);
                    db.exportTableContent();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        loadSharedPreferences(context, SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE);
        Bundle bundle = new Bundle();

        Long time = new GregorianCalendar().getTimeInMillis() + (scheduledDays * StaticFields.ONE_DAY_IN_MILLISECONDS);
        bundle.putLong("scheduled_alarm", time);

        Intent intentAlarm = new Intent(context, AlarmReceiver.class);
        intentAlarm.putExtra("alarmmanager", bundle);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT |
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }
    private void loadSharedPreferences(Context context, String sharedPrefKey) {
        sh = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        scheduledDays = sh.getInt(sharedPrefKey, 1);
    }
}
