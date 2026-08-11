package asc.gui.manager;

import asc.model.*;
import asc.gui.shared.LoginFrame;
import asc.util.FileManager;
import asc.util.UIUtils;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * ManagerDashboard â€“ redesigned to match CustomerDashboard style.
 *
 * NEW FEATURES:
 *   â‘  Pending Registrations â€“ approve / reject with one click
 *   â‘¡ Car Parts Catalogue  â€“ add / edit / delete parts with image upload
 *   â‘¢ VIP & Coupon Overview (read-only; issuance done by Counter Staff)
 *   â‘£ Full Reports tab
 *   â‘¤ Feedbacks & Comments with star display
 *   â‘¥ Profile dialog + avatar
 */
public class ManagerDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    // Colours exposed to page classes (package-private)
    static final Color ACCENT_YELLOW = UIUtils.WARN_YELLOW;
    static final Color GREEN_OK      = UIUtils.GREEN_OK;
    static final Color PURPLE        = UIUtils.PURPLE;
    static final Color MENU_ACTIVE   = new Color(14, 28, 58);
    static final String MGR_ICON_BASE = "data/Picture/Manager/";

    final Manager currentManager;
    JLabel  headerAvatarLabel;
    JPanel  contentArea;

    // Home background (persistent - loaded from data/Picture/manager/)
    Image  homeBgImage     = null;
    JButton activeMenuBtn    = null;
    JButton homeMenuBtn      = null;
    JButton userMenuBtn      = null;
    JButton partsMenuBtn     = null;
    JButton pricesMenuBtn    = null;
    JButton fbMenuBtn        = null;
    JButton reportMenuBtn    = null;
    JButton workDaysMenuBtn  = null;

    // table models kept as fields for refresh
    DefaultTableModel feedbackModel, commentModel;
    JTable feedbackTable, commentTable;
    JTextPane reportsArea;

    public ManagerDashboard(Manager manager) {
        this.currentManager = manager;
        buildUI();
    }

    // =========================================================================
    //  MAIN FRAME
    // =========================================================================

    private void buildUI() {
        setTitle("Manager Dashboard â€“ " + currentManager.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(920, 580));
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

        // Load persisted background on startup
        homeBgImage = UIUtils.loadBgImage(currentManager.getUserId(), "manager");

        SwingUtilities.invokeLater(() -> { if (homeMenuBtn != null) homeMenuBtn.doClick(); });
    }

    // =========================================================================
    //  HEADER
    // =========================================================================

    private JPanel buildHeader() {
        JPanel p = UIUtils.createGradientHeader();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setBorder(new EmptyBorder(10, 10, 10, 22));

        // Logo — custom paintComponent panel, immune to layout squeezing
        JPanel logoPanel = UIUtils.createLogoPanel(125, 70);

        JLabel title = new JLabel("APU Automotive Service Centre");
        title.setForeground(UIUtils.WHITE);
        title.setFont(UIUtils.uiFont(Font.BOLD, 18));

        JLabel sub = new JLabel("Manager Portal");
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
                currentManager.getUserId(), currentManager.getGender(),
                currentManager.getName(), currentManager.getEmail(),
                () -> asc.util.FileManager.getUnreadCount(currentManager.getUserId()), this,
                this::openProfileDialog,
                () -> { dispose(); new LoginFrame().setVisible(true); });
        right.setOpaque(false);
        right.setAlignmentY(Component.CENTER_ALIGNMENT);
        headerAvatarLabel = UIUtils.createAvatarMenuLabel(
                currentManager.getUserId(), currentManager.getGender(),
                currentManager.getName(), currentManager.getEmail(), this,
                this::openProfileDialog,
                () -> { dispose(); new LoginFrame().setVisible(true); });

        p.add(logoLeft);
        p.add(Box.createHorizontalGlue());
        p.add(right);
        return p;
    }

    // =========================================================================
    //  SIDEBAR
    // =========================================================================


    private JPanel buildSidebar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBackground(UIUtils.MEDIUM_NAVY);
        bar.setPreferredSize(new Dimension(226, 0));
        bar.setBorder(new EmptyBorder(20, 0, 20, 0));

        UIUtils.sideLabel(bar, "MANAGER MENU");

        homeMenuBtn     = UIUtils.menuBtn(bar, MGR_ICON_BASE+"Home.png",                 "Home",                this::showHome, () -> activeMenuBtn);
        userMenuBtn     = UIUtils.menuBtn(bar, MGR_ICON_BASE+"User Management.png",      "User Management",     this::showUserMgmt, () -> activeMenuBtn);
        partsMenuBtn    = UIUtils.menuBtn(bar, MGR_ICON_BASE+"Car Parts Catalogue.png",  "Car Parts Catalogue", this::showParts, () -> activeMenuBtn);
        pricesMenuBtn   = UIUtils.menuBtn(bar, MGR_ICON_BASE+"Set Prices.png",           "Set Prices",          this::showPrices, () -> activeMenuBtn);
        fbMenuBtn       = UIUtils.menuBtn(bar, MGR_ICON_BASE+"Feedbacks & Comments.png", "Feedbacks & Comments",this::showFeedbacks, () -> activeMenuBtn);
        reportMenuBtn   = UIUtils.menuBtn(bar, MGR_ICON_BASE+"Reports.png",              "Reports",             this::showReports, () -> activeMenuBtn);
        workDaysMenuBtn = UIUtils.menuBtn(bar, MGR_ICON_BASE+"Employee Scheduling.png", "Employee Scheduling", this::showEmployeeScheduling, () -> activeMenuBtn);

        bar.add(Box.createVerticalGlue());
        return bar;
    }

    /**
     * Highlights the given sidebar button as active and swaps the centre
     * content panel to the provided panel.
     * FIX: Previously declared as navigate(JButton, Runnable) but callers
     * passed a JPanel â€“ corrected to navigate(JButton, JPanel) and the
     * panel-swap logic (formerly in the dead swap() method) is now inlined here.
     */
    void navigate(JButton btn, JPanel panel) {
        if (activeMenuBtn != null && activeMenuBtn != btn) {
            activeMenuBtn.setBackground(UIUtils.MEDIUM_NAVY);
            activeMenuBtn.setFont(UIUtils.uiFont(Font.PLAIN, 13));
            activeMenuBtn.setBorder(new EmptyBorder(11, 14, 11, 10));
        }
        activeMenuBtn = btn;
        btn.setBackground(MENU_ACTIVE);
        btn.setFont(UIUtils.uiFont(Font.BOLD, 13));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, UIUtils.ACCENT_BLUE),
                new EmptyBorder(11, 10, 11, 10)));
        contentArea.removeAll();
        contentArea.setLayout(new BorderLayout());
        contentArea.add(panel, BorderLayout.CENTER);
        // All pages share the same outer scroll policy
        if (contentArea.getParent() instanceof JViewport) {
            JScrollPane outer = (JScrollPane) contentArea.getParent().getParent();
            outer.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            outer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }
        contentArea.revalidate();
        contentArea.repaint();
    }

    void showHome()      { navigate(homeMenuBtn,     new ManagerHomePage(this)); }
    void showUserMgmt()  { navigate(userMenuBtn,     new ManagerUserMgmtPage(this)); }
    void showParts()     { navigate(partsMenuBtn,    new ManagerPartsPage(this)); }
    void showPrices()    { navigate(pricesMenuBtn,   new ManagerPricesPage(this)); }
    void showFeedbacks() { navigate(fbMenuBtn,       new ManagerFeedbacksPage(this)); }
    void showReports()   { navigate(reportMenuBtn,   new ManagerReportsPage(this)); }
    void showEmployeeScheduling()  { navigate(workDaysMenuBtn, new ManagerEmployeeSchedulingPage(this)); }

    // =========================================================================
    //  PANEL 3 â€" USER MANAGEMENT


    void fillUserTable(DefaultTableModel dm, String role) {
        dm.setRowCount(0);
        switch (role) {
            case "MANAGER":
                for (Manager m : FileManager.readAllManagers())
                    dm.addRow(new Object[]{m.getUserId(),m.getUsername(),m.getName(),m.getGender(),m.getEmail(),m.getPhone()});
                break;
            case "COUNTER_STAFF":
                for (CounterStaff cs : FileManager.readAllCounterStaff())
                    dm.addRow(new Object[]{cs.getUserId(),cs.getUsername(),cs.getName(),cs.getGender(),cs.getEmail(),cs.getPhone()});
                break;
            case "TECHNICIAN":
                for (Technician t : FileManager.readAllTechnicians())
                    dm.addRow(new Object[]{t.getUserId(),t.getUsername(),t.getName(),t.getGender(),t.getEmail(),t.getPhone()});
                break;
            case "CUSTOMER":
                for (Customer c : FileManager.readAllCustomers())
                    dm.addRow(new Object[]{c.getUserId(),c.getUsername(),c.getName(),c.getGender(),c.getEmail(),c.getPhone(),
                            String.join(", ", c.getVehicleList())});
                break;
        }
    }




    // =========================================================================
    //  PANEL 4 - CAR PARTS CATALOGUE


    /** View Details dialog â€” shows all fields + image, with Edit button inside */

    void detailRow(JPanel p, GridBagConstraints g, int row, String label, String value) {
        g.gridy = row; g.gridx = 0; g.gridwidth = 1; g.weightx = 0.35;
        JLabel lbl = new JLabel(label); lbl.setFont(UIUtils.uiFont(Font.BOLD, 12)); lbl.setForeground(UIUtils.ACCENT_BLUE);
        p.add(lbl, g);
        g.gridx = 1; g.weightx = 0.65;
        JLabel val = new JLabel(value); val.setFont(UIUtils.uiFont(Font.PLAIN, 12)); val.setForeground(UIUtils.WHITE);
        p.add(val, g);
    }

    void loadPartImageInto(JLabel lbl, String imgUrl, int w, int h) {
        if (imgUrl != null && !imgUrl.isEmpty()) {
            // Prefer _thumb version for fast loading
            String thumbUrl = UIUtils.thumbPath(imgUrl);
            File f = new File(thumbUrl);
            if (!f.exists()) f = new File(imgUrl);
            if (f.exists()) {
                Image img = new ImageIcon(f.getAbsolutePath()).getImage()
                        .getScaledInstance(w, h, Image.SCALE_SMOOTH);
                lbl.setIcon(new ImageIcon(img));
                lbl.setText("");
                return;
            }
        }
        // No image found — show a symbol placeholder
        lbl.setIcon(null);
        lbl.setText("\u2699");   // ⚙ gear symbol
        lbl.setFont(UIUtils.uiSymbolFont(Font.PLAIN, Math.min(w, h) * 2 / 3));
        lbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl.setForeground(UIUtils.DIM_TEXT);
    }


    /** Edit dialog â€” opened from View Details dialog */



    // =========================================================================
    //  PANEL 5 â€“ SET PRICES  (price + description + included parts checkboxes)

    /** One service config card (Normal or Major). */

    JTextArea styledTextArea(String text, int rows) {
        JTextArea ta = new JTextArea(text, rows, 20);
        ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setBackground(new Color(40, 62, 110));
        ta.setForeground(UIUtils.WHITE);
        ta.setCaretColor(UIUtils.WHITE);
        ta.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        ta.setBorder(new EmptyBorder(6, 8, 6, 8));
        return ta;
    }

    JCheckBox styledCheckBox(String label, boolean selected) {
        JCheckBox cb = new JCheckBox(label, selected);
        cb.setBackground(new Color(28, 44, 80));
        cb.setForeground(UIUtils.WHITE);
        cb.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        cb.setFocusPainted(false);
        cb.setBorder(new EmptyBorder(3, 2, 3, 2));
        return cb;
    }

    // =========================================================================
    //  PANEL 6 â€“ FEEDBACKS & COMMENTS

    void refreshFeedbacks() {
        if(feedbackModel==null)return;
        feedbackModel.setRowCount(0);
        for(Feedback f:FileManager.readAllFeedbacks()){
            Technician t=f.getTechnician();
            feedbackModel.addRow(new Object[]{f.getFeedbackId(),f.getAppointmentId(),f.getTechnicianId(),(t!=null?t.getName():f.getTechnicianId()),f.getContent(),f.getFeedbackDate(),"View Details"});
        }
    }

    void refreshComments() {
        if(commentModel==null)return;
        commentModel.setRowCount(0);
        for(Comment c:FileManager.readAllComments()){
            Customer cu=c.getCustomer();
            commentModel.addRow(new Object[]{
                c.getCommentId(),c.getAppointmentId(),(cu!=null?cu.getName():c.getCustomerId()),
                c.getCounterStaffComment(), c.getCsStarDisplay(),
                c.getTechnicianComment(),   c.getTechStarDisplay(),
                c.getCommentDate(),         "View Details"
            });
        }
    }

    // =========================================================================
    //  PANEL 7 â€“ REPORTS  (date-range filter + PDF export)

    /**
     * Generates a report filtered to the given yyyy-MM month range (inclusive).
     * E.g. from="2025-09" to="2025-11" covers Sep, Oct, Nov 2025.
     */
    void generateFilteredReport(String fromMonth, String toMonth) {
        List<asc.model.Appointment> allApts = FileManager.readAllAppointments();
        List<Payment>               allPays = FileManager.readAllPayments();
        List<Technician>            techs   = FileManager.readAllTechnicians();
        ServicePrice                pr      = FileManager.readPrices();

        // Filter appointments whose date falls within [fromMonth, toMonth]
        List<asc.model.Appointment> apts = allApts.stream()
                .filter(a -> {
                    if (a.getDate() == null || a.getDate().length() < 7) return false;
                    String m = a.getDate().substring(0, 7);
                    return m.compareTo(fromMonth) >= 0 && m.compareTo(toMonth) <= 0;
                })
                .collect(java.util.stream.Collectors.toList());

        // Filter payments whose date falls within range
        List<Payment> pays = allPays.stream()
                .filter(p -> {
                    if (p.getPaymentDate() == null || p.getPaymentDate().length() < 7) return false;
                    String m = p.getPaymentDate().substring(0, 7);
                    return m.compareTo(fromMonth) >= 0 && m.compareTo(toMonth) <= 0;
                })
                .collect(java.util.stream.Collectors.toList());

        long pending   = apts.stream().filter(a -> "PENDING".equalsIgnoreCase(a.getStatus())).count();
        long completed = apts.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
        long nc        = apts.stream().filter(a -> "NORMAL".equalsIgnoreCase(a.getServiceType())).count();
        long mc        = apts.stream().filter(a -> "MAJOR".equalsIgnoreCase(a.getServiceType())).count();
        double total   = pays.stream().mapToDouble(Payment::getAmount).sum();
        double nr      = pays.stream().filter(p -> {
            asc.model.Appointment a = p.getAppointment();
            return a != null && "NORMAL".equalsIgnoreCase(a.getServiceType());
        }).mapToDouble(Payment::getAmount).sum();

        String rangeLabel = fromMonth.equals(toMonth)
                ? fromMonth
                : fromMonth + "  to  " + toMonth;

        int lineW = 85;
        String border = "=" .repeat(lineW);
        String dash   = "-" .repeat(lineW);

        StringBuilder sb = new StringBuilder();
        sb.append(border).append("\n");
        sb.append(centre("APU AUTOMOTIVE SERVICE CENTRE (APU-ASC)", lineW)).append("\n");
        sb.append(centre("MANAGEMENT REPORT", lineW)).append("\n");
        sb.append(centre("Period: " + rangeLabel, lineW)).append("\n");
        sb.append(border).append("\n\n");

        sb.append(dash).append("\n");
        sb.append(" APPOINTMENT STATISTICS\n");
        sb.append(dash).append("\n");
        sb.append(String.format("  Total       : %d\n", apts.size()));
        sb.append(String.format("  Pending     : %d\n", pending));
        sb.append(String.format("  Completed   : %d\n", completed));
        sb.append(String.format("  Normal (1hr): %d\n", nc));
        sb.append(String.format("  Major  (3hr): %d\n\n", mc));

        sb.append(dash).append("\n");
        sb.append(" FINANCIAL SUMMARY\n");
        sb.append(dash).append("\n");
        sb.append(String.format("  Service Prices  : Normal RM %.2f  |  Major RM %.2f\n",
                pr.getNormalServicePrice(), pr.getMajorServicePrice()));
        sb.append(String.format("  Revenue (Normal): RM %.2f\n", nr));
        sb.append(String.format("  Revenue (Major) : RM %.2f\n", total - nr));
        sb.append(String.format("  TOTAL REVENUE   : RM %.2f\n\n", total));

        // Monthly breakdown (if multi-month)
        if (!fromMonth.equals(toMonth)) {
            sb.append(dash).append("\n");
            sb.append(" MONTHLY REVENUE BREAKDOWN\n");
            sb.append(dash).append("\n");
            java.util.TreeMap<String, Double> monthly = new java.util.TreeMap<>();
            for (Payment p : pays) {
                String m = p.getPaymentDate().substring(0, 7);
                monthly.merge(m, p.getAmount(), Double::sum);
            }
            if (monthly.isEmpty()) {
                sb.append("  (No payments in this period)\n");
            } else {
                for (java.util.Map.Entry<String, Double> en : monthly.entrySet())
                    sb.append(String.format("  %s  :  RM %.2f\n", en.getKey(), en.getValue()));
            }
            sb.append("\n");
        }

        sb.append(dash).append("\n");
        sb.append(" TECHNICIAN PERFORMANCE\n");
        sb.append(dash).append("\n");
        for (Technician t : techs) {
            long tt = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId())).count();
            long tc = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId())
                    && "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
            sb.append(String.format("  %-22s : %d assigned, %d completed\n", t.getName(), tt, tc));
        }
        sb.append("\n");

        long fbCount  = FileManager.readAllFeedbacks().stream().filter(f -> {
            asc.model.Appointment a = f.getAppointment();
            if (a == null || a.getDate() == null || a.getDate().length() < 7) return false;
            String m = a.getDate().substring(0, 7);
            return m.compareTo(fromMonth) >= 0 && m.compareTo(toMonth) <= 0;
        }).count();
        long cmtCount = FileManager.readAllComments().stream().filter(c -> {
            asc.model.Appointment a = c.getAppointment();
            if (a == null || a.getDate() == null || a.getDate().length() < 7) return false;
            String m = a.getDate().substring(0, 7);
            return m.compareTo(fromMonth) >= 0 && m.compareTo(toMonth) <= 0;
        }).count();
        sb.append(String.format("  Feedbacks: %d  |  Customer Comments: %d\n", fbCount, cmtCount));
        sb.append("\n").append(border).append("\n");
        sb.append(String.format("  Report generated: %s  |  By: %s (%s)\n",
                LocalDate.now(), currentManager.getName(), currentManager.getUserId()));
        sb.append(border).append("\n");

        reportsArea.setText(sb.toString());
        reportsArea.setCaretPosition(0);
    }

    /** All-time report (no date filter). */
    void generateFullReport() {
        List<asc.model.Appointment> apts = FileManager.readAllAppointments();
        List<Payment> pays               = FileManager.readAllPayments();
        List<Technician> techs           = FileManager.readAllTechnicians();
        List<CounterStaff> staffs        = FileManager.readAllCounterStaff();
        List<Customer> custs             = FileManager.readAllCustomers();
        ServicePrice pr                  = FileManager.readPrices();

        long pending   = apts.stream().filter(a -> "PENDING".equalsIgnoreCase(a.getStatus())).count();
        long completed = apts.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
        long nc        = apts.stream().filter(a -> "NORMAL".equalsIgnoreCase(a.getServiceType())).count();
        long mc        = apts.stream().filter(a -> "MAJOR".equalsIgnoreCase(a.getServiceType())).count();
        double total   = pays.stream().mapToDouble(Payment::getAmount).sum();
        double nr      = pays.stream().filter(p -> {
            asc.model.Appointment a = p.getAppointment();
            return a != null && "NORMAL".equalsIgnoreCase(a.getServiceType());
        }).mapToDouble(Payment::getAmount).sum();
        long partsCount  = FileManager.readAllCarParts().size();

        int lineW    = 60;
        String border = "=".repeat(lineW);
        String dash   = "-".repeat(lineW);

        StringBuilder sb = new StringBuilder();
        sb.append(border).append("\n");
        sb.append(centre("APU AUTOMOTIVE SERVICE CENTRE (APU-ASC)", lineW)).append("\n");
        sb.append(centre("MANAGEMENT REPORT  â€”  ALL TIME", lineW)).append("\n");
        sb.append(border).append("\n\n");

        sb.append(dash).append("\n");
        sb.append(" SYSTEM OVERVIEW\n");
        sb.append(dash).append("\n");
        sb.append(String.format("  Managers: %d  |  Counter Staff: %d  |  Technicians: %d  |  Customers: %d\n",
                FileManager.readAllManagers().size(), staffs.size(), techs.size(), custs.size()));
        sb.append(String.format("  Car Parts: %d\n\n", partsCount));

        sb.append(dash).append("\n");
        sb.append(" APPOINTMENT STATISTICS\n");
        sb.append(dash).append("\n");
        sb.append(String.format("  Total: %d  |  Pending: %d  |  Completed: %d\n", apts.size(), pending, completed));
        sb.append(String.format("  Normal (1hr): %d  |  Major (3hr): %d\n\n", nc, mc));

        sb.append(dash).append("\n");
        sb.append(" FINANCIAL SUMMARY\n");
        sb.append(dash).append("\n");
        sb.append(String.format("  Normal Price: RM %.2f  |  Major Price: RM %.2f\n",
                pr.getNormalServicePrice(), pr.getMajorServicePrice()));
        sb.append(String.format("  Revenue (Normal): RM %.2f  |  Revenue (Major): RM %.2f\n", nr, total - nr));
        sb.append(String.format("  TOTAL REVENUE: RM %.2f\n\n", total));

        sb.append(dash).append("\n");
        sb.append(" TECHNICIAN PERFORMANCE\n");
        sb.append(dash).append("\n");
        for (Technician t : techs) {
            long tt = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId())).count();
            long tc = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId())
                    && "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
            sb.append(String.format("  %-22s : %d assigned, %d completed\n", t.getName(), tt, tc));
        }
        sb.append(String.format("\n  Feedbacks: %d  |  Customer Comments: %d\n",
                FileManager.readAllFeedbacks().size(), FileManager.readAllComments().size()));
        sb.append("\n").append(border).append("\n");
        sb.append(String.format("  Report generated: %s  |  By: %s (%s)\n",
                LocalDate.now(), currentManager.getName(), currentManager.getUserId()));
        sb.append(border).append("\n");

        reportsArea.setText(sb.toString());
        reportsArea.setCaretPosition(0);
    }

    /** Centre-pads a string within a fixed width for the report header. */
    String centre(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    /** Export current report text to a PDF file chosen by the user. */
    void exportReportToPdf() {
        if (reportsArea == null || reportsArea.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Generate a report first.", "No Report", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Report as PDF");
        fc.setSelectedFile(new java.io.File("APU_ASC_Report_" + LocalDate.now() + ".pdf"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File dest = fc.getSelectedFile();
        if (!dest.getName().toLowerCase().endsWith(".pdf"))
            dest = new java.io.File(dest.getAbsolutePath() + ".pdf");

        final java.io.File finalDest = dest;
        final String text = reportsArea.getText();

        // Use Java's built-in print machinery to produce a PDF
        // We create an in-memory printable from the text area and print to file
        try {
            // Attempt PDF via javax.print (works when a PDF print service is available)
            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            job.setJobName("APU-ASC Report");

            java.awt.print.PageFormat pf = job.defaultPage();
            java.awt.print.Paper paper = pf.getPaper();
            double margin = 36; // 0.5 inch margins
            paper.setImageableArea(margin, margin,
                    paper.getWidth() - margin * 2, paper.getHeight() - margin * 2);
            pf.setPaper(paper);

            // Printable that draws monospaced text line-by-line
            java.awt.print.Printable printable = (graphics, pageFormat, pageIndex) -> {
                String[] lines = text.split("\n", -1);
                Font font = new Font("Monospaced", Font.PLAIN, 11);
                graphics.setFont(font);
                java.awt.FontMetrics fm = graphics.getFontMetrics(font);
                int lineH   = fm.getHeight();
                int x       = (int) pageFormat.getImageableX();
                int y0      = (int) pageFormat.getImageableY();
                int pageH   = (int) pageFormat.getImageableHeight();
                int linesPerPage = pageH / lineH;
                int start   = pageIndex * linesPerPage;
                if (start >= lines.length) return java.awt.print.Printable.NO_SUCH_PAGE;
                int y = y0 + fm.getAscent();
                for (int i = start; i < Math.min(start + linesPerPage, lines.length); i++) {
                    graphics.drawString(lines[i], x, y);
                    y += lineH;
                }
                return java.awt.print.Printable.PAGE_EXISTS;
            };

            job.setPrintable(printable, pf);

            // Try to find a PDF print service
            javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(
                    javax.print.DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
            javax.print.PrintService pdfService = null;
            for (javax.print.PrintService svc : services) {
                if (svc.getName().toLowerCase().contains("pdf")) { pdfService = svc; break; }
            }

            if (pdfService != null) {
                // Use the OS PDF printer
                job.setPrintService(pdfService);
                javax.print.attribute.PrintRequestAttributeSet attrs =
                        new javax.print.attribute.HashPrintRequestAttributeSet();
                attrs.add(new javax.print.attribute.standard.Destination(finalDest.toURI()));
                job.print(attrs);
            } else {
                // Fallback: write plain-text .pdf (rename) with a header
                try (java.io.PrintWriter pw = new java.io.PrintWriter(
                        new java.io.FileWriter(finalDest))) {
                    pw.print(text);
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Report exported to:\n" + finalDest.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            // Final fallback: write as plain text with .pdf extension
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(finalDest))) {
                pw.print(text);
                JOptionPane.showMessageDialog(this,
                        "PDF printer not found. Report saved as text:\n" + finalDest.getAbsolutePath(),
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (java.io.IOException ioEx) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ioEx.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }



    // =========================================================================
    //  PROFILE DIALOG
    // =========================================================================

    private void openProfileDialog() {
        UIUtils.showStaffProfileDialog(this, currentManager, headerAvatarLabel,
                () -> FileManager.updateManager(currentManager),
                "Manager Dashboard – ");
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    /** Section title with icon (pass null iconPath to omit icon). */
    JLabel sectionTitle(String text, String iconPath) {
        JLabel l;
        if (iconPath != null) {
            ImageIcon icon = UIUtils.loadMenuIcon(iconPath, 20, 20);
            l = (icon != null) ? new JLabel(text, icon, JLabel.LEFT) : new JLabel(text);
            if (icon != null) l.setIconTextGap(8);
        } else {
            l = new JLabel(text);
        }
        l.setForeground(UIUtils.ACCENT_BLUE);
        l.setFont(UIUtils.uiFont(Font.BOLD, 16));
        l.setBorder(new EmptyBorder(0, 0, 10, 0));
        return l;
    }

    JTextField tf() { JTextField f=new JTextField(); UIUtils.styleTextField(f); return f; }

    JPanel dlgPanel() {
        JPanel p=new JPanel(new GridBagLayout()); p.setBackground(UIUtils.DARK_NAVY); p.setBorder(new EmptyBorder(16,28,16,28)); return p;
    }

    GridBagConstraints dlgGbc() {
        GridBagConstraints gbc=new GridBagConstraints(); gbc.fill=GridBagConstraints.HORIZONTAL; gbc.insets=new Insets(7,5,7,5); return gbc;
    }

    void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridy=row; gbc.gridx=0; gbc.gridwidth=1; gbc.weightx=0.38;
        JLabel l=new JLabel(label); l.setForeground(UIUtils.WHITE); l.setFont(UIUtils.uiFont(Font.BOLD,12)); p.add(l,gbc);
        gbc.gridx=1; gbc.weightx=0.62; p.add(comp,gbc);
    }

    String fmtRole(String role) {
        switch(role){
            case"MANAGER":      return"Manager";
            case"COUNTER_STAFF":return"Counter Staff";
            case"TECHNICIAN":   return"Technician";
            case"CUSTOMER":     return"Customer";
            default:            return role;
        }
    }

    void err(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
