package at.co.netconsulting.runningtracker.calculation;

import android.content.Context;
import org.greenrobot.eventbus.Subscribe;
import at.co.netconsulting.runningtracker.general.StaticFields;

public class KalmanFilter {
    private long timeStamp; // millis
    private double latitude; // degree
    private double longitude; // degree
    private float variance; // P matrix. Initial estimate of error
    private Exporter exporter;
    private Context context;

    public KalmanFilter(Context context) {
        exporter = new Exporter(context);
        registerBus();
        variance = -1;
        this.context = context;
    }

    private void registerBus() {
        BusProvider.getInstance().register(this);
    }

    @Subscribe
    public void onLocationUpdate(GPSSingleData singleData) {
        // if gps receiver is able to return 'accuracy' of position, change last variable
        process(singleData.getSpeed(), singleData.getLatitude(), singleData.getLongitude(), singleData.getTimestamp(),
                StaticFields.MIN_ACCURACY);
    }

    // Init method (use this after constructor, and before process)
    // if you are using last known data from gps)
    public void setState(double latitude, double longitude, long timeStamp, float accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeStamp = timeStamp;
        this.variance = accuracy * accuracy;
    }

    /**
     * Kalman filter processing for latitude and longitude
     *
     * newLatitude - new measurement of latitude
     * newLongitude - new measurement of longitude
     * accuracy - measurement of 1 standard deviation error in meters
     * newTimeStamp - time of measurement in millis
     */
    public void process(float newSpeed, double newLatitude, double newLongitude, long newTimeStamp, float newAccuracy) {
        // Uncomment this, if you are receiving accuracy from your gps
        // if (newAccuracy < Constants.MIN_ACCURACY) {
        //      newAccuracy = Constants.MIN_ACCURACY;
        // }
        if (variance < 0) {
            // if variance < 0, object is unitialised, so initialise with current values
            setState(newLatitude, newLongitude, newTimeStamp, newAccuracy);
        } else {
            // else apply Kalman filter
            long duration = newTimeStamp - this.timeStamp;
            if (duration > 0) {
                // time has moved on, so the uncertainty in the current position increases
                variance += duration * newSpeed * newSpeed / 1000;
                timeStamp = newTimeStamp;
            }

            // Kalman gain matrix 'k' = Covariance * Inverse(Covariance + MeasurementVariance)
            // because 'k' is dimensionless,
            // it doesn't matter that variance has different units to latitude and longitude
            float k = variance / (variance + newAccuracy * newAccuracy);
            // apply 'k'
            latitude += k * (newLatitude - latitude);
            longitude += k * (newLongitude - longitude);
            // new Covariance matrix is (IdentityMatrix - k) * Covariance
            variance = (1 - k) * variance;

            // Export new point
            exportNewPoint(newSpeed, longitude, latitude, duration);
        }
    }

    private void exportNewPoint(float speed, double longitude, double latitude, long timestamp) {
        GPSSingleData newGPSdata = new GPSSingleData(speed, longitude, latitude, timestamp);
        exporter.writeData(newGPSdata.toString());
    }
}
