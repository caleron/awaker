package com.awaker.util;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Übernimmt das Loggen in Datei und Konsole. Wird nur über statische Methoden aufgerufen.
 */
public class Log {
    private static PrintWriter writer;
    private static final String format = "%1s (%2s): %3s%n";

    /**
     * Initialisieren des Loggers
     */
    static {
        Date now = new Date();
        DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss");

        try {
            writer = new PrintWriter("logs/log " + format.format(now) + ".log");
        } catch (FileNotFoundException e) {
            System.err.println("Can't open error file");
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

        log("error", sw.toString());
    }

    /**
     * Trägt einen Fehler als Warnung in den Log ein.
     *
     * @param err Die Fehlermeldung
     */
    public static void error(String err) {
        System.out.println(err);
        log("error", err);
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
        writer.print(out);
        writer.flush();
    }
}
