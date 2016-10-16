package com.awaker.server.json;

/**
 * Repräsentiert einen Befehl, der von einem Client geschickt wurde. Beinhaltet alle möglichen Felder, die durch
 * Deserialisierung von JSON durch Gson gesetzt werden.
 */
@SuppressWarnings("unused")
public class JsonCommand {
    //TODO commands können auch vom server zum client gesendet werden, etwa beim Hinzufügen eines Tracks zu einer Playlist,
    //damit nicht zu jedem Client die gesamte Library geschickt werden muss

    private static final String PLAY = "play";
    private static final String PLAY_ID = "playId";
    private static final String PLAY_ID_LIST = "playIdList";
    private static final String PLAY_FROM_POSITION = "playFromPosition";
    private static final String PAUSE = "pause";
    private static final String STOP = "stop";
    private static final String TOGGLE_PLAY_PAUSE = "togglePlayPause";
    private static final String CHECK_FILE = "checkFile";
    private static final String PLAY_NEXT = "playNext";
    private static final String PLAY_PREVIOUS = "playPrevious";
    private static final String SET_SHUFFLE = "setShuffle";
    private static final String SET_REPEAT_MODE = "setRepeatMode";
    private static final String SET_VOLUME = "setVolume";
    private static final String SET_WHITE_BRIGHTNESS = "setWhiteBrightness";
    private static final String SET_ANIMATION_BRIGHTNESS = "setAnimationBrightness";
    private static final String SET_COLOR_MODE = "setColorMode";
    private static final String SET_COLOR = "setColor";
    private static final String SET_RGBCOLOR = "setRGBColor";
    private static final String CHANGE_VISUALIZATION = "changeVisualization";
    private static final String CREATE_PLAYLIST = "createPlaylist";
    private static final String REMOVE_PLAYLIST = "removePlaylist";
    private static final String ADD_TRACKS_TO_PLAYLIST = "addTracksToPlaylist";
    private static final String REMOVE_TRACKS_FROM_PLAYLIST = "removeTracksFromPlaylist";
    private static final String PLAY_PLAYLIST = "playPlaylist";
    private static final String PLAY_TRACK_OF_PLAYLIST = "playTrackOfPlaylist";
    private static final String ADD_TRACKS_TO_QUEUE = "addTracksToQueue";
    private static final String REMOVE_TRACKS_FROM_QUEUE = "removeTracksFromQueue";
    private static final String PLAY_TRACK_OF_QUEUE = "playTrackOfQueue";
    private static final String PLAY_TRACK_NEXT = "playTrackNext";
    private static final String GET_STATUS = "getStatus";
    private static final String GET_LIBRARY = "getLibrary";
    private static final String SEND_STRING = "sendString";
    private static final String SHUTDOWN_SERVER = "shutdownServer";
    private static final String SHUTDOWN_RASPI = "shutdownRaspi";
    private static final String REBOOT_RASPI = "rebootRaspi";
    private static final String REBOOT_SERVER = "rebootServer";
    private static final String GET_CONFIG = "getConfig";
    private static final String SET_CONFIG = "setConfig";
    private static final String GET_CONFIG_LIST = "getConfigList";
    private static final String GET_CONFIG_OPTIONS = "getConfigOptions";

    public String action;

    public String name;
    public String value;
    public Integer playlistId;
    public Integer trackId;
    public Integer[] idList;

    public String title;
    public String artist;
    public String fileName;

    public Integer position;
    public Integer length;
    public String repeatMode;

    public Integer red;
    public Integer green;
    public Integer blue;
    public Boolean smooth;

    public Boolean shuffle;
    public Integer volume;
    public Integer brightness;
    public Integer color;

    public String colorMode;
    public String visualisation;
    public String text;
}
