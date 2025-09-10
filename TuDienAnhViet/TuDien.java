package TuDienAnhViet;

import java.io.*;
import java.util.*;

/**
 * TuDien: nạp dữ liệu từ CSV (KhoTu.csv) 
 * Lưu trữ vào HashMap<String, Entry>.
 */
public class TuDien {
    // Inner class biểu diễn một bản ghi
    public static class Entry {
        public String tiengAnh;
        public String tiengViet;
        public String tuLoai;
        public String viDu;

        public Entry(String a, String v, String t, String vd) {
            this.tiengAnh = a;
            this.tiengViet = v;
            this.tuLoai = t;
            this.viDu = vd;
        }

        // Chuẩn hóa để gửi qua mạng (tránh chuỗi |||)
        public String serializeForNetwork() {
            return escape(tiengViet) + "|||" + escape(tuLoai) + "|||" + escape(viDu);
        }

        private String escape(String s) {
            if (s == null) return "";
            return s.replace("|||", "\\|||").replace("\n", "\\n").replace("\r", "");
        }
    }

    private final Map<String, Entry> map = new HashMap<>();

    public TuDien() {}

    // Chuẩn hóa key: trim + lowercase
    private static String normalizeKey(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    public Entry lookup(String word) {
        if (word == null) return null;
        return map.get(normalizeKey(word));
    }

    // Nạp dữ liệu từ CSV
    public void loadFromCSV(String csvPath) throws IOException {
        map.clear();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "UTF-8"))) {
            String header = br.readLine(); // bỏ qua header nếu có
            if (header == null) return;
            // kiểm tra header có phải dòng tiêu đề không
            boolean headerLooksLike = header.toLowerCase().contains("tienganh") || header.toLowerCase().contains("tiengviet");
            if (!headerLooksLike) {
                // dòng đầu tiên là dữ liệu thật → xử lý luôn
                processCsvLine(header);
            }
            String line;
            while ((line = br.readLine()) != null) {
                processCsvLine(line);
            }
        }
    }

    // Kỳ vọng CSV có 4 cột: TiengAnh,TiengViet,TuLoai,ViDu
    private void processCsvLine(String line) {
        List<String> cols = parseCsvLine(line);
        if (cols.size() < 2) return;
        String a = cols.size() > 0 ? cols.get(0) : "";
        String v = cols.size() > 1 ? cols.get(1) : "";
        String t = cols.size() > 2 ? cols.get(2) : "";
        String vd = cols.size() > 3 ? cols.get(3) : "";
        Entry e = new Entry(a, v, t, vd);
        map.put(normalizeKey(a), e);
    }

    // Parser CSV cơ bản: hỗ trợ field có dấu ngoặc kép
    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) return tokens;
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // bật/tắt inQuotes, xử lý dấu ngoặc kép kép
                if (inQuotes && i + 1 < line.length() && line.charAt(i+1) == '"') {
                    cur.append('"');
                    i++; // bỏ qua ký tự tiếp theo
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        tokens.add(cur.toString());
        // trim khoảng trắng
        for (int i = 0; i < tokens.size(); i++) {
            tokens.set(i, tokens.get(i).trim());
        }
        return tokens;
    }

    // Lấy số lượng bản ghi
    public int size() {
        return map.size();
    }

    // Thêm bản ghi trong runtime
    public void addEntry(Entry e) {
        if (e == null || e.tiengAnh == null) return;
        map.put(normalizeKey(e.tiengAnh), e);
    }
}
