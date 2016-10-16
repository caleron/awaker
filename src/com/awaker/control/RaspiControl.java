package com.awaker.control;

import com.awaker.Awaker;
import com.awaker.global.*;
import com.awaker.server.json.Answer;
import com.awaker.server.json.JsonCommand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class RaspiControl implements CommandHandler {

    public static void init() {
        CommandRouter.registerHandler(ControlCommand.class, new RaspiControl());
    }

    @Override
    public Answer handleCommand(Command command, JsonCommand data) {
        if (!(command instanceof ControlCommand)) {
            throw new RuntimeException("Received Wrong Command");
        }

        ControlCommand cmd = (ControlCommand) command;

        //in jedem Fall wird der Server beendet
        EventRouter.raiseEvent(GlobalEvent.SHUTDOWN);

        switch (cmd) {
            case REBOOT_RASPI:
                reboot();
                break;
            case SHUTDOWN_SERVER:
                System.exit(0);
                break;
            case SHUTDOWN_RASPI:
                shutdown();
                break;
            case REBOOT_SERVER:
                restartApplication();
                break;
        }
        return null;
    }

    private static boolean reboot() {
        String operatingSystem = System.getProperty("os.name");

        return "Linux".equals(operatingSystem) && execCommand("reboot now");
    }

    /**
     * http://stackoverflow.com/a/25666/6655315
     */
    private static boolean shutdown() {
        String operatingSystem = System.getProperty("os.name");

        return "Linux".equals(operatingSystem) && execCommand("shutdown -h now");
    }

    private static boolean execCommand(String cmd) {
        try {
            Runtime.getRuntime().exec(cmd);
            System.exit(0);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Diese Methode dient, dazu, das Programm neu zu starten.
     * <p>
     * Quelle: http://stackoverflow.com/questions/4159802/how-can-i-restart-a-java-application
     */
    private static void restartApplication() {
        String operatingSystem = System.getProperty("os.name");

        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

        final File currentCodePath;
        try {
            currentCodePath = new File(Awaker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            return;
        }

        final ArrayList<String> command = new ArrayList<>();

        if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
            command.add("sudo");
        }

        //wenns keine JAR-Datei ist, vorerst abbrechen, in Zukunft für exe-Release dies auch dann funktionieren lassen
        if (currentCodePath.getName().endsWith(".jar")) {
            //Befehl bauen: java -jar application.jar
            command.add(javaBin);
            command.add("-jar");
            command.add(currentCodePath.getPath());
        } else if (currentCodePath.getName().endsWith(".exe")) {
            //codepath ist dann der Pfad der exe
            command.add(currentCodePath.toString());
        } else {
            //Programm liegt als .class vor (während Debugging höchstwahrscheinlich)
            command.add(javaBin);
            command.add("-cp");
            command.add(currentCodePath.getPath());
            command.add(Awaker.class.getName());
        }

        final ProcessBuilder builder = new ProcessBuilder(command);
        try {
            builder.start();
            System.exit(0);
        } catch (IOException ignored) {
        }
    }
}
