package com.app.aml.UX;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class ReferenceGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static String generate(String prefix) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
        StringBuilder randomPart = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            randomPart.append(ALPHABET.charAt(ThreadLocalRandom.current().nextInt(ALPHABET.length())));
        }
        return String.format("%s-%s-%s", prefix, datePart, randomPart);
    }
}