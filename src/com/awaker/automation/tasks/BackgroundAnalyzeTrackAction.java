package com.awaker.automation.tasks;

import com.awaker.analyzer.aot.ThreadedAotAnalyzer;
import com.awaker.data.DbManager;
import com.awaker.data.TrackWrapper;
import com.awaker.global.UserActivityCenter;
import com.awaker.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundAnalyzeTrackAction extends BaseTaskAction {

    static final AtomicBoolean isRunning = new AtomicBoolean(false);

    public BackgroundAnalyzeTrackAction(int id) {
        super(id);
    }

    @Override
    public void run() {
        try {
            //set the isRunning flag to true, if it is false
            if (!isRunning.compareAndSet(false, true)) {
                //if already running, skip, just to be safe
                Log.error("BackgroundAnalyzeTrackAction already running? what?");
                return;
            }
            //only analyze when still idle
            while (UserActivityCenter.isIdle()) {
                if (!analyzeOneTrack()) {
                    //cancel if one analyzation fails
                    break;
                }
            }
        } catch (Exception ex) {
            Log.error(ex);
        }
        isRunning.set(false);
    }

    private boolean analyzeOneTrack() {
        TrackWrapper track = DbManager.getTrackWithoutColors();
        if (track == null) {
            return false;
        }
        ThreadedAotAnalyzer analyzer = new ThreadedAotAnalyzer();

        if (!analyzer.analyze(track)) {
            Log.message("error analyzing track" + track.toString());
            return false;
        }

        DbManager.setMusicColors(track.getId(), analyzer.getOutputArray(), 1);
        return true;
    }
}
