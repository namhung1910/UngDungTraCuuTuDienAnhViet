package TuDienAnhViet;

import java.util.*;
import org.bson.Document;

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

    private final MongoDBConnector connector;

    public TuDien() {
        connector = new MongoDBConnector();
    }

    public Entry lookupEn(String word) {
        if (word == null) return null;
        Document doc = connector.lookupEn(word);
        if (doc == null) return null;
        return documentToEntry(doc);
    }

    public Entry lookupVn(String word) {
        if (word == null) return null;
        Document doc = connector.lookupVn(word);
        if (doc == null) return null;
        return documentToEntry(doc);
    }

    public Entry lookup(String word) {
        return lookupEn(word);
    }

    private Entry documentToEntry(Document doc) {
        return new Entry(
            doc.getString("TiengAnh"),
            doc.getString("TiengViet"),
            doc.getString("TuLoai"),
            doc.getString("ViDu")
        );
    }

    public void addEntry(Entry e) {
        if (e == null) return;
        connector.addEntry(e.tiengAnh, e.tiengViet, e.tuLoai, e.viDu);
    }

    public boolean removeEntryByEnglish(String tiengAnh) {
        if (tiengAnh == null) return false;
        Entry entry = lookupEn(tiengAnh);
        if (entry != null) {
            connector.deleteEntry(tiengAnh);
            return true;
        }
        return false;
    }

    public void updateEntry(String oldEnglish, Entry newEntry) {
        connector.updateEntry(oldEnglish, newEntry.tiengAnh, newEntry.tiengViet, newEntry.tuLoai, newEntry.viDu);
    }

    public List<String> getSuggestions(boolean en2vi, String prefix, int maxSuggestions) {
        return connector.getSuggestions(en2vi, prefix, maxSuggestions);
    }

    public Collection<Entry> getAllEntries() {
        List<Entry> entries = new ArrayList<>();
        for (Document doc : connector.getEntries()) {
            entries.add(documentToEntry(doc));
        }
        return entries;
    }

    public int size() {
        return (int) connector.getEntries().size();
    }
}
