package com.awaker.automation.tasks;

import com.awaker.audio.AudioCommand;
import com.awaker.audio.PlayList;
import com.awaker.data.TrackWrapper;
import com.awaker.global.router.CommandRouter;
import com.awaker.server.json.CommandData;

/**
 * Task to play music.
 */
public class PlayMusicTask extends BaseTaskAction {

    private final TrackWrapper trackToPlay;
    private final PlayList playList;

    public PlayMusicTask(int id) {
        super(id);
        this.trackToPlay = null;
        this.playList = null;
    }

    /**
     * Creates a task to play a track of a playlist.
     *
     * @param id          the id of the task
     * @param trackToPlay the track to play
     * @param playList    the playlist which contains the trackToPlay
     */
    public PlayMusicTask(int id, TrackWrapper trackToPlay, PlayList playList) {
        super(id);
        this.trackToPlay = trackToPlay;
        this.playList = playList;
    }

    /**
     * Creates a task to play a track.
     *
     * @param id          the id of the task
     * @param trackToPlay the track to play
     */
    public PlayMusicTask(int id, TrackWrapper trackToPlay) {
        super(id);
        this.trackToPlay = trackToPlay;
        this.playList = null;
    }

    /**
     * Creates a task to play a playlist
     *
     * @param id       the id of the task
     * @param playList the playlist to play
     */
    public PlayMusicTask(int id, PlayList playList) {
        super(id);
        this.trackToPlay = null;
        this.playList = playList;
    }

    @Override
    public void run() {
        if (trackToPlay == null && playList == null) {
            CommandRouter.handleCommand(AudioCommand.PLAY);

        } else if (playList != null && trackToPlay != null) {
            CommandData data = new CommandData();
            data.playlistId = playList.getId();
            data.trackId = trackToPlay.getId();

            CommandRouter.handleCommand(AudioCommand.PLAY_TRACK_OF_PLAYLIST, data);

        } else if (playList != null) {
            CommandData data = new CommandData();
            data.playlistId = playList.getId();
            CommandRouter.handleCommand(AudioCommand.PLAY_PLAYLIST, data);

        } else {
            CommandData data = new CommandData();
            data.trackId = trackToPlay.getId();
            CommandRouter.handleCommand(AudioCommand.PLAY_ID, data);
        }
    }
}
