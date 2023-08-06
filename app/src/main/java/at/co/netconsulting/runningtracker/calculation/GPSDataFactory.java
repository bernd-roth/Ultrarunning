package at.co.netconsulting.runningtracker.calculation;

import org.greenrobot.eventbus.EventBus;
import java.util.List;
import at.co.netconsulting.runningtracker.db.DatabaseHandler;

public class GPSDataFactory {
    private List<String> gpsDataLines;
    private EventBus bus;

    private final long SLEEP_TIME = 1000;

    public GPSDataFactory(DatabaseHandler db) {
        Importer importer = new Importer(db);
        gpsDataLines = importer.readData();
        registerBus();
        startFactory();
    }

    private void registerBus() {
        bus = BusProvider.getInstance();
    }

    private void startFactory() {
        Thread thread = new Thread(() -> {
            for (String string : gpsDataLines) {
                GPSSingleData gpsSingleData = proccessLine(string);
                bus.post(gpsSingleData);

                // Simulate GPS intervals
                // pauseThread(SLEEP_TIME);
            }
        });
        thread.start();
    }

    private GPSSingleData proccessLine(String gpsLine) {
        if(!gpsLine.isEmpty()) {
            String[] gpsParts = gpsLine.split(" ");
            return new GPSSingleData(Long.valueOf(gpsParts[4]), Double.valueOf(gpsParts[2]),
                    Double.valueOf(gpsParts[3]), Float.valueOf(gpsParts[1]));
        } else {
            return new GPSSingleData(0, 0, 0, 0);
        }
    }

    private void pauseThread(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
