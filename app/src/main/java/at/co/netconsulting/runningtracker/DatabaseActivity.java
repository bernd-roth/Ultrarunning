package at.co.netconsulting.runningtracker;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xmlpull.v1.XmlPullParserException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.general.SharedPref;
import at.co.netconsulting.runningtracker.general.StaticFields;
import at.co.netconsulting.runningtracker.pojo.ProgressDialogEventBus;
import at.co.netconsulting.runningtracker.pojo.Run;
import at.co.netconsulting.runningtracker.service.GpxForegroundService;
import at.co.netconsulting.runningtracker.util.StaticVariables;
import at.co.netconsulting.runningtracker.view.RestAPI;
import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;

public class DatabaseActivity extends AppCompatActivity {
    private int numberInDays;
    private SharedPreferences sharedpreferences;
    private PendingIntent pendingIntent;
    private TextView textViewExportDatabaseScheduled, textViewPercentage;
    private Long nextBackInMilliseconds;
    private ProgressDialog progressDialog, progressDialogImportingGPXFile;
    private DatabaseHandler db;
    private EditText editTextNumber, editTextURL, editTextLiveURL;
    private String httpUrl;
    private TextView textViewExportToServer, textViewExportDatabase;
    private LinearLayout linearlayout;
    private AlertDialog.Builder alert;
    private AlertDialog dialog;
    private final int NOTIFICATION_ID = 1;
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

        progressDialogImportingGPXFile = new ProgressDialog(DatabaseActivity.this);
        progressDialogImportingGPXFile.setMessage("Importing GPX file..."); // Setting Message
        progressDialogImportingGPXFile.setTitle("Importing recorded run"); // Setting Title
        progressDialogImportingGPXFile.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
        progressDialogImportingGPXFile.setCancelable(false);

        editTextNumber = new EditText(DatabaseActivity.this);
        editTextNumber.setSingleLine(true);
        editTextNumber.setText(String.valueOf(numberInDays));

        editTextURL = new EditText(DatabaseActivity.this);
        editTextLiveURL = new EditText(DatabaseActivity.this);

        linearlayout = findViewById(R.id.ll);

        textViewPercentage = findViewById(R.id.textViewPercentage);
        textViewPercentage.setVisibility(View.INVISIBLE);

