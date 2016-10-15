package com.awaker.audio;

import com.awaker.config.Config;
import com.awaker.config.ConfigChangeListener;
import com.awaker.config.ConfigKey;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.server.json.Playlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Einzelinstanz-Klasse zur Repräsentation der Warteschlange
 */
class TrackQueue implements ConfigChangeListener {
    //Sobald die Trackliste verändert wird, soll der Verweis auf null gesetzt werden
    private PlayList basePlaylist;

    private ArrayList<TrackWrapper> tracks;
    private ArrayList<TrackWrapper> shuffledTracks;
    private int currentTrackIndex = 0;
    private boolean usingShuffledList = false;

    private static TrackQueue instance;

    /**
     * Gibt die Instanz zurück. Erstellt diese, falls noch nicht vorhanden.
     *
     * @return Die Instanz der TrackQueue.
     */
    public static TrackQueue getInstance() {
        if (instance == null)
            instance = new TrackQueue();

        return instance;
    }

    /**
     * Privater Konstruktor, da Einzelinstanz-Klasse
     */
    private TrackQueue() {
        Config.addSyncListener(this, new ConfigKey[]{ConfigKey.REPEAT_MODE, ConfigKey.SHUFFLE});
        setPlaylist(-1);
    }

    /**
     * Setzt die Trackliste. Setzt dabei den aktuellen Track auf Afang zurück und mischt die Liste, wenn nötig.
     *
     * @param list Die Trackliste.
     */
    void setTrackList(List<TrackWrapper> list) {
        currentTrackIndex = 0;
        tracks = new ArrayList<>(list);
        //Mischen, wenn nötig
        if (Config.getBool(ConfigKey.SHUFFLE)) {
            //noinspection unchecked
            shuffledTracks = new ArrayList<>();
            shuffledTracks.addAll(tracks);
            Collections.shuffle(shuffledTracks);
            usingShuffledList = true;
        } else {
            usingShuffledList = false;
        }
    }

    /**
     * Setzt die Playlist anhand dessen ID. Falls die ID -1 ist, werden alle Tracks verwendet.
     *
     * @param id Die ID der Playlist oder -1 für alle Tracks.
     */
    void setPlaylist(int id) {
        if (id == -1) {
            basePlaylist = null;
            setTrackList(MediaManager.getAllTracks());
        } else {
            PlayList playlist = MediaManager.getPlayList(id);
            basePlaylist = playlist;
            setTrackList(playlist.getTracks());
        }
    }

