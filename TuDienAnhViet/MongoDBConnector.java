package TuDienAnhViet;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MongoDBConnector {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "TuDienAnhViet";
    private static final String COLLECTION_NAME = "KhoTu";
    private static final String HISTORY_COLLECTION_NAME = "LichSuDich";

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private MongoCollection<Document> historyCollection;

    public MongoDBConnector() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DATABASE_NAME);
        collection = database.getCollection(COLLECTION_NAME);
        historyCollection = database.getCollection(HISTORY_COLLECTION_NAME);
    }

    public Document lookupEn(String word) {
        if (word == null) return null;
        // Tìm kiếm không phân biệt hoa thường nhưng vẫn giữ nguyên giá trị gốc trong DB
        Document query = new Document("$or", Arrays.asList(
            new Document("TiengAnh", word.trim()),
            new Document("TiengAnh", word.trim().toLowerCase()),
            new Document("TiengAnh", word.trim().toUpperCase())
        ));
        return collection.find(query).first();
    }

    public Document lookupVn(String word) {
        if (word == null) return null;
        // Tìm kiếm không phân biệt hoa thường nhưng vẫn giữ nguyên giá trị gốc trong DB
        Document query = new Document("$or", Arrays.asList(
            new Document("TiengViet", word.trim()),
            new Document("TiengViet", word.trim().toLowerCase()),
            new Document("TiengViet", word.trim().toUpperCase())
        ));
        return collection.find(query).first();
    }

    public List<String> getSuggestions(boolean en2vi, String prefix, int maxSuggestions) {
        String field = en2vi ? "TiengAnh" : "TiengViet";
        List<String> suggestions = new ArrayList<>();
        
        // Sử dụng $regex với cờ 'i' để tìm kiếm không phân biệt hoa thường
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
        FindIterable<Document> iterable = collection.find();
        for (Document doc : iterable) {
            entries.add(doc);
        }
        return entries;
    }

    public void addEntry(String tiengAnh, String tiengViet, String tuLoai, String viDu) {
        // Giữ nguyên định dạng hoa/thường của người dùng
        Document doc = new Document()
                .append("TiengAnh", tiengAnh.trim())
                .append("TiengViet", tiengViet.trim())
                .append("TuLoai", tuLoai)
                .append("ViDu", viDu);
        collection.insertOne(doc);
    }

    public void updateEntry(String oldTiengAnh, String tiengAnh, String tiengViet, String tuLoai, String viDu) {
        Document query = new Document("TiengAnh", oldTiengAnh);
        Document update = new Document("$set", new Document()
                .append("TiengAnh", tiengAnh)
                .append("TiengViet", tiengViet)
                .append("TuLoai", tuLoai)
                .append("ViDu", viDu));
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
        }
    }
}