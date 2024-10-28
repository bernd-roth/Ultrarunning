package at.co.netconsulting.runningtracker.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import org.greenrobot.eventbus.EventBus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import at.co.netconsulting.runningtracker.MapsActivity;
import at.co.netconsulting.runningtracker.R;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.ProgressDialogEventBus;
import at.co.netconsulting.runningtracker.pojo.Run;

public class GpxForegroundService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private String NOTIFICATION_CHANNEL_ID = "co.at.netconsulting.runningtracker";
    private Notification notification;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager manager;
    private NotificationChannel serviceChannel;
    private DatabaseHandler db;
    private Thread mWatchdogThread = null;
    private WatchDogRunner mWatchdogRunner;
    private String notificationService;
    private NotificationManager nMgr;
    private SimpleDateFormat sdf;
    private List<Run> selectedRun;
    private File download_folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    //own implementations
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Foreground Service Channel GPX",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    //overrides
    @Override
    public void onCreate() {
        super.onCreate();
        initializeObject();
        createNotificationChannel();
    }
    private void initializeObject() {
        this.sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        this.db = new DatabaseHandler(GpxForegroundService.this);
    }
    private void initializeThread() {
        List<Run> selRun = selectedRun;
        if (mWatchdogThread == null || !mWatchdogThread.isAlive()) {
            mWatchdogRunner = new WatchDogRunner(getApplicationContext(), this.db, this.sdf);
            mWatchdogThread = new Thread(mWatchdogRunner, "WorkoutWatchdog");
        }
        if (!mWatchdogThread.isAlive()) {
            mWatchdogThread.start();
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        selectedRun = (List<Run>) intent.getSerializableExtra("SELECTED_RUN");
        initializeThread();
        createPendingIntent();

        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.gather_information))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.icon_notification))
                //.setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.icon_notification)
                //show notification on home screen to everyone
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //without FOREGROUND_SERVICE_IMMEDIATE, notification can take up to 10 secs to be shown
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        notification = notificationBuilder.build();

        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }
    private PendingIntent createPendingIntent() {
        Intent stopIntent = new Intent(this, MapsActivity.class);
        stopIntent.setAction("ACTION.STOPFOREGROUND_ACTION");
        stopIntent.putExtra("EXTRA_NOTIFICATION_ID", 0);
        PendingIntent stopPendingIntent = PendingIntent.getActivity(this,
                0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        return stopPendingIntent;
    }
    @Override
    public void onDestroy() {
        cancelNotification();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        mWatchdogRunner.stop();
        EventBus.getDefault().post(new ProgressDialogEventBus(false));
        super.onDestroy();
    }
    private void cancelNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        notificationService = Context.NOTIFICATION_SERVICE;
        nMgr = (NotificationManager) getApplicationContext().getSystemService(notificationService);
        nMgr.cancel(NOTIFICATION_ID);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class WatchDogRunner implements Runnable {
        private static final int NOTIFICATION_ID = 1;
        private final SimpleDateFormat sdf;
        private DatabaseHandler db;
        private Context context;
        volatile boolean exporting = true;

        public WatchDogRunner(Context context, DatabaseHandler db, SimpleDateFormat sdf) {
            this.context = context;
            this.db = db;
            this.sdf = sdf;
        }

        @Override
        public void run() {
            try {
                List<Run> runEntries = new ArrayList<>();
                int numberOfRun = -1;

                if(selectedRun == null) {
                    runEntries = db.getAllEntries();
                } else if(selectedRun.size()>0) {
                    for(int i = 0; i<selectedRun.size(); i++) {
                        numberOfRun = selectedRun.get(i).getNumber_of_run();

                        List<Run> tmpRun = db.getSingleEntryForStatistics(numberOfRun);
                        for(int j = 0; j<tmpRun.size(); j++) {
                            Run run = tmpRun.get(j);
                            runEntries.add(run);
                        }
                    }
                }
                createNotification(context);
                generateGfx(runEntries);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void stop() {
            exporting = false;
        }
        private void cancelNotification(Context context) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            String notificationService = Context.NOTIFICATION_SERVICE;
            NotificationManager nMgr = (NotificationManager) context.getApplicationContext().getSystemService(notificationService);
            nMgr.cancel(NOTIFICATION_ID);
            EventBus.getDefault().post(new ProgressDialogEventBus(false));
        }

        private void zipAllGpxFiles() {
            File zipFile = new File(download_folder, "all_runs.zip");
            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // Filter for .gpx files in the directory
                File[] gpxFiles = download_folder.listFiles((dir, name) -> name.endsWith(".gpx"));

                if (gpxFiles != null) {
                    for (File gpxFile : gpxFiles) {
                        try (FileInputStream fis = new FileInputStream(gpxFile)) {
                            ZipEntry zipEntry = new ZipEntry(gpxFile.getName());
                            zos.putNextEntry(zipEntry);

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, length);
                            }
                            zos.closeEntry();
                        }
                        gpxFile.delete();
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void generateGfx(List<Run> run) throws IOException, ParseException {
            FileWriter writer = null;
            int lastRun = 0;
            int currentRun;
            boolean start = true;

            if (!run.isEmpty()) {
                ListIterator<Run> iterator = run.listIterator();

                while (iterator.hasNext()) {
                    if (exporting != true) {
                        String footer = "\t\t</trkseg>\n\t</trk>\n</gpx>";
                        writer.append(footer);
                        writer.flush();
                        writer.close();
                        cancelNotification(getApplicationContext());
                        return;
                    }
                    Run curInt = iterator.next();
                    currentRun = curInt.getNumber_of_run();

                    if (currentRun != lastRun) {
                        if (start) {
                            if (lastRun != 0) {
                                writeFooter(writer);
                            }
                            writer = createFileName(download_folder, curInt);
                            createHeader(writer, curInt.getDateTime());
                            lastRun = curInt.getNumber_of_run();
                            start = false;
                        } else {
                            writeFooter(writer);
                            writer = createFileName(download_folder, curInt);
                            createHeader(writer, curInt.getDateTime());
                            lastRun = curInt.getNumber_of_run();
                            start = true;
                        }
                    } else {
                        createBody(writer, curInt);
                    }
                }
                writeFooter(writer);
                zipAllGpxFiles();
                cancelNotification(context);
            } else {
                zipAllGpxFiles();
                cancelNotification(context);
            }
        }
        private void writeFooter(FileWriter writer) throws IOException {
            String footer = "\t\t</trkseg>\n\t</trk>\n</gpx>";
            writer.append(footer);
            writer.flush();
            writer.close();
        }
        private FileWriter createFileName(File download_folder, Run curInt) throws IOException {
            FileWriter writer;
            String[] sDateTime = curInt.getDateTime().split(" ");
            String[] sTime = sDateTime[1].split(":");
            String fileName = sDateTime[0] + "_" + sTime[0] + "_" + sTime[1] + "_" + sTime[2];
            writer = new FileWriter(new File(download_folder, "" + fileName + ".gpx"), false);
            return writer;
        }
        private void createBody(FileWriter writer, Run curInt) throws IOException, ParseException {
            String segments = "";

            Date date = sdf.parse(curInt.getDateTime());
            long dateTimeToMilliseconds = date.getTime();

            String mobileDateTime = getFormatTimeWithTZ(new Date(dateTimeToMilliseconds));

            segments += "\t\t\t<trkpt lat=\"" + curInt.getLat()
                    + "\" lon=\""
                    + curInt.getLng()
                    + "\">\n\t\t\t\t<time>"
                    + mobileDateTime
                    + "</time>\n"
                    + "\t\t\t\t<ele>"
                    + curInt.getAltitude()
                    + "</ele>\n"
                    + "\t\t\t\t<run>"
                    + curInt.getNumber_of_run()
                    + "</run>\n"
                    + "\t\t\t</trkpt>\n";
            writer.append(segments);

            try {
                writer.flush();
            } catch (IOException e) {
                Log.e("generateGfx", "Error Writting Path", e);
            }
        }
        private void createHeader(FileWriter writer, String name) throws IOException {
            String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n\t<trk>\n";
            writer.append(header);
            name = "\t\t<name>" + name + "</name>\n\t\t<trkseg>\n";
            writer.append(name);
        }
        public String getFormatTimeWithTZ(Date currentTime) {
            SimpleDateFormat timeZoneDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            return timeZoneDate.format(currentTime);
        }
        private void createNotification(Context context) {
            String NOTIFICATION_CHANNEL_ID = "co.at.netconsulting.runningtracker";
            NotificationManager manager = null;
            NotificationChannel serviceChannel;

            serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            manager = this.context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context.getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Data exporting")
                    .setContentText("Exporting data to GPX files")
                    .setOnlyAlertOnce(true)
                    .setSmallIcon(R.drawable.icon_notification)
                    //notification cannot be dismissed by user
                    .setOngoing(true)
                    //show notification on home screen to everyone
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    //without FOREGROUND_SERVICE_IMMEDIATE, notification can take up to 10 secs to be shown
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

            notificationBuilder.build();

            manager.notify(NOTIFICATION_ID, notificationBuilder
                    .setContentTitle("Data exporting")
                    .setContentText("Exporting data to GPX files")
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_notification))
                    .build());
        }
    }
}