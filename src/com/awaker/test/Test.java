package com.awaker.test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class Test {
    public static void main(String[] args) {
        String command = "aäß@\n";

        System.out.println(Arrays.toString(command.getBytes(StandardCharsets.UTF_8)));
        System.out.println(String.format("%s hochgeladen (%s/%s)", 23 + "%", 300000, 400535));
        System.out.println(System.getProperty("user.home"));

        System.out.println("asfg".contains(""));
    }
}
