package com.awaker.util;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Übernimmt das Loggen in Datei und Konsole. Wird nur über statische Methoden verwendet.
 */
public class Log {
    //PrintWriter überschreibt alte Datei
    private static PrintWriter logWriter;
    //FileWriter hier verwendet, da neue Fehler nur angehängt werden sollen
    private static FileWriter errorWriter;
    private static final String format = "%1s (%2s): %3s%n";
    private static final String ERROR_FILE = "logs/errors.log";


    /*
      Initialisieren des Loggers
     */
    static {
        Date now = new Date();
        DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss");

        //check if logs dir exists, create it otherwise
        File logDir = new File("logs");
        if (!logDir.exists() || !logDir.isDirectory()) {
            if (!logDir.mkdir()) {
                System.out.println("could not create logs directory");
            } else {
                System.out.println("created logs directory");
            }
        }

        try {
            logWriter = new PrintWriter("logs/log " + format.format(now) + ".log");
            errorWriter = new FileWriter(ERROR_FILE, true);
        } catch (IOException e) {
            System.err.println("Can't open log/error files");
            e.printStackTrace();
        }
    }

    /**
     * Schreibt eine Exception in den Log
     *
     * @param e die Exception
     */
    public static void error(Throwable e) {
        e.printStackTrace();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        String stacktrace = sw.toString();
        log("error", stacktrace);
        writeError(stacktrace);
    }

    /**
     * Trägt einen Fehler als Warnung in den Log ein.
     *
     * @param err Die Fehlermeldung
     */
    public static void error(String err) {
        System.out.println(err);
        log("error", err);
        writeError(err);
    }

    /**
     * Schreibt einen String in die Fehlerdatei.
     *
     * @param err Der zu schreibende String
     */
    private static void writeError(String err) {
        try {
            String out = String.format(format, new Date(), "error", err);
            errorWriter.write(out);
            errorWriter.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Trägt eine Nachricht als Info in den Log ein.
     *
     * @param msg Die Nachricht
     */
    public static void message(String msg) {
        System.out.println(msg);
        log("info", msg);
    }

    private static void log(String level, String msg) {
        Date date = new Date();

        String out = String.format(format, date, level, msg);
        logWriter.print(out);
        logWriter.flush();
    }
}
