package TuDienAnhViet;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private JFrame frame;
    private JTextArea logArea;
    private JTextField tfPort;
    private JButton btnStart;
    private JButton btnStop;
    private JButton loadCsvBtn;

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private ExecutorService clientPool = null;

    private final TuDien tuDien = new TuDien();

    public Server() {
        setupGui();
        File f = new File("src/KhoTu.csv");
        if (f.exists()) {
            try {
                tuDien.loadFromCSV(f.getPath());
                log("Tự động tải src/KhoTu.csv, mục từ = " + tuDien.size());
            } catch (Exception ex) {
                log("Lỗi khi tự động tải CSV: " + ex.getMessage());
            }
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
        btnStart = new JButton("Khởi động");
        btnStop = new JButton("Dừng");
        btnStop.setEnabled(false);
        loadCsvBtn = new JButton("Tải dữ liệu KhoTu.csv");
        top.add(btnStart);
        top.add(btnStop);
        top.add(loadCsvBtn);
        frame.add(top, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane sp = new JScrollPane(logArea);
        frame.add(sp, BorderLayout.CENTER);

        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());
        loadCsvBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(".");
            int r = chooser.showOpenDialog(frame);
            if (r == JFileChooser.APPROVE_OPTION) {
                File chosen = chooser.getSelectedFile();
                try {
                    tuDien.loadFromCSV(chosen.getPath());
                    log("Đã tải " + chosen.getPath() + ", số mục từ = " + tuDien.size());
                } catch (Exception ex) {
                    log("Lỗi khi tải CSV: " + ex.getMessage());
                }
            }
        });

        frame.setVisible(true);
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + java.time.LocalTime.now().withNano(0) + "] " + s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException ex) {
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
                    } catch (IOException e) {
                        if (running) log("Lỗi khi chấp nhận client: " + e.getMessage());
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
        try (Socket s = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {

            String req;
            while ((req = in.readLine()) != null) {
                if (req.startsWith("SUGGEST|||")) {
                    handleSuggestionRequest(req, out, s);
                } else {
                    handleLookupRequest(req, out, s);
                }
            }
        } catch (IOException ex) {
            log("Kết thúc kết nối client: " + ex.getMessage());
        }
    }

    private void handleLookupRequest(String req, BufferedWriter out, Socket s) throws IOException {
        String mode = "EN2VI";
        String word = req.trim();
        int idx = req.indexOf("|||");
        if (idx >= 0) {
            mode = req.substring(0, idx).trim();
            word = req.substring(idx + 3);
        }
        boolean en2vi = !"VI2EN".equalsIgnoreCase(mode);

        log("Yêu cầu từ " + s.getRemoteSocketAddress() + " -> mode=" + mode + " word='" + word + "'");

        TuDien.Entry e = en2vi ? tuDien.lookupEn(word) : tuDien.lookupVn(word);
        if (e != null) {
            String resp = "FOUND|||" + e.serializeForNetwork(en2vi);
            out.write(resp);
            out.newLine();
            out.flush();
            log("Đã gửi kết quả (tìm thấy) cho từ '" + word + "' (mode=" + mode + ")");
        } else {
            out.write("NOTFOUND");
            out.newLine();
            out.flush();
            log("Không tìm thấy từ '" + word + "' (mode=" + mode + ")");
        }
    }

    private void handleSuggestionRequest(String req, BufferedWriter out, Socket s) throws IOException {
        String[] parts = req.split("\\|\\|\\|");
        if (parts.length < 3) {
            out.write("INVALID_SUGGEST_REQUEST");
            out.newLine();
            out.flush();
            return;
        }

        String mode = parts[1];
        String prefix = parts[2];
        boolean en2vi = !"VI2EN".equalsIgnoreCase(mode);
        
        java.util.List<String> suggestions = tuDien.getSuggestions(en2vi, prefix, 5);
        StringBuilder response = new StringBuilder("SUGGESTIONS");
        for (String suggestion : suggestions) {
            response.append("|||").append(suggestion);
        }
        
        out.write(response.toString());
        out.newLine();
        out.flush();
        log("Đã gửi " + suggestions.size() + " gợi ý cho '" + prefix + "' (mode=" + mode + ")");
    }

    private void stopServer() {
        running = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {}
        if (clientPool != null) clientPool.shutdownNow();
        log("Máy chủ đã dừng.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
    }
}