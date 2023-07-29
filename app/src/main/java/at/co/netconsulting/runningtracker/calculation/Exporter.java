package at.co.netconsulting.runningtracker.calculation;

import android.content.Context;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;

public class Exporter {

    private Context context;
    private DatabaseHandler db;

    public Exporter(Context context) {
        this.context = context;
        db = new DatabaseHandler(this.context);
    }

    public void writeData(String line) {
        db.exportTableContent(line);
    }
}
