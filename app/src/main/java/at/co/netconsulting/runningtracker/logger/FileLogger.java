package at.co.netconsulting.runningtracker.logger;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.content.Context;
import at.co.netconsulting.runningtracker.general.StaticFields;

public class FileLogger {

    public static void logToFile(Context context, String tag, String message) {
        File logFile = new File(context.getExternalFilesDir(null), StaticFields.FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(tag + ": " + message);
            writer.newLine();
        } catch (IOException e) {
            Log.e(StaticFields.LOG_TAG, "Error writing to log file", e);
        }
    }
}