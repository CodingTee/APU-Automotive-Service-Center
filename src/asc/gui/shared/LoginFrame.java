package asc.gui.shared;

import asc.model.*;
import asc.util.FileManager;
import asc.util.UIUtils;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.*;

public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JButton        togglePwdBtn;
    private boolean        pwdVisible = false;
    private JLabel         statusLabel;

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("APU Automotive Service Centre - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        // ── Root panel (dark navy background) ────────────────────────────────
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIUtils.DARK_NAVY);

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(28, 50, 100),
                        getWidth(), getHeight(), new Color(18, 32, 62)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // bottom accent line
                g2.setColor(UIUtils.ACCENT_BLUE);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                g2.dispose();
            }
        };
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(18, 30, 14, 30));
        header.setOpaque(false);

        // Java logo image
        ImageIcon logoIcon = new ImageIcon("data/Picture/NavBar/Java logo.png");
        Image scaledImg = logoIcon.getImage().getScaledInstance(125, 65, Image.SCALE_SMOOTH);
        JLabel carIcon = new JLabel(new ImageIcon(scaledImg), JLabel.CENTER);
        carIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLbl = new JLabel("APU Automotive Service Centre", JLabel.CENTER);
        titleLbl.setFont(UIUtils.uiFont(Font.BOLD, 20));
        titleLbl.setForeground(UIUtils.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("APU-ASC Management System", JLabel.CENTER);
        subLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        subLbl.setForeground(UIUtils.DIM_TEXT);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(carIcon);
        header.add(Box.createVerticalStrut(4));
        header.add(titleLbl);
        header.add(Box.createVerticalStrut(2));
        header.add(subLbl);

        // ── Centre card ───────────────────────────────────────────────────────
        JPanel cardWrap = new JPanel(new GridBagLayout());
        cardWrap.setBackground(UIUtils.DARK_NAVY);
        cardWrap.setBorder(new EmptyBorder(18, 40, 10, 40));

        JPanel card = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIUtils.MEDIUM_NAVY);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 18, 1),
                new EmptyBorder(22, 36, 20, 36)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 4, 7, 4);

        // "LOGIN" heading inside card
        JLabel loginHdr = new JLabel("LOGIN", JLabel.CENTER);
        loginHdr.setFont(UIUtils.uiFont(Font.BOLD, 18));
        loginHdr.setForeground(UIUtils.ACCENT_BLUE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 4, 16, 4);
        card.add(loginHdr, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 4, 6, 4);

        // Username row
        JLabel userLbl = new JLabel("Username:");
        userLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        userLbl.setForeground(UIUtils.WHITE);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.32;
        card.add(userLbl, gbc);

        usernameField = new JTextField(18);
        UIUtils.styleTextField(usernameField);
        usernameField.setPreferredSize(new Dimension(220, 34));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.68;
        card.add(usernameField, gbc);

        // Password row
        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        passLbl.setForeground(UIUtils.WHITE);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.32;
        card.add(passLbl, gbc);

        // ── Password field with inline show/hide toggle (icon inside the field) ──
        passwordField = new JPasswordField(18);
        // Do NOT call styleTextField — borders are on the wrapper panel instead
        passwordField.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        passwordField.setBackground(new Color(46, 68, 118));
        passwordField.setForeground(UIUtils.WHITE);
        passwordField.setCaretColor(UIUtils.WHITE);
        passwordField.setBorder(null);
        passwordField.setOpaque(true);

        // Load toggle icons (small so the panel stays 34 px tall like username)
        ImageIcon showIcon = new ImageIcon("data/Picture/Login and Register/open.png");
        ImageIcon hideIcon = new ImageIcon("data/Picture/Login and Register/close.png");
        Image showScaled = showIcon.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
        Image hideScaled = hideIcon.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);

        togglePwdBtn = new JButton(new ImageIcon(hideScaled));
        togglePwdBtn.setPreferredSize(new Dimension(22, 22));
        togglePwdBtn.setToolTipText("Show / Hide password");
        togglePwdBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        togglePwdBtn.setBorderPainted(false);
        togglePwdBtn.setContentAreaFilled(false);
        togglePwdBtn.setFocusPainted(false);
        togglePwdBtn.setOpaque(false);
        togglePwdBtn.addActionListener(e -> {
            pwdVisible = !pwdVisible;
            if (pwdVisible) {
                passwordField.setEchoChar((char) 0);
                togglePwdBtn.setIcon(new ImageIcon(showScaled));
            } else {
                passwordField.setEchoChar('\u2022');
                togglePwdBtn.setIcon(new ImageIcon(hideScaled));
            }
        });

        // Wrapper panel: paints the text-field background + border,
        // with the field on the left and toggle button on the right — both INSIDE the border.
        JPanel pwdPanel = new JPanel(new BorderLayout(4, 0)) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(46, 68, 118));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
            }
        };
        pwdPanel.setOpaque(false);
        pwdPanel.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 8, 1),
                new EmptyBorder(5, 9, 5, 2)
        ));
        pwdPanel.add(passwordField, BorderLayout.CENTER);
        pwdPanel.add(togglePwdBtn, BorderLayout.EAST);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.68;
        card.add(pwdPanel, gbc);

        // Status label (inline error / hint)
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setFont(UIUtils.uiFont(Font.ITALIC, 11));
        statusLabel.setForeground(UIUtils.ERROR_RED);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        card.add(statusLabel, gbc);

        // Button row — uses RoundedButton so colour actually renders
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 14, 0));
        btnPanel.setOpaque(false);

        UIUtils.RoundedButton loginBtn = new UIUtils.RoundedButton("Login", UIUtils.ACCENT_BLUE);
        loginBtn.setFont(UIUtils.uiFont(Font.BOLD, 13));
        loginBtn.setPreferredSize(new Dimension(140, 38));
        loginBtn.addActionListener(e -> performLogin());

        UIUtils.RoundedButton registerBtn = new UIUtils.RoundedButton("Register as Customer", UIUtils.SUCCESS_GREEN);
        registerBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
        registerBtn.setPreferredSize(new Dimension(160, 38));
        registerBtn.addActionListener(e -> {
            RegisterDialog dlg = new RegisterDialog(LoginFrame.this);
            dlg.setVisible(true);
        });

        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 4, 4, 4);
        card.add(btnPanel, gbc);

        cardWrap.add(card, new GridBagConstraints());

        // Enter key triggers login
        passwordField.addActionListener(e -> performLogin());
        usernameField.addActionListener(e -> passwordField.requestFocus());

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(UIUtils.MEDIUM_NAVY);
        footer.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel footerLbl = new JLabel("APU Automotive Service Centre  |  v1.0");
        footerLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        footerLbl.setForeground(UIUtils.DIM_TEXT);
        footer.add(footerLbl);

        root.add(header,   BorderLayout.NORTH);
        root.add(cardWrap, BorderLayout.CENTER);
        root.add(footer,   BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            flash("Please enter your username and password.");
            return;
        }

        User user = FileManager.authenticate(username, password);
        if (user == null) {
            flash("Invalid username or password.");
            passwordField.setText("");
            passwordField.requestFocus();
            return;
        }

        dispose();
        user.openDashboard();
    }

    /** Show a short red message inline, then fade it after 3 s. */
    private void flash(String msg) {
        statusLabel.setForeground(UIUtils.ERROR_RED);
        statusLabel.setText(msg);
        Timer t = new Timer(3000, e -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }
}
