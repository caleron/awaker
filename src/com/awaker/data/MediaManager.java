package com.awaker.data;

import com.mpatric.mp3agic.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MediaManager {

    public MediaManager() {

    }

    public static FileInputStream getFileStream(TrackWrapper track) {
        try {
            if (track.filePath != null && track.filePath.length() > 0) {
                return new FileInputStream(track.filePath);
            }

            TrackWrapper newTrack = DbManager.getTrack(track.title, track.artist);
            if (newTrack != null) {
                return new FileInputStream(newTrack.filePath);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TrackWrapper downloadFile(InputStream is, int length, String fileName) {
        fileName = "media/" + fileName;

        final int BUFFER_SIZE = 8192;
        byte[] buffer = new byte[BUFFER_SIZE];

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
        } catch (FileNotFoundException ignored) {
        }
        try {
            if (fos == null) {
                Random rand = new Random();
                fileName = "media/" + rand.nextInt() + ".mp3";
                fos = new FileOutputStream(fileName);
            }
            System.out.println("download begin");
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
            //fertig
            fos.close();

            if (totalBytesRead < length) {
                System.out.println("Lengths do not match, cancelling integration in Database");
                File deleteFile = new File(fileName);
                if (deleteFile.delete()) {
                    System.out.println("File deleted");
                }
                return null;
            }

            System.out.println("download finished");

            //Tags lesen und in Datenbank packen
            TrackWrapper track = readFile(new File(fileName));
            DbManager.addTrack(track);

            return track;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void startScanFiles() {
        new Thread(MediaManager::scanFiles).start();
    }

    private static void scanFiles() {
        System.out.println("Dateiscan gestartet");

        File folder = new File("media/");
        //Pfade der dateien sind relativ, also beginnen mit media/
        File[] files = folder.listFiles(file -> file.isFile() && file.getPath().endsWith(".mp3"));

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        ArrayList<TrackWrapper> tracksOfDb = DbManager.getMusic();

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
        System.out.println("Scan abgeschlossen");
    }

    private static TrackWrapper readFile(File file) {
        try {
            Mp3File mp3File = new Mp3File(file);

            String title = null, artist = null, album = null;

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
                //Wenn keine/falsche Tags, überspringen
                System.out.println("Track " + file.getPath() + " beim Scan übersprungen");
            }

            if (title == null || title.length() == 0) {
                //etwa wenn keine Tags, dann den Dateinamen ohne Erweiterung als Titel nehmen
                title = file.getName().replace(".mp3", "");
            }

            if (artist == null)
                artist = "";

            if (album == null)
                artist = "";

            return new TrackWrapper(-1, title, artist, album, file.getPath());

        } catch (IOException | UnsupportedTagException | InvalidDataException e) {
            e.printStackTrace();
        }
        return null;
    }
}
