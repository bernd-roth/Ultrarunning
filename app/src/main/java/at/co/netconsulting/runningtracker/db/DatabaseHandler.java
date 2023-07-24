package at.co.netconsulting.runningtracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import at.co.netconsulting.runningtracker.pojo.Run;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
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

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RUNS);

        // Create tables again
        onCreate(db);
    }

    // code to add the new contact
    public void addRun(Run run) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_DATE_TIME, run.getDateTime());
        values.put(KEY_LAT, run.getLat());
        values.put(KEY_LNG, run.getLng());
        values.put(KEY_METERS_COVERED, run.getMeters_covered());
        values.put(KEY_SPEED, run.getSpeed());
        values.put(KEY_HEART_RATE, run.getHeart_rate());
        values.put(KEY_COMMENT, run.getComment());
        values.put(KEY_NUMBER_OF_RUN, run.getNumber_of_run());

        // Inserting Row
        db.insert(TABLE_RUNS, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    //code to get single entry
    public Run getSingleEntry(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RUNS, new String[] { KEY_ID,
                        KEY_DATE_TIME, KEY_LAT, KEY_LNG, KEY_METERS_COVERED, KEY_SPEED, KEY_HEART_RATE, KEY_COMMENT, KEY_NUMBER_OF_RUN }, KEY_ID + "=?",
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
                cursor.getInt(8)
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

    // code to get all entries in a list view
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
                run.setSpeed(cursor.getFloat(5));
                run.setHeart_rate(cursor.getInt(6));
                run.setComment(cursor.getString(7));
                run.setNumber_of_run(cursor.getInt(8));
                // Adding contact to list
                allEntryList.add(run);
            } while (cursor.moveToNext());
        }
        return allEntryList;
    }

    public void delete() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RUNS, null, null);
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
                + KEY_NUMBER_OF_RUN + " INTEGER" + ")";
        db.execSQL(CREATE_RUNS_TABLE);
    }
}