package at.co.netconsulting.runningtracker.calculation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import at.co.netconsulting.runningtracker.db.DatabaseHandler;
import at.co.netconsulting.runningtracker.pojo.Run;

public class Importer {
    private List<String> gpsDataLines;
    private DatabaseHandler db;

    public Importer(DatabaseHandler db) {
        this.db = db;
    }

    public List<String> readData() {
        List<String> gpsDataLines = new ArrayList<>();

        int id;
        double lat;
        double lng;
        float speed;
        long dateTimeInMs;

        List<Run> allRuns = db.getEntriesForKalmanFilter();

        for(Run s : allRuns) {
            id = s.getId();
            lat = s.getLat();
            lng = s.getLng();
            speed = s.getSpeed();
            dateTimeInMs = s.getDateTimeInMs();

            gpsDataLines.add(id + " " + lat + " " + lng + " " + speed + " " + dateTimeInMs);
        }

//        String listString = db.getEntriesForKalmanFilter().stream().map(Object::toString)
//                .collect(Collectors.joining(" "));

//        gpsDataLines.add(String.valueOf(db.getEntriesForKalmanFilter()));
        return gpsDataLines;
    }
}
