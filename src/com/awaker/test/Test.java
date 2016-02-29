package com.awaker.test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        String command = "aäß@\n";

        System.out.println(Arrays.toString(command.getBytes(StandardCharsets.UTF_8)));
        System.out.println(System.getProperty("user.home"));

        File folder = new File("media/");
        File[] files = folder.listFiles(file -> file.isFile() && file.getPath().endsWith(".mp3"));

        System.out.println(files[0].getName());

        System.out.println(Arrays.toString(files));
    }
}
