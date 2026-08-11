package asc.gui.customer;

import asc.model.*;
import asc.gui.shared.LoginFrame;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class CustomerDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    // ── constants exposed to page classes ──────────────────────────────────
    static final Color ACCENT_YELLOW = UIUtils.WARN_YELLOW;
    static final Color GREEN_OK      = UIUtils.GREEN_OK;
    static final Color PURPLE        = UIUtils.PURPLE;
    static final Color MENU_ACTIVE   = new Color(14, 28, 58);
    static final String ICON_BASE    = "data/Picture/Customer/";

    final Customer currentCustomer;
    JLabel  headerAvatarLabel;
    JPanel  contentArea;
    JButton activeMenuBtn   = null;
    JButton homeMenuBtn     = null;
    JButton historyMenuBtn  = null;
    JButton healthMenuBtn   = null;
    JButton commentMenuBtn  = null;
    JButton receiptMenuBtn  = null;
    JButton chartMenuBtn    = null;
    JButton vipMenuBtn      = null;
    JButton topupMenuBtn    = null;
    JButton partsMenuBtn    = null;

    Image homeBgImage = null;

    // product state shared with CustomerProductsPage
    int productsActiveTab = 0;
    String productsActiveCategory = "All";
    String productsSearchKw = "";
    int productsSortMode = 0;

    public CustomerDashboard(Customer customer) {
        this.currentCustomer = customer;
        buildUI();
        // ── Check for today's appointments ────────────────────────────────
        String today = java.time.LocalDate.now().toString();
        for (asc.model.Appointment apt : asc.util.FileManager.readAllAppointments()) {
            if (apt.getCustomerId().equals(currentCustomer.getUserId())
                    && apt.getDate().equals(today)
                    && asc.model.Appointment.STATUS_SCHEDULED.equals(apt.getStatus())) {
                String msg = "You have a " + apt.getServiceType() + " service appointment today at "
                        + apt.getTime() + " (ID: " + apt.getAppointmentId() + ").";
                if (!asc.util.FileManager.notificationExistsToday(
                        currentCustomer.getUserId(),
                        asc.model.Notification.APPOINTMENT_REMINDER, msg)) {
                    asc.util.FileManager.saveNotification(new asc.model.Notification(
                            currentCustomer.getUserId(),
                            asc.model.Notification.APPOINTMENT_REMINDER,
                            "Appointment Today", msg));
                }
            }
        }
        if (currentCustomer.getVehicleList().isEmpty()) {
            SwingUtilities.invokeLater(this::promptAddVehicle);
        }
    }

    private void promptAddVehicle() {
        JPanel msg = new JPanel(new BorderLayout(0, 10));
        msg.setBackground(UIUtils.DARK_NAVY);
        msg.setBorder(new EmptyBorder(6, 6, 6, 6));
        JLabel icon = new JLabel("\uD83D\uDE97", JLabel.CENTER);
        icon.setFont(new Font("Dialog", Font.PLAIN, 36));
        JLabel title = new JLabel("No vehicle registered yet!", JLabel.CENTER);
        title.setFont(UIUtils.uiFont(Font.BOLD, 14)); title.setForeground(UIUtils.ACCENT_BLUE);
        JLabel sub = new JLabel("<html><center>Add your vehicle now so staff can book<br>appointments for you.</center></html>", JLabel.CENTER);
        sub.setFont(UIUtils.uiFont(Font.PLAIN, 12)); sub.setForeground(UIUtils.LIGHT_GRAY);
        JPanel centre = new JPanel(); centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS)); centre.setBackground(UIUtils.DARK_NAVY);
        icon.setAlignmentX(CENTER_ALIGNMENT); title.setAlignmentX(CENTER_ALIGNMENT); sub.setAlignmentX(CENTER_ALIGNMENT);
        centre.add(icon); centre.add(Box.createVerticalStrut(6)); centre.add(title); centre.add(Box.createVerticalStrut(4)); centre.add(sub);
        msg.add(centre, BorderLayout.CENTER);
        String[] options = {"Add Vehicle Now", "Maybe Later"};
        int choice = JOptionPane.showOptionDialog(this, msg, "Welcome to APU-ASC!", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 0) openVehicleDialog();
    }

    private void buildUI() {
        setTitle("Customer Dashboard \u2013 " + currentCustomer.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 580));
        setSize(1080, 720); // fallback before maximize
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
        homeBgImage = UIUtils.loadBgImage(currentCustomer.getUserId(), "customer");
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
        JLabel sub = new JLabel("Customer Portal");
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
                currentCustomer.getUserId(), currentCustomer.getGender(),
                currentCustomer.getName(), currentCustomer.getEmail(),
                () -> asc.util.FileManager.getUnreadCount(currentCustomer.getUserId()),
                this,
                this::openProfileDialog,
                this::openVehicleDialog,
                () -> { dispose(); new LoginFrame().setVisible(true); },
                true, // showVip = true (Customer only)
                this::openCustomerSupport); // onCustomerSupport
        right.setOpaque(false);
        right.setAlignmentY(Component.CENTER_ALIGNMENT);
        // Re-create the avatar label directly so we have a stable reference for photo refresh
        headerAvatarLabel = UIUtils.createAvatarMenuLabel(
                currentCustomer.getUserId(), currentCustomer.getGender(),
                currentCustomer.getName(), currentCustomer.getEmail(), this,
                this::openProfileDialog, this::openVehicleDialog,
                () -> { dispose(); new LoginFrame().setVisible(true); },
                true, // showVip = true (Customer only)
                this::openCustomerSupport); // onCustomerSupport

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
        UIUtils.sideLabel(bar, "CUSTOMER MENU");
        homeMenuBtn     = UIUtils.menuBtn(bar, ICON_BASE + "Home.png",            "Home",            this::showHome, () -> activeMenuBtn);
        historyMenuBtn  = UIUtils.menuBtn(bar, ICON_BASE + "Service History.png", "Service History", this::showHistory, () -> activeMenuBtn);
        healthMenuBtn   = UIUtils.menuBtn(bar, ICON_BASE + "VehicleHealth.png",   "Vehicle Health",  this::showVehicleHealth, () -> activeMenuBtn);
        {   // tint sidebar icon white
            ImageIcon wi = loadMenuIconWhite(ICON_BASE + "VehicleHealth.png", 20, 20);
            if (wi != null) healthMenuBtn.setIcon(wi);
        }
        commentMenuBtn  = UIUtils.menuBtn(bar, ICON_BASE + "Leave a Comment.png", "Leave a Comment", this::showComment, () -> activeMenuBtn);
        bar.add(Box.createVerticalStrut(12));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 90, 150));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        bar.add(sep);
        bar.add(Box.createVerticalStrut(12));
        receiptMenuBtn = UIUtils.menuBtn(bar, ICON_BASE + "Receipt & Print.png", "Receipt & Print", this::showReceipt, () -> activeMenuBtn);
        chartMenuBtn   = UIUtils.menuBtn(bar, ICON_BASE + "Spending Chart.png",  "Spending Chart",  this::showChart, () -> activeMenuBtn);
        vipMenuBtn     = UIUtils.menuBtn(bar, ICON_BASE + "VIP & Coupons.png",   "VIP & Coupons",   this::showVip, () -> activeMenuBtn);
        topupMenuBtn   = UIUtils.menuBtn(bar, ICON_BASE + "Top-Up Wallet.png",  "Top-Up Wallet",   this::showTopup, () -> activeMenuBtn);
        partsMenuBtn   = UIUtils.menuBtn(bar, ICON_BASE + "Car Parts Info.png",  "Products",        this::showCarParts, () -> activeMenuBtn);
        bar.add(Box.createVerticalGlue());
        return bar;
    }

    /** Load an icon and tint all non-transparent pixels white (for dark-sidebar visibility). */
    private ImageIcon loadMenuIconWhite(String path, int w, int h) {
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            ImageIcon orig = new ImageIcon(f.getAbsolutePath());
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
                    orig.getIconWidth(), orig.getIconHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bi.createGraphics();
            orig.paintIcon(null, g2, 0, 0);
            g2.dispose();
            for (int y = 0; y < bi.getHeight(); y++)
                for (int x = 0; x < bi.getWidth(); x++) {
                    int alpha = (bi.getRGB(x, y) >> 24) & 0xFF;
                    if (alpha > 0) bi.setRGB(x, y, (alpha << 24) | 0xFFFFFF);
                }
            return new ImageIcon(bi.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception ex) { return null; }
    }

    void swap(JPanel panel) {
        contentArea.removeAll();
        contentArea.setLayout(new BorderLayout());
        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    void navigate(JButton targetBtn, JPanel panel) {
        if (activeMenuBtn != null && activeMenuBtn != targetBtn) {
            activeMenuBtn.setBackground(UIUtils.MEDIUM_NAVY);
            activeMenuBtn.setFont(UIUtils.uiFont(Font.PLAIN, 13));
            activeMenuBtn.setBorder(new EmptyBorder(11, 14, 11, 10));
        }
        activeMenuBtn = targetBtn;
        targetBtn.setBackground(MENU_ACTIVE); targetBtn.setFont(UIUtils.uiFont(Font.BOLD, 13));
        targetBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, UIUtils.ACCENT_BLUE),
                new EmptyBorder(11, 10, 11, 10)));
        swap(panel);
    }

    void showHome()      { navigate(homeMenuBtn,     new CustomerHomePage(this)); }
    void showHistory()   { navigate(historyMenuBtn,  new CustomerHistoryPage(this)); }
    void showHistory(String highlightAptId) { navigate(historyMenuBtn, new CustomerHistoryPage(this, highlightAptId)); }
    void showVehicleHealth() { navigate(healthMenuBtn, new CustomerVehicleHealthPage(this)); }
    void showVehicleHealth(String highlightRecordId) { navigate(healthMenuBtn, new CustomerVehicleHealthPage(this, highlightRecordId)); }
    void showComment()   { navigate(commentMenuBtn,  new CustomerCommentPage(this)); }
    void showReceipt()   { navigate(receiptMenuBtn,  new CustomerReceiptPage(this)); }
    void showChart()     { navigate(chartMenuBtn,    new CustomerChartPage(this)); }
    void showVip()       { navigate(vipMenuBtn,      new CustomerVipPage(this)); }
    void showTopup()     { navigate(topupMenuBtn,    new CustomerTopupPage(this)); }
    void showCarParts()  { navigate(partsMenuBtn,    new CustomerProductsPage(this)); }

    // =========================================================================
    //  PANEL 2 - SERVICE HISTORY

    // =========================================================================
    //  PANEL 3 - LEAVE A COMMENT

    // =========================================================================
    //  PANEL 5 - RECEIPT

    // =========================================================================
    //  PANEL 6 - SPENDING CHART

    // =========================================================================
    //  PANEL 7 - VIP & COUPONS

    // =========================================================================
    //  PANEL 7b - TOP-UP WALLET

    JPanel buildFooterStat(String label, String value, Color accent) {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false);
        JLabel vl = new JLabel(value, JLabel.CENTER); vl.setFont(UIUtils.uiFont(Font.BOLD, 13)); vl.setForeground(accent); vl.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel ll = new JLabel(label, JLabel.CENTER); ll.setFont(UIUtils.uiFont(Font.PLAIN, 10)); ll.setForeground(UIUtils.DIM_TEXT); ll.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(vl); p.add(ll); return p;
    }

    double computeWalletBalance() {
        double in=0, out=0;
        for (Payment p : FileManager.readAllPayments()) {
            if (!p.getCustomerId().equals(currentCustomer.getUserId())) continue;
            if (p.getAppointmentId().startsWith("TOPUP-")) in += p.getAmount();
            else if ("Use Balance".equalsIgnoreCase(p.getPaymentMethod())) out += p.getAmount();
        }
        return in - out;
    }

    JPanel buildTierProgressBar(VipAccount vip) {
        double ratio = vip.tierProgressRatio(); int next = vip.nextTierThreshold(); Color barColor = vip.tierColor();
        JPanel wrapper = new JPanel(); wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS)); wrapper.setOpaque(false); wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        String ptsLabel = VipAccount.TIER_BLACKGOLD.equals(vip.getTier()) ? "MAX TIER REACHED" : String.format("%,d / %,d pts  (%,d more to reach %s)", vip.getPoints(), next, vip.pointsToNextTier(), vip.nextTierName());
        JLabel ptsLbl = new JLabel(ptsLabel); ptsLbl.setFont(UIUtils.uiFont(Font.BOLD, 12)); ptsLbl.setForeground(barColor);
        JPanel track = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30,45,80)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                int filled = (int)(getWidth() * Math.min(ratio, 1.0));
                if (filled > 0) { g2.setColor(barColor); g2.fillRoundRect(0, 0, filled, getHeight(), 8, 8); }
                String pct = VipAccount.TIER_BLACKGOLD.equals(vip.getTier()) ? "100%" : String.format("%.0f%%", ratio * 100);
                g2.setFont(UIUtils.uiFont(Font.BOLD, 11)); g2.setColor(Color.WHITE); FontMetrics fm = g2.getFontMetrics();
                g2.drawString(pct, (getWidth()-fm.stringWidth(pct))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2); g2.dispose();
            }
        };
        track.setOpaque(false); track.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        wrapper.add(ptsLbl); wrapper.add(Box.createVerticalStrut(4)); wrapper.add(track);
        return wrapper;
    }

    JPanel buildTierBenefitsRow(VipAccount vip) {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0)); row.setBackground(UIUtils.DARK_NAVY);
        String[] tiers = {VipAccount.TIER_NONE, VipAccount.TIER_BRONZE, VipAccount.TIER_GOLD, VipAccount.TIER_BLACKGOLD};
        String[] labels = {"NONE","BRONZE","GOLD","BLACKGOLD"};
        String[] discInfo = {"No discount","No % discount","2% off services","5% off services"};
        String[] coupInfo = {"No coupon","RM 20 / month","RM 20 / month","RM 50 / month"};
        int[] pts = {0, VipAccount.THRESHOLD_BRONZE, VipAccount.THRESHOLD_GOLD, VipAccount.THRESHOLD_BLACKGOLD};
        Color[] colors = {new Color(150,170,200), new Color(205,127,50), new Color(255,200,60), new Color(255,215,0)};
        for (int i = 0; i < 4; i++) {
            boolean current = vip.getTier().equals(tiers[i]); Color accent = current ? colors[i] : UIUtils.DIM_TEXT;
            JPanel card = new JPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(current ? new Color(36,58,102) : new Color(26,40,70));
            card.setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(accent, 10, current ? 2 : 1), new EmptyBorder(10, 12, 10, 12)));
            if (current) { JLabel cur = new JLabel("Current Tier"); cur.setFont(UIUtils.uiFont(Font.BOLD, 10)); cur.setForeground(accent); card.add(cur); card.add(Box.createVerticalStrut(3)); }
            JLabel tl = new JLabel(labels[i]); tl.setFont(UIUtils.uiFont(Font.BOLD, 12)); tl.setForeground(accent);
            JLabel dl = new JLabel(discInfo[i]); dl.setFont(UIUtils.uiFont(Font.PLAIN, 10)); dl.setForeground(UIUtils.WHITE);
            JLabel cl = new JLabel(coupInfo[i]); cl.setFont(UIUtils.uiFont(Font.PLAIN, 10)); cl.setForeground(UIUtils.LIGHT_GRAY);
            String thrTxt = pts[i] == 0 ? "Starting tier" : (pts[i] >= 1000 ? (pts[i]/1000)+"K" : String.valueOf(pts[i])) + "+ pts";
            JLabel thr = new JLabel(thrTxt); thr.setFont(UIUtils.uiFont(Font.ITALIC, 10)); thr.setForeground(UIUtils.DIM_TEXT);
            card.add(tl); card.add(Box.createVerticalStrut(4)); card.add(dl); card.add(cl); card.add(thr);
            row.add(card);
        }
        return row;
    }

    JPanel buildHealthRecordsSection() {
        JPanel section = new JPanel(new BorderLayout(0, 6)); section.setBackground(UIUtils.DARK_NAVY);
        JLabel hdr = new JLabel("\uD83D\uDCC2  Vehicle Health Records (VIP Exclusive)"); hdr.setFont(UIUtils.uiSymbolFont(Font.BOLD, 14)); hdr.setForeground(UIUtils.ACCENT_BLUE);
        section.add(hdr, BorderLayout.NORTH);
        List<VehicleHealthRecord> records = FileManager.findHealthRecordsByCustomerId(currentCustomer.getUserId());
        if (records.isEmpty()) { JLabel none = new JLabel("No vehicle health records yet.", JLabel.LEFT); none.setForeground(UIUtils.DIM_TEXT); none.setFont(UIUtils.uiFont(Font.ITALIC, 11)); section.add(none, BorderLayout.CENTER); return section; }
        String[] cols = {"Record ID","Date","Appointment","Tyre Pressure","Brake (mm)","Oil Type","Spark Plug","Mileage","Notes"};
        DefaultTableModel dm = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        for (VehicleHealthRecord r : records) dm.addRow(new Object[]{r.getRecordId(), r.getRecordDate(), r.getAppointmentId(), r.getTirePressure(), r.getBrakeThickness(), r.getOilType(), r.getSparkPlugStatus(), r.getMileage()+" km", r.getNotes()});
        JTable table = UIUtils.createStyledTable(dm); table.setRowHeight(28);
        JScrollPane scroll = new JScrollPane(table); scroll.setPreferredSize(new Dimension(-1, 160)); UIUtils.styleScrollPane(scroll);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    // =========================================================================
    //  PANEL 8 - PRODUCTS


    JPanel buildServicesContent() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14)); wrapper.setBackground(UIUtils.DARK_NAVY); wrapper.setBorder(new EmptyBorder(14, 0, 0, 0));
        ServicePrice sp = FileManager.readPrices(); List<CarPart> allParts = FileManager.readAllCarParts();
        JPanel cardsRow = new JPanel(new GridLayout(1, 2, 16, 0)); cardsRow.setBackground(UIUtils.DARK_NAVY);
        cardsRow.add(buildServiceCard("Normal Service","1 Hour",sp.getNormalServicePrice(),sp.getNormalDescription(),sp.getNormalIncludedPartIds(),allParts,UIUtils.ACCENT_BLUE));
        cardsRow.add(buildServiceCard("Major Service","3 Hours",sp.getMajorServicePrice(),sp.getMajorDescription(),sp.getMajorIncludedPartIds(),allParts,GREEN_OK));
        JLabel note = new JLabel("\u2139  Prices and included parts are set by the Manager. Parts not listed may incur additional charges.", JLabel.CENTER); note.setFont(UIUtils.uiSymbolFont(Font.ITALIC,11)); note.setForeground(UIUtils.DIM_TEXT);
        wrapper.add(cardsRow, BorderLayout.CENTER); wrapper.add(note, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildServiceCard(String title, String duration, double price, String description, List<String> includedIds, List<CarPart> allParts, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 0)); card.setBackground(UIUtils.TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(new Color(40,65,115),16,1), BorderFactory.createMatteBorder(0,4,0,0,accent)), new EmptyBorder(20,18,20,20)));
        JPanel hdrRow = new JPanel(new BorderLayout(0,0)); hdrRow.setBackground(UIUtils.TABLE_BG);
        JLabel titleLbl = new JLabel(title); titleLbl.setFont(UIUtils.uiFont(Font.BOLD,18)); titleLbl.setForeground(accent);
        JLabel durLbl = new JLabel("\uD83D\uDD52  "+duration); durLbl.setFont(UIUtils.uiSymbolFont(Font.PLAIN,12)); durLbl.setForeground(UIUtils.DIM_TEXT);
        JLabel priceLbl = new JLabel(String.format("RM %.2f",price)); priceLbl.setFont(UIUtils.uiFont(Font.BOLD,28)); priceLbl.setForeground(accent);
        JLabel perLbl = new JLabel("per service"); perLbl.setFont(UIUtils.uiFont(Font.PLAIN,11)); perLbl.setForeground(UIUtils.DIM_TEXT);
        JPanel leftHdr = new JPanel(); leftHdr.setLayout(new BoxLayout(leftHdr,BoxLayout.Y_AXIS)); leftHdr.setBackground(UIUtils.TABLE_BG); leftHdr.add(titleLbl); leftHdr.add(Box.createVerticalStrut(2)); leftHdr.add(durLbl);
        JPanel rightHdr = new JPanel(); rightHdr.setLayout(new BoxLayout(rightHdr,BoxLayout.Y_AXIS)); rightHdr.setBackground(UIUtils.TABLE_BG); rightHdr.add(priceLbl); rightHdr.add(perLbl);
        hdrRow.add(leftHdr, BorderLayout.WEST); hdrRow.add(rightHdr, BorderLayout.EAST);
        JPanel bodyPanel = new JPanel(); bodyPanel.setLayout(new BoxLayout(bodyPanel,BoxLayout.Y_AXIS)); bodyPanel.setBackground(UIUtils.TABLE_BG); bodyPanel.setBorder(new EmptyBorder(14,0,0,0));
        JSeparator sep = new JSeparator(); sep.setForeground(new Color(50,75,130)); sep.setMaximumSize(new Dimension(Integer.MAX_VALUE,1)); bodyPanel.add(sep); bodyPanel.add(Box.createVerticalStrut(12));
        if (description != null && !description.isBlank()) { JTextArea descArea = new JTextArea(description); descArea.setEditable(false); descArea.setLineWrap(true); descArea.setWrapStyleWord(true); descArea.setBackground(new Color(24,38,72)); descArea.setForeground(UIUtils.LIGHT_GRAY); descArea.setFont(UIUtils.uiFont(Font.ITALIC,12)); descArea.setBorder(new EmptyBorder(8,10,8,10)); descArea.setAlignmentX(Component.LEFT_ALIGNMENT); descArea.setMaximumSize(new Dimension(Integer.MAX_VALUE,72)); bodyPanel.add(descArea); bodyPanel.add(Box.createVerticalStrut(12)); }
        JLabel inclHdr = new JLabel("\u2714  Included in this service:"); inclHdr.setFont(UIUtils.uiSymbolFont(Font.BOLD,12)); inclHdr.setForeground(accent); inclHdr.setAlignmentX(Component.LEFT_ALIGNMENT); bodyPanel.add(inclHdr); bodyPanel.add(Box.createVerticalStrut(6));
        Map<String,CarPart> partMap = new java.util.LinkedHashMap<>(); for (CarPart cp : allParts) partMap.put(cp.getPartId(),cp);
        if (includedIds == null || includedIds.isEmpty()) { JLabel none = new JLabel("  No parts specified yet."); none.setFont(UIUtils.uiFont(Font.ITALIC,11)); none.setForeground(UIUtils.DIM_TEXT); none.setAlignmentX(Component.LEFT_ALIGNMENT); bodyPanel.add(none); }
        else { for (String id : includedIds) { CarPart cp = partMap.get(id); if (cp==null) continue; boolean partFree=cp.getPrice()==0.0; JPanel row=new JPanel(new BorderLayout(8,0)); row.setBackground(new Color(28,44,84)); row.setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(new Color(45,72,130),8,1),new EmptyBorder(7,12,7,12))); row.setAlignmentX(Component.LEFT_ALIGNMENT); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); JLabel nameL=new JLabel("\u2705  "+cp.getName()); nameL.setFont(UIUtils.uiSymbolFont(Font.PLAIN,12)); nameL.setForeground(UIUtils.WHITE); JPanel rp=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0)); rp.setBackground(new Color(28,44,84)); JLabel priceTag=new JLabel(partFree?"Free":"RM "+String.format("%.2f",cp.getPrice())); priceTag.setFont(UIUtils.uiFont(Font.BOLD,11)); priceTag.setForeground(partFree?GREEN_OK:new Color(160,200,255)); JLabel inclTag=new JLabel("Included"); inclTag.setFont(UIUtils.uiFont(Font.BOLD,10)); inclTag.setForeground(GREEN_OK); rp.add(priceTag); rp.add(inclTag); row.add(nameL,BorderLayout.CENTER); row.add(rp,BorderLayout.EAST); bodyPanel.add(row); bodyPanel.add(Box.createVerticalStrut(5)); } }
        long extraCount = allParts.stream().filter(cp->(includedIds==null||!includedIds.contains(cp.getPartId()))&&cp.isVisible()).count();
        if (extraCount>0) { bodyPanel.add(Box.createVerticalStrut(6)); JLabel extra=new JLabel("\u26A0  "+extraCount+" other part(s) available at additional charge \u2014 see Car Parts tab."); extra.setFont(UIUtils.uiSymbolFont(Font.ITALIC,11)); extra.setForeground(ACCENT_YELLOW); extra.setAlignmentX(Component.LEFT_ALIGNMENT); bodyPanel.add(extra); }
        JScrollPane bodyScroll=new JScrollPane(bodyPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); bodyScroll.setBorder(null); bodyScroll.getViewport().setBackground(UIUtils.TABLE_BG); bodyScroll.getVerticalScrollBar().setUnitIncrement(16); bodyScroll.getViewport().addComponentListener(new ComponentAdapter() { public void componentResized(ComponentEvent e) { bodyPanel.setPreferredSize(new Dimension(bodyScroll.getViewport().getWidth(),bodyPanel.getPreferredSize().height)); bodyPanel.revalidate(); } });
        card.add(hdrRow, BorderLayout.NORTH); card.add(bodyScroll, BorderLayout.CENTER);
        return card;
    }

    JButton makePillBtn(String label, boolean active) { JButton b = new JButton(label); b.setFont(UIUtils.uiFont(Font.PLAIN,11)); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); stylePill(b,active); return b; }

    void stylePill(JButton b, boolean active) {
        if (active) { b.setBackground(UIUtils.ACCENT_BLUE); b.setForeground(UIUtils.WHITE); b.setFont(UIUtils.uiFont(Font.BOLD,11)); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIUtils.ACCENT_BLUE,1,true),new EmptyBorder(4,12,4,12))); }
        else { b.setBackground(new Color(30,48,90)); b.setForeground(UIUtils.LIGHT_GRAY); b.setFont(UIUtils.uiFont(Font.PLAIN,11)); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(50,78,140),1,true),new EmptyBorder(4,12,4,12))); }
        b.setContentAreaFilled(false); b.setOpaque(true);
    }

    // =========================================================================
    //  VEHICLE DIALOG
    // =========================================================================

    private void openVehicleDialog() {
        JDialog dlg = new JDialog(this,"My Vehicles",true); dlg.setSize(500,520); dlg.setLocationRelativeTo(this); dlg.setResizable(false);
        JPanel root = new JPanel(new BorderLayout(0,14)); root.setBackground(UIUtils.DARK_NAVY); root.setBorder(new EmptyBorder(20,20,16,20));
        JLabel titleLbl = new JLabel("\uD83D\uDE97  My Vehicles",JLabel.CENTER); titleLbl.setFont(UIUtils.uiSymbolFont(Font.BOLD,16)); titleLbl.setForeground(UIUtils.ACCENT_BLUE); root.add(titleLbl,BorderLayout.NORTH);
        JPanel listPanel = new JPanel(); listPanel.setLayout(new BoxLayout(listPanel,BoxLayout.Y_AXIS)); listPanel.setBackground(UIUtils.TABLE_BG); listPanel.setBorder(new EmptyBorder(8,10,8,10));
        JScrollPane listScroll = new JScrollPane(listPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); listScroll.setBorder(BorderFactory.createLineBorder(UIUtils.ACCENT_BLUE,1)); listScroll.getViewport().setBackground(UIUtils.TABLE_BG); listScroll.setPreferredSize(new Dimension(460,160));
        JPanel addCard = new JPanel(new GridBagLayout()); addCard.setBackground(UIUtils.TABLE_BG); addCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIUtils.SUCCESS_GREEN,1),new EmptyBorder(12,14,12,14)));
        GridBagConstraints ag = new GridBagConstraints(); ag.fill=GridBagConstraints.HORIZONTAL; ag.insets=new Insets(5,6,5,6);
        JLabel addTitle = new JLabel("+ Add New Vehicle"); addTitle.setFont(UIUtils.uiFont(Font.BOLD,13)); addTitle.setForeground(UIUtils.SUCCESS_GREEN); ag.gridx=0; ag.gridy=0; ag.gridwidth=2; addCard.add(addTitle,ag);
        JTextField modelF=new JTextField(); modelF.setPreferredSize(new Dimension(220,30)); UIUtils.styleTextField(modelF);
        JTextField plateF=new JTextField(); plateF.setPreferredSize(new Dimension(220,30)); UIUtils.styleTextField(plateF);
        JLabel previewLbl=new JLabel(" "); previewLbl.setFont(UIUtils.uiFont(Font.ITALIC,11)); previewLbl.setForeground(UIUtils.WARN_YELLOW); previewLbl.setHorizontalAlignment(JLabel.CENTER);
        Runnable updatePreview=()->{ String m=modelF.getText().trim(),p=plateF.getText().trim().toUpperCase(); previewLbl.setText((!m.isEmpty()||!p.isEmpty())? "\u2192 "+m+(!m.isEmpty()&&!p.isEmpty()?" ":"")+p:" "); };
        javax.swing.event.DocumentListener docL = new javax.swing.event.DocumentListener() { public void insertUpdate(javax.swing.event.DocumentEvent e){updatePreview.run();} public void removeUpdate(javax.swing.event.DocumentEvent e){updatePreview.run();} public void changedUpdate(javax.swing.event.DocumentEvent e){updatePreview.run();} };
        modelF.getDocument().addDocumentListener(docL); plateF.getDocument().addDocumentListener(docL);
        ag.gridwidth=1; ag.weightx=0.38; ag.gridx=0; ag.gridy=1; JLabel mlLbl=new JLabel("Car Model:"); mlLbl.setForeground(UIUtils.WHITE); mlLbl.setFont(UIUtils.uiFont(Font.BOLD,12)); addCard.add(mlLbl,ag); ag.gridx=1; ag.weightx=0.62; addCard.add(modelF,ag);
        ag.gridx=0; ag.gridy=2; ag.weightx=0.38; JLabel plLbl=new JLabel("Plate Number:"); plLbl.setForeground(UIUtils.WHITE); plLbl.setFont(UIUtils.uiFont(Font.BOLD,12)); addCard.add(plLbl,ag); ag.gridx=1; ag.weightx=0.62; addCard.add(plateF,ag);
        ag.gridx=0; ag.gridy=3; ag.gridwidth=2; ag.weightx=1.0; addCard.add(previewLbl,ag);
        UIUtils.RoundedButton addBtn=new UIUtils.RoundedButton("Add Vehicle",UIUtils.SUCCESS_GREEN); addBtn.setPreferredSize(new Dimension(150,34));
        ag.gridy=4; ag.insets=new Insets(8,6,4,6); JPanel addBtnRow=new JPanel(new FlowLayout(FlowLayout.CENTER,0,0)); addBtnRow.setBackground(UIUtils.TABLE_BG); addBtnRow.add(addBtn); addCard.add(addBtnRow,ag);
        Runnable[] rebuildRef={null};
        rebuildRef[0]=()->{
            listPanel.removeAll();
            java.util.List<String> vehicles=currentCustomer.getVehicleList();
            if(vehicles.isEmpty()){JLabel none=new JLabel("No vehicles registered.",JLabel.CENTER); none.setForeground(UIUtils.DIM_TEXT); none.setFont(UIUtils.uiFont(Font.ITALIC,12)); none.setAlignmentX(Component.CENTER_ALIGNMENT); listPanel.add(Box.createVerticalStrut(12)); listPanel.add(none);}
            else{for(int i=0;i<vehicles.size();i++){final String veh=vehicles.get(i); final boolean isPrimary=(i==0); JPanel row=new JPanel(new BorderLayout(10,0)); row.setOpaque(true); row.setBackground(isPrimary?new Color(30,58,106):new Color(38,54,84)); row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,isPrimary?3:1,0,0,isPrimary?UIUtils.ACCENT_BLUE:new Color(60,85,130)),new EmptyBorder(9,12,9,10))); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,46)); JLabel vLbl=new JLabel((isPrimary?"\u2605  ":"      ")+veh); vLbl.setFont(UIUtils.uiSymbolFont(isPrimary?Font.BOLD:Font.PLAIN,13)); vLbl.setForeground(isPrimary?UIUtils.ACCENT_BLUE:UIUtils.WHITE); JPanel btnsPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0)); btnsPanel.setOpaque(false); if(!isPrimary){UIUtils.RoundedButton sp2=new UIUtils.RoundedButton("Set Primary",new Color(50,100,180)); sp2.setFont(UIUtils.uiFont(Font.BOLD,11)); sp2.setPreferredSize(new Dimension(100,28)); sp2.addActionListener(ev->{currentCustomer.removeVehicle(veh);java.util.List<String> updated=currentCustomer.getVehicleList();updated.add(0,veh);currentCustomer.setVehicleInfo(String.join("||",updated));FileManager.updateCustomer(currentCustomer);rebuildRef[0].run();}); btnsPanel.add(sp2);} UIUtils.RoundedButton delBtn=new UIUtils.RoundedButton("Remove",UIUtils.ERROR_RED); delBtn.setFont(UIUtils.uiFont(Font.BOLD,11)); delBtn.setPreferredSize(new Dimension(82,28)); delBtn.addActionListener(ev->{if(vehicles.size()==1){JOptionPane.showMessageDialog(dlg,"You must have at least one vehicle.","Cannot Remove",JOptionPane.WARNING_MESSAGE);return;}if(JOptionPane.showConfirmDialog(dlg,"Remove \""+veh+"\"?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){currentCustomer.removeVehicle(veh);FileManager.updateCustomer(currentCustomer);rebuildRef[0].run();}}); btnsPanel.add(delBtn); row.add(vLbl,BorderLayout.CENTER); row.add(btnsPanel,BorderLayout.EAST); listPanel.add(row); listPanel.add(Box.createVerticalStrut(5));}}
            listPanel.revalidate(); listPanel.repaint();
        };
        rebuildRef[0].run();
        addBtn.addActionListener(e->{String model=modelF.getText().trim(),plate=plateF.getText().trim().toUpperCase(); if(model.isEmpty()||plate.isEmpty()){JOptionPane.showMessageDialog(dlg,"Enter both car model and plate number.","Validation",JOptionPane.WARNING_MESSAGE);return;} String combined=model+" "+plate; if(currentCustomer.getVehicleList().contains(combined)){JOptionPane.showMessageDialog(dlg,"This vehicle is already registered.","Duplicate",JOptionPane.WARNING_MESSAGE);return;} currentCustomer.addVehicle(combined); FileManager.updateCustomer(currentCustomer); modelF.setText(""); plateF.setText(""); previewLbl.setText(" "); rebuildRef[0].run();});
        JLabel hint=new JLabel("\u2605 = Primary vehicle  |  Use 'Set Primary' to reorder",JLabel.CENTER); hint.setFont(UIUtils.uiSymbolFont(Font.ITALIC,10)); hint.setForeground(UIUtils.DIM_TEXT);
        JPanel centre=new JPanel(new BorderLayout(0,10)); centre.setBackground(UIUtils.DARK_NAVY); centre.add(listScroll,BorderLayout.CENTER); centre.add(addCard,BorderLayout.SOUTH);
        root.add(centre,BorderLayout.CENTER); root.add(hint,BorderLayout.SOUTH);
        dlg.setContentPane(root); dlg.setVisible(true);
        // refresh header avatar & go home instead of full rebuild (avoids flash)
        headerAvatarLabel.setIcon(UIUtils.loadCircularAvatar(
                currentCustomer.getUserId(), currentCustomer.getGender(), 48));
        showHome();
    }

    // =========================================================================
    //  CUSTOMER SUPPORT DIALOG
    // =========================================================================

    private void openCustomerSupport() {
        CustomerSupportDialog dlg = new CustomerSupportDialog(this, currentCustomer);
        dlg.setVisible(true);
    }

    // =========================================================================
    //  PROFILE DIALOG
    // =========================================================================

    private void openProfileDialog() {
        JDialog dlg = new JDialog(this,"My Profile",true); dlg.setSize(490,640); dlg.setLocationRelativeTo(this); dlg.setResizable(false);
        JPanel outer = new JPanel(new GridBagLayout()); outer.setBackground(UIUtils.DARK_NAVY);
        JPanel formCard = new JPanel(new GridBagLayout()); formCard.setBackground(UIUtils.TABLE_BG);
        formCard.setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE,14,1),new EmptyBorder(20,50,25,50)));
        GridBagConstraints g = new GridBagConstraints(); g.fill=GridBagConstraints.HORIZONTAL; g.insets=new Insets(7,5,7,5);
        JLabel titleLbl=new JLabel("Edit My Profile",JLabel.CENTER); titleLbl.setFont(UIUtils.uiFont(Font.BOLD,16)); titleLbl.setForeground(UIUtils.ACCENT_BLUE); g.gridx=0; g.gridy=0; g.gridwidth=2; formCard.add(titleLbl,g);
        JLabel avatar=UIUtils.createProfileAvatarLabel(currentCustomer.getUserId(),currentCustomer.getGender(),90,dlg,()->headerAvatarLabel.setIcon(UIUtils.loadCircularAvatar(currentCustomer.getUserId(),currentCustomer.getGender(),48)));
        JLabel hint=new JLabel("Click photo to change",JLabel.CENTER); hint.setForeground(UIUtils.DIM_TEXT); hint.setFont(UIUtils.uiFont(Font.ITALIC,11));
        JPanel avSec=new JPanel(new BorderLayout(0,4)); avSec.setBackground(UIUtils.TABLE_BG); avSec.add(avatar,BorderLayout.CENTER); avSec.add(hint,BorderLayout.SOUTH);
        g.gridy=1; formCard.add(avSec,g); g.gridwidth=1;
        JTextField usernameF=new JTextField(currentCustomer.getUsername()); UIUtils.styleTextField(usernameF);
        JTextField nameF=new JTextField(currentCustomer.getName()); UIUtils.styleTextField(nameF);
        JComboBox<String> genderCb=UIUtils.createGenderCombo(currentCustomer.getGender());
        JTextField emailF=new JTextField(currentCustomer.getEmail()); UIUtils.styleTextField(emailF);
        JTextField phoneF=new JTextField(currentCustomer.getPhone()); UIUtils.styleTextField(phoneF);
        JTextField vehicleF=new JTextField(String.join(", ", currentCustomer.getVehicleList())); UIUtils.styleTextField(vehicleF);
        vehicleF.setEditable(false); vehicleF.setBackground(new Color(30,48,90));
        JPanel vehRow = new JPanel(new BorderLayout(6, 0)); vehRow.setBackground(UIUtils.TABLE_BG);
        vehRow.add(vehicleF, BorderLayout.CENTER);
        JButton manageVehBtn = new JButton("Manage Vehicles");
        manageVehBtn.setBackground(UIUtils.ACCENT_BLUE); manageVehBtn.setForeground(UIUtils.WHITE);
        manageVehBtn.setFont(UIUtils.uiFont(Font.BOLD, 11)); manageVehBtn.setFocusPainted(false);
        manageVehBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        manageVehBtn.addActionListener(e -> { dlg.dispose(); openVehicleDialog(); });
        vehRow.add(manageVehBtn, BorderLayout.EAST);
        JPasswordField pwF=new JPasswordField(); UIUtils.styleTextField(pwF);
        JPasswordField cpwF=new JPasswordField(); UIUtils.styleTextField(cpwF);
        profileRow(formCard,g,2,"Username:",usernameF); profileRow(formCard,g,3,"Full Name:",nameF); profileRow(formCard,g,4,"Gender:",genderCb);
        profileRow(formCard,g,5,"Email:",emailF); profileRow(formCard,g,6,"Phone:",phoneF); profileRow(formCard,g,7,"Vehicle:",vehicleF);
        profileRow(formCard,g,8,"New Password:",pwF); profileRow(formCard,g,9,"Confirm Password:",cpwF);
        UIUtils.RoundedButton save=new UIUtils.RoundedButton("Save Profile",UIUtils.SUCCESS_GREEN); save.setPreferredSize(new Dimension(200,36));
        save.addActionListener(e->{
            String u=usernameF.getText().trim(),n=nameF.getText().trim(),ge=UIUtils.resolveGender(genderCb),em=emailF.getText().trim(),ph=phoneF.getText().trim();
            String pw=new String(pwF.getPassword()),cf=new String(cpwF.getPassword());
            if(u.isEmpty()||n.isEmpty()||em.isEmpty()||ph.isEmpty()){JOptionPane.showMessageDialog(dlg,"Required fields cannot be empty.","Error",JOptionPane.ERROR_MESSAGE);return;}
            if(!ValidationUtils.isValidEmail(em)){JOptionPane.showMessageDialog(dlg,"Invalid email.","Error",JOptionPane.ERROR_MESSAGE);return;}
            if(!ValidationUtils.isValidPhone(ph)){JOptionPane.showMessageDialog(dlg,"Phone must be 10-11 digits.","Error",JOptionPane.ERROR_MESSAGE);return;}
            if(!pw.isEmpty()&&!pw.equals(cf)){JOptionPane.showMessageDialog(dlg,"Passwords do not match.","Error",JOptionPane.ERROR_MESSAGE);return;}
            currentCustomer.setUsername(u); currentCustomer.setName(n); currentCustomer.setGender(ge); currentCustomer.setEmail(em); currentCustomer.setPhone(ph);
            if(!pw.isEmpty()) currentCustomer.setPassword(pw);
            FileManager.updateCustomer(currentCustomer);
            setTitle("Customer Dashboard \u2013 "+currentCustomer.getName());
            headerAvatarLabel.setIcon(UIUtils.loadCircularAvatar(currentCustomer.getUserId(),ge,48));
            JOptionPane.showMessageDialog(dlg,"Profile updated!","Success",JOptionPane.INFORMATION_MESSAGE); dlg.dispose();
        });
        g.gridx=0; g.gridy=10; g.gridwidth=2; formCard.add(save,g);
        outer.add(formCard,new GridBagConstraints()); dlg.setContentPane(outer); dlg.setVisible(true);
    }

    // =========================================================================
    //  STAR RATING HELPERS
    // =========================================================================

    private static final String STAR_BRIGHT_PATH = "data/Picture/Customer/star bright.png";
    private static final String STAR_DIM_PATH    = "data/Picture/Customer/star not bright.png";

    JLabel[] makeStarLabels() {
        JLabel[] stars = new JLabel[5];
        for (int i = 0; i < 5; i++) {
            stars[i] = new JLabel(UIUtils.loadMenuIcon(STAR_BRIGHT_PATH, 22, 22));
            stars[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        return stars;
    }
    void attachStarListeners(JLabel[] stars,int[] state){for(int i=0;i<stars.length;i++){final int idx=i+1; final JLabel[] ref=stars; stars[i].addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){state[0]=idx;paintStars(ref,idx);}public void mouseEntered(MouseEvent e){paintStars(ref,idx);}public void mouseExited(MouseEvent e){paintStars(ref,state[0]);}});}}
    void paintStars(JLabel[] stars, int n) {
        ImageIcon bright = UIUtils.loadMenuIcon(STAR_BRIGHT_PATH, 22, 22);
        ImageIcon dim    = UIUtils.loadMenuIcon(STAR_DIM_PATH, 22, 22);
        for (int i = 0; i < stars.length; i++) {
            stars[i].setIcon(i < n ? bright : dim);
        }
    }
    JPanel starPanel(JLabel[] stars){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); p.setBackground(UIUtils.TABLE_BG); for(JLabel s:stars) p.add(s); return p;}

    // =========================================================================
    //  UI HELPERS
    // =========================================================================

    JLabel sectionTitle(String text, String iconPath) { JLabel l; if(iconPath!=null){ImageIcon icon=UIUtils.loadMenuIcon(iconPath,20,20); l=(icon!=null)?new JLabel(text,icon,JLabel.LEFT):new JLabel(text); if(icon!=null) l.setIconTextGap(8);}else l=new JLabel(text); l.setForeground(UIUtils.ACCENT_BLUE); l.setFont(UIUtils.uiFont(Font.BOLD,16)); l.setBorder(new EmptyBorder(0,0,12,0)); return l; }
    JLabel fieldLabel(String text){JLabel l=new JLabel(text); l.setForeground(UIUtils.WHITE); l.setFont(UIUtils.uiFont(Font.BOLD,12)); return l;}
    JPanel emptyState(String icon,String msg){JPanel p=new JPanel(new GridBagLayout()); p.setBackground(UIUtils.DARK_NAVY); JPanel inner=new JPanel(); inner.setLayout(new BoxLayout(inner,BoxLayout.Y_AXIS)); inner.setBackground(UIUtils.DARK_NAVY); JLabel iconLbl=new JLabel(icon,JLabel.CENTER); iconLbl.setFont(UIUtils.uiFont(Font.PLAIN,52)); iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT); JLabel msgLbl=new JLabel(msg,JLabel.CENTER); msgLbl.setForeground(UIUtils.DIM_TEXT); msgLbl.setFont(UIUtils.uiFont(Font.ITALIC,14)); msgLbl.setAlignmentX(Component.CENTER_ALIGNMENT); inner.add(Box.createVerticalStrut(10)); inner.add(iconLbl); inner.add(Box.createVerticalStrut(14)); inner.add(msgLbl); p.add(inner,new GridBagConstraints()); return p;}
    JPanel miniCard(String label,String value,Color accent){JPanel c=new JPanel(new GridLayout(2,1,0,4)); c.setBackground(UIUtils.TABLE_BG); c.setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(accent,10,1),new EmptyBorder(10,14,10,14))); JLabel vl=new JLabel(value,JLabel.CENTER); vl.setFont(UIUtils.uiFont(Font.BOLD,20)); vl.setForeground(accent); JLabel ll=new JLabel(label,JLabel.CENTER); ll.setFont(UIUtils.uiFont(Font.PLAIN,11)); ll.setForeground(UIUtils.DIM_TEXT); c.add(vl); c.add(ll); return c;}
    JButton makeBtn(String text,Color bg,int w,int h){UIUtils.RoundedButton b=new UIUtils.RoundedButton(text,bg); b.setPreferredSize(new Dimension(w,h)); return b;}
    JTextArea makeTextArea(){JTextArea ta=new JTextArea(4,32); ta.setLineWrap(true); ta.setWrapStyleWord(true); ta.setBackground(new Color(40,62,110)); ta.setForeground(UIUtils.WHITE); ta.setCaretColor(UIUtils.WHITE); ta.setFont(UIUtils.uiFont(Font.PLAIN,13)); ta.setBorder(new EmptyBorder(6,8,6,8)); return ta;}
    void addFormRow(JPanel card,GridBagConstraints g,String labelText,JComponent comp){g.gridx=0; g.gridwidth=1; g.weightx=0.35; card.add(fieldLabel(labelText),g); g.gridx=1; g.weightx=0.65; card.add(comp,g);}
    void profileRow(JPanel card,GridBagConstraints g,int row,String labelText,JComponent comp){g.gridy=row; g.gridx=0; g.gridwidth=1; g.weightx=0.35; card.add(fieldLabel(labelText),g); g.gridx=1; g.weightx=0.65; card.add(comp,g);}
}
