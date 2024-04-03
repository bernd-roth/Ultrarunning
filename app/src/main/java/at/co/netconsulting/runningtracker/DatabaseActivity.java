package at.co.netconsulting.runningtracker;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;
import at.co.netconsulting.runningtracker.pojo.Run;
import at.co.netconsulting.runningtracker.view.RestAPI;
public class DatabaseActivity extends AppCompatActivity {
    private int numberInDays;
    private SharedPreferences sharedpreferences;
    private PendingIntent pendingIntent;
    private TextView textViewExportDatabaseScheduled, textViewPercentage;
    private Long nextBackInMilliseconds;
    private ProgressDialog progressDialog;
    private DatabaseHandler db;
    private EditText editTextNumber, editTextURL;
    private String httpUrl;
    private TextView textViewExportToServer, textViewExportDatabase;
    private LinearLayout linearlayout;
    private AlertDialog.Builder alert;
    private AlertDialog dialog;
    private final int NOTIFICATION_ID = 1;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE);
        initObjects();
    }

    private void initObjects() {
        this.getSupportActionBar().hide();

        textViewExportDatabaseScheduled = findViewById(R.id.textViewExportDatabaseScheduled);
        textViewExportDatabaseScheduled.setText(getResources().getString(R.string.export_database_scheduled));
        textViewExportDatabaseScheduled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createAlertDialogDatabaseBackup();
            }
        });

        textViewExportDatabase = findViewById(R.id.textViewExportDatabase);

        progressDialog = new ProgressDialog(DatabaseActivity.this);
        progressDialog.setMessage("Exporting..."); // Setting Message
        progressDialog.setTitle("Exporting recorded runs"); // Setting Title
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
        progressDialog.setCancelable(false);

        editTextNumber = new EditText(DatabaseActivity.this);
        editTextNumber.setSingleLine(true);
        editTextNumber.setText(String.valueOf(numberInDays));

        editTextURL = new EditText(DatabaseActivity.this);

        linearlayout = findViewById(R.id.ll);

        progressBar = (ProgressBar) findViewById(R.id.determinateBar);

        textViewPercentage = findViewById(R.id.textViewPercentage);
        textViewPercentage.setVisibility(View.INVISIBLE);

        db = new DatabaseHandler(this);
    }
    private void loadSharedPreferences(String sharedPrefKey) {
        SharedPreferences sh;
        switch(sharedPrefKey) {
            case SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                numberInDays = sh.getInt(sharedPrefKey, 1);
                break;
            case SharedPref.STATIC_SHARED_PREF_URL_SAVE:
                sh = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                httpUrl = sh.getString(sharedPrefKey, null);
                break;
        }
    }
    private void saveSharedPreferences(String sharedPreference) {
        if(sharedPreference.equals("SCHEDULE_SAVE")) {
            sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            int numberInDays = Integer.parseInt(editTextNumber.getText().toString());
            editor.putInt(SharedPref.STATIC_SHARED_PREF_SCHEDULE_SAVE, numberInDays);
            editor.commit();
        }
    }
    private void createAlertDialogDatabaseBackup() {
        alert = new AlertDialog.Builder(this);
        alert.setMessage("0: no backup at all");
        alert.setTitle("Backup interval in days");
        alert.setView(editTextNumber);
        alert.setCancelable(false);
        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                int scheduledDays = Integer.parseInt(editTextNumber.getText().toString());

                if(scheduledDays > 0) {
                    nextBackInMilliseconds = new GregorianCalendar().getTimeInMillis() + (scheduledDays * StaticFields.ONE_DAY_IN_MILLISECONDS);

                    Bundle bundle = new Bundle();
                    bundle.putLong("scheduled_alarm", nextBackInMilliseconds);

                    Intent intentAlarm = new Intent(getApplicationContext(), AlarmReceiver.class);
                    intentAlarm.putExtra("alarmmanager", bundle);

                    pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

                    AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextBackInMilliseconds, pendingIntent);

                    saveSharedPreferences("SCHEDULE_SAVE");
                } else {
                    Intent intentAlarm = new Intent(getApplicationContext(), AlarmReceiver.class);

                    pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

                    AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    alarmManager.cancel(pendingIntent);

                    textViewExportDatabaseScheduled.setText(getResources().getString(R.string.export_database_scheduled));
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        if(dialog==null) {
            dialog = alert.create();
            dialog.show();
        } else {
            dialog.show();
        }
    }
    private String calculateNextBackupDate(Long milliseconds) {
        Date date = new Date(milliseconds);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        return String.format("%02d:%02d:%02d %02d-%02d-%d", hour, minute, seconds, day, month, year);
    }
    public void exportDatabase(View v) {
        progressDialog.show();
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
    public void exportGPXFile(View view) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        textViewPercentage.setVisibility(View.VISIBLE);
        textViewPercentage.setText("0%");
        new Thread(new Runnable() {
            public void run() {
                try {
                    List<Run> run = db.getAllEntries();
                    int countOfRun = db.getCountOfRuns();
                    createNotification();
                    generateGfx(run, countOfRun, progressBar);
                    cancelNotification();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                progressDialog.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        textViewPercentage.setVisibility(View.GONE);
                    }
                });
            }
            private void createNotification() {
                String NOTIFICATION_CHANNEL_ID = "co.at.netconsulting.runningtracker";
                NotificationManager manager = null;
                NotificationChannel serviceChannel;

                serviceChannel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "Foreground Service Channel",
                        NotificationManager.IMPORTANCE_DEFAULT
                );

                manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(serviceChannel);

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
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
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_notification))
                        .build());
            }
        }).start();
    }
    public void generateGfx(List<Run> run, int countOfRun, ProgressBar progressBar) throws IOException, ParseException {
        FileWriter writer = null;
        File download_folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        int lastRun = 0;
        int currentRun;
        boolean start = true;
        int counter = 0;
        double result = 0;

        if(!run.isEmpty()) {
            ListIterator<Run> iterator = run.listIterator();

            while (iterator.hasNext()) {
                Run curInt = iterator.next();
                currentRun = curInt.getNumber_of_run();

                if(currentRun != lastRun) {
                    if(start) {
                        if(lastRun!=0) {
                            String footer = "\t\t</trkseg>\n\t</trk>\n</gpx>";
                            writer.append(footer);
                            writer.flush();
                            writer.close();
                        }
                        writer = createFileName(download_folder, curInt);
                        createHeader(writer, curInt.getDateTime());
                        lastRun = curInt.getNumber_of_run();
                        start = false;
                        counter++;
                        result = Math.ceil((counter*100)/countOfRun);
                        progressBar.setProgress((int) result);
                        textViewPercentage.setText(String.format("%d%%", (int) result));
                    } else {
                        String footer = "\t\t</trkseg>\n\t</trk>\n</gpx>";
                        writer.append(footer);
                        writer.flush();
                        writer.close();
                        writer = createFileName(download_folder, curInt);
                        createHeader(writer, curInt.getDateTime());
                        lastRun = curInt.getNumber_of_run();
                        start = true;
                        counter++;
                        result = Math.ceil((counter*100)/countOfRun);
                        progressBar.setProgress((int) result);
                        textViewPercentage.setText(String.format("%d%%", (int) result));
                    }
                } else {
                    createBody(writer, curInt);
                }
            }
            String footer = "\t\t</trkseg>\n\t</trk>\n</gpx>";
            writer.append(footer);
            writer.flush();
            writer.close();
        } else {
            progressBar.setVisibility(View.GONE);
            textViewPercentage.setVisibility(View.GONE);
            cancelNotification();
        }
    }
    private void cancelNotification() {
        String notificationService = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(notificationService);
        nMgr.cancel(NOTIFICATION_ID);
    }
    @NonNull
    private static FileWriter createFileName(File download_folder, Run curInt) throws IOException {
        FileWriter writer;
        String[] sDateTime = curInt.getDateTime().split(" ");
        String[] sTime = sDateTime[1].split(":");
        String fileName = sDateTime[0] + "_" + sTime[0] + "_" + sTime[1] + "_" + sTime[2];
        writer = new FileWriter(new File(download_folder, "" + fileName + ".gpx"), false);
        return writer;
    }

    private void createBody(FileWriter writer, Run curInt) throws IOException {
        String segments = "";
        Date date = new Date(curInt.getDateTimeInMs());
        String mobileDateTime = getFormatTimeWithTZ(date);

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

    public static String getFormatTimeWithTZ(Date currentTime) {
        SimpleDateFormat timeZoneDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        return timeZoneDate.format(currentTime);
    }

    private void createHeader(FileWriter writer, String name) throws IOException {
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n\t<trk>\n";
        writer.append(header);
        name = "\t\t<name>" + name + "</name>\n\t\t<trkseg>\n";
        writer.append(name);
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

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(DatabaseActivity.this);
        if(allEntries.size() == 0) {
            builderSingle.setTitle(getResources().getString(R.string.no_run_available));
        } else {
            builderSingle.setTitle(getResources().getString(R.string.select_one_run));
        }
        builderSingle.setIcon(R.drawable.icon_notification);

        // prevents closing alertdialog when clicking outside of it
        builderSingle.setCancelable(false);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(DatabaseActivity.this, android.R.layout.select_dialog_singlechoice);
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
    public void exportToServer(View v) {
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_URL_SAVE);
        if(editTextURL.getParent()!=null) {
            ((ViewGroup)editTextURL.getParent()).removeView(editTextURL);
        }
            AlertDialog.Builder alert = new AlertDialog.Builder(DatabaseActivity.this);
            alert.setTitle("Please, type your URL");
            alert.setView(editTextURL);
            editTextURL.setText(httpUrl);

            alert.setCancelable(false);
            alert.setPositiveButton("Export", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String httpUrl = editTextURL.getText().toString();
                    List<Run> allEntries = new ArrayList<Run>(db.getAllEntries());

                    RestAPI restAPI = new RestAPI(getApplicationContext(), httpUrl);
                    restAPI.postRequest(new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                        }
                    }, allEntries.iterator());
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            alert.setNeutralButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_URL_SAVE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedpreferences.edit();

                    String httpUrl = editTextURL.getText().toString();
                    editor.putString(SharedPref.STATIC_SHARED_PREF_URL_SAVE, httpUrl);
                    editor.commit();
                }
            });
            alert.show();
    }
}