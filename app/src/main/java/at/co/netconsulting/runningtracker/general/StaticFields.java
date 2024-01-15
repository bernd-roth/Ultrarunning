package at.co.netconsulting.runningtracker.general;

import java.text.DecimalFormat;

public class StaticFields {
    public final static float MIN_ACCURACY = 3f;
    public final static int STATIC_INTEGER_MIN_DISTANCE_METER = 10;
    public final static int STATIC_LONG_MIN_TIME_MS = 10;
    public final static String STATIC_SAVE_ON_COMMENT_PAUSE = "COMMENT_AND_PAUSE";
    public final static DecimalFormat df = new DecimalFormat("0.00");
    public final static String STATIC_STRING_PERSON = "Anonym";
    public final static boolean STATIC_BOOLEAN_DISTANCE_COVERED = false;
    public final static double TIME_INTERVAL = 1.0;
    public final static long ONE_DAY_IN_MILLISECONDS = 86400000;
}