package TuDienAnhViet;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MongoDBConnector {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "TuDienAnhViet";
    private static final String COLLECTION_NAME = "KhoTu";
    private static final String HISTORY_COLLECTION_NAME = "LichSuDich";

    private static MongoDBConnector instance;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private MongoCollection<Document> historyCollection;

    // 1. Chuyển sang private constructor để dùng Singleton
    private MongoDBConnector() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DATABASE_NAME);
        collection = database.getCollection(COLLECTION_NAME);
        historyCollection = database.getCollection(HISTORY_COLLECTION_NAME);
    }

    // 2. Phương thức getInstance (Singleton)
    public static synchronized MongoDBConnector getInstance() {
        if (instance == null) {
            instance = new MongoDBConnector();
        }
        return instance;
    }

    // 3. Tối ưu lookup dùng 1 hàm private và $regex
    private Document lookup(String field, String word) {
        if (word == null) return null;
        // Dùng $regex với cờ 'i' (không phân biệt hoa thường) và neo (^) ($)
        // để tìm chính xác từ, không phải tìm tiền tố.
        Document query = new Document(field,
                new Document("$regex", "^" + Pattern.quote(word.trim()) + "$")
                        .append("$options", "i"));
        return collection.find(query).first();
    }

    public Document lookupEn(String word) {
        return lookup("TiengAnh", word);
    }

    public Document lookupVn(String word) {
        return lookup("TiengViet", word);
    }

    public List<String> getSuggestions(boolean en2vi, String prefix, int maxSuggestions) {
        String field = en2vi ? "TiengAnh" : "TiengViet";
        List<String> suggestions = new ArrayList<>();
        
        Document regex = new Document(field,
                new Document("$regex", "^" + Pattern.quote(prefix.trim()))
                        .append("$options", "i"));
        
        FindIterable<Document> results = collection.find(regex).limit(maxSuggestions);
        for (Document doc : results) {
            suggestions.add(doc.getString(field));
        }
        return suggestions;
    }

    public List<Document> getEntries() {
        List<Document> entries = new ArrayList<>();
        // Sắp xếp theo TiengAnh A-Z
        FindIterable<Document> iterable = collection.find().sort(Sorts.ascending("TiengAnh"));
        for (Document doc : iterable) {
            entries.add(doc);
        }
        return entries;
    }

    // 4. Thêm hàm đếm số lượng từ (hiệu quả hơn)
    public long getEntryCount() {
        return collection.countDocuments();
    }

    public void addEntry(String tiengAnh, String tiengViet, String tuLoai, String viDu, String phienAm) {
        Document doc = new Document()
                .append("TiengAnh", tiengAnh.trim())
                .append("TiengViet", tiengViet.trim())
                .append("TuLoai", tuLoai)
                .append("ViDu", viDu)
                .append("PhienAm", phienAm);
        collection.insertOne(doc);
    }

    public void updateEntry(String oldTiengAnh, String tiengAnh, String tiengViet, String tuLoai, String viDu, String phienAm) {
        Document query = new Document("TiengAnh", oldTiengAnh);
        Document update = new Document("$set", new Document()
                .append("TiengAnh", tiengAnh)
                .append("TiengViet", tiengViet)
                .append("TuLoai", tuLoai)
                .append("ViDu", viDu)
                .append("PhienAm", phienAm));
        collection.updateOne(query, update);
    }

    public void deleteEntry(String tiengAnh) {
        Document query = new Document("TiengAnh", tiengAnh);
        collection.deleteOne(query);
    }

    public void saveHistory(String direction, String english, String vietnamese) {
        Document doc = new Document()
                .append("direction", direction)
                .append("english", english)
                .append("vietnamese", vietnamese)
                .append("timestamp", new Date());
        historyCollection.insertOne(doc);
    }

    public List<Document> getHistory() {
        List<Document> history = new ArrayList<>();
        FindIterable<Document> iterable = historyCollection.find().sort(new Document("timestamp", -1));
        for (Document doc : iterable) {
            history.add(doc);
        }
        return history;
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            instance = null; // Reset instance khi close
        }
    }
}