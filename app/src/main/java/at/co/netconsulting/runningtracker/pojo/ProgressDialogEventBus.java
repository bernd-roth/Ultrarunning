package at.co.netconsulting.runningtracker.pojo;

import android.app.ProgressDialog;

public class ProgressDialogEventBus {
    public boolean isProgressDialogRunning;

    public ProgressDialogEventBus(boolean isProgressDialogRunning) {
        this.isProgressDialogRunning = isProgressDialogRunning;
    }
}
