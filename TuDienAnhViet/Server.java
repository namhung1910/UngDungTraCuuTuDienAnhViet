package TuDienAnhViet;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Server GUI + TCP server.
 * - Khởi động server trên cổng (mặc định 12345).
 * - Mỗi client: đọc một dòng (từ), tra trong Từ điển, gửi trả kết quả.
 */
public class Server {
    private JFrame frame;
    private JTextArea logArea;
    private JButton btnStart;
    private JButton btnStop;
    private JTextField portField;

    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private volatile boolean running = false;
    private TuDien tuDien;

    public Server() {
        tuDien = new TuDien();
        setupGui();
    }

    private void setupGui() {
        frame = new JFrame("Máy chủ Từ điển Anh - Việt");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(750, 500);
        frame.setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Cổng:"));
        portField = new JTextField("12345", 6);
        top.add(portField);
        btnStart = new JButton("Khởi động");
        btnStop = new JButton("Dừng");
        btnStop.setEnabled(false);
        top.add(btnStart);
        top.add(btnStop);
        JButton loadCsvBtn = new JButton("Tải dữ liệu KhoTu.csv");
        top.add(loadCsvBtn);

        frame.add(top, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane sp = new JScrollPane(logArea);
        frame.add(sp, BorderLayout.CENTER);

        // hành động
        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());
        loadCsvBtn.addActionListener(e -> {
            try {
                tuDien.loadFromCSV("src/KhoTu.csv");
                log("Đã tải KhoTu.csv, số mục từ = " + tuDien.size());
            } catch (Exception ex) {
                log("Lỗi khi tải CSV: " + ex.getMessage());
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
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Cổng nhập không hợp lệ", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Tải sẵn CSV nếu có
        try {
            tuDien.loadFromCSV("src/KhoTu.csv");
            log("Đã tự động tải KhoTu.csv, số mục từ = " + tuDien.size());
        } catch (Exception ex) {
            log("Không tìm thấy KhoTu.csv hoặc lỗi khi tải: " + ex.getMessage());
        }

        try {
            serverSocket = new ServerSocket(port);
            clientPool = Executors.newCachedThreadPool();
            running = true;
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            log("Máy chủ đã khởi động trên cổng " + port);

            // luồng chấp nhận client
            Thread acceptThread = new Thread(() -> {
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
            acceptThread.start();
        } catch (IOException ex) {
            log("Không thể mở cổng cho máy chủ: " + ex.getMessage());
        }
    }

    private void handleClient(Socket client) {
        try (Socket s = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"))) {

            String req;
            while ((req = in.readLine()) != null) {
                String word = req.trim();
                log("Yêu cầu từ " + s.getRemoteSocketAddress() + " -> tra cứu('" + word + "')");
                TuDien.Entry e = tuDien.lookup(word);
                if (e != null) {
                    String resp = "FOUND|||" + e.serializeForNetwork();
                    out.write(resp);
                    out.newLine();
                    out.flush();
                    log("Đã gửi kết quả (tìm thấy) cho từ '" + word + "'");
                } else {
                    out.write("NOTFOUND");
                    out.newLine();
                    out.flush();
                    log("Không tìm thấy từ '" + word + "', đã gửi thông báo NOTFOUND");
                }
            }
            log("Client " + s.getRemoteSocketAddress() + " đã ngắt kết nối.");
        } catch (IOException ex) {
            log("Lỗi khi xử lý client: " + ex.getMessage());
        }
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
