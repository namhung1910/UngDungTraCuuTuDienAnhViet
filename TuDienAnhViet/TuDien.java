package TuDienAnhViet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TuDien {
    public static class Entry {
        public String tiengAnh;
        public String tiengViet;
        public String tuLoai;
        public String viDu;

        public Entry() {}

        public Entry(String a, String v, String t, String ex) {
            this.tiengAnh = a;
            this.tiengViet = v;
            this.tuLoai = t;
            this.viDu = ex;
        }

        public String serializeForNetwork(boolean en2vi) {
            String first = en2vi ? tiengViet : tiengAnh;
            return escape(first) + "|||" + escape(nullToEmpty(tuLoai)) + "|||" + escape(nullToEmpty(viDu));
        }

        private String nullToEmpty(String s) {
            return s == null ? "" : s;
        }

        private String escape(String s) {
            if (s == null) return "";
            String r = s.replace("\\", "\\\\");
            r = r.replace("|||", "\\|||");
            r = r.replace("\n", "\\n");
            return r;
        }
    }

    private final Map<String, Entry> mapEn = new ConcurrentHashMap<>();
    private final Map<String, Entry> mapVn = new ConcurrentHashMap<>();

    public TuDien() {}

    public void loadFromCSV(String csvPath) throws IOException {
        Map<String, Entry> newEn = new ConcurrentHashMap<>();
        Map<String, Entry> newVn = new ConcurrentHashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLineMaybeHeader = true;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<String> cols = parseCsvLine(line);
                if (firstLineMaybeHeader) {
                    firstLineMaybeHeader = false;
                    if (cols.size() >= 1) {
                        String c0 = cols.get(0).toLowerCase();
                        if (c0.contains("tienganh") || c0.contains("tieng anh") || c0.contains("tiếng anh")) {
                            continue;
                        }
                    }
                }
                while (cols.size() < 4) cols.add("");
                String a = cols.get(0);
                String v = cols.get(1);
                String t = cols.get(2);
                String ex = cols.get(3);
                Entry e = new Entry(a, v, t, ex);
                if (a != null && !a.trim().isEmpty()) {
                    newEn.put(normalizeKey(a), e);
                }
                if (v != null && !v.trim().isEmpty()) {
                    newVn.put(normalizeKey(v), e);
                }
            }
        }

        mapEn.clear();
        mapEn.putAll(newEn);
        mapVn.clear();
        mapVn.putAll(newVn);
    }

    private List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }

    private String normalizeKey(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    public Entry lookupEn(String word) {
        if (word == null) return null;
        return mapEn.get(normalizeKey(word));
    }

    public Entry lookupVn(String word) {
        if (word == null) return null;
        return mapVn.get(normalizeKey(word));
    }

    public Entry lookup(String word) {
        return lookupEn(word);
    }

    public void addEntry(Entry e) {
        if (e == null) return;
        if (e.tiengAnh != null && !e.tiengAnh.trim().isEmpty())
            mapEn.put(normalizeKey(e.tiengAnh), e);
        if (e.tiengViet != null && !e.tiengViet.trim().isEmpty())
            mapVn.put(normalizeKey(e.tiengViet), e);
    }

    public int size() {
        return mapEn.size();
    }

    public List<String> getSuggestions(boolean en2vi, String prefix, int maxSuggestions) {
        Map<String, Entry> map = en2vi ? mapEn : mapVn;
        String normalizedPrefix = normalizeKey(prefix);
        List<String> suggestions = new ArrayList<>();
        
        for (String key : map.keySet()) {
            if (key.startsWith(normalizedPrefix)) {
                Entry entry = map.get(key);
                String word = en2vi ? entry.tiengAnh : entry.tiengViet;
                if (!suggestions.contains(word)) {
                    suggestions.add(word);
                    if (suggestions.size() >= maxSuggestions) {
                        break;
                    }
                }
            }
        }
        
        return suggestions;
    }
}