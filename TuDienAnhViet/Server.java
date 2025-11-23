package TuDienAnhViet;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
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

    // 1. Loại bỏ historyConnector, sẽ dùng getInstance() trực tiếp
    // private final MongoDBConnector historyConnector = new MongoDBConnector();

    // Material Design Colors
    private final Color BG = new Color(240, 248, 240);
    private final Color CARD_BG = new Color(232, 245, 233);
    private final Color PRIMARY = new Color(76, 175, 80);
    private final Color PRIMARY_LIGHT = new Color(200, 230, 201);
    private final Color PRIMARY_DARK = new Color(56, 142, 60);
    private final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private final Color BORDER = new Color(200, 230, 200);
    private final int CORNER_RADIUS = 12;

    // (Giữ nguyên các class UI nội, vì Đại Ca yêu cầu không đổi UI)
    // Custom Material Components
    private class RoundedPanel extends JPanel {
        private Color backgroundColor;

        public RoundedPanel(Color bgColor) {
            super();
            setOpaque(false);
            backgroundColor = bgColor;
        }

        public RoundedPanel() { this(CARD_BG); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(backgroundColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
        }
    }

    private class ModernButton extends JButton {
        private Color normalColor, hoverColor, pressColor;

        public ModernButton(String text, Color color) {
            super(text);
            normalColor = color;
            hoverColor = color.darker();
            pressColor = color.darker().darker();
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(normalColor);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(hoverColor); }
                public void mouseExited(MouseEvent e) { setBackground(normalColor); }
                public void mousePressed(MouseEvent e) { setBackground(pressColor); }
                public void mouseReleased(MouseEvent e) { setBackground(hoverColor); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private class RoundedTextField extends JTextField {
        public RoundedTextField() {
            super();
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 8, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
            super.paintComponent(g);
        }
    }

    private class RoundedTextArea extends JTextArea {
        public RoundedTextArea() {
            super();
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 8, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
            super.paintComponent(g);
        }
    }
    // (Kết thúc các class UI nội)

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

    // 2. Thêm hàm helper để giảm lặp code UI
    private ModernButton createMenuButton(String text, Color color, ActionListener listener) {
        ModernButton button = new ModernButton(text, color);
        button.setMaximumSize(new Dimension(180, 40));
        if (listener != null) {
            button.addActionListener(listener);
        }
        return button;
    }

    private void setupGui() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        frame = new JFrame("Từ điển Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BG);
        frame.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Panel with Server Controls
        RoundedPanel top = new RoundedPanel();
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblTitle = new JLabel("Từ điển Server");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(PRIMARY_DARK);
        lblTitle.setBorder(new EmptyBorder(0, 0, 0, 20));

        JLabel lblPort = new JLabel("Cổng:");
        lblPort.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPort.setForeground(PRIMARY_DARK);

        tfPort = new RoundedTextField();
        tfPort.setText("12345");
        tfPort.setPreferredSize(new Dimension(100, 35));
        tfPort.setBackground(PRIMARY_LIGHT);

        btnStart = new ModernButton("Khởi động lại", PRIMARY);
        btnStop = new ModernButton("Dừng", PRIMARY_DARK);
        btnStop.setEnabled(false);

        top.add(lblTitle);
        top.add(lblPort);
        top.add(tfPort);
        top.add(btnStart);
        top.add(btnStop);
        frame.add(top, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setBackground(BG);

        // Dictionary Management Panel
        RoundedPanel crudPanel = new RoundedPanel();
        crudPanel.setLayout(new BorderLayout(10, 10));
        crudPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        // Left Menu Panel
        RoundedPanel leftPanel = new RoundedPanel(PRIMARY);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 3. Dùng helper để tạo button gọn gàng hơn
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(createMenuButton("Làm mới", PRIMARY_DARK, e -> refreshTable()));
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(createMenuButton("Thêm từ mới", PRIMARY_DARK, e -> onAdd()));
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(createMenuButton("Sửa từ", PRIMARY_DARK, e -> onEdit()));
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(createMenuButton("Xóa từ", PRIMARY_DARK, e -> onDelete()));
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(createMenuButton("Thêm từ nhanh", PRIMARY_DARK, e -> onImportCsv()));

        // Right Content Panel
        RoundedPanel rightPanel = new RoundedPanel();
        rightPanel.setLayout(new BorderLayout(10, 10));
        rightPanel.setBorder(new EmptyBorder(0, 10, 0, 0));

        // Search Panel at the top
        RoundedPanel searchPanel = new RoundedPanel(PRIMARY_LIGHT);
        searchPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSearch.setForeground(PRIMARY_DARK);

        RoundedTextField searchField = new RoundedTextField();
        searchField.setPreferredSize(new Dimension(300, 35));
        searchField.setBackground(Color.WHITE);

        JComboBox<String> directionBox = new JComboBox<>(new String[]{"Anh-Việt", "Việt-Anh"});
        directionBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        directionBox.setPreferredSize(new Dimension(120, 35));

        ModernButton searchButton = new ModernButton("Tìm kiếm", PRIMARY);
        
        searchPanel.add(lblSearch);
        searchPanel.add(searchField);
        searchPanel.add(directionBox);
        searchPanel.add(searchButton);
        rightPanel.add(searchPanel, BorderLayout.NORTH);

        // Dictionary Table
        tableModel = new DefaultTableModel(new Object[]{"Tiếng Anh", "Tiếng Việt", "Từ loại", "Ví dụ", "Phiên âm"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return true; }
        };
        tableEntries = new JTable(tableModel);
        tableEntries.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tableEntries.setRowHeight(30);
        tableEntries.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tableEntries.getTableHeader().setBackground(PRIMARY_LIGHT);
        tableEntries.getTableHeader().setForeground(PRIMARY_DARK);
        
        JScrollPane tableScroll = new JScrollPane(tableEntries);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        rightPanel.add(tableScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        crudPanel.add(splitPane, BorderLayout.CENTER);

        // Thêm xử lý sự kiện tìm kiếm
        searchButton.addActionListener(e -> {
            String searchTerm = searchField.getText().trim();
            if (searchTerm.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Vui lòng nhập từ cần tìm!");
                return;
            }

            boolean isEnToVi = directionBox.getSelectedIndex() == 0;
            TuDien.Entry result = isEnToVi ? tuDien.lookupEn(searchTerm) : tuDien.lookupVn(searchTerm);
            
            if (result != null) {
                tableModel.setRowCount(0);
                tableModel.addRow(new Object[]{result.tiengAnh, result.tiengViet, result.tuLoai, result.viDu, result.phienAm});
                log("Tìm thấy từ: " + searchTerm);
            } else {
                JOptionPane.showMessageDialog(frame, "Không tìm thấy từ: " + searchTerm);
                log("Không tìm thấy từ: " + searchTerm);
            }
        });
        
        // Thêm phím Enter để tìm kiếm
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    searchButton.doClick();
                }
            }
        });

        tabbedPane.addTab("Quản lý từ", crudPanel);

        // Log Panel
        RoundedPanel logPanel = new RoundedPanel();
        logPanel.setLayout(new BorderLayout(10, 10));
        logPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblLog = new JLabel("Nhật ký hoạt động");
        lblLog.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLog.setForeground(PRIMARY_DARK);
        lblLog.setBorder(new EmptyBorder(0, 0, 10, 0));
        logPanel.add(lblLog, BorderLayout.NORTH);

        logArea = new RoundedTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        logArea.setBackground(Color.WHITE);
        logArea.setForeground(TEXT_PRIMARY);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.getViewport().setBackground(Color.WHITE);
        logPanel.add(logScroll, BorderLayout.CENTER);

        tabbedPane.addTab("Log", logPanel);

        frame.add(tabbedPane, BorderLayout.CENTER);
        btnStart.addActionListener(this::startServer);
        btnStop.addActionListener(this::stopServer);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (TuDien.Entry entry : tuDien.getAllEntries()) 
            tableModel.addRow(new Object[]{entry.tiengAnh, entry.tiengViet, entry.tuLoai, entry.viDu, entry.phienAm});
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
            // 4. Dùng instance singleton
            MongoDBConnector.getInstance().saveHistory(direction, english, vietnamese);
            out.write("HISTORY_SAVED"); out.newLine(); out.flush();
            log("Đã lưu lịch sử dịch: " + direction + " - " + english + " - " + vietnamese);
        } catch (Exception ex) {
            log("Lỗi khi lưu lịch sử: " + ex.getMessage());
            out.write("ERROR_SAVING_HISTORY"); out.newLine(); out.flush();
        }
    }

    private void handleGetHistoryRequest(BufferedWriter out, Socket s) throws IOException {
        try {
            // 5. Dùng instance singleton
            java.util.List<Document> historyDocs = MongoDBConnector.getInstance().getHistory();
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
        // 6. Dùng instance singleton để close
        MongoDBConnector.getInstance().close();
        log("Máy chủ đã dừng.");
    }

    private TuDien.Entry showEntryDialog(String title, TuDien.Entry initialData) {
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        JTextField tfEng = new JTextField(20), tfViet = new JTextField(20),
                tfTuLoai = new JTextField(20), tfViDu = new JTextField(20),
                tfPhienAm = new JTextField(20);

        if (initialData != null) {
            tfEng.setText(initialData.tiengAnh);
            tfViet.setText(initialData.tiengViet);
            tfTuLoai.setText(initialData.tuLoai);
            tfViDu.setText(initialData.viDu);
            tfPhienAm.setText(initialData.phienAm);
        }

        panel.add(new JLabel("Tiếng Anh:")); panel.add(tfEng);
        panel.add(new JLabel("Tiếng Việt:")); panel.add(tfViet);
        panel.add(new JLabel("Từ loại:")); panel.add(tfTuLoai);
        panel.add(new JLabel("Ví dụ:")); panel.add(tfViDu);
        panel.add(new JLabel("Phiên âm:")); panel.add(tfPhienAm);

        JPanel buttonPanel = new JPanel();
        JButton btnSave = new JButton("Lưu");
        JButton btnCancel = new JButton("Hủy");
        buttonPanel.add(btnSave); buttonPanel.add(btnCancel);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        final TuDien.Entry[] result = new TuDien.Entry[1];
        btnSave.addActionListener(e -> {
            String eng = tfEng.getText().trim(), viet = tfViet.getText().trim(),
                    tuLoai = tfTuLoai.getText().trim(), viDu = tfViDu.getText().trim(),
                    phienAm = tfPhienAm.getText().trim();
            if (eng.isEmpty() || viet.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Tiếng Anh và Tiếng Việt không được để trống");
                return;
            }
            result[0] = new TuDien.Entry(eng, viet, tuLoai, viDu, phienAm);
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
        String oldPhienAm = (String) tableModel.getValueAt(row, 4);
        TuDien.Entry oldEntry = new TuDien.Entry(oldEng, oldViet, oldTuLoai, oldViDu, oldPhienAm);

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
                if (headers.length < 4 || !headers[0].trim().equals("TiengAnh") || !headers[1].trim().equals("TiengViet") ||
                        !headers[2].trim().equals("TuLoai") || !headers[3].trim().equals("ViDu")) {
                    JOptionPane.showMessageDialog(frame,
                            "File CSV không đúng định dạng!\nCần có ít nhất 4 cột: TiengAnh,TiengViet,TuLoai,ViDu\nCó thể thêm cột PhienAm", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int successCount = 0, skipCount = 0;
                String errorLines = "";

                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] values = line.split(",");
                    if (values.length < 4) {
                        errorLines += "Dòng không đúng định dạng: " + line + "\n";
                        continue;
                    }

                    String phienAm = values.length >= 5 ? values[4].trim() : "";
                    TuDien.Entry entry = new TuDien.Entry(values[0].trim(), values[1].trim(), values[2].trim(), values[3].trim(), phienAm);
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