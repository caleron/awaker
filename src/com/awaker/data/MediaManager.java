package com.awaker.data;

import com.awaker.audio.PlayList;
import com.awaker.server.json.Playlist;
import com.awaker.util.Log;
import com.mpatric.mp3agic.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Verwendet Mp3agic https://github.com/mpatric/mp3agic
 */
public class MediaManager {

    private static Random rand = new Random();
    private static ArrayList<TrackWrapper> allTracks;
    private static ArrayList<PlayList> playLists;

    private static List<MediaEventListener> listeners = new ArrayList<>();

    public static void addListener(MediaEventListener listener) {
        listeners.add(listener);
    }

    private static void raiseMediaReadyEvent() {
        for (MediaEventListener listener : listeners) {
            listener.mediaReady();
        }
    }

    public static void init() {
        new Thread(MediaManager::scanFiles).start();
    }

    /**
     * Gibt den InputStream zu einem Track zurück
     *
     * @param track Der Track
     * @return Der InputStream
     */
    public static FileInputStream getFileStream(TrackWrapper track) {
        if (track == null)
            return null;

        try {
            if (track.filePath != null && track.filePath.length() > 0) {
                return new FileInputStream(track.filePath);
            }

            TrackWrapper newTrack = DbManager.getTrack(track.title, track.artist);
            if (newTrack != null) {
                return new FileInputStream(newTrack.filePath);
            }
        } catch (FileNotFoundException e) {
            Log.error(e);
        }
        return null;
    }


    private static void loadTracks() {
        allTracks = DbManager.getAllTracks();
        if (allTracks != null) {
            Log.message("Mediathek enthält " + allTracks.size() + " Tracks");
        } else {
            Log.message("allTracks ist null");
        }
    }

    private static void loadData() {
        loadTracks();
        loadPlaylists();
        raiseMediaReadyEvent();
    }

    private static void loadPlaylists() {
        playLists = DbManager.getAllPlaylists(allTracks);
    }

