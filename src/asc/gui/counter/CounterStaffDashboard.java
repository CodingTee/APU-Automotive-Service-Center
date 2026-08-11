package asc.gui.counter;

import asc.model.*;
import asc.gui.shared.LoginFrame;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CounterStaffDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    static final Color  ACCENT_YELLOW   = UIUtils.WARN_YELLOW;
    static final Color  GREEN_OK        = UIUtils.GREEN_OK;
    static final Color  PURPLE          = UIUtils.PURPLE;
    static final Color  MENU_ACTIVE     = new Color(14, 28, 58);
    static final Color  CARD2           = new Color(30, 51, 90);
    static final Color  CARD3           = new Color(20, 34, 61);
    static final String ICON_BASE       = "data/Picture/Counter Staff/";
    static final String[] TIME_SLOTS    = {"08:00","09:00","10:00","11:00","13:00","14:00","15:00","16:00"};

    final CounterStaff currentStaff;
    private JLabel  headerAvatarLabel;
    private JPanel  contentArea;
    Image homeBgImage;
    private JButton activeMenuBtn;
    JButton homeMenuBtn;
    JButton customersMenuBtn;
    JButton aptsMenuBtn;
    JButton scheduleMenuBtn;
    JButton paymentsMenuBtn;
    JButton topupMenuBtn;

    public CounterStaffDashboard(CounterStaff staff) {
        this.currentStaff = staff;
        buildUI();
    }

    private void buildUI() {
        setTitle("Counter Staff Dashboard - " + currentStaff.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 600));
        setSize(1100, 720);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIUtils.DARK_NAVY);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(UIUtils.DARK_NAVY);

        JScrollPane contentScroll = new JScrollPane(contentArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contentScroll.setBorder(null);
        contentScroll.getViewport().setBackground(UIUtils.DARK_NAVY);
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(contentScroll,  BorderLayout.CENTER);
        setContentPane(root);

        homeBgImage = UIUtils.loadBgImage(currentStaff.getUserId(), "counter");
        SwingUtilities.invokeLater(() -> { if (homeMenuBtn != null) homeMenuBtn.doClick(); });
    }

    private JPanel buildHeader() {
        JPanel p = UIUtils.createGradientHeader();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setBorder(new EmptyBorder(10, 10, 10, 22));

        // Logo — custom paintComponent panel, immune to layout squeezing
        JPanel logoPanel = UIUtils.createLogoPanel(125, 70);

        JLabel title = new JLabel("APU Automotive Service Centre");
        title.setForeground(UIUtils.WHITE);
        title.setFont(UIUtils.uiFont(Font.BOLD, 18));

        JLabel sub = new JLabel("Counter Staff Portal");
        sub.setForeground(UIUtils.LIGHT_GRAY);
        sub.setFont(UIUtils.uiFont(Font.PLAIN, 12));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        textCol.setAlignmentY(Component.CENTER_ALIGNMENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        textCol.add(title);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(sub);

        JPanel logoLeft = new JPanel();
        logoLeft.setLayout(new BoxLayout(logoLeft, BoxLayout.X_AXIS));
        logoLeft.setOpaque(false);
        logoLeft.setAlignmentY(Component.CENTER_ALIGNMENT);
        logoLeft.add(logoPanel);
        logoLeft.add(textCol);
        logoLeft.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoLeft.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showHome(); }
        });

        JPanel right = UIUtils.createHeaderRightPanel(
                currentStaff.getUserId(), currentStaff.getGender(),
                currentStaff.getName(), currentStaff.getEmail(),
                () -> asc.util.FileManager.getUnreadCount(currentStaff.getUserId()), this,
                this::openProfileDialog,
                () -> { dispose(); new LoginFrame().setVisible(true); });
        right.setOpaque(false);
        right.setAlignmentY(Component.CENTER_ALIGNMENT);
        headerAvatarLabel = UIUtils.createAvatarMenuLabel(
                currentStaff.getUserId(), currentStaff.getGender(),
                currentStaff.getName(), currentStaff.getEmail(), this,
                this::openProfileDialog,
                () -> { dispose(); new LoginFrame().setVisible(true); });

        p.add(logoLeft);
        p.add(Box.createHorizontalGlue());
        p.add(right);
        return p;
    }

    private JPanel buildSidebar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBackground(UIUtils.MEDIUM_NAVY);
        bar.setPreferredSize(new Dimension(218, 0));
        bar.setBorder(new EmptyBorder(20, 0, 20, 0));

        UIUtils.sideLabel(bar, "COUNTER STAFF MENU");

        homeMenuBtn      = UIUtils.menuBtn(bar, ICON_BASE + "Home.png",               "Home",                this::showHome, () -> activeMenuBtn);
        customersMenuBtn = UIUtils.menuBtn(bar, ICON_BASE + "Customer Management.png", "Customer Management", this::showCustomers, () -> activeMenuBtn);
        aptsMenuBtn      = UIUtils.menuBtn(bar, ICON_BASE + "Appointments.png",        "Appointments",        this::showAppointments, () -> activeMenuBtn);
        scheduleMenuBtn  = UIUtils.menuBtn(bar, ICON_BASE + "My Schedule.png",          "My Schedule",         this::showSchedule, () -> activeMenuBtn);
        topupMenuBtn     = UIUtils.menuBtn(bar, ICON_BASE + "Top-Up Wallet.png",     "Top-Up Wallet",       this::showTopup, () -> activeMenuBtn);
        paymentsMenuBtn  = UIUtils.menuBtn(bar, ICON_BASE + "Payments & Receipts.png", "Payments & Receipts", this::showPayments, () -> activeMenuBtn);

        bar.add(Box.createVerticalGlue());
        return bar;
    }

    private void swap(JPanel panel) {
        contentArea.removeAll();
        contentArea.setLayout(new BorderLayout());
        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private void navigate(JButton targetBtn, JPanel panel) {
        if (activeMenuBtn != null && activeMenuBtn != targetBtn) {
            activeMenuBtn.setBackground(UIUtils.MEDIUM_NAVY);
            activeMenuBtn.setFont(UIUtils.uiFont(Font.PLAIN, 13));
            activeMenuBtn.setBorder(new EmptyBorder(11, 14, 11, 10));
        }
        activeMenuBtn = targetBtn;
        targetBtn.setBackground(MENU_ACTIVE);
        targetBtn.setFont(UIUtils.uiFont(Font.BOLD, 13));
        targetBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, UIUtils.ACCENT_BLUE),
                new EmptyBorder(11, 10, 11, 10)));
        swap(panel);
    }

    void showHome()          { navigate(homeMenuBtn,      new CounterHomePage(this)); }
    void showCustomers()     { navigate(customersMenuBtn, new CounterCustomerPage(this)); }
    void showAppointments()  { navigate(aptsMenuBtn,      new CounterAppointmentPage(this)); }
    void showSchedule()      { navigate(scheduleMenuBtn,  new CounterSchedulePage(this)); }
    void showTopup()         { navigate(topupMenuBtn,     new CounterTopupPage(this)); }
    void showPayments()      { navigate(paymentsMenuBtn,  new CounterPaymentPage(this)); }

    void showReceiptWindow(Payment pay, Appointment apt) {
        Customer     c  = pay.getCustomer();
        Technician   t  = (apt != null) ? apt.getTechnician() : null;
        CounterStaff cs = (apt != null) ? apt.getCounterStaff() : null;

        // Paper-style panel – same approach as CounterPaymentPage provisional receipt
        Color paperBg    = new Color(252, 252, 250);
        Color textDark   = new Color(30,  30,  30);
        Color textGray   = new Color(100, 100, 100);
        Color accentBlue = new Color(30,  80, 160);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(paperBg);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));

        // ── Header (centered) ─────────────────────────────────────────────────
        p.add(rcLbl("APU AUTOMOTIVE SERVICE CENTRE", Font.BOLD, 15, textDark, paperBg));
        p.add(rcLbl("No. 12, Jalan Teknologi 1, 57000 KL", Font.PLAIN, 10, textGray, paperBg));
        p.add(rcLbl("Tel: +603-8888 9999  |  asc@apu.edu.my", Font.PLAIN, 10, textGray, paperBg));
        p.add(Box.createVerticalStrut(8));
        p.add(rcDivider(accentBlue, 2, paperBg));
        p.add(rcLbl("OFFICIAL PAYMENT RECEIPT", Font.BOLD, 13, accentBlue, paperBg));
        p.add(rcDivider(new Color(200, 200, 200), 1, paperBg));
        p.add(Box.createVerticalStrut(8));

        // ── Receipt info rows (label left, value right) ───────────────────────
        p.add(rcRow("Receipt No.",    pay.getReceiptNumber(),  textDark, textGray, paperBg));
        p.add(rcRow("Payment ID",     pay.getPaymentId(),      textDark, textGray, paperBg));
        p.add(rcRow("Payment Date",   pay.getPaymentDate(),    textDark, textGray, paperBg));
        p.add(Box.createVerticalStrut(8));
        p.add(rcDivider(new Color(200, 200, 200), 1, paperBg));
        p.add(Box.createVerticalStrut(4));

        if (apt != null) {
            p.add(rcRow("Appointment",   apt.getAppointmentId(),  textDark, textGray, paperBg));
            p.add(rcRow("Customer",      c != null ? c.getName() : apt.getCustomerId(), textDark, textGray, paperBg));
            p.add(rcRow("Technician",    t != null ? t.getName() : apt.getTechnicianId(), textDark, textGray, paperBg));
            p.add(rcRow("Counter Staff", cs != null ? cs.getName() : apt.getCounterStaffId(), textDark, textGray, paperBg));
            p.add(rcRow("Service",       apt.getServiceType() + " (" + apt.getDurationHours() + " hr(s))", textDark, textGray, paperBg));
            p.add(rcRow("Date & Time",   apt.getDate() + "  " + apt.getTime(), textDark, textGray, paperBg));
            p.add(rcRow("Vehicle",       apt.getVehicleInfo(), textDark, textGray, paperBg));
        } else {
            p.add(rcRow("Transaction",  "Wallet Top-Up", textDark, textGray, paperBg));
            p.add(rcRow("Customer",     c != null ? c.getName() : pay.getCustomerId(), textDark, textGray, paperBg));
            p.add(rcRow("Customer ID",  pay.getCustomerId(), textDark, textGray, paperBg));
        }

        p.add(Box.createVerticalStrut(8));
        p.add(rcDivider(new Color(200, 200, 200), 1, paperBg));
        p.add(Box.createVerticalStrut(4));
        p.add(rcTotalRow("Amount Paid",     String.format("RM %.2f", pay.getAmount()),   accentBlue, paperBg));
        p.add(rcRow("Payment Method",        pay.getPaymentMethod(),  textDark, textGray, paperBg));
        p.add(Box.createVerticalStrut(10));
        p.add(rcDivider(accentBlue, 1, paperBg));
        p.add(Box.createVerticalStrut(6));
        p.add(rcLbl("Thank you for choosing APU-ASC!", Font.BOLD, 11, accentBlue, paperBg));
        p.add(Box.createVerticalStrut(4));
        p.add(rcLbl("Keep this receipt for your records.", Font.ITALIC, 10, textGray, paperBg));

        JScrollPane scroll = new JScrollPane(p);
        scroll.setPreferredSize(new Dimension(440, 460));
        scroll.setBorder(new UIUtils.RoundedBorder(new Color(30, 80, 160), 10, 1));
        scroll.getViewport().setBackground(paperBg);
        scroll.setBackground(paperBg);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(accentBlue, 10, 1),
                null));
        JOptionPane.showMessageDialog(this, scroll, "Receipt - " + pay.getReceiptNumber(), JOptionPane.PLAIN_MESSAGE);
    }

    // ── Paper-receipt helpers (for showReceiptWindow) ─────────────────────────
    private static JLabel rcLbl(String text, int style, int size, Color fg, Color bg) {
        JLabel l = new JLabel(text, JLabel.CENTER);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(fg);
        l.setBackground(bg);
        l.setOpaque(true);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, size + 10));
        return l;
    }

    private static JSeparator rcDivider(Color c, int thick, Color bg) {
        JSeparator sep = new JSeparator();
        sep.setForeground(c);
        sep.setBackground(bg);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, thick + 4));
        return sep;
    }

    private static JPanel rcRow(String label, String value, Color valueFg, Color labelFg, Color bg) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(bg);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel k = new JLabel(label + ":"); k.setFont(new Font("SansSerif", Font.PLAIN, 11)); k.setForeground(labelFg);
        JLabel v = new JLabel(value, JLabel.RIGHT); v.setFont(new Font("SansSerif", Font.BOLD, 11)); v.setForeground(valueFg);
        row.add(k, BorderLayout.WEST); row.add(v, BorderLayout.EAST);
        return row;
    }

    private static JPanel rcTotalRow(String label, String value, Color valueFg, Color bg) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(bg);
        row.setBorder(new EmptyBorder(3, 0, 3, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel k = new JLabel(label + ":"); k.setFont(new Font("SansSerif", Font.BOLD, 13)); k.setForeground(new Color(60, 60, 60));
        JLabel v = new JLabel(value, JLabel.RIGHT); v.setFont(new Font("SansSerif", Font.BOLD, 15)); v.setForeground(valueFg);
        row.add(k, BorderLayout.WEST); row.add(v, BorderLayout.EAST);
        return row;
    }

    private void openProfileDialog() {
        UIUtils.showStaffProfileDialog(this, currentStaff, headerAvatarLabel,
                () -> FileManager.updateCounterStaff(currentStaff),
                "Counter Staff Dashboard - ");
    }

    JLabel sectionTitle(String text, String iconPath) {
        JLabel l;
        if (iconPath != null) {
            ImageIcon icon = UIUtils.loadMenuIcon(iconPath, 20, 20);
            l = (icon != null) ? new JLabel(text, icon, JLabel.LEFT) : new JLabel(text);
            if (icon != null) l.setIconTextGap(8);
        } else { l = new JLabel(text); }
        l.setForeground(UIUtils.ACCENT_BLUE);
        l.setFont(UIUtils.uiFont(Font.BOLD, 16));
        l.setBorder(new EmptyBorder(0, 0, 10, 0));
        return l;
    }

    JPanel makeCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD2);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1), new EmptyBorder(14, 16, 14, 16)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (title != null && !title.trim().isEmpty()) {
            JLabel t = new JLabel(title);
            t.setForeground(UIUtils.WHITE); t.setFont(UIUtils.uiFont(Font.BOLD, 14));
            t.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(t); card.add(Box.createVerticalStrut(10));
        }
        return card;
    }

    JPanel inputBox(String label, JComponent comp) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD2);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setForeground(UIUtils.LIGHT_GRAY); l.setFont(UIUtils.uiFont(Font.BOLD, 11));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l); p.add(Box.createVerticalStrut(4)); p.add(comp);
        return p;
    }

    JTextField tf() { JTextField f = new JTextField(); UIUtils.styleTextField(f); return f; }

    void darkTable(JTable t) {
        t.setBackground(CARD2); t.setForeground(UIUtils.WHITE); t.setRowHeight(28);
        t.setGridColor(new Color(19, 35, 65));
        t.getTableHeader().setBackground(new Color(43, 59, 91));
        t.getTableHeader().setForeground(UIUtils.WHITE);
    }

    DocumentListener doc(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { r.run(); }
            public void removeUpdate(DocumentEvent e)  { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }

    String safe(String s) { return s == null ? "" : s; }
    boolean customerFind(Customer c, String q) { if (c == null) return false; String k = safe(q).toLowerCase(); String all = (safe(c.getUserId())+" "+safe(c.getUsername())+" "+safe(c.getName())+" "+safe(c.getPhone())+" "+safe(c.getEmail())+" "+safe(c.getVehicleInfo())).toLowerCase(); return all.contains(k); }
    String comboId(JComboBox<String> cb) { Object o = cb.getSelectedItem(); if (o==null) return ""; String s=o.toString(); int p=s.indexOf('|'); return (p>=0?s.substring(0,p):s).trim(); }
    Customer comboCustomer(JComboBox<String> cb) { String id=comboId(cb); return id.isEmpty()?null:FileManager.findCustomerById(id); }
    void selectById(JComboBox<String> cb, String id) { if(id!=null&&!id.isEmpty()) for(int i=0;i<cb.getItemCount();i++) if(cb.getItemAt(i).startsWith(id+" ")){cb.setSelectedIndex(i);return;} if(cb.getItemCount()>0) cb.setSelectedIndex(0); }
    void msgErr(Component parent, String msg) { JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    JPanel dlgPanel() { JPanel p=new JPanel(new GridBagLayout()); p.setBackground(UIUtils.DARK_NAVY); p.setBorder(new EmptyBorder(16,28,16,28)); return p; }
    GridBagConstraints dlgGbc() { GridBagConstraints gbc=new GridBagConstraints(); gbc.fill=GridBagConstraints.HORIZONTAL; gbc.insets=new Insets(7,5,7,5); return gbc; }
    void addDlgRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) { gbc.gridy=row; gbc.gridx=0; gbc.gridwidth=1; gbc.weightx=0.38; JLabel l=new JLabel(label); l.setForeground(UIUtils.WHITE); l.setFont(UIUtils.uiFont(Font.BOLD,12)); p.add(l,gbc); gbc.gridx=1; gbc.weightx=0.62; p.add(comp,gbc); }
}
