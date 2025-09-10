package TuDienAnhViet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * Client GUI:
 * - nhập từ tiếng Anh -> gửi tới server -> hiện kết quả (Tiếng Việt, Từ loại, Ví dụ)
 */
public class Client {
    private JFrame frame;
    private JTextField tfServer;
    private JTextField tfPort;
    private JTextField tfWord;
    private JTextArea taTiengViet;
    private JTextField tfTuLoai;
    private JTextArea taViDu;
    private JTextArea logArea;

    public Client() {
        setupGui();
    }

    private void setupGui() {
        frame = new JFrame("Từ điển Anh - Việt");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(750, 550);
        frame.setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Máy chủ:"));
        tfServer = new JTextField("localhost", 10);
        top.add(tfServer);
        top.add(new JLabel("Cổng:"));
        tfPort = new JTextField("12345", 6);
        top.add(tfPort);

        top.add(new JLabel("Từ cần tra:"));
        tfWord = new JTextField(20);
        top.add(tfWord);
        JButton btnLookup = new JButton("Tra cứu");
        JButton btnClear = new JButton("Xóa");
        top.add(btnLookup);
        top.add(btnClear);

        frame.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5,5,5,5);
        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.WEST;
        center.add(new JLabel("Tiếng Việt:"), g);
        g.gridx = 1; g.gridy = 0; g.fill = GridBagConstraints.BOTH; g.weightx = 1; g.weighty = 0.3;
        taTiengViet = new JTextArea(3, 40);
        taTiengViet.setLineWrap(true);
        taTiengViet.setWrapStyleWord(true);
        center.add(new JScrollPane(taTiengViet), g);

        g.gridx = 0; g.gridy = 1; g.fill = GridBagConstraints.NONE; g.weightx = 0; g.weighty = 0;
        center.add(new JLabel("Từ loại:"), g);
        g.gridx = 1; g.gridy = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        tfTuLoai = new JTextField();
        center.add(tfTuLoai, g);

        g.gridx = 0; g.gridy = 2; g.fill = GridBagConstraints.NONE; g.weightx = 0;
        center.add(new JLabel("Ví dụ:"), g);
        g.gridx = 1; g.gridy = 2; g.fill = GridBagConstraints.BOTH; g.weighty = 0.6;
        taViDu = new JTextArea(6, 40);
        taViDu.setLineWrap(true);
        taViDu.setWrapStyleWord(true);
        center.add(new JScrollPane(taViDu), g);

        frame.add(center, BorderLayout.CENTER);

        logArea = new JTextArea(6, 60);
        logArea.setEditable(false);
        frame.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        btnLookup.addActionListener(e -> doLookup());
        btnClear.addActionListener(e -> clearFields());
        tfWord.addActionListener(e -> doLookup()); 

        frame.setVisible(true);
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + java.time.LocalTime.now().withNano(0) + "] " + s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void clearFields() {
        tfWord.setText("");
        taTiengViet.setText("");
        tfTuLoai.setText("");
        taViDu.setText("");
        log("Đã xóa dữ liệu trên màn hình.");
    }

    private void doLookup() {
        String server = tfServer.getText().trim();
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Cổng nhập không hợp lệ");
            return;
        }
        String word = tfWord.getText().trim();
        if (word.isEmpty()) return;

        try (Socket sock = new Socket(server, port);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), "UTF-8"))) {

            out.write(word);
            out.newLine();
            out.flush();

            String resp = in.readLine();
            if (resp == null) {
                log("Không nhận được phản hồi từ máy chủ.");
                return;
            }
            log("Phản hồi gốc: " + resp);
            if (resp.startsWith("FOUND|||")) {
                String payload = resp.substring("FOUND|||".length());
                String[] parts = payload.split("\\|\\|\\|", -1);
                String tiengViet = unescape(parts.length>0?parts[0]:"");
                String tuLoai = unescape(parts.length>1?parts[1]:"");
                String viDu = unescape(parts.length>2?parts[2]:"");
                taTiengViet.setText(tiengViet);
                tfTuLoai.setText(tuLoai);
                taViDu.setText(viDu);
                log("Đã tìm thấy từ: " + word);
            } else if (resp.startsWith("NOTFOUND")) {
                taTiengViet.setText("");
                tfTuLoai.setText("");
                taViDu.setText("");
                JOptionPane.showMessageDialog(frame, "Không tìm thấy từ: " + word);
                log("Không tìm thấy '" + word + "'");
            } else {
                log("Phản hồi không xác định: " + resp);
            }

        } catch (IOException ex) {
            log("Lỗi IO: " + ex.getMessage());
            JOptionPane.showMessageDialog(frame, "Lỗi kết nối: " + ex.getMessage());
        }
    }

    private String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\|||", "|||");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