    /**
     * Führt den Dateiscan im Ordner media durch
     */
    private static void scanFiles() {
        Log.message("Dateiscan gestartet");

        File folder = new File("media/");
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                Log.message("Cant create media folder");
                return;
            }
        }

        //Pfade der dateien sind relativ, also beginnen mit media/
        File[] files = folder.listFiles(file -> file.isFile() && file.getPath().endsWith(".mp3"));

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        ArrayList<TrackWrapper> tracksOfDb = DbManager.getAllTracks();

        //Schnittmenge der Listen entfernen
        if (tracksOfDb != null && fileList.size() > 0) {
            for (int i = 0; i < fileList.size(); i++) {
                String path = fileList.get(i).getPath();

                boolean trackExistingInDb = false;

                for (int j = 0; j < tracksOfDb.size(); j++) {
                    TrackWrapper track = tracksOfDb.get(j);
                    if (track.filePath.equals(path)) {
                        tracksOfDb.remove(j);
                        trackExistingInDb = true;
                        break;
                    }
                }

                if (trackExistingInDb) {
                    fileList.remove(i);
                    i--;
                }
            }
        }

        //überzählige Dateien einlesen und in Datenbank schreiben
        ArrayList<TrackWrapper> newTracks = new ArrayList<>();

        for (File file : fileList) {
            TrackWrapper track = readFile(file);
            if (file != null) {
                newTracks.add(track);
            }
        }
        DbManager.addTracks(newTracks);

        //Überzählige DB-Einträge löschen
        if (tracksOfDb != null) {
            tracksOfDb.forEach(DbManager::removeTrack);
        }
        Log.message("Scan abgeschlossen");

        loadData();
    }

    /**
     * Lädt einen Track in eine Datei und integriert diesen in die Datenbank.
     *
     * @param is       Der InputStream
     * @param length   Die Dateilänge
     * @param fileName Der Dateiname
     * @return TrackWrapper zum Track
     */
    public static TrackWrapper downloadFile(InputStream is, int length, String fileName) {
        fileName = "media/" + fileName;

        final int BUFFER_SIZE = 8192;
        byte[] buffer = new byte[BUFFER_SIZE];

        File newFile = new File(fileName);

        //sicherstellen, dass Datei noch nicht existiert
        while (newFile.exists()) {
            fileName = fileName.replace(".mp3", "") + rand.nextInt() + ".mp3";
            newFile = new File(fileName);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(newFile);
        } catch (FileNotFoundException ignored) {
        }
        try {
            if (fos == null) {
                fileName = "media/" + rand.nextInt() + ".mp3";
                fos = new FileOutputStream(fileName);
            }
            Log.message("download begin");
            //Anzahl gelesener Bytes beim letzten Aufruf von read()
            int readCount = is.read(buffer);
            //Insgesamt gelesene Bytes
            int totalBytesRead = readCount;
            while (readCount > 0) {
                //Gelesene Bytes schreiben
                fos.write(buffer, 0, readCount);

                if (length - totalBytesRead < BUFFER_SIZE) {
                    //Nur so viele Bytes lesen wie nötig
                    readCount = is.read(buffer, 0, length - totalBytesRead);
                } else {
                    readCount = is.read(buffer);
                }
                totalBytesRead += readCount;
            }
            //readCount ist beim letzten Durchlauf -1, zum Ausgleich wieder um 1 erhöhen
            totalBytesRead++;
            //fertig
            fos.close();

            if (totalBytesRead < length) {
                Log.message("Lengths do not match, cancelling integration in Database");
                if (newFile.delete()) {
                    Log.message("File deleted");
                }
                return null;
            }

            Log.message("download finished");

            //Tags lesen und in Datenbank packen
            TrackWrapper track = readFile(newFile);

            if (track == null) {
                Log.message("Error reading Tags, integration cancelled.");
                if (newFile.delete()) {
                    Log.message("File deleted");
                }
                return null;
            }

            //Prüfen, ob Track bereits in Datenbank
            TrackWrapper existingTrack = DbManager.getTrack(track.title, track.artist);

            if (existingTrack != null) {
                Log.message("Track already existing, integration cancelled, returning existing Track");
                if (newFile.delete()) {
                    Log.message("File deleted");
                }
            } else {
                DbManager.addTrack(track);
                loadTracks();
            }

            //Neu aus Datenbank laden, damit ID und Referenzen richtig sind
            track = DbManager.getTrack(track.title, track.artist);

            return track;
        } catch (IOException e) {
            Log.error(e);
        }
        return null;
    }

    /**
     * Liest die Tracks einer mp3-Datei aus
     *
     * @param file Die auszulesende mp3-Datei
     * @return TrackWrapper mit den ausgelesenen Tags
     */
    private static TrackWrapper readFile(File file) {
        try {
            Mp3File mp3File = new Mp3File(file);

            String title = null, artist = null, album = null;
            int lengthInSeconds = (int) mp3File.getLengthInSeconds();

            if (mp3File.hasId3v2Tag()) {
                ID3v2 tag = mp3File.getId3v2Tag();
                title = tag.getTitle();
                artist = tag.getArtist();
                album = tag.getAlbum();
            } else if (mp3File.hasId3v1Tag()) {
                ID3v1 tag = mp3File.getId3v1Tag();
                title = tag.getTitle();
                artist = tag.getArtist();
                album = tag.getAlbum();
            } else {
                Log.message("Track " + file.getPath() + " hat keine Tags");
            }

            if (title == null || title.length() == 0) {
                //etwa wenn keine Tags, dann den Dateinamen ohne Erweiterung als Titel nehmen
                title = file.getName().replace(".mp3", "");
            }

            if (artist == null)
                artist = "";

            if (album == null)
                album = "";

            return new TrackWrapper(-1, title, artist, album, file.getPath(), lengthInSeconds);

        } catch (IOException | UnsupportedTagException | InvalidDataException e) {
            Log.error(e);
        }
        return null;
    }

    /**
     * Erstellt eine neue Playlist und lädt danach alle Playlists neu.
     *
     * @param name Der Name der Playlist.
     */
    public static void createPlaylist(String name) {
        DbManager.createPlaylist(name);
        loadPlaylists();
    }


    /**
     * Löscht eine Playlist und lädt danach alle Playlists neu.
     *
     * @param playListId Die ID der zu löschenden Playlist.
     */
    public static void removePlaylist(int playListId) {

        DbManager.removePlaylist(playListId);
        loadPlaylists();
    }

    /**
     * Fügt einen Track zu einer Playlist hinzu.
     *
     * @param playListId Die ID der Playlist
     * @param list       Liste von Track-IDs
     */
    public static void addTracksToPlaylist(int playListId, Integer[] list) {
        PlayList playList = getPlayList(playListId);
        if (playList == null)
            return;

        for (Integer trackId : list) {
            TrackWrapper track = getTrack(trackId);

            if (track == null)
                continue;

            DbManager.addTrackToPlaylist(playList, track);
            playList.addTrack(track);
        }
    }


    /**
     * Entfernt einen Track von einer Playlist.
     *
     * @param playListId Die ID der Playlist
     * @param list       Liste von Track-IDs
     */
    public static void removeTracksFromPlaylist(int playListId, Integer[] list) {
        PlayList playList = getPlayList(playListId);
        if (playList == null)
            return;

        for (Integer trackId : list) {
            TrackWrapper track = getTrack(trackId);

            if (track == null)
                continue;

            DbManager.removeTrackFromPlaylist(playList, track);
            playList.removeTrack(track);
        }
    }

    /**
     * Sucht eine Playlist mit der angegebenen ID raus.
     *
     * @param playListId Die ID der Playlist.
     * @return Die Playlist mit der ID
     */
    public static PlayList getPlayList(int playListId) {
        PlayList playList = null;
        //Playlist raussuchen
        for (PlayList list : playLists) {
            if (list.getId() == playListId) {
                playList = list;
                break;
            }
        }
        return playList;
    }

    /**
     * Sucht einen Track mit der angegebenen ID raus.
     *
     * @param trackId Die ID des Tracks
     * @return Der Track mit der ID
     */
    public static TrackWrapper getTrack(int trackId) {
        TrackWrapper track = null;

        //Track raussuchen
        for (TrackWrapper track1 : allTracks) {
            if (track1.getId() == trackId) {
                track = track1;
                break;
            }
        }
        return track;
    }

    public static ArrayList<TrackWrapper> getAllTracks() {
        return allTracks;
    }

    public static ArrayList<Playlist> getPlayListsForJson() {
        ArrayList<Playlist> list = new ArrayList<>();
        for (PlayList playList : playLists) {
            list.add(playList.toJSONPlaylist());
        }
        return list;
    }
}
