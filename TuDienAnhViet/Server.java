package TuDienAnhViet;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.bson.Document;

public class Server {
    private JFrame frame;
    private JTextArea logArea;
    private JTextField tfPort;
    private JButton btnStart, btnStop;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private ExecutorService clientPool = null;
    private final TuDien tuDien = new TuDien();
    private JTable tableEntries;
    private DefaultTableModel tableModel;
    private final MongoDBConnector historyConnector = new MongoDBConnector();

    public Server() {
        setupGui();
        try {
            log("Tự động tải từ CSDL TuDienAnhViet, mục từ = " + tuDien.size());
            startServer(null);
        } catch (Exception ex) {
            log("Lỗi khi khởi động: " + ex.getMessage());
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }

    private void setupGui() {
        frame = new JFrame("Từ điển Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Cổng:"));
        tfPort = new JTextField("12345", 7);
        top.add(tfPort);
        btnStart = new JButton("Khởi động lại");
        btnStop = new JButton("Dừng");
        btnStop.setEnabled(false);
        top.add(btnStart);
        top.add(btnStop);
        frame.add(top, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel crudPanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"Tiếng Anh", "Tiếng Việt", "Từ loại", "Ví dụ"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return true; }
        };
        tableEntries = new JTable(tableModel);
        crudPanel.add(new JScrollPane(tableEntries), BorderLayout.CENTER);

        JPanel crudButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnRefresh = new JButton("Làm mới");
        JButton btnAdd = new JButton("Thêm");
        JButton btnEdit = new JButton("Sửa");
        JButton btnDelete = new JButton("Xóa");
        JButton btnImportCsv = new JButton("Thêm từ nhanh");
        crudButtons.add(btnRefresh);
        crudButtons.add(btnAdd);
        crudButtons.add(btnEdit);
        crudButtons.add(btnDelete);
        crudButtons.add(btnImportCsv);
        crudPanel.add(crudButtons, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refreshTable());
        btnAdd.addActionListener(e -> onAdd());
        btnEdit.addActionListener(e -> onEdit());
        btnDelete.addActionListener(e -> onDelete());
        btnImportCsv.addActionListener(e -> onImportCsv());

        tabbedPane.addTab("Quản lý từ", crudPanel);

        JPanel logPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea();
        logArea.setEditable(false);
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        tabbedPane.addTab("Log", logPanel);