        db = new DatabaseHandler(this);
        //set screen orientaiton to potrait modus automatically
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
                    String pattern = "EEEE dd/MM/yyyy HH:mm:ss";
                    String message = String.format(getString(R.string.database_export),
                            new SimpleDateFormat(pattern, new Locale("en", "UK")).format(new Date(nextBackInMilliseconds)));
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

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
        if(((TextView) view ).getText().toString().equals(getResources().getString(R.string.export_selected_to_gpx_file))) {
            List<Run> allEntriesGroupedByRun = db.getAllEntriesGroupedByRun();
            boolean[] checkedItems = new boolean[allEntriesGroupedByRun.size()];
            CharSequence[] dateList = new CharSequence[allEntriesGroupedByRun.size()];

            for(int i = 0; i<allEntriesGroupedByRun.size(); i++) {
                dateList[i] = allEntriesGroupedByRun.get(i).getDateTime();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose run to export");
            builder.setMultiChoiceItems(dateList, checkedItems, new OnMultiChoiceClickListener() {
                public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                }
            });
            builder.setCancelable(false);
            builder.setPositiveButton("Done", (dialog, which) -> {
                List<Run> selectedRun = new ArrayList<>();
                for (int i = 0; i < checkedItems.length; i++) {
                    if (checkedItems[i]) {
                        Log.d("CHECKED_ITEM", "Checked item: " + i + " - Item: " + checkedItems[i]);
                        selectedRun.add(allEntriesGroupedByRun.get(i));
                    }
                }
                if(selectedRun.size()>0) {
                    Context context = getApplicationContext();
                    Intent intentForegroundService = new Intent(getApplicationContext(), GpxForegroundService.class);
                    intentForegroundService.putExtra("SELECTED_RUN", (Serializable) selectedRun);
                    intentForegroundService.setAction("ACTION_START_GPXFOREGROUNDSERVICE");
                    context.startForegroundService(intentForegroundService);

                    if (!EventBus.getDefault().isRegistered(this)) {
                        EventBus.getDefault().register(this);
                    }
                    progressDialog = new ProgressDialog(DatabaseActivity.this);
                    progressDialog.setTitle("Exporting recorded runs"); // Setting Title
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                } else {
                    Toast.makeText(getApplicationContext(), "Please, choose at least one run to export!", Toast.LENGTH_LONG).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.create();
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else {
            Context context = getApplicationContext();
            Intent intentForegroundService = new Intent(getApplicationContext(), GpxForegroundService.class);
            intentForegroundService.setAction("ACTION_START_GPXFOREGROUNDSERVICE");
            context.startForegroundService(intentForegroundService);

            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
            }
            progressDialog = new ProgressDialog(DatabaseActivity.this);
            progressDialog.setTitle("Exporting recorded runs"); // Setting Title
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }
    private void cancelNotification() {
        String notificationService = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(notificationService);
        nMgr.cancel(NOTIFICATION_ID);
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
        String buttonText = ( (TextView) view ).getText().toString();
        if(buttonText.startsWith("Delete")) {
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
            alert.setTitle(R.string.please_type_your_url);
            alert.setView(editTextURL);
            editTextURL.setText(httpUrl);

            alert.setCancelable(false);
            alert.setPositiveButton(R.string.export_positive_button, new DialogInterface.OnClickListener() {
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

    @Subscribe
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, GpxForegroundService.class));
        progressDialog.dismiss();
        EventBus.getDefault().unregister(this);
    }

    public void reorgDatabase(View view) throws ParseException {
        SimpleDateFormat dateformatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        TreeMap<Long, Run> run = new TreeMap<>();
        List<Run> allEntriesGroupedByRun = db.getAllEntriesGroupedByRun();

        for(int i = 0; i<allEntriesGroupedByRun.size(); i++) {
            String dateTime = allEntriesGroupedByRun.get(i).getDateTime();

            Date date = dateformatter.parse(dateTime);
            long msec = date.getTime();
            run.put(msec, allEntriesGroupedByRun.get(i));
        }

        int newNumberOfRun = 0;
        long key = 0;
        for (Map.Entry<Long, Run> entry : run.entrySet()) {
            key = entry.getKey();

            int oldNumberOfRun = run.get(key).getNumber_of_run();
            newNumberOfRun++;
            db.updateNumberOfRun(oldNumberOfRun, newNumberOfRun);
        }
        db.close();
    }

    public void importGPXFile(View view) {
        Intent data = new Intent(Intent.ACTION_GET_CONTENT);
        data.setType("*/*");
        data = Intent.createChooser(data, getString(R.string.choose_a_file));
        startActivityResultLauncherImportGPXFile.launch(data);
    }

    ActivityResultLauncher<Intent> startActivityResultLauncherImportGPXFile = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                // Handle the returned Uri
                Log.d("startActivityResultLauncherImportGPXFile", "FileChooser works");

                progressDialogImportingGPXFile.show();
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Run run = new Run();
                            GPXParser parser = new GPXParser();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                            long millisFromEpoch;
                            double oldLat = 0, oldLng = 0, newLat, newLon, meters_covered = 0;
                            int counter = 0;

                            int lastRun = db.getLastEntry();
                            lastRun+=1;

                            Gpx parsedGpx = null;
                            try {
                                InputStream is = getContentResolver().openInputStream(result.getData().getData());
                                parsedGpx = parser.parse(is); // consider using a background thread
                            } catch (IOException | XmlPullParserException e) {
                                e.printStackTrace();
                            }
                            if (parsedGpx == null) {
                                Log.e("GpxParsingError", "Parsing error");
                            } else {
                                List<Track> tracks = parsedGpx.getTracks();
                                for (int i = 0; i < tracks.size(); i++) {
                                    Track track = tracks.get(i);
                                    List<TrackSegment> segments = track.getTrackSegments();
                                    for (int j = 0; j < segments.size(); j++) {
                                        TrackSegment segment = segments.get(j);
                                        for (TrackPoint trackPoint : segment.getTrackPoints()) {
                                            String msg = "    point: lat " + trackPoint.getLatitude() + ", lon " + trackPoint.getLongitude() + ", time " + trackPoint.getTime();
                                            Log.d(StaticVariables.TAG, msg);
                                            millisFromEpoch = Instant.parse( trackPoint.getTime().toString()).toEpochMilli();

                                            Date date=new Date(millisFromEpoch);
                                            String output = sdf.format(date);

                                            if(counter==0) {
                                                oldLat=trackPoint.getLatitude();
                                                oldLng=trackPoint.getLongitude();
                                                counter++;
                                            } else {
                                                newLat=trackPoint.getLatitude();
                                                newLon=trackPoint.getLongitude();
                                                meters_covered += calculateDistance(oldLat, oldLng, newLat, newLon);
                                                oldLat=newLat;
                                                oldLng=newLon;
                                                counter++;
                                            }
                                            run.setDateTime(output);
                                            run.setLat(trackPoint.getLatitude());
                                            run.setLng(trackPoint.getLongitude());
                                            run.setMeters_covered(meters_covered);
                                            run.setAltitude(trackPoint.getElevation());
                                            run.setDateTimeInMs(millisFromEpoch);
                                            run.setNumber_of_run(lastRun);
                                            db.addRun(run);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        progressDialogImportingGPXFile.dismiss();
                    }
                }).start();
            }
        }
    );
    private double calculateDistance(double oldLat, double oldLng, double newLat, double newLon) {
        double x = oldLat * StaticVariables.d2r;
        double y = newLat * StaticVariables.d2r;
        return Math.acos( Math.sin(x) * Math.sin(y) + Math.cos(x) * Math.cos(y) * Math.cos(StaticVariables.d2r * (oldLng - newLon))) * StaticVariables.d2km;
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onExportingToGPXFile(ProgressDialogEventBus progressDialogEventBus) {
        if(!progressDialogEventBus.isProgressDialogRunning) {
            progressDialog.dismiss();
            EventBus.getDefault().unregister(this);
        }
    }
    public void liveExportToServer(View view) {
        loadSharedPreferences(SharedPref.STATIC_SHARED_PREF_URL_SAVE);
        if(editTextLiveURL.getParent()!=null) {
            ((ViewGroup)editTextLiveURL.getParent()).removeView(editTextLiveURL);
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(DatabaseActivity.this);
        alert.setTitle(R.string.please_type_your_url);
        alert.setView(editTextLiveURL);
        editTextLiveURL.setText(editTextLiveURL.getText().toString().toUpperCase());

        alert.setCancelable(false);
        alert.setPositiveButton(R.string.save_live_url, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sharedpreferences = getSharedPreferences(SharedPref.STATIC_SHARED_PREF_LIVE_URL_SAVE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();

                String httpUrl = editTextLiveURL.getText().toString();
                editor.putString(SharedPref.STATIC_SHARED_PREF_URL_SAVE, httpUrl);
                editor.commit();
            }
        });
        alert.setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert.show();
    }
}