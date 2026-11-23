package com.rahulshankar.dialysisapp.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds demo-dataset CSV text for tests. Uses real newline characters (never the two-character sequence backslash-n). */
final class DemoCsv {
    private DemoCsv() { }

    static final List<String> HEADERS = List.of(
            "demo_id", "food_name", "local_name_tamil", "category", "preparation", "basis",
            "energy_kcal", "protein_g", "carbohydrates_g", "fat_g", "fibre_g",
            "potassium_mg", "phosphorus_mg", "sodium_mg", "calcium_mg", "magnesium_mg",
            "iron_mg", "data_source", "source_record_id", "data_status", "citation", "license_note");

    /** "tamil" written with unicode escapes so the test does not depend on the compiler's source encoding. */
    static final String TAMIL = "தமிழ்";

    /** A valid record in which every column has a different, recognisable value. */
    static Map<String, String> defaults() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("demo_id", "D1");
        m.put("food_name", "Test Food");
        m.put("local_name_tamil", TAMIL);
        m.put("category", "Demo");
        m.put("preparation", "Boiled");
        m.put("basis", "per 100g");
        m.put("energy_kcal", "10");
        m.put("protein_g", "1.5");
        m.put("carbohydrates_g", "2.5");
        m.put("fat_g", "3.5");
        m.put("fibre_g", "4.5");
        m.put("potassium_mg", "111");
        m.put("phosphorus_mg", "222");
        m.put("sodium_mg", "333");
        m.put("calcium_mg", "44");
        m.put("magnesium_mg", "55");
        m.put("iron_mg", "6.6");
        m.put("data_source", "Test Source");
        m.put("source_record_id", "SRC-1");
        m.put("data_status", "SYNTHETIC");
        m.put("citation", "Test citation");
        m.put("license_note", "Test licence");
        return m;
    }

    /** One CSV record built from {@link #defaults()} with the given column overrides ("column", "value", ...). */
    static String row(String... overrides) {
        Map<String, String> m = defaults();
        for (int i = 0; i < overrides.length; i += 2) {
            if (!m.containsKey(overrides[i])) throw new IllegalArgumentException("Unknown column " + overrides[i]);
            m.put(overrides[i], overrides[i + 1]);
        }
        List<String> fields = new ArrayList<>();
        for (String h : HEADERS) fields.add(quote(m.get(h)));
        return String.join(",", fields);
    }

    static String header() { return String.join(",", HEADERS); }

    /** Header plus the given records, LF-separated, with a trailing newline. */
    static String csv(String... records) {
        StringBuilder sb = new StringBuilder(header()).append('\n');
        for (String r : records) sb.append(r).append('\n');
        return sb.toString();
    }

    static String quote(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    static InputStream stream(String text) { return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)); }
}
