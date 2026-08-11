package asc.gui.shared;

import asc.model.Customer;
import asc.model.VipAccount;
import asc.util.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.security.SecureRandom;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Registration dialog – Customer self-registration only.
 * Account is created immediately (no Manager approval needed).
 * Other roles (Manager, Counter Staff, Technician) are created by the Manager.
 */
public class RegisterDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private JTextField     uF, nF, eF, phF;
    private JPasswordField pwF, cpwF;
    private JButton        pwToggleBtn, cpwToggleBtn;
    private JLabel         pwWarnLbl;
    private boolean        pwVisible = false, cpwVisible = false;
    private JComboBox<String> genderBox;

    private File   chosenAvatarFile = null;
    private JLabel avatarPreviewLabel;

    public RegisterDialog(Frame parent) {
        super(parent, "Registration", true);
        initComponents();
    }

    private void initComponents() {
        setSize(500, 720);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIUtils.DARK_NAVY);

        // ── Top: avatar + title ──────────────────────────────────────────────
        JPanel avatarPanel = new JPanel();
        avatarPanel.setBackground(UIUtils.MEDIUM_NAVY);
        avatarPanel.setBorder(new EmptyBorder(18, 10, 14, 10));
        avatarPanel.setLayout(new BoxLayout(avatarPanel, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel("APU-ASC Customer Registration", JLabel.CENTER);
        titleLbl.setFont(UIUtils.uiFont(Font.BOLD, 16));
        titleLbl.setForeground(UIUtils.WHITE);
        titleLbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("Your account will be ready immediately after registration", JLabel.CENTER);
        subLbl.setFont(UIUtils.uiFont(Font.ITALIC, 11));
        subLbl.setForeground(UIUtils.GREEN_OK);
        subLbl.setAlignmentX(CENTER_ALIGNMENT);

        avatarPreviewLabel = new JLabel(UIUtils.loadCircularAvatar(null, "", 72));
        avatarPreviewLabel.setAlignmentX(CENTER_ALIGNMENT);
        avatarPreviewLabel.setToolTipText("Click to choose a profile picture");
        avatarPreviewLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Prevent dialog resize when icon changes
        avatarPreviewLabel.setPreferredSize(new Dimension(72, 72));
        avatarPreviewLabel.setMaximumSize(new Dimension(72, 72));
        avatarPreviewLabel.setMinimumSize(new Dimension(72, 72));
        avatarPreviewLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { pickAvatar(); }
        });

        JLabel hintLbl = new JLabel("Click avatar to upload photo (optional)", JLabel.CENTER);
        hintLbl.setFont(UIUtils.uiFont(Font.ITALIC, 11));
        hintLbl.setForeground(UIUtils.LIGHT_GRAY);
        hintLbl.setAlignmentX(CENTER_ALIGNMENT);

        avatarPanel.add(titleLbl);
        avatarPanel.add(Box.createVerticalStrut(4));
        avatarPanel.add(subLbl);
        avatarPanel.add(Box.createVerticalStrut(8));
        avatarPanel.add(avatarPreviewLabel);
        avatarPanel.add(Box.createVerticalStrut(4));
        avatarPanel.add(hintLbl);

        // ── Centre: form fields ──────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIUtils.DARK_NAVY);
        form.setBorder(new EmptyBorder(12, 32, 10, 32));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        uF   = new JTextField();     UIUtils.styleTextField(uF);
        nF   = new JTextField();     UIUtils.styleTextField(nF);
        eF   = new JTextField();     UIUtils.styleTextField(eF);
        phF  = new JTextField();     UIUtils.styleTextField(phF);

        // Password fields — NO styleTextField; borders are on wrapper panels (matching LoginFrame)
        pwF  = new JPasswordField();
        pwF.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        pwF.setBackground(new Color(46, 68, 118));
        pwF.setForeground(UIUtils.WHITE);
        pwF.setCaretColor(UIUtils.WHITE);
        pwF.setBorder(null);
        pwF.setOpaque(true);

        cpwF = new JPasswordField();
        cpwF.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        cpwF.setBackground(new Color(46, 68, 118));
        cpwF.setForeground(UIUtils.WHITE);
        cpwF.setCaretColor(UIUtils.WHITE);
        cpwF.setBorder(null);
        cpwF.setOpaque(true);

        // ── Eye toggle icons ───────────────────────────────────────────────
        ImageIcon showIcon = new ImageIcon("data/Picture/Login and Register/open.png");
        ImageIcon hideIcon = new ImageIcon("data/Picture/Login and Register/close.png");
        Image showScaled = showIcon.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
        Image hideScaled = hideIcon.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);

        // ── Auto-generate password button ───────────────────────────────────
        ImageIcon autoGenIcon = new ImageIcon("data/Picture/Login and Register/AutoGeneration.png");
        Image autoGenScaled = autoGenIcon.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
        JButton autoGenBtn = new JButton(new ImageIcon(autoGenScaled));
        autoGenBtn.setPreferredSize(new Dimension(22, 22));
        autoGenBtn.setToolTipText("Auto-generate password");
        autoGenBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        autoGenBtn.setBorderPainted(false);
        autoGenBtn.setContentAreaFilled(false);
        autoGenBtn.setFocusPainted(false);
        autoGenBtn.setOpaque(false);
        autoGenBtn.addActionListener(e -> {
            String pwd = generatePassword();
            pwF.setText(pwd);
            cpwF.setText(pwd);
            checkPwStrength();
            // Show both passwords so user can see what was generated
            if (!pwVisible) {
                pwVisible = true;
                pwF.setEchoChar((char) 0);
                pwToggleBtn.setIcon(new ImageIcon(showScaled));
            }
            if (!cpwVisible) {
                cpwVisible = true;
                cpwF.setEchoChar((char) 0);
                cpwToggleBtn.setIcon(new ImageIcon(showScaled));
            }
        });

        // ── Password eye toggle button ──────────────────────────────────────
        pwToggleBtn = new JButton(new ImageIcon(hideScaled));
        pwToggleBtn.setPreferredSize(new Dimension(22, 22));
        pwToggleBtn.setToolTipText("Show / Hide password");
        pwToggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pwToggleBtn.setBorderPainted(false);
        pwToggleBtn.setContentAreaFilled(false);
        pwToggleBtn.setFocusPainted(false);
        pwToggleBtn.setOpaque(false);
        pwToggleBtn.addActionListener(e -> {
            pwVisible = !pwVisible;
            if (pwVisible) {
                pwF.setEchoChar((char) 0);
                pwToggleBtn.setIcon(new ImageIcon(showScaled));
            } else {
                pwF.setEchoChar('\u2022');
                pwToggleBtn.setIcon(new ImageIcon(hideScaled));
            }
        });

        // ── Confirm password eye toggle button ──────────────────────────────
        cpwToggleBtn = new JButton(new ImageIcon(hideScaled));
        cpwToggleBtn.setPreferredSize(new Dimension(22, 22));
        cpwToggleBtn.setToolTipText("Show / Hide password");
        cpwToggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cpwToggleBtn.setBorderPainted(false);
        cpwToggleBtn.setContentAreaFilled(false);
        cpwToggleBtn.setFocusPainted(false);
        cpwToggleBtn.setOpaque(false);
        cpwToggleBtn.addActionListener(e -> {
            cpwVisible = !cpwVisible;
            if (cpwVisible) {
                cpwF.setEchoChar((char) 0);
                cpwToggleBtn.setIcon(new ImageIcon(showScaled));
            } else {
                cpwF.setEchoChar('\u2022');
                cpwToggleBtn.setIcon(new ImageIcon(hideScaled));
            }
        });

        // ── Password wrapper panel (field + auto-gen + eye toggle) ──────────
        JPanel pwPanel = new JPanel(new BorderLayout(4, 0)) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(46, 68, 118));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
            }
        };
        pwPanel.setOpaque(false);
        pwPanel.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 8, 1),
                new EmptyBorder(5, 9, 5, 2)));
        JPanel pwBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        pwBtnPanel.setOpaque(false);
        pwBtnPanel.add(autoGenBtn);
        pwBtnPanel.add(pwToggleBtn);
        pwPanel.add(pwF, BorderLayout.CENTER);
        pwPanel.add(pwBtnPanel, BorderLayout.EAST);

        // ── Password strength warning (below password field) ────────────────
        pwWarnLbl = new JLabel("* Your password is too easy");
        pwWarnLbl.setFont(UIUtils.uiFont(Font.ITALIC, 10));
        pwWarnLbl.setForeground(UIUtils.ERROR_RED);
        pwWarnLbl.setVisible(false);
        pwWarnLbl.setPreferredSize(new Dimension(0, 16));
        pwWarnLbl.setMinimumSize(new Dimension(0, 16));

        // ── Password column: field panel + warning label ────────────────────
        JPanel pwColPanel = new JPanel(new BorderLayout(0, 1));
        pwColPanel.setOpaque(false);
        pwColPanel.add(pwPanel, BorderLayout.CENTER);
        pwColPanel.add(pwWarnLbl, BorderLayout.SOUTH);

        // Real-time strength check
        pwF.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { checkPwStrength(); }
            public void removeUpdate(DocumentEvent e)  { checkPwStrength(); }
            public void changedUpdate(DocumentEvent e) { checkPwStrength(); }
        });

        // ── Confirm password wrapper panel (field + eye toggle) ─────────────
        JPanel cpwPanel = new JPanel(new BorderLayout(4, 0)) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(46, 68, 118));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
            }
        };
        cpwPanel.setOpaque(false);
        cpwPanel.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 8, 1),
                new EmptyBorder(5, 9, 5, 2)));
        JPanel cpwBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        cpwBtnPanel.setOpaque(false);
        cpwBtnPanel.add(cpwToggleBtn);
        cpwPanel.add(cpwF, BorderLayout.CENTER);
        cpwPanel.add(cpwBtnPanel, BorderLayout.EAST);

        genderBox = UIUtils.createGenderCombo("");
        genderBox.addActionListener(e -> {
            if (chosenAvatarFile == null) {
                String g = UIUtils.resolveGender(genderBox);
                avatarPreviewLabel.setIcon(UIUtils.loadCircularAvatar(null, g, 72));
            }
        });

        // Role is fixed to CUSTOMER — other roles are created by Manager only
        JLabel roleValueLbl = new JLabel("CUSTOMER");
        roleValueLbl.setForeground(UIUtils.ACCENT_BLUE);
        roleValueLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));

        addFormRow(form, gbc, 0, "Role:",          roleValueLbl);
        addFormRow(form, gbc, 1, "Username:",      uF);
        addFormRow(form, gbc, 2, "Password:",      pwColPanel);
        addFormRow(form, gbc, 3, "Confirm Pass:",  cpwPanel);
        addFormRow(form, gbc, 4, "Full Name:",     nF);
        addFormRow(form, gbc, 5, "Gender:",        genderBox);
        addFormRow(form, gbc, 6, "Email:",         eF);
        addFormRow(form, gbc, 7, "Phone:",         phF);

        // ── Bottom: register button ──────────────────────────────────────────
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(UIUtils.DARK_NAVY);
        btnPanel.setBorder(new EmptyBorder(5, 32, 16, 32));

        JButton regBtn = new UIUtils.RoundedButton("Submit Registration", UIUtils.ACCENT_BLUE);
        regBtn.setPreferredSize(new Dimension(220, 38));
        regBtn.addActionListener(e -> doRegister());
        btnPanel.add(regBtn);

        root.add(avatarPanel, BorderLayout.NORTH);
        root.add(form,        BorderLayout.CENTER);
        root.add(btnPanel,    BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void pickAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Profile Picture");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (*.png,*.jpg,*.jpeg)", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            chosenAvatarFile = chooser.getSelectedFile();
            try {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(chosenAvatarFile);
                if (img != null) {
                    avatarPreviewLabel.setIcon(UIUtils.cropToCircle(img, 72));
                }
            } catch (Exception ignored) {}
        }
    }

    private static String generatePassword() {
        String upper   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower   = "abcdefghijklmnopqrstuvwxyz";
        String digits  = "0123456789";
        String special = "!@#$%&";
        String all     = upper + lower + digits + special;
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        // Guarantee at least one of each type
        sb.append(upper.charAt(rng.nextInt(upper.length())));
        sb.append(lower.charAt(rng.nextInt(lower.length())));
        sb.append(digits.charAt(rng.nextInt(digits.length())));
        sb.append(special.charAt(rng.nextInt(special.length())));
        // Fill remaining 8 chars randomly
        for (int i = 4; i < 12; i++) sb.append(all.charAt(rng.nextInt(all.length())));
        // Shuffle so guaranteed chars are not always at the start
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return new String(arr);
    }

    private void checkPwStrength() {
        String pwd = new String(pwF.getPassword());
        pwWarnLbl.setVisible(!pwd.isEmpty() && !isPasswordStrong(pwd));
    }

    private static boolean isPasswordStrong(String pwd) {
        if (pwd.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : pwd.toCharArray()) {
            if (Character.isUpperCase(c))      hasUpper   = true;
            else if (Character.isLowerCase(c)) hasLower   = true;
            else if (Character.isDigit(c))     hasDigit   = true;
            else                               hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private void doRegister() {
        String username = uF.getText().trim();
        String password = new String(pwF.getPassword());
        String confirm  = new String(cpwF.getPassword());
        String name     = nF.getText().trim();
        String gender   = UIUtils.resolveGender(genderBox);
        String email    = eF.getText().trim();
        String phone    = phF.getText().trim();

        // Validation
        if (username.isEmpty() || name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            msg("All fields are required.", JOptionPane.WARNING_MESSAGE); return;
        }
        if (password.isEmpty()) {
            msg("Password is required.", JOptionPane.WARNING_MESSAGE); return;
        }
        if (!password.equals(confirm)) {
            msg("Passwords do not match.", JOptionPane.WARNING_MESSAGE); return;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            msg("Invalid email format.", JOptionPane.WARNING_MESSAGE); return;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            msg("Phone must be 10\u201311 digits.", JOptionPane.WARNING_MESSAGE); return;
        }
        if (FileManager.isUsernameExists(username)) {
            msg("Username already taken.", JOptionPane.WARNING_MESSAGE); return;
        }

        // Create customer account immediately — no approval needed, vehicle added later
        String custId = FileManager.generateCustomerId();
        Customer newCustomer = new Customer(
                custId, username, password, name, email, phone, "", gender);
        FileManager.saveCustomer(newCustomer);

        // Auto-create VIP account at NONE tier (0 pts) — every customer gets VIP on registration
        if (FileManager.findVipByCustomerId(custId) == null) {
            VipAccount autoVip = VipAccount.enrol(FileManager.generateVipId(), custId, phone);
            FileManager.saveVipAccount(autoVip);
        }

        // Save avatar if chosen
        if (chosenAvatarFile != null) UIUtils.saveAvatar(custId, chosenAvatarFile);

        JOptionPane.showMessageDialog(this,
                "Registration successful!\n\nWelcome to APU-ASC, " + name + "!\n" +
                "Your Customer ID: " + custId + "\nYou can log in now.",
                "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void msg(String text, int type) {
        JOptionPane.showMessageDialog(this, text, "Validation", type);
    }

    private void addFormRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.38;
        JLabel l = new JLabel(label);
        l.setForeground(UIUtils.WHITE);
        l.setFont(UIUtils.uiFont(Font.BOLD, 12));
        p.add(l, gbc);
        gbc.gridx = 1; gbc.weightx = 0.62;
        p.add(comp, gbc);
    }
}