    /**
     * Setzt den aktuellen Track anhand dessen ID. Falls die ID nciht existiert, geschieht nichts.
     *
     * @param trackId Die ID des Tracks.
     */
    void setCurrentTrack(int trackId) {
        ArrayList<TrackWrapper> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            TrackWrapper track = currentList.get(i);
            if (track.getId() == trackId)
                currentTrackIndex = i;
        }
    }

    /**
     * Wählt den nächsten Track aus.
     *
     * @return Der neue aktuelle Track.
     */
    TrackWrapper nextTrack() {
        //Falls der Track wiederholt werden soll, nix machen
        if (!Config.getString(ConfigKey.REPEAT_MODE).equals("track")) {
            if (currentTrackIndex < tracks.size() - 1) {
                currentTrackIndex++;
            } else if (Config.getString(ConfigKey.REPEAT_MODE).equals("all")) {
                //falls der Index der letzte in der Liste ist und der Modus "all" ist, von vorne beginnnen
                currentTrackIndex = 0;
            } else {
                /* Falls nicht wiederholt werden soll und der Index über das Ende hinausgehen würde, genau das tun,
                 * damit currentTrack null zurückgibt.
                 */
                currentTrackIndex++;
            }
        }

        return currentTrack();
    }

    /**
     * Wählt den vorigen Track aus. Falls der aktuelle Track bereits der erste in der Liste ist, geschieht nichts.
     *
     * @return Der neue aktuelle Track.
     */
    TrackWrapper previousTrack() {
        //Wenn nur der Track wiederholt werden soll, nix ändern
        if (!Config.getString(ConfigKey.REPEAT_MODE).equals("track")) {
            //sonst um 1 reduzieren, wenn möglich
            if (currentTrackIndex > 0) {
                currentTrackIndex--;
            }
        }

        return currentTrack();
    }

    /**
     * Gibt den aktuellen Track zurück.
     *
     * @return Der aktuelle Track.
     */
    TrackWrapper currentTrack() {
        if (currentTrackIndex >= 0 && tracks.size() > currentTrackIndex) {
            if (usingShuffledList) {
                return shuffledTracks.get(currentTrackIndex);
            }
            return tracks.get(currentTrackIndex);
        }
        return null;
    }

    /**
     * Fügt der Warteschlange einen Track am Ende hinzu.
     *
     * @param id Die Id des Tracks.
     * @return Der Track.
     */
    TrackWrapper addToQueue(int id) {
        TrackWrapper track = MediaManager.getTrack(id);
        basePlaylist = null;
        tracks.add(track);
        shuffledTracks.add(track);
        return track;
    }

    /**
     * Entfernt einen Track aus der Warteschlange.
     *
     * @param id Die Id des Tracks.
     */
    void removeFromQueue(int id) {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId() == id) {
                tracks.remove(i);
                break;
            }
        }

        for (int i = 0; i < shuffledTracks.size(); i++) {
            if (shuffledTracks.get(i).getId() == id) {
                shuffledTracks.remove(i);
                break;
            }
        }
    }

    /**
     * Fügt den Track zur ID als nächstes in die Warteschlange ein.
     *
     * @param id Die ID des Tracks.
     * @return Der eingefügte Track.
     */
    TrackWrapper playAsNext(int id) {
        TrackWrapper track = MediaManager.getTrack(id);
        basePlaylist = null;
        tracks.add(currentTrackIndex + 1, track);
        shuffledTracks.add(currentTrackIndex + 1, track);
        return track;
    }

    /**
     * Wird bei Konfigurationsänderungen ausgelöst.
     *
     * @param key Der Schlüssel, von dem sich der Wert geändert hat.
     */
    @Override
    public void configChanged(ConfigKey key) {
        if (key.equals(ConfigKey.SHUFFLE)) {
            if (!usingShuffledList && Config.getBool(key)) {
                /*
                Wechseln auf gemischte Liste
                Der Teil der Liste unterhalb des aktuellen Tracks wird dabei nur gemischt, der übrige Teil inklusive
                dem aktuellen Track bleibt unberührt.  */
                List<TrackWrapper> unshuffledPart = new ArrayList<>(tracks.subList(0, currentTrackIndex + 1));
                List<TrackWrapper> shuffledPart = new ArrayList<>(tracks.subList(currentTrackIndex + 1, tracks.size()));

                Collections.shuffle(shuffledPart);

                ArrayList<TrackWrapper> newShuffledList = new ArrayList<>(unshuffledPart);
                newShuffledList.addAll(shuffledPart);

                shuffledTracks = newShuffledList;
                usingShuffledList = true;
            } else if (usingShuffledList && !Config.getBool(key)) {
                /* Wechseln auf normale Liste
                Dabei wird der aktuelle Titel in der normalen Liste gesucht und der currentTrackIndex darauf gesetzt. */
                TrackWrapper currentTrack = shuffledTracks.get(currentTrackIndex);

                for (int i = 0; i < tracks.size(); i++) {
                    TrackWrapper track = tracks.get(i);
                    if (track.equals(currentTrack)) {
                        currentTrackIndex = i;
                        break;
                    }
                }
                usingShuffledList = false;
            }
        }
    }

    /**
     * Gibt die aktuell zu verwendende Liste zurück.
     *
     * @return shuffledTracks oder tracks.
     */
    private ArrayList<TrackWrapper> getCurrentList() {
        return usingShuffledList ? shuffledTracks : tracks;
    }

    /**
     * Gibt die ID der Playlist zurück, welcher der Warteschlange als Basis dient.
     *
     * @return ID der Playlist oder -1, falls es keine Playlist ist.
     */
    private int basePlaylistId() {
        if (basePlaylist != null) {
            return basePlaylist.getId();
        }
        return -1;
    }

    /**
     * Gibt ein Objekt zur JSON-Repräsentation dieser Warteschlange als Playlist zurück.
     *
     * @return Die Playlist.
     */
    Playlist toJSONPlaylist() {
        ArrayList<Integer> idList = new ArrayList<>();
        ArrayList<TrackWrapper> currentList = getCurrentList();

        if (currentList != null) {
            for (TrackWrapper track : currentList) {
                idList.add(track.getId());
            }
        }
        return new Playlist(basePlaylistId(), "", idList);
    }
}