        frame.add(tabbedPane, BorderLayout.CENTER);
        btnStart.addActionListener(this::startServer);
        btnStop.addActionListener(this::stopServer);
        frame.setVisible(true);
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (TuDien.Entry entry : tuDien.getAllEntries()) 
            tableModel.addRow(new Object[]{entry.tiengAnh, entry.tiengViet, entry.tuLoai, entry.viDu});
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + java.time.LocalTime.now().withNano(0) + "] " + s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startServer(java.awt.event.ActionEvent e) {
        if (running) stopServer(null);

        int port;
        try { port = Integer.parseInt(tfPort.getText().trim()); } 
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Cổng không hợp lệ");
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            clientPool = Executors.newCachedThreadPool();
            Thread acceptThread = new Thread(() -> {
                log("Máy chủ đang lắng nghe cổng " + port);
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        log("Kết nối mới từ " + client.getRemoteSocketAddress());
                        clientPool.submit(() -> handleClient(client));
                    } catch (IOException e1) {
                        if (running) log("Lỗi khi chấp nhận client: " + e1.getMessage());
                    }
                }
            });
            acceptThread.setDaemon(true);
            acceptThread.start();
        } catch (IOException ex) {
            log("Không thể mở cổng cho máy chủ: " + ex.getMessage());
        }
    }

    private void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

            String req;
            while ((req = in.readLine()) != null) {
                if (req.startsWith("SUGGEST|||")) handleSuggestionRequest(req, out, client);
                else if (req.startsWith("SAVE_HISTORY|||")) handleSaveHistoryRequest(req, out, client);
                else if (req.equals("GET_HISTORY")) handleGetHistoryRequest(out, client);
                else handleLookupRequest(req, out, client);
            }
        } catch (IOException ex) {
            log("Kết thúc kết nối client: " + ex.getMessage());
        }
    }

    private void handleLookupRequest(String req, BufferedWriter out, Socket s) throws IOException {
        String[] parts = req.split("\\|\\|\\|", 2);
        String mode = parts.length > 1 ? parts[0].trim() : "EN2VI";
        String word = parts.length > 1 ? parts[1] : req.trim();
        boolean en2vi = !"VI2EN".equalsIgnoreCase(mode);

        log("Yêu cầu từ " + s.getRemoteSocketAddress() + " -> mode=" + mode + " word='" + word + "'");

        TuDien.Entry e = en2vi ? tuDien.lookupEn(word) : tuDien.lookupVn(word);
        if (e != null) {
            out.write("FOUND|||" + e.serializeForNetwork(en2vi)); out.newLine(); out.flush();
            log("Đã gửi kết quả (tìm thấy) cho từ '" + word + "' (mode=" + mode + ")");
        } else {
            out.write("NOTFOUND"); out.newLine(); out.flush();
            log("Không tìm thấy từ '" + word + "' (mode=" + mode + ")");
        }
    }

    private void handleSuggestionRequest(String req, BufferedWriter out, Socket s) throws IOException {
        String[] parts = req.split("\\|\\|\\|");
        if (parts.length < 3) {
            out.write("INVALID_SUGGEST_REQUEST"); out.newLine(); out.flush(); return;
        }
        String mode = parts[1];
        String prefix = parts[2];
        boolean en2vi = !"VI2EN".equalsIgnoreCase(mode);

        java.util.List<String> suggestions = tuDien.getSuggestions(en2vi, prefix, 5);
        StringBuilder response = new StringBuilder("SUGGESTIONS");
        for (String suggestion : suggestions) response.append("|||").append(suggestion);

        out.write(response.toString()); out.newLine(); out.flush();
        log("Đã gửi " + suggestions.size() + " gợi ý cho '" + prefix + "' (mode=" + mode + ")");
    }

    private void handleSaveHistoryRequest(String req, BufferedWriter out, Socket s) throws IOException {
        String[] parts = req.split("\\|\\|\\|");
        if (parts.length < 4) {
            out.write("INVALID_SAVE_HISTORY_REQUEST"); out.newLine(); out.flush(); return;
        }
        String direction = parts[1];
        String english = parts[2];
        String vietnamese = parts[3];
        try {
            historyConnector.saveHistory(direction, english, vietnamese);
            out.write("HISTORY_SAVED"); out.newLine(); out.flush();
            log("Đã lưu lịch sử dịch: " + direction + " - " + english + " - " + vietnamese);
        } catch (Exception ex) {
            log("Lỗi khi lưu lịch sử: " + ex.getMessage());
            out.write("ERROR_SAVING_HISTORY"); out.newLine(); out.flush();
        }
    }

    private void handleGetHistoryRequest(BufferedWriter out, Socket s) throws IOException {
        try {
            java.util.List<Document> historyDocs = historyConnector.getHistory();
            StringBuilder response = new StringBuilder("HISTORY_LIST");
            for (Document doc : historyDocs) {
                response.append("|||").append(doc.getString("direction"))
                        .append("|||").append(doc.getString("english"))
                        .append("|||").append(doc.getString("vietnamese"));
            }
            out.write(response.toString()); out.newLine(); out.flush();
            log("Đã gửi lịch sử dịch cho " + s.getRemoteSocketAddress());
        } catch (Exception ex) {
            log("Lỗi khi lấy lịch sử: " + ex.getMessage());
            out.write("ERROR_GETTING_HISTORY"); out.newLine(); out.flush();
        }
    }

    private void stopServer(java.awt.event.ActionEvent e) {
        running = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException e1) {}
        if (clientPool != null) clientPool.shutdownNow();
        historyConnector.close();
        log("Máy chủ đã dừng.");
    }

    private TuDien.Entry showEntryDialog(String title, TuDien.Entry initialData) {
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField tfEng = new JTextField(20), tfViet = new JTextField(20),
                   tfTuLoai = new JTextField(20), tfViDu = new JTextField(20);

        if (initialData != null) {
            tfEng.setText(initialData.tiengAnh);
            tfViet.setText(initialData.tiengViet);
            tfTuLoai.setText(initialData.tuLoai);
            tfViDu.setText(initialData.viDu);
        }

        panel.add(new JLabel("Tiếng Anh:")); panel.add(tfEng);
        panel.add(new JLabel("Tiếng Việt:")); panel.add(tfViet);
        panel.add(new JLabel("Từ loại:")); panel.add(tfTuLoai);
        panel.add(new JLabel("Ví dụ:")); panel.add(tfViDu);

        JPanel buttonPanel = new JPanel();
        JButton btnSave = new JButton("Lưu");
        JButton btnCancel = new JButton("Hủy");
        buttonPanel.add(btnSave); buttonPanel.add(btnCancel);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        final TuDien.Entry[] result = new TuDien.Entry[1];
        btnSave.addActionListener(e -> {
            String eng = tfEng.getText().trim(), viet = tfViet.getText().trim(),
                   tuLoai = tfTuLoai.getText().trim(), viDu = tfViDu.getText().trim();
            if (eng.isEmpty() || viet.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Tiếng Anh và Tiếng Việt không được để trống");
                return;
            }
            result[0] = new TuDien.Entry(eng, viet, tuLoai, viDu);
            dialog.dispose();
        });

        btnCancel.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
        return result[0];
    }

    private void onAdd() {
        TuDien.Entry newEntry = showEntryDialog("Thêm từ", null);
        if (newEntry == null) return;
        try {
            tuDien.addEntry(newEntry);
            refreshTable();
            log("Đã thêm từ '" + newEntry.tiengAnh + "' vào CSDL");
        } catch (Exception ex) {
            log("Lỗi khi thêm từ vào CSDL: " + ex.getMessage());
            JOptionPane.showMessageDialog(frame, "Không thể thêm từ vào CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onEdit() {
        int row = tableEntries.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Vui lòng chọn dòng cần sửa."); return;
        }
        String oldEng = (String) tableModel.getValueAt(row, 0);
        String oldViet = (String) tableModel.getValueAt(row, 1);
        String oldTuLoai = (String) tableModel.getValueAt(row, 2);
        String oldViDu = (String) tableModel.getValueAt(row, 3);
        TuDien.Entry oldEntry = new TuDien.Entry(oldEng, oldViet, oldTuLoai, oldViDu);

        TuDien.Entry newEntry = showEntryDialog("Sửa từ", oldEntry);
        if (newEntry == null) return;

        try {
            tuDien.updateEntry(oldEng, newEntry);
            refreshTable();
            log("Đã cập nhật từ '" + oldEng + "' trong CSDL và bộ nhớ");
        } catch (Exception ex) {
            log("Lỗi khi cập nhật từ trong CSDL: " + ex.getMessage());
            JOptionPane.showMessageDialog(frame, "Không thể cập nhật từ trong CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDelete() {
        int row = tableEntries.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Vui lòng chọn dòng cần xóa."); return;
        }
        String eng = (String) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(frame, "Bạn có muốn xóa từ: " + eng + " không?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean removed = tuDien.removeEntryByEnglish(eng);
                if (removed) {
                    refreshTable();
                    log("Đã xóa từ '" + eng + "' khỏi CSDL");
                } else log("Lỗi: Không thể xóa từ khỏi CSDL");
            } catch (Exception ex) {
                log("Lỗi khi xóa từ khỏi CSDL: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame, "Không thể xóa từ khỏi CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onImportCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv"); }
            public String getDescription() { return "CSV Files (*.csv)"; }
        });

        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(selectedFile))) {
                String line = br.readLine();
                if (line == null) {
                    JOptionPane.showMessageDialog(frame, "File CSV trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String[] headers = line.split(",");
                if (headers.length != 4 || !headers[0].trim().equals("TiengAnh") || !headers[1].trim().equals("TiengViet") || 
                    !headers[2].trim().equals("TuLoai") || !headers[3].trim().equals("ViDu")) {
                    JOptionPane.showMessageDialog(frame, 
                        "File CSV không đúng định dạng!\nCần có đúng 4 cột: TiengAnh,TiengViet,TuLoai,ViDu", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int successCount = 0, skipCount = 0;
                String errorLines = "";

                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] values = line.split(",");
                    if (values.length != 4) {
                        errorLines += "Dòng không đúng định dạng: " + line + "\n";
                        continue;
                    }

                    TuDien.Entry entry = new TuDien.Entry(values[0].trim(), values[1].trim(), values[2].trim(), values[3].trim());
                    if (tuDien.lookupEn(entry.tiengAnh) != null) { skipCount++; continue; }

                    try { tuDien.addEntry(entry); successCount++; } 
                    catch (Exception ex) { errorLines += "Lỗi thêm từ: " + line + " - " + ex.getMessage() + "\n"; }
                }

                refreshTable();
                String message = String.format("Đã nhập %d từ thành công, bỏ qua %d từ trùng lặp.", successCount, skipCount);
                if (!errorLines.isEmpty()) message += "\n\nCác lỗi phát sinh:\n" + errorLines;
                JOptionPane.showMessageDialog(frame, message, "Kết quả nhập", 
                    errorLines.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Lỗi đọc file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(Server::new); }
}