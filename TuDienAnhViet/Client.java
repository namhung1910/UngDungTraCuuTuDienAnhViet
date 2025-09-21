package TuDienAnhViet;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Client {
    private JFrame frame;
    private JTextField tfWord;
    private JComboBox<String> cbMode;
    private JLabel lblTarget;
    private JTextArea taTarget, taViDu, taTuLoai;
    private JList<String> suggestionList, historyList;
    private JPopupMenu suggestionPopup;
    private DefaultListModel<String> suggestionListModel, historyListModel;
    private javax.swing.Timer suggestionTimer;
    private java.util.List<String> currentSuggestions;
    private java.util.List<HistoryItem> historyItems;

    private final Color BG = new Color(240, 248, 240);
    private final Color CARD_BG = new Color(232, 245, 233);
    private final Color PRIMARY = new Color(76, 175, 80);
    private final Color PRIMARY_LIGHT = new Color(200, 230, 201);
    private final Color PRIMARY_DARK = new Color(56, 142, 60);
    private final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private final Color BORDER = new Color(200, 230, 200);
    private final int CORNER_RADIUS = 12;
    private final String DEFAULT_SERVER = "localhost";
    private final int DEFAULT_PORT = 12345;

    private class HistoryItem {
        String direction, english, vietnamese;

        HistoryItem(String direction, String english, String vietnamese) {
            this.direction = direction;
            this.english = english;
            this.vietnamese = vietnamese;
        }

        @Override
        public String toString() {
            return direction.equals("EN2VI") ? "Anh-Việt: " + english + " → " + vietnamese : "Việt-Anh: " + vietnamese + " → " + english;
        }
    }

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
            setBorder(new EmptyBorder(10, 20, 10, 20));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(normalColor);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(hoverColor); }
                @Override public void mouseExited(MouseEvent e) { setBackground(normalColor); }
                @Override public void mousePressed(MouseEvent e) { setBackground(pressColor); }
                @Override public void mouseReleased(MouseEvent e) { setBackground(hoverColor); }
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

    private class RoundedComboBox extends JComboBox<String> {
        public RoundedComboBox(String[] items) {
            super(items);
            setOpaque(false);
            setBorder(new EmptyBorder(4, 12, 4, 12));
            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    label.setBorder(new EmptyBorder(4, 8, 4, 8));
                    if (isSelected) label.setBackground(PRIMARY_LIGHT);
                    return label;
                }
            });
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

    private class RoundedList extends JList<String> {
        RoundedList(DefaultListModel<String> model) {
            super(model);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(PRIMARY_LIGHT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
            super.paintComponent(g);
        }
    }

    public Client() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
        catch (Exception ignored) {}
        setupGui();
        loadHistory();
    }

    private void applyStyle(JComponent c) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        if (c instanceof JTextField) {
            c.setBackground(PRIMARY_LIGHT); c.setForeground(TEXT_PRIMARY);
        } else if (c instanceof JTextArea) {
            JTextArea ta = (JTextArea) c;
            ta.setBackground(PRIMARY_LIGHT); ta.setForeground(TEXT_PRIMARY); ta.setCaretColor(PRIMARY);
        } else if (c instanceof JLabel) c.setForeground(TEXT_PRIMARY);
        else if (c instanceof JList) {
            c.setBackground(PRIMARY_LIGHT); c.setBorder(new EmptyBorder(4, 4, 4, 4));
            JList<?> l = (JList<?>) c;
            l.setSelectionBackground(PRIMARY); l.setSelectionForeground(Color.WHITE);
        }
    }

    private JPanel createCardPanel(String title, Component content) {
        RoundedPanel card = new RoundedPanel(CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        if (title != null) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setForeground(PRIMARY_DARK);
            titleLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
            card.add(titleLabel, BorderLayout.NORTH);
        }
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JScrollPane createScroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(null);
        sp.getViewport().setOpaque(false);
        return sp;
    }

    private void setupGui() {
        frame = new JFrame("Từ điển Anh - Việt");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, PRIMARY, 0, getHeight(), PRIMARY_DARK);
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
            }
        };
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        JLabel lblLogo = new JLabel("Từ điển Anh-Việt");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setBorder(new EmptyBorder(0, 0, 0, 20));
        
        JLabel lblWord = new JLabel("Từ cần tra:");
        lblWord.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblWord.setForeground(Color.WHITE);
        tfWord = new RoundedTextField();
        tfWord.setPreferredSize(new Dimension(200, 40));
        
        ModernButton btnLookup = new ModernButton("Tra cứu", PRIMARY_DARK);
        ModernButton btnClear = new ModernButton("Xóa", new Color(120, 180, 120));
        
        cbMode = new RoundedComboBox(new String[]{"Anh → Việt", "Việt → Anh"});
        cbMode.setPreferredSize(new Dimension(150, 40));
        
        for (JComponent c : new JComponent[]{tfWord, cbMode}) applyStyle(c);
        
        top.add(lblLogo); top.add(lblWord); top.add(tfWord);
        top.add(cbMode); top.add(btnLookup); top.add(btnClear);
        frame.add(top, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(700);
        splitPane.setResizeWeight(0.7);
        splitPane.setBorder(null);

        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        leftPanel.setBackground(BG);
        leftPanel.setBorder(new EmptyBorder(0, 0, 0, 5));

        JPanel resultPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        resultPanel.setBackground(BG);
        
        JPanel translationPanel = new JPanel(new BorderLayout());
        lblTarget = new JLabel("Tiếng Việt:");
        lblTarget.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTarget.setForeground(PRIMARY_DARK);
        lblTarget.setBorder(new EmptyBorder(0, 0, 8, 0));

        taTarget = new RoundedTextArea();
        taTarget.setLineWrap(true);
        taTarget.setWrapStyleWord(true);
        applyStyle(taTarget);
        JScrollPane translationScroll = createScroll(taTarget);
        
        translationPanel.add(lblTarget, BorderLayout.NORTH);
        translationPanel.add(translationScroll, BorderLayout.CENTER);
        
        JPanel wordTypePanel = new JPanel(new BorderLayout());
        JLabel lblTuLoaiTitle = new JLabel("Từ loại");
        lblTuLoaiTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTuLoaiTitle.setForeground(PRIMARY_DARK);
        lblTuLoaiTitle.setBorder(new EmptyBorder(0, 0, 8, 0));

        taTuLoai = new RoundedTextArea();
        taTuLoai.setLineWrap(true);
        taTuLoai.setWrapStyleWord(true);
        applyStyle(taTuLoai);
        taTuLoai.setEditable(false);
        
        JScrollPane tuLoaiScroll = createScroll(taTuLoai);
        
        wordTypePanel.add(lblTuLoaiTitle, BorderLayout.NORTH);
        wordTypePanel.add(tuLoaiScroll, BorderLayout.CENTER);
        
        resultPanel.add(createCardPanel(null, translationPanel));
        resultPanel.add(createCardPanel(null, wordTypePanel));
        leftPanel.add(createCardPanel("Kết quả dịch", resultPanel));

        JPanel examplePanel = new JPanel(new BorderLayout());
        taViDu = new RoundedTextArea();
        taViDu.setLineWrap(true);
        taViDu.setWrapStyleWord(true);
        applyStyle(taViDu);
        
        JScrollPane exampleScroll = createScroll(taViDu);
        examplePanel.add(exampleScroll, BorderLayout.CENTER);
        
        leftPanel.add(createCardPanel("Ví dụ", examplePanel));
        splitPane.setLeftComponent(leftPanel);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BG);
        
        JLabel lblHistory = new JLabel("Lịch sử dịch");
        lblHistory.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHistory.setForeground(PRIMARY_DARK);
        lblHistory.setHorizontalAlignment(SwingConstants.CENTER);
        lblHistory.setBorder(new EmptyBorder(0, 0, 10, 0));

        historyListModel = new DefaultListModel<>();
        historyList = new RoundedList(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setFixedCellHeight(30);
        applyStyle(historyList);
        
        JScrollPane historyScroll = createScroll(historyList);
        
        rightPanel.add(lblHistory, BorderLayout.NORTH);
        rightPanel.add(historyScroll, BorderLayout.CENTER);
        
        splitPane.setRightComponent(createCardPanel(null, rightPanel));
        frame.add(splitPane, BorderLayout.CENTER);

        setupSuggestionPopup();
        setupEventHandlers(btnLookup, btnClear);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private void setupSuggestionPopup() {
        suggestionListModel = new DefaultListModel<>();
        suggestionList = new RoundedList(suggestionListModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(5); 
        suggestionList.setFixedCellHeight(30);
        applyStyle(suggestionList);
        
        suggestionPopup = new JPopupMenu(); 
        suggestionPopup.setBorder(new LineBorder(BORDER, 1));
        JScrollPane sp = createScroll(suggestionList);
        sp.setPreferredSize(new Dimension(tfWord.getPreferredSize().width, 150));
        suggestionPopup.add(sp); 
        suggestionPopup.setFocusable(false);
    }
    
    private void setupEventHandlers(ModernButton btnLookup, ModernButton btnClear) {
        btnLookup.addActionListener(e -> doLookup());
        btnClear.addActionListener(e -> clearFields());
        tfWord.addActionListener(e -> { suggestionPopup.setVisible(false); doLookup(); });
        cbMode.addActionListener(e -> lblTarget.setText(cbMode.getSelectedIndex() == 0 ? "Tiếng Việt:" : "Tiếng Anh:"));
        
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = historyList.getSelectedIndex();
                    if (index >= 0 && index < historyItems.size()) {
                        HistoryItem item = historyItems.get(index);
                        cbMode.setSelectedIndex(item.direction.equals("EN2VI") ? 0 : 1);
                        tfWord.setText(item.direction.equals("EN2VI") ? item.english : item.vietnamese);
                        doLookup();
                    }
                }
            }
        });

        tfWord.addKeyListener(new KeyAdapter(){
            @Override 
            public void keyReleased(KeyEvent e){
                if (e.getKeyCode() == KeyEvent.VK_DOWN && suggestionPopup.isVisible()) { 
                    suggestionList.requestFocus(); 
                    suggestionList.setSelectedIndex(0); 
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) { 
                    suggestionPopup.setVisible(false); 
                    return; 
                }
                if (suggestionTimer != null && suggestionTimer.isRunning()) suggestionTimer.stop();
                suggestionTimer = new javax.swing.Timer(300, e2 -> requestSuggestions());
                suggestionTimer.setRepeats(false); 
                suggestionTimer.start();
            }
        });

        suggestionList.addMouseListener(new MouseAdapter(){ 
            @Override public void mouseClicked(MouseEvent e){ if (e.getClickCount() == 1) selectSuggestion(); }
        });
        
        suggestionList.addKeyListener(new KeyAdapter(){ 
            @Override 
            public void keyReleased(KeyEvent e){
                if (e.getKeyCode() == KeyEvent.VK_ENTER) selectSuggestion();
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { 
                    suggestionPopup.setVisible(false); 
                    tfWord.requestFocus(); 
                }
            }
        });
        
        tfWord.addFocusListener(new FocusAdapter(){ 
            @Override public void focusLost(FocusEvent e){ if (e.getOppositeComponent() != suggestionList) suggestionPopup.setVisible(false); }
        });
    }
    
    private void clearFields() {
        tfWord.setText(""); 
        taTarget.setText(""); 
        taTuLoai.setText(""); 
        taViDu.setText("");
        suggestionPopup.setVisible(false);
    }

    private void doLookup() {
        String word = tfWord.getText().trim(); 
        if (word.isEmpty()) return;
        
        boolean en2vi = cbMode.getSelectedIndex() == 0; 
        String mode = en2vi ? "EN2VI" : "VI2EN";
        String reqLine = mode + "|||" + word;

        try (Socket sock = new Socket(DEFAULT_SERVER, DEFAULT_PORT);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {

            out.write(reqLine); out.newLine(); out.flush();
            String resp = in.readLine();
            if (resp == null) return;
            
            if (resp.startsWith("FOUND|||")) {
                String[] parts = resp.substring("FOUND|||".length()).split("\\|\\|\\|", -1);
                taTarget.setText(parts.length > 0 ? unescape(parts[0]) : ""); 
                taTuLoai.setText(parts.length > 1 ? unescape(parts[1]) : ""); 
                taViDu.setText(parts.length > 2 ? unescape(parts[2]) : "");
                saveHistory(mode, word, taTarget.getText());
            } else if (resp.startsWith("NOTFOUND")) {
                taTarget.setText(""); taTuLoai.setText(""); taViDu.setText("");
                JOptionPane.showMessageDialog(frame, "Không tìm thấy từ: " + word);
            } else JOptionPane.showMessageDialog(frame, "Phản hồi không hợp lệ từ server.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Lỗi kết nối: " + ex.getMessage());
        }
    }

    private void saveHistory(String direction, String word, String translation) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (Socket sock = new Socket(DEFAULT_SERVER, DEFAULT_PORT);
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                     BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {

                    String request = "SAVE_HISTORY|||" + direction + "|||" + word + "|||" + translation;
                    out.write(request); out.newLine(); out.flush();
                    in.readLine();
                } catch (Exception ex) { ex.printStackTrace(); }
                return null;
            }

            @Override
            protected void done() { loadHistory(); }
        }.execute();
    }

    private void loadHistory() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (Socket sock = new Socket(DEFAULT_SERVER, DEFAULT_PORT);
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                     BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {

                    out.write("GET_HISTORY"); out.newLine(); out.flush();
                    String response = in.readLine();
                    if (response != null && response.startsWith("HISTORY_LIST")) {
                        String[] parts = response.split("\\|\\|\\|");
                        historyItems = new ArrayList<>();
                        for (int i = 1; i < parts.length; i += 3) 
                            if (i + 2 < parts.length) historyItems.add(new HistoryItem(parts[i], parts[i+1], parts[i+2]));
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
                return null;
            }

            @Override
            protected void done() {
                historyListModel.clear();
                if (historyItems != null) for (HistoryItem item : historyItems) historyListModel.addElement(item.toString());
            }
        }.execute();
    }

    private void requestSuggestions() {
        String text = tfWord.getText().trim();
        if (text.isEmpty()) { suggestionPopup.setVisible(false); return; }
        
        boolean en2vi = cbMode.getSelectedIndex() == 0; 
        String mode = en2vi ? "EN2VI" : "VI2EN";
        String request = "SUGGEST|||" + mode + "|||" + text;

        new SwingWorker<java.util.List<String>, Void>() {
            @Override 
            protected java.util.List<String> doInBackground() {
                try (Socket sock = new Socket(DEFAULT_SERVER, DEFAULT_PORT);
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                     BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {

                    out.write(request); out.newLine(); out.flush();
                    String response = in.readLine();
                    if (response != null && response.startsWith("SUGGESTIONS|||")) {
                        String[] parts = response.split("\\|\\|\\|");
                        java.util.List<String> suggestions = new ArrayList<>();
                        for (int i = 1; i < parts.length; i++) suggestions.add(parts[i]);
                        return suggestions;
                    }
                } catch (Exception ex) { }
                return Collections.emptyList();
            }
            
            @Override 
            protected void done() {
                try { showSuggestions(get()); } 
                catch (InterruptedException | ExecutionException ignored) {}
            }
        }.execute();
    }

    private void showSuggestions(java.util.List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) { suggestionPopup.setVisible(false); return; }
        currentSuggestions = suggestions;
        suggestionListModel.clear();
        for (String s : suggestions) suggestionListModel.addElement(s);
        suggestionList.setFixedCellWidth(Math.max(300, tfWord.getWidth()));
        if (!suggestionPopup.isVisible()) { 
            suggestionPopup.show(tfWord, 0, tfWord.getHeight()); 
            suggestionList.setSelectedIndex(-1); 
        }
    }

    private void selectSuggestion() {
        int i = suggestionList.getSelectedIndex();
        if (i >= 0 && currentSuggestions != null && i < currentSuggestions.size()) {
            tfWord.setText(currentSuggestions.get(i));
            suggestionPopup.setVisible(false); 
            tfWord.requestFocus(); 
            doLookup();
        }
    }

    private String unescape(String s) {
        return s == null ? "" : s.replace("\\n", "\n").replace("\\|||", "|||").replace("\\\\", "\\");
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(Client::new); }
}