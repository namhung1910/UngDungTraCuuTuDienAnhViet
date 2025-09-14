package TuDienAnhViet;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Client {
    private JFrame frame;
    private JTextField tfServer, tfPort, tfWord, tfTuLoai;
    private JComboBox<String> cbMode;
    private JLabel lblTarget;
    private JTextArea taTarget, taViDu, logArea;
    private JList<String> suggestionList;
    private JPopupMenu suggestionPopup;
    private DefaultListModel<String> suggestionListModel;
    private javax.swing.Timer suggestionTimer;
    private java.util.List<String> currentSuggestions;

    // màu chủ đạo
    private final Color BG = new Color(245, 250, 253);
    private final Color PANEL = new Color(225, 245, 255);
    private final Color PRIMARY = new Color(30, 144, 255);
    private final Color PRIMARY_DARK = new Color(15, 110, 210);
    private final Color INSET = new Color(220, 235, 250);
    private final Color NAVY = new Color(0, 60, 120); // xanh nước biển cho hai nút

    public Client() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        setupGui();
    }

    private void applyStyle(JComponent c) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if (c instanceof JButton) {
            c.setBackground(PRIMARY); c.setForeground(Color.WHITE);
            c.setOpaque(true);
            ((JButton)c).setFocusPainted(false);
            c.setBorder(new EmptyBorder(6,12,6,12));
        } else if (c instanceof JTextField || c instanceof JComboBox) {
            c.setBackground(Color.WHITE); c.setBorder(new CompoundBorder(new LineBorder(INSET,1), new EmptyBorder(4,6,4,6)));
        } else if (c instanceof JTextArea) {
            c.setBackground(Color.WHITE); c.setBorder(new LineBorder(INSET,1));
        } else if (c instanceof JLabel) {
            c.setForeground(PRIMARY_DARK);
        }
    }

    private void setupGui() {
        frame = new JFrame("Từ điển (Client)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1050,620);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));
        top.setBackground(PANEL);
        top.setBorder(new EmptyBorder(6,6,6,6));
        JLabel l1 = new JLabel("Máy chủ:"), l2 = new JLabel("Cổng:"), l3 = new JLabel("Từ cần tra:");
        tfServer = new JTextField("localhost",10); tfPort = new JTextField("12345",6); tfWord = new JTextField(24);
        JButton btnLookup = new JButton("Tra cứu"), btnClear = new JButton("Xóa");
        cbMode = new JComboBox<>(new String[]{"Tiếng Anh -> Tiếng Việt","Tiếng Việt -> Tiếng Anh"});
        cbMode.setSelectedIndex(0);
        for (JComponent c : new JComponent[]{l1,l2,l3,tfServer,tfPort,tfWord,btnLookup,btnClear,cbMode}) applyStyle(c);

        // override màu riêng cho 2 nút: xanh nước biển (navy)
        btnLookup.setBackground(NAVY); btnLookup.setForeground(Color.BLACK); btnLookup.setOpaque(true);
        btnClear.setBackground(NAVY); btnClear.setForeground(Color.BLACK); btnClear.setOpaque(true);

        top.add(l1); top.add(tfServer); top.add(l2); top.add(tfPort); top.add(l3); top.add(tfWord);
        top.add(cbMode); top.add(btnLookup); top.add(btnClear);
        frame.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.anchor = GridBagConstraints.NORTHWEST;

        lblTarget = new JLabel("Tiếng Việt:"); applyStyle(lblTarget);
        taTarget = new JTextArea(4,56); taTarget.setLineWrap(true); taTarget.setWrapStyleWord(true); applyStyle(taTarget);
        tfTuLoai = new JTextField(); applyStyle(tfTuLoai);
        taViDu = new JTextArea(8,56); taViDu.setLineWrap(true); taViDu.setWrapStyleWord(true); applyStyle(taViDu);

        g.gridx=0; g.gridy=0; g.fill = GridBagConstraints.NONE; g.weightx=0; g.weighty=0;
        center.add(lblTarget, g);

        g.gridx=1; g.gridy=0; g.fill = GridBagConstraints.BOTH; g.weightx=1; g.weighty=0.25;
        center.add(new JScrollPane(taTarget), g);

        g.gridx=0; g.gridy=1; g.fill = GridBagConstraints.NONE; g.weightx=0; g.weighty=0;
        JLabel lblTuLoai = new JLabel("Từ loại:"); applyStyle(lblTuLoai);
        center.add(lblTuLoai, g);

        g.gridx=1; g.gridy=1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx=1; g.weighty=0;
        center.add(tfTuLoai, g);

        g.gridx=0; g.gridy=2; g.fill = GridBagConstraints.NONE; g.weightx=0; g.weighty=0;
        JLabel lblViDu = new JLabel("Ví dụ:"); applyStyle(lblViDu);
        center.add(lblViDu, g);

        g.gridx=1; g.gridy=2; g.fill = GridBagConstraints.BOTH; g.weightx=1; g.weighty=0.6;
        center.add(new JScrollPane(taViDu), g);

        frame.add(center, BorderLayout.CENTER);

        logArea = new JTextArea(8,120); logArea.setEditable(false); applyStyle(logArea);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new CompoundBorder(new LineBorder(INSET,1), new EmptyBorder(4,4,4,4)));
        frame.add(logScroll, BorderLayout.SOUTH);

        suggestionListModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionListModel); suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(5); suggestionList.setFixedCellHeight(20);
        suggestionPopup = new JPopupMenu(); JScrollPane sp = new JScrollPane(suggestionList);
        sp.setPreferredSize(new Dimension(tfWord.getPreferredSize().width,120));
        suggestionPopup.add(sp); suggestionPopup.setFocusable(false);
        suggestionList.setBorder(new LineBorder(Color.WHITE,6));

        btnLookup.addActionListener(e -> doLookup());
        btnClear.addActionListener(e -> clearFields());
        tfWord.addActionListener(e -> { hideSuggestionPopup(); doLookup(); });
        cbMode.addActionListener(e -> lblTarget.setText(cbMode.getSelectedIndex()==0? "Tiếng Việt:" : "Tiếng Anh:"));

        tfWord.addKeyListener(new KeyAdapter(){
            @Override public void keyReleased(KeyEvent e){
                if (e.getKeyCode()==KeyEvent.VK_DOWN) { if (suggestionPopup.isVisible()) { suggestionList.requestFocus(); suggestionList.setSelectedIndex(0); } return; }
                if (e.getKeyCode()==KeyEvent.VK_ENTER || e.getKeyCode()==KeyEvent.VK_ESCAPE) { hideSuggestionPopup(); return; }
                if (suggestionTimer!=null && suggestionTimer.isRunning()) suggestionTimer.stop();
                suggestionTimer = new javax.swing.Timer(300, ev -> requestSuggestions());
                suggestionTimer.setRepeats(false); suggestionTimer.start();
            }
        });

        suggestionList.addMouseListener(new MouseAdapter(){ @Override public void mouseClicked(MouseEvent e){ if (e.getClickCount()==1) selectSuggestion(); }});
        suggestionList.addKeyListener(new KeyAdapter(){ @Override public void keyReleased(KeyEvent e){
            if (e.getKeyCode()==KeyEvent.VK_ENTER) selectSuggestion();
            else if (e.getKeyCode()==KeyEvent.VK_ESCAPE) { hideSuggestionPopup(); tfWord.requestFocus(); }
        }});
        tfWord.addFocusListener(new FocusAdapter(){ @Override public void focusLost(FocusEvent e){
            if (e.getOppositeComponent()!=suggestionList) hideSuggestionPopup();
        }});

        for (Component c : top.getComponents()) if (c instanceof JComponent) ((JComponent)c).setOpaque(true);
        top.setBackground(PANEL); center.setBackground(BG);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + LocalTime.now().withNano(0) + "] " + s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void clearFields() {
        tfWord.setText(""); taTarget.setText(""); tfTuLoai.setText(""); taViDu.setText("");
        hideSuggestionPopup(); log("Đã xóa dữ liệu trên màn hình.");
    }

    private void doLookup() {
        String server = tfServer.getText().trim();
        int port;
        try { port = Integer.parseInt(tfPort.getText().trim()); } catch (NumberFormatException e) { JOptionPane.showMessageDialog(frame, "Cổng nhập không hợp lệ"); return; }
        String word = tfWord.getText().trim(); if (word.isEmpty()) return;
        boolean en2vi = cbMode.getSelectedIndex()==0; String mode = en2vi? "EN2VI":"VI2EN";
        String reqLine = mode + "|||" + word;

        try (Socket sock = new Socket(server, port);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {

            out.write(reqLine); out.newLine(); out.flush();
            String resp = in.readLine();
            if (resp == null) { log("Không nhận được phản hồi từ máy chủ."); return; }
            log("Phản hồi gốc: " + resp);
            if (resp.startsWith("FOUND|||")) {
                String[] parts = splitPayload(resp.substring("FOUND|||".length()));
                String field1 = parts.length>0? unescape(parts[0]) : "";
                String tuLoai = parts.length>1? unescape(parts[1]) : "";
                String viDu = parts.length>2? unescape(parts[2]) : "";
                taTarget.setText(field1); tfTuLoai.setText(tuLoai); taViDu.setText(viDu);
                log("Hiển thị kết quả cho từ '" + word + "' (mode=" + mode + ").");
            } else if (resp.startsWith("NOTFOUND")) {
                taTarget.setText(""); tfTuLoai.setText(""); taViDu.setText("");
                log("Không tìm thấy từ '" + word + "'."); JOptionPane.showMessageDialog(frame, "Không tìm thấy từ: " + word);
            } else {
                log("Phản hồi không hợp lệ từ server: " + resp); JOptionPane.showMessageDialog(frame, "Phản hồi không hợp lệ từ server.");
            }
        } catch (IOException ex) {
            log("Lỗi IO: " + ex.getMessage()); JOptionPane.showMessageDialog(frame, "Lỗi kết nối: " + ex.getMessage());
        }
    }

    private void requestSuggestions() {
        String text = tfWord.getText().trim();
        if (text.isEmpty()) { hideSuggestionPopup(); return; }
        boolean en2vi = cbMode.getSelectedIndex()==0; String mode = en2vi? "EN2VI":"VI2EN";
        String request = "SUGGEST|||" + mode + "|||" + text;

        new SwingWorker<java.util.List<String>, Void>() {
            @Override protected java.util.List<String> doInBackground() {
                String server = tfServer.getText().trim(); int port;
                try { port = Integer.parseInt(tfPort.getText().trim()); } catch (NumberFormatException e) { log("Cổng không hợp lệ khi lấy gợi ý."); return Collections.emptyList(); }
                try (Socket sock = new Socket(server, port);
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                     BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {

                    out.write(request); out.newLine(); out.flush();
                    String response = in.readLine();
                    if (response!=null && response.startsWith("SUGGESTIONS|||")) {
                        String[] parts = response.split("\\|\\|\\|");
                        java.util.List<String> suggestions = new ArrayList<>();
                        for (int i=1;i<parts.length;i++) suggestions.add(parts[i]);
                        return suggestions;
                    }
                } catch (Exception ex) { log("Lỗi khi lấy gợi ý: " + ex.getMessage()); }
                return Collections.emptyList();
            }
            @Override protected void done() {
                try { showSuggestions(get()); } catch (InterruptedException | ExecutionException ignored) {}
            }
        }.execute();
    }

    private void showSuggestions(java.util.List<String> suggestions) {
        if (suggestions==null || suggestions.isEmpty()) { hideSuggestionPopup(); return; }
        currentSuggestions = suggestions;
        suggestionListModel.clear();
        for (String s : suggestions) suggestionListModel.addElement(s);
        suggestionList.setFixedCellWidth(Math.max(300, tfWord.getWidth()));
        if (!suggestionPopup.isVisible()) { suggestionPopup.show(tfWord, 0, tfWord.getHeight()); suggestionList.setSelectedIndex(-1); }
    }

    private void hideSuggestionPopup() { suggestionPopup.setVisible(false); }

    private void selectSuggestion() {
        int i = suggestionList.getSelectedIndex();
        if (i >= 0 && currentSuggestions != null && i < currentSuggestions.size()) {
            tfWord.setText(currentSuggestions.get(i));
            hideSuggestionPopup(); tfWord.requestFocus(); doLookup();
        }
    }

    private String[] splitPayload(String payload) { return payload.split("\\|\\|\\|", -1); }

    private String unescape(String s) {
        if (s==null) return "";
        String r = s.replace("\\n", "\n");
        r = r.replace("\\|||", "|||");
        r = r.replace("\\\\", "\\");
        return r;
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new Client()); }
}
