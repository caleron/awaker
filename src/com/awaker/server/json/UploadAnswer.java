package com.awaker.server.json;

import com.awaker.data.TrackWrapper;

public class UploadAnswer {
    public String status;
    public TrackWrapper track;
    public String filename;

    public UploadAnswer(TrackWrapper trackWrapper, String filename) {
        if (trackWrapper == null) {
            status = "failed";
            return;
        }
        status = "success";

        this.track = trackWrapper;
        this.filename = filename;
    }
}
