package at.co.netconsulting.runningtracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import at.co.netconsulting.runningtracker.db.DatabaseHandler;

public class AlarmReceiver extends BroadcastReceiver {
    private DatabaseHandler db;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() != null) {
            Bundle extras = intent.getExtras();
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

            Long scheduledAlarm = extras.getLong("scheduled_alarm");
            Integer scheduledDays = extras.getInt("scheduled_days");

            Bundle bundle = new Bundle();
            bundle.putInt("scheduled_days", scheduledDays);

            Long time = new GregorianCalendar().getTimeInMillis() + (scheduledDays * 60 * 60 * 1000);
            bundle.putLong("scheduled_alarm", time);

            Intent intentAlarm = new Intent(context, AlarmReceiver.class);
            intentAlarm.putExtra("alarmmanager", bundle);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }
}
