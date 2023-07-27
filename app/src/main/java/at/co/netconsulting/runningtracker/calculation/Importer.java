package at.co.netconsulting.runningtracker.calculation;

import java.util.ArrayList;
import java.util.List;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;

public class Importer {
    private List<String> gpsDataLines;
    private DatabaseHandler db;

    public Importer(DatabaseHandler db) {
        this.db = db;
    }

    public List<String> readData() {
        List<String> gpsDataLines = new ArrayList<>();
        gpsDataLines.add(String.valueOf(db.getEntriesForKalmanFilter()));
        return gpsDataLines;
    }
}
