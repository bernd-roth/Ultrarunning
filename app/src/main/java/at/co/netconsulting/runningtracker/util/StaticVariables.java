package at.co.netconsulting.runningtracker.util;

import at.co.netconsulting.runningtracker.DatabaseActivity;

public class StaticVariables {
    public static final String TAG = DatabaseActivity.class.getSimpleName();
    public static final double r2d = 180.0D / 3.141592653589793D;
    public static final double d2r = 3.141592653589793D / 180.0D;
    public static final double d2km = 111189.57696D * r2d;
    public static final int DAYS_PER_YEAR = 365;
    public static final float CAMERA_ZOOM = 15f;
}
