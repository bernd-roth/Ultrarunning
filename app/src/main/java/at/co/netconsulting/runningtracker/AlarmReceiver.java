package at.co.netconsulting.runningtracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import at.co.netconsulting.runningtracker.db.DatabaseHandler;

public class AlarmReceiver extends BroadcastReceiver {
    private DatabaseHandler db;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() != null) {
            ArrayList scheduledAlarms = intent.getExtras().getSerializable("scheduled_alarm", ArrayList.class);
            scheduledAlarms.remove(0);
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

            Long scheduledAlarm = (Long) scheduledAlarms.get(0);

            Intent intentAlarm = new Intent(context, AlarmReceiver.class);
            intentAlarm.putExtra("scheduled_alarm", scheduledAlarms);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Long time = scheduledAlarm;
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }
}
