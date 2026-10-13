package com.volunteerintegration.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * RFC 4180 형식 CSV 리더 (외부 라이브러리 없이).
 * <ul>
 *   <li>UTF-8 BOM 제거</li>
 *   <li>큰따옴표로 감싼 값 안의 쉼표·줄바꿈·이스케이프("") 처리</li>
 *   <li>첫 줄을 헤더로 읽어 컬럼명으로 값을 꺼낼 수 있게 한다</li>
 * </ul>
 */
public final class CsvReader {

    private CsvReader() {
    }

    /** 한 행. 컬럼명으로 값을 꺼내며 빈 문자열은 null로 돌려준다. */
    public static final class Row {
        private final Map<String, Integer> index;
        private final List<String> values;
        private final long lineNo;

        Row(Map<String, Integer> index, List<String> values, long lineNo) {
            this.index = index;
            this.values = values;
            this.lineNo = lineNo;
        }

        public String get(String column) {
            Integer i = index.get(column);
            if (i == null) {
                throw new IllegalArgumentException("CSV에 없는 컬럼: " + column);
            }
            if (i >= values.size()) {
                return null;
            }
            String v = values.get(i).trim();
            return v.isEmpty() ? null : v;
        }

        public long lineNo() {
            return lineNo;
        }
    }

    /** 파일을 읽으며 행마다 consumer를 호출한다. 읽은 행 수를 돌려준다. */
    public static long forEach(Path file, Consumer<Row> consumer) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return forEach(reader, consumer);
        }
    }

    public static long forEach(Reader reader, Consumer<Row> consumer) throws IOException {
        Parser parser = new Parser(reader);
        List<String> header = parser.next();
        if (header == null) {
            return 0;
        }
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            index.put(header.get(i).replace("﻿", "").trim(), i);
        }
        long count = 0;
        List<String> values;
        while ((values = parser.next()) != null) {
            if (values.size() == 1 && values.get(0).isEmpty()) {
                continue; // 빈 줄
            }
            consumer.accept(new Row(index, values, parser.line));
            count++;
        }
        return count;
    }

    /** 문자 단위 상태 기계. */
    private static final class Parser {
        private final Reader in;
        private long line = 0;
        private int pushback = -2;

        Parser(Reader in) {
            this.in = in;
        }

        private int read() throws IOException {
            if (pushback != -2) {
                int c = pushback;
                pushback = -2;
                return c;
            }
            return in.read();
        }

        List<String> next() throws IOException {
            int c = read();
            if (c == -1) {
                return null;
            }
            line++;
            List<String> fields = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            boolean quoted = false;
            while (true) {
                if (quoted) {
                    if (c == -1) {
                        fields.add(sb.toString());
                        return fields; // 닫히지 않은 따옴표: 있는 만큼 돌려준다
                    }
                    if (c == '"') {
                        int n = read();
                        if (n == '"') {
                            sb.append('"');
                        } else {
                            quoted = false;
                            pushback = n;
                        }
                    } else {
                        if (c == '\n') {
                            line++;
                        }
                        sb.append((char) c);
                    }
                } else {
                    if (c == -1 || c == '\n') {
                        fields.add(stripCr(sb));
                        return fields;
                    }
                    if (c == ',') {
                        fields.add(stripCr(sb));
                        sb.setLength(0);
                    } else if (c == '"' && sb.length() == 0) {
                        quoted = true;
                    } else {
                        sb.append((char) c);
                    }
                }
                c = read();
            }
        }

        private static String stripCr(StringBuilder sb) {
            int len = sb.length();
            if (len > 0 && sb.charAt(len - 1) == '\r') {
                return sb.substring(0, len - 1);
            }
            return sb.toString();
        }
    }
}
