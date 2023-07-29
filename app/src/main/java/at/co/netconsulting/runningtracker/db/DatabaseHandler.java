package at.co.netconsulting.runningtracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import at.co.netconsulting.runningtracker.pojo.Run;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "DATABASE_RUN";
    private static final String TABLE_RUNS = "TABLE_RUN";
    private static final String KEY_ID = "id";
    private static final String KEY_DATE_TIME = "date_time";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";
    private static final String KEY_METERS_COVERED = "km";
	private static final String KEY_SPEED = "speed";
	private static final String KEY_HEART_RATE = "heart_rate";
    private static final String KEY_COMMENT = "comment";
    private static final String KEY_NUMBER_OF_RUN = "number_of_run";
    private static final String KEY_DATETIME_IN_MS = "date_time_ms";
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
        values.put(KEY_HEART_RATE, run.getHeart_rate());
        values.put(KEY_COMMENT, run.getComment());
        values.put(KEY_NUMBER_OF_RUN, run.getNumber_of_run());
        values.put(KEY_DATETIME_IN_MS, run.getDateTimeInMs());

        // Inserting Row
        db.insert(TABLE_RUNS, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    //code to get single entry
    public Run getSingleEntry(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RUNS, new String[] { KEY_ID,
                        KEY_DATE_TIME, KEY_LAT, KEY_LNG, KEY_METERS_COVERED, KEY_SPEED, KEY_HEART_RATE, KEY_COMMENT, KEY_NUMBER_OF_RUN, KEY_DATETIME_IN_MS }, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Run run = new Run(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1),
                cursor.getDouble(2),
                cursor.getDouble(3),
                cursor.getDouble(4),
                cursor.getInt(5),
                cursor.getInt(6),
                cursor.getString(7),
                cursor.getInt(8),
                cursor.getLong(9)
        );
        return run;
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
        return run.getNumber_of_run();
    }

    // code to get all entries in a list
    public List<Run> getAllEntries() {
        List<Run> allEntryList = new ArrayList<Run>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_RUNS;

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
                run.setHeart_rate(cursor.getInt(6));
                run.setComment(cursor.getString(7));
                run.setNumber_of_run(cursor.getInt(8));
                run.setDateTimeInMs(cursor.getInt(9));
                // Adding contact to list
                allEntryList.add(run);
            } while (cursor.moveToNext());
        }
        return allEntryList;
    }

    public List<Run> getEntriesForKalmanFilter() {
        List<Run> kalmanFilterList = new ArrayList<Run>();
        // Select All Query
        String selectQuery = "SELECT"
                + " id,"
                + " lat,"
                + " lng,"
                + " speed,"
                + " date_time_ms"
                + " FROM " + TABLE_RUNS
                + " WHERE number_of_run = (SELECT MAX(number_of_run) FROM " + TABLE_RUNS + ")";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Run run = new Run();
                run.setId(Integer.parseInt(cursor.getString(0)));
                run.setLat(cursor.getDouble(1));
                run.setLng(cursor.getDouble(2));
                run.setSpeed(cursor.getFloat(3));
                run.setDateTimeInMs(cursor.getInt(4));
                // Adding contact to list
                kalmanFilterList.add(run);
            } while (cursor.moveToNext());
        }
        return kalmanFilterList;
    }

    public void delete() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RUNS, null, null);
    }

    public void exportTableContent() {
        try {
            file = new File(context.getExternalFilesDir(null), "run.csv");
            file.createNewFile();
            csvWrite = new CSVWriter(new FileWriter(file));
            db = getReadableDatabase();
            curCSV = db.rawQuery("SELECT * FROM " + TABLE_RUNS,null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while(curCSV.moveToNext()) {
                String arrStr[] ={curCSV.getString(0),
                        curCSV.getString(1),
                        curCSV.getString(2),
                        curCSV.getString(3),
                        curCSV.getString(4),
                        curCSV.getString(5),
                        curCSV.getString(6),
                        curCSV.getString(7),
                        curCSV.getString(8),
                        curCSV.getString(9)
                };
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
        } catch(Exception sqlEx) {
            Log.e("DatabaseHandler", sqlEx.getMessage(), sqlEx);
        }
    }

    public void exportTableContent(String line) {
        try {
            String[] lineSplitted = line.split(" ");

            file = new File(context.getExternalFilesDir(null), "Kalman_filtered.csv");
            boolean fileExists = file.createNewFile();
            if(fileExists) {
                csvWrite = new CSVWriter(new FileWriter(file, true));
            } else {
                csvWrite = new CSVWriter(new FileWriter(file));
            }
            csvWrite.writeNext(lineSplitted);
            csvWrite.close();
            curCSV.close();
        } catch(Exception sqlEx) {
            Log.e("DatabaseHandler", sqlEx.getMessage(), sqlEx);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RUNS_TABLE = "CREATE TABLE " + TABLE_RUNS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_DATE_TIME + " DATETIME ,"
                + KEY_LAT + " DOUBLE,"
                + KEY_LNG + " DOUBLE,"
                + KEY_METERS_COVERED + " DOUBLE,"
                + KEY_SPEED + " DOUBLE,"
                + KEY_HEART_RATE + " INTEGER,"
                + KEY_COMMENT + " STRING,"
                + KEY_NUMBER_OF_RUN + " INTEGER,"
                + KEY_DATETIME_IN_MS + " LONG"
                + ")";
        db.execSQL(CREATE_RUNS_TABLE);
    }
}