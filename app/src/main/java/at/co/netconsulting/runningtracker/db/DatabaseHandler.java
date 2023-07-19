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
	private static final String KEY_SPEED = "speed";
	private static final String KEY_HEART_RATE = "heart_rate";

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
        values.put(KEY_LAT, run.getLat());
        values.put(KEY_LNG, run.getLng());
        values.put(KEY_SPEED, run.getLng());
        values.put(KEY_HEART_RATE, run.getLng());

        // Inserting Row
        db.insert(TABLE_RUNS, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    // code to get the single contact
    Run getContact(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RUNS, new String[] { KEY_ID,
                        KEY_DATE_TIME, KEY_LAT, KEY_LNG, KEY_SPEED, KEY_HEART_RATE }, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Run run = new Run(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getDouble(2),
                cursor.getDouble(3),
                cursor.getFloat(4),
                cursor.getInt(5));
        // return contact
        return run;
    }

    // code to get all contacts in a list view
    public List<Run> getAllContacts() {
        List<Run> contactList = new ArrayList<Run>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_RUNS;

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
                run.setSpeed(cursor.getFloat(4));
                run.setHeart_rate(cursor.getInt(5));
                // Adding contact to list
                contactList.add(run);
            } while (cursor.moveToNext());
        }
        return contactList;
    }

    // code to update the single contact
//    public int updateContact(Contact contact) {
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        ContentValues values = new ContentValues();
//        values.put(KEY_NAME, contact.getName());
//        values.put(KEY_PH_NO, contact.getPhoneNumber());
//
//        // updating row
//        return db.update(TABLE_CONTACTS, values, KEY_ID + " = ?",
//                new String[] { String.valueOf(contact.getID()) });
//    }
//
//    // Deleting single contact
//    public void deleteContact(Contact contact) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        db.delete(TABLE_CONTACTS, KEY_ID + " = ?",
//                new String[] { String.valueOf(contact.getID()) });
//        db.close();
//    }
//
//    // Getting contacts Count
//    public int getContactsCount() {
//        String countQuery = "SELECT  * FROM " + TABLE_CONTACTS;
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(countQuery, null);
//        cursor.close();
//
//        // return count
//        return cursor.getCount();
//    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RUNS_TABLE = "CREATE TABLE " + TABLE_RUNS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_DATE_TIME + " DATETIME ,"
                + KEY_LAT + " DOUBLE,"
                + KEY_LNG + " DOUBLE,"
                + KEY_SPEED + " DOUBLE,"
                + KEY_HEART_RATE + " INTEGER" + ")";
        db.execSQL(CREATE_RUNS_TABLE);
    }
}