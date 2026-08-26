package com.behsazan.corebanking.calendar2.datasetimport.application;

import java.util.ArrayList;
import java.util.List;

public final class Calendar2Csv {
    private Calendar2Csv() {}

    public static List<String> split(String line) {
        ArrayList<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (quoted) throw new IllegalArgumentException("ساختار CSV معتبر نیست؛ علامت نقل‌قول بسته نشده است.");
        values.add(current.toString());
        return values;
    }

    public static void requireHeader(String line, List<String> expected, String fileName) {
        if (line == null) throw new IllegalArgumentException("فایل " + fileName + " فاقد Header است.");
        List<String> actual = split(line);
        if (!actual.isEmpty() && !actual.getFirst().isEmpty() && actual.getFirst().charAt(0) == '\ufeff') {
            actual.set(0, actual.getFirst().substring(1));
        }
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("Header فایل " + fileName + " با مدل CAL2 سازگار نیست. Header دریافت‌شده: "
                    + String.join(",", actual));
        }
    }
}
