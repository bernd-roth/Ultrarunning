package at.co.netconsulting.runningtracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import at.co.netconsulting.runningtracker.pojo.Run;
import timber.log.Timber;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 7;
    private static final String DATABASE_NAME = "DATABASE_RUN";
    private static final String TABLE_RUNS = "TABLE_RUN";
    private static final String KEY_ID = "id";
    private static final String KEY_DATE_TIME = "date_time";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";
    private static final String KEY_METERS_COVERED = "km";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_COMMENT = "comment";
    private static final String KEY_NUMBER_OF_RUN = "number_of_run";
    private static final String KEY_DATETIME_IN_MS = "date_time_ms";
    private static final String KEY_LAPS = "laps";
    private static final String KEY_ALTITUDE = "altitude";
    private static final String KEY_PERSON = "person";
    private Context context;
    private File file;
    private CSVWriter csvWrite;
    private SQLiteDatabase db;
    private Cursor curCSV;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RUNS);

        // Create tables again
        onCreate(db);
    }

    // code to add the new data
    public void addRun(Run run) {
        db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_DATE_TIME, run.getDateTime());
        values.put(KEY_LAT, run.getLat());
        values.put(KEY_LNG, run.getLng());
        values.put(KEY_METERS_COVERED, run.getMeters_covered());
        values.put(KEY_SPEED, run.getSpeed());
        values.put(KEY_COMMENT, run.getComment());
        values.put(KEY_NUMBER_OF_RUN, run.getNumber_of_run());
        values.put(KEY_DATETIME_IN_MS, run.getDateTimeInMs());
        values.put(KEY_LAPS, run.getLaps());
        values.put(KEY_ALTITUDE, run.getAltitude());
        values.put(KEY_PERSON, run.getPerson());

        // Inserting Row
        db.insert(TABLE_RUNS, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    public int getLastEntry() {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_RUNS;
        Cursor cursor = db.rawQuery(selectQuery, null);

        Run run = new Run();

        if (cursor.moveToLast()) {
            do {
                run.setNumber_of_run(cursor.getInt(8));
            } while (cursor.moveToNext());
        } else {
            run.setNumber_of_run(0);
        }
        cursor.close();
        return run.getNumber_of_run();
    }

    public void updateComment(String comment) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_COMMENT, comment);

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_RUNS + " SET " + KEY_COMMENT
                + " = \"" + comment + "\" WHERE " + KEY_NUMBER_OF_RUN
                + " = (" +
                " SELECT MAX(" + KEY_NUMBER_OF_RUN + ") FROM "
                + TABLE_RUNS + ")");
    }
    public void updateCommentById(int id, String comment) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_COMMENT, comment);

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_RUNS + " SET " + KEY_COMMENT
                + " = \"" + comment + "\" WHERE " + KEY_NUMBER_OF_RUN
                + " = " + id);
    }

    public List<Run> getAllEntriesGroupedByRun() {
        List<Run> allEntryList = new ArrayList<Run>();

        // Select All Query
        String selectQuery = "SELECT "
                + KEY_ID + ", "
                + KEY_DATE_TIME + ", "
                + KEY_LAT + ", "
                + KEY_LNG + ", MAX("
                + KEY_METERS_COVERED + "), "
                + KEY_SPEED + ", "
                + KEY_COMMENT + ", "
                + KEY_NUMBER_OF_RUN + ", MAX("
                + KEY_DATETIME_IN_MS + ") - MIN("
                + KEY_DATETIME_IN_MS + "), "
                + KEY_LAPS + ", "
                + KEY_ALTITUDE + ", "
                + KEY_PERSON
                + " FROM " + TABLE_RUNS + " GROUP BY " + KEY_NUMBER_OF_RUN;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Run run = new Run();
                run.setId(Integer.parseInt(cursor.getString(0)));
                run.setDateTime(cursor.getString(1));
                run.setLat(cursor.getDouble(2));
                run.setLng(cursor.getDouble(3));
                run.setMeters_covered(cursor.getDouble(4));
                run.setSpeed((float) cursor.getDouble(5));
                run.setComment(cursor.getString(6));
                run.setNumber_of_run(cursor.getInt(7));
                run.setDateTimeInMs(cursor.getInt(8));
                run.setLaps(cursor.getInt(9));
                run.setAltitude(cursor.getDouble(10));
                run.setPerson(cursor.getString(11));
                allEntryList.add(run);
            } while (cursor.moveToNext());
        }
        return allEntryList;
    }

    public List<Run> getSingleEntryForStatistics(int numberOfRun) {
        List<Run> allEntryList = new ArrayList<Run>();

        // Select All Query
        String selectQuery = "SELECT "
                + KEY_ID + ", "
                + KEY_DATE_TIME + ", "
                + KEY_LAT + ", "
                + KEY_LNG + ", "
                + KEY_METERS_COVERED + ", "
                + KEY_SPEED + ", "
                + KEY_COMMENT + ", "
                + KEY_NUMBER_OF_RUN + ", "
                + KEY_DATETIME_IN_MS + ", "
                + KEY_LAPS + ", "
                + KEY_ALTITUDE + ", "
                + KEY_PERSON
                + " FROM " + TABLE_RUNS + " WHERE " + KEY_NUMBER_OF_RUN + " = " + numberOfRun;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Run run = new Run();
                run.setId(Integer.parseInt(cursor.getString(0)));
                run.setDateTime(cursor.getString(1));
                run.setLat(cursor.getDouble(2));
                run.setLng(cursor.getDouble(3));
                run.setMeters_covered(cursor.getDouble(4));
                run.setSpeed((float) cursor.getDouble(5));
                run.setComment(cursor.getString(6));
                run.setNumber_of_run(cursor.getInt(7));
                run.setDateTimeInMs(cursor.getLong(8));
                run.setLaps(cursor.getInt(9));
                run.setAltitude(cursor.getDouble(10));
                run.setPerson(cursor.getString(11));
                allEntryList.add(run);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return allEntryList;
    }

    public List<Run> getAllEntriesOrderedByRunNumber() {
        List<Run> allEntryList = new ArrayList<Run>();

        // Select All Query
        String selectQuery = "SELECT "
                + KEY_NUMBER_OF_RUN + ", "
                + KEY_DATE_TIME + ", "
                + KEY_COMMENT + ", "
                + KEY_METERS_COVERED
                + " FROM " + TABLE_RUNS + " GROUP BY "
                + KEY_NUMBER_OF_RUN + " ORDER BY "
                + KEY_NUMBER_OF_RUN + " DESC";

        List<Run> runs = new ArrayList<Run>();

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Run run = new Run();

                run.setNumber_of_run(cursor.getInt(0));
                run.setDateTime(cursor.getString(1));
                run.setComment(cursor.getString(2));
                run.setMeters_covered(cursor.getDouble(3));

                runs.add(run);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return runs;
    }

    public List<Run> getSingleEntryOrderedByDateTime(int numberOfRun) {
        List<Run> allEntryList = new ArrayList<Run>();

        // Select All Query
        String selectQuery = "SELECT "
                + KEY_LAT + ", "
                + KEY_LNG + ", "
                + KEY_SPEED + ", "
                + KEY_METERS_COVERED
                + " FROM " + TABLE_RUNS + " WHERE " + KEY_NUMBER_OF_RUN + " = " + numberOfRun;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Run run = new Run();
                run.setLat(cursor.getDouble(0));
                run.setLng(cursor.getDouble(1));
                run.setSpeed(cursor.getFloat(2));
                run.setMeters_covered(cursor.getDouble(3));
                allEntryList.add(run);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return allEntryList;
    }
    public List<Run> getLastCommentEntryOfSelectedRun(int numberOfRun) {
        List<Run> allEntryList = new ArrayList<Run>();

        // Select All Query
        String selectQuery = "SELECT "
                + KEY_COMMENT
                + " FROM " + TABLE_RUNS + " WHERE " + KEY_NUMBER_OF_RUN + " = " + numberOfRun;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Run run = new Run();
                run.setComment(cursor.getString(0));
                allEntryList.add(run);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return allEntryList;
    }

    public int countDataOfRun(int numberOfRun) {
        // Select All Query
        String selectQuery = "SELECT COUNT("
                + KEY_NUMBER_OF_RUN
                + ") FROM " + TABLE_RUNS + " WHERE " + KEY_NUMBER_OF_RUN + " = " + numberOfRun;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();

        return count;
    }

    public void delete() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RUNS, null, null);
    }

    public void deleteSingleEntry(int number_of_run) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RUNS, "number_of_run = ?", new String[]{String.valueOf(number_of_run)});
    }
    public void exportTableContent() {
        try {
            exportAllDatabases(context);
        } catch(Exception sqlEx) {
            Log.e("DatabaseHandler", sqlEx.getMessage(), sqlEx);
        }
    }
    public static void exportAllDatabases(final Context context) {
        try {
            File download_folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File data = Environment.getDataDirectory();
            final File[] databases = new File(context.getFilesDir().getParentFile().getPath() + "/databases").listFiles();

            String resultDate = formatCurrentDate();

            for (File databaseFile : databases) {
                if (download_folder.canWrite()) {
                    if (databaseFile.getName().startsWith("DATABASE_RUN")) {
                        String currentDBPath = "//data//" + context.getPackageName() + "//databases//DATABASE_RUN";
                        String backupDBPath = databaseFile.getName() + "_" + resultDate;
                        File currentDB = new File(data, currentDBPath);
                        File backupDB = new File(download_folder, backupDBPath);

                        if (currentDB.exists()) {
                            FileChannel src = new FileInputStream(currentDB).getChannel();
                            FileChannel dst = new FileOutputStream(backupDB).getChannel();
                            dst.transferFrom(src, 0, src.size());
                            src.close();
                            dst.close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(context, "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Timber.e("DatabaseHandler: Exception: " + e.getMessage());
        }
    }

    private static String formatCurrentDate() {
        long milliSeconds = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy_HH_mm");
        Date resultdate = new Date(milliSeconds);
        return sdf.format(resultdate);
    }

    public List<Run> getAllEntriesForYearCalculation() {
        List<Run> allEntryList = new ArrayList<Run>();

        // Select All Query
        String selectQuery = "SELECT "
                + "MAX("
                + KEY_DATE_TIME + "), "
                + KEY_METERS_COVERED + ", "
                + KEY_NUMBER_OF_RUN + ", "
                + KEY_ALTITUDE
                + " FROM " + TABLE_RUNS + " GROUP BY " + KEY_NUMBER_OF_RUN;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Run run = new Run();
                run.setDateTime(cursor.getString(0));
                run.setMeters_covered(cursor.getDouble(1));
                run.setNumber_of_run(cursor.getInt(2));
                run.setAltitude(cursor.getDouble(3));
                allEntryList.add(run);
            } while (cursor.moveToNext());
        }
        return allEntryList;
    }

    public List<Run> getAllEntries() {
        List<Run> allEntryList = new ArrayList<Run>();

        // Select All Query
        String selectQuery = "SELECT "
                + KEY_DATE_TIME + ", "
                + KEY_LAT + ", "
                + KEY_LNG + ", "
                + KEY_METERS_COVERED + ", "
                + KEY_SPEED + ", "
                + KEY_COMMENT + ", "
                + KEY_NUMBER_OF_RUN + ", "
                + KEY_DATETIME_IN_MS + ", "
                + KEY_LAPS + ", "
                + KEY_ALTITUDE + ", "
                + KEY_PERSON
                + " FROM " + TABLE_RUNS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Run run = new Run();
                run.setDateTime(cursor.getString(0));
                run.setLat(cursor.getDouble(1));
                run.setLng(cursor.getDouble(2));
                run.setMeters_covered(cursor.getDouble(3));
                run.setSpeed((float) cursor.getDouble(4));
                run.setComment(cursor.getString(5));
                run.setNumber_of_run(cursor.getInt(6));
                run.setDateTimeInMs(cursor.getInt(7));
                run.setLaps(cursor.getInt(8));
                run.setAltitude(cursor.getDouble(9));
                run.setPerson(cursor.getString(10));
                allEntryList.add(run);
            } while (cursor.moveToNext());
        }
        return allEntryList;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RUNS_TABLE = "CREATE TABLE " + TABLE_RUNS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_DATE_TIME + " DATETIME ,"
                + KEY_LAT + " TEXT,"
                + KEY_LNG + " TEXT,"
                + KEY_METERS_COVERED + " DOUBLE,"
                + KEY_SPEED + " DOUBLE,"
                + KEY_COMMENT + " STRING,"
                + KEY_NUMBER_OF_RUN + " INTEGER,"
                + KEY_DATETIME_IN_MS + " LONG,"
                + KEY_LAPS + " INTEGER,"
                + KEY_ALTITUDE + " DOUBLE,"
                + KEY_PERSON + " STRING"
                + ")";
        db.execSQL(CREATE_RUNS_TABLE);

        String CREATE_INDEX_TABLE =
                "CREATE INDEX idx_number_of_run ON " + TABLE_RUNS + "(" + KEY_NUMBER_OF_RUN +");";
        db.execSQL(CREATE_INDEX_TABLE);
    }
}