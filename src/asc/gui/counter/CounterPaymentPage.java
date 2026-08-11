package asc.gui.counter;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;

/**
 * Counter Payment Page
 *  - Main view: two tabs (Unpaid | History) with filter search in top-right
 *  - Clicking a row opens a full checkout dialog:
 *      LEFT  = live provisional receipt (updates as coupon/method changes)
 *      RIGHT = payment controls (method, coupon, amount, complete btn)
 *  - After Complete Payment → print dialog → back to main view
 */
public class CounterPaymentPage extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CounterStaffDashboard main;

    // Table models
    private DefaultTableModel unpaidModel;
    private DefaultTableModel payModel;
    private JTable            unpaidTable;
    private JTable            payTable;

    // Filter field (shared across tabs)
    private JTextField filterField;

    public CounterPaymentPage(CounterStaffDashboard m) {
        main = m;
        setLayout(new BorderLayout(0, 8));
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(18, 18, 14, 18));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
        refreshAll();
    }

    // =========================================================================
    //  PAGE HEADER  –  title + filter search (top-right)
    // =========================================================================

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(UIUtils.DARK_NAVY);

        JLabel title = main.sectionTitle("Payments & Receipts",
                CounterStaffDashboard.ICON_BASE + "Payments & Receipts(Blue).png");
        title.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Filter bar – top-right
        filterField = main.tf();
        filterField.setPreferredSize(new Dimension(230, 30));
        JLabel hint = new JLabel("\uD83D\uDD0D");
        hint.setForeground(UIUtils.ACCENT_BLUE);
        hint.setFont(UIUtils.uiFont(Font.PLAIN, 14));
        UIUtils.RoundedButton clearBtn = new UIUtils.RoundedButton("Clear", new Color(65, 95, 155));
        clearBtn.setPreferredSize(new Dimension(58, 30));
        clearBtn.addActionListener(e -> { filterField.setText(""); applyFilter(); });

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterBar.setBackground(UIUtils.DARK_NAVY);
        filterBar.add(hint);
        filterBar.add(filterField);
        filterBar.add(clearBtn);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        p.add(title,     BorderLayout.WEST);
        p.add(filterBar, BorderLayout.EAST);
        return p;
    }

    // =========================================================================
    //  TABS
    // =========================================================================

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UIUtils.MEDIUM_NAVY);
        tabs.setForeground(UIUtils.WHITE);
        tabs.setFont(UIUtils.uiFont(Font.BOLD, 12));
        tabs.addTab("\uD83D\uDCCB  Unpaid Appointments", makeUnpaidTab());
        tabs.addTab("\uD83D\uDCB3  Payment History",      makeHistoryTab());
        return tabs;
    }

    // ── UNPAID TAB ────────────────────────────────────────────────────────────
    private JPanel makeUnpaidTab() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(UIUtils.DARK_NAVY);
        p.setBorder(new EmptyBorder(8, 0, 0, 0));

        unpaidModel = new DefaultTableModel(
                new String[]{"Appt ID", "Customer", "Car Plate", "Type",
                             "Price (RM)", "Parts (RM)", "VIP Disc.", "Date", "Action"}, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        unpaidTable = UIUtils.createStyledTable(unpaidModel);
        main.darkTable(unpaidTable);

        int[] uw = {70, 120, 130, 70, 80, 80, 70, 90, 95};
        for (int i = 0; i < uw.length; i++)
            unpaidTable.getColumnModel().getColumn(i).setPreferredWidth(uw[i]);

        // "Load Order" button-style renderer
        unpaidTable.getColumnModel().getColumn(8).setCellRenderer(new LoadOrderRenderer());

        unpaidTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = unpaidTable.rowAtPoint(e.getPoint());
                if (row < 0) return;
                openCheckoutDialog((String) unpaidModel.getValueAt(row, 0));
            }
        });

        JScrollPane sp = new JScrollPane(unpaidTable);
        UIUtils.styleScrollPane(sp);

        JPanel btmBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btmBar.setBackground(UIUtils.DARK_NAVY);
        UIUtils.RoundedButton refresh = new UIUtils.RoundedButton("\u21BB  Refresh", new Color(65, 95, 155));
        refresh.setPreferredSize(new Dimension(110, 30));
        refresh.addActionListener(e -> refreshAll());
        btmBar.add(refresh);

        p.add(sp,     BorderLayout.CENTER);
        p.add(btmBar, BorderLayout.SOUTH);
        return p;
    }

    // ── HISTORY TAB ───────────────────────────────────────────────────────────
    private JPanel makeHistoryTab() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(UIUtils.DARK_NAVY);
        p.setBorder(new EmptyBorder(8, 0, 0, 0));

        payModel = new DefaultTableModel(
                new String[]{"Receipt No.", "Payment ID", "Appt ID", "Customer",
                             "Type", "Amount (RM)", "Method", "Date"}, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        payTable = UIUtils.createStyledTable(payModel);
        main.darkTable(payTable);

        int[] pw = {100, 90, 70, 130, 70, 90, 110, 90};
        for (int i = 0; i < pw.length; i++)
            payTable.getColumnModel().getColumn(i).setPreferredWidth(pw[i]);

        JScrollPane sp = new JScrollPane(payTable);
        UIUtils.styleScrollPane(sp);

        JPanel btmBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btmBar.setBackground(UIUtils.DARK_NAVY);

        UIUtils.RoundedButton viewBtn    = new UIUtils.RoundedButton("View Receipt", UIUtils.ACCENT_BLUE);
        UIUtils.RoundedButton refreshBtn = new UIUtils.RoundedButton("\u21BB  Refresh", new Color(65, 95, 155));
        viewBtn.setPreferredSize(new Dimension(130, 30));
        refreshBtn.setPreferredSize(new Dimension(110, 30));
        viewBtn.addActionListener(e -> viewReceipt());
        refreshBtn.addActionListener(e -> refreshAll());
        btmBar.add(viewBtn); btmBar.add(refreshBtn);

        p.add(sp,     BorderLayout.CENTER);
        p.add(btmBar, BorderLayout.SOUTH);
        return p;
    }

    // =========================================================================
    //  CHECKOUT DIALOG  –  left: provisional receipt  |  right: payment controls
    // =========================================================================

    private void openCheckoutDialog(String aptId) {
        Appointment apt = FileManager.findAppointmentById(aptId);
        if (apt == null || !canCollect(apt)) {
            JOptionPane.showMessageDialog(this, "Appointment not found or already paid.",
                    "Error", JOptionPane.WARNING_MESSAGE); return;
        }
        Customer     cust = apt.getCustomer();
        ServicePrice sp   = FileManager.readPrices();
        VipAccount   vip  = FileManager.findVipByCustomerId(apt.getCustomerId());

        double basePrice  = sp.getPriceForService(apt.getServiceType());
        double partsTotal = FileManager.getPartsForAppointment(apt.getAppointmentId())
                .stream().mapToDouble(AppointmentPart::getSubtotal).sum();
        double preVipTotal = basePrice + partsTotal;
        double vipDisc    = vip != null ? preVipTotal * vip.getDiscountPct() / 100.0 : 0;

        // Auto-issue monthly coupon(s) before opening dialog
        if (vip != null) {
            java.util.List<Coupon> issued = FileManager.issueMonthlyCouponIfNeeded(vip);
            if (!issued.isEmpty()) {
                StringBuilder sb = new StringBuilder("Monthly VIP coupon(s) issued!\n\n");
                for (Coupon c : issued) {
                    sb.append("Code: ").append(c.getCode())
                      .append("  Value: RM ").append(String.format("%.2f", c.getDiscountValue()))
                      .append("\n");
                }
                sb.append("Expires: ").append(issued.get(0).getExpiryDate());
                JOptionPane.showMessageDialog(this, sb.toString(),
                        "VIP Coupon Issued", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        // ── Dialog ───────────────────────────────────────────────────────────
        JDialog dlg = new JDialog(main, "Checkout \u2014 " + aptId, true);
        dlg.setSize(980, 640);
        dlg.setLocationRelativeTo(main);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(12, 0));
        root.setBackground(UIUtils.DARK_NAVY);
        root.setBorder(new EmptyBorder(16, 18, 16, 18));

        // ── STATE shared between left & right ─────────────────────────────────
        final String[] selectedMethod = {""}; // empty = none chosen
        final Coupon[] selectedCoupon = {null};

        // ── LEFT: Provisional Receipt ─────────────────────────────────────────
        JPanel receiptPanel = new JPanel();
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setBackground(new Color(252, 252, 250));
        receiptPanel.setBorder(new EmptyBorder(24, 32, 24, 32));

        Runnable[] rebuildReceipt = {null};

        JScrollPane receiptScroll = new JScrollPane(receiptPanel);
        receiptScroll.setBorder(new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1));
        receiptScroll.getViewport().setBackground(new Color(252, 252, 250));
        receiptScroll.setPreferredSize(new Dimension(430, 0));
        receiptScroll.getVerticalScrollBar().setUnitIncrement(12);

        // ── RIGHT: Payment Controls ───────────────────────────────────────────
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(CounterStaffDashboard.CARD2);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(22, 24, 22, 24)));

        // Payment method
        JLabel methodLbl = new JLabel("Payment Method");
        methodLbl.setForeground(UIUtils.WHITE);
        methodLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        methodLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel methodWarnLbl = new JLabel("  \u26A0 Please select a payment method");
        methodWarnLbl.setForeground(UIUtils.ERROR_RED);
        methodWarnLbl.setFont(UIUtils.uiFont(Font.ITALIC, 11));
        methodWarnLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodWarnLbl.setVisible(false);

        JPanel payGrid = new JPanel(new GridLayout(2, 3, 8, 8));
        payGrid.setBackground(CounterStaffDashboard.CARD2);
        payGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        payGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        String[] methodNames = {"Cash", "Credit Card", "QR Scan", "Wallet Balance", "Online Transfer", "Others"};
        JButton[] payBtns = new JButton[methodNames.length];
        for (int i = 0; i < methodNames.length; i++) payBtns[i] = payMethodBtn(methodNames[i]);
        for (JButton b : payBtns) {
            b.addActionListener(e -> {
                for (JButton x : payBtns) x.setBackground(CounterStaffDashboard.CARD3);
                b.setBackground(UIUtils.ACCENT_BLUE);
                selectedMethod[0] = b.getText();
                methodWarnLbl.setVisible(false);
                rebuildReceipt[0].run();
            });
            payGrid.add(b);
        }

        // Coupon
        JLabel couponLbl = new JLabel("Coupon");
        couponLbl.setForeground(UIUtils.WHITE);
        couponLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        couponLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        java.util.List<Coupon> avail = FileManager.findAvailableCoupons(apt.getCustomerId());
        JComboBox<String> couponBox = new JComboBox<>();
        UIUtils.styleCombo(couponBox);
        couponBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        couponBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        couponBox.addItem("-- No coupon --");
        for (Coupon c : avail) {
            String disc = "FIXED".equals(c.getDiscountType())
                    ? "RM " + String.format("%.2f", c.getDiscountValue())
                    : c.getDiscountValue() + "%";
            couponBox.addItem(c.getCouponId() + " | " + c.getCode() + " | " + disc + " off");
        }
        couponBox.addActionListener(e -> {
            String sel = (String) couponBox.getSelectedItem();
            if (sel == null || sel.startsWith("--")) { selectedCoupon[0] = null; }
            else {
                String cid = sel.split("\\|")[0].trim();
                selectedCoupon[0] = avail.stream()
                        .filter(c -> c.getCouponId().equals(cid)).findFirst().orElse(null);
            }
            rebuildReceipt[0].run();
        });

        // Amount display
        JLabel amtTitleLbl = new JLabel("Amount Due");
        amtTitleLbl.setForeground(UIUtils.DIM_TEXT);
        amtTitleLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        amtTitleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel amtValLbl = new JLabel("RM " + String.format("%.2f", preVipTotal - vipDisc));
        amtValLbl.setForeground(UIUtils.WARN_YELLOW);
        amtValLbl.setFont(UIUtils.uiFont(Font.BOLD, 32));
        amtValLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Wallet balance
        double walBal = walletBalance(apt.getCustomerId());
        JLabel walLbl = new JLabel(String.format("Wallet Balance: RM %.2f", walBal));
        walLbl.setForeground(walBal > 0 ? UIUtils.GREEN_OK : UIUtils.DIM_TEXT);
        walLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        walLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Complete Payment button
        UIUtils.RoundedButton completeBtn = new UIUtils.RoundedButton(
                "Complete Payment", UIUtils.SUCCESS_GREEN);
        completeBtn.setFont(UIUtils.uiFont(Font.BOLD, 14));
        completeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        completeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        UIUtils.RoundedButton cancelBtn = new UIUtils.RoundedButton(
                "Cancel", new Color(80, 80, 100));
        cancelBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
        cancelBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancelBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cancelBtn.addActionListener(e -> dlg.dispose());

        rightPanel.add(methodLbl);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(payGrid);
        rightPanel.add(methodWarnLbl);
        rightPanel.add(Box.createVerticalStrut(16));
        rightPanel.add(sep());
        rightPanel.add(Box.createVerticalStrut(14));
        rightPanel.add(couponLbl);
        rightPanel.add(Box.createVerticalStrut(6));
        rightPanel.add(couponBox);
        rightPanel.add(Box.createVerticalStrut(18));
        rightPanel.add(sep());
        rightPanel.add(Box.createVerticalStrut(14));
        rightPanel.add(amtTitleLbl);
        rightPanel.add(Box.createVerticalStrut(4));
        rightPanel.add(amtValLbl);
        rightPanel.add(Box.createVerticalStrut(4));
        rightPanel.add(walLbl);
        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(Box.createVerticalStrut(18));
        rightPanel.add(completeBtn);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(cancelBtn);

        // ── Receipt builder (runs on every change) ────────────────────────────
        java.util.List<AppointmentPart> parts =
                FileManager.getPartsForAppointment(apt.getAppointmentId());

        rebuildReceipt[0] = () -> {
            Coupon cpn = selectedCoupon[0];
            double afterVip    = preVipTotal - vipDisc;
            double couponDisc  = 0;
            if (cpn != null)
                couponDisc = "FIXED".equals(cpn.getDiscountType())
                        ? cpn.getDiscountValue()
                        : afterVip * cpn.getDiscountValue() / 100.0;
            double finalAmt = Math.max(0, afterVip - couponDisc);

            amtValLbl.setText("RM " + String.format("%.2f", finalAmt));

            receiptPanel.removeAll();
            Color paperBg    = new Color(252, 252, 250);
            Color textDark   = new Color(30,  30,  30);
            Color textGray   = new Color(120, 120, 120);
            Color accentBlue = new Color(30,  80, 160);

            receiptPanel.add(rLbl("APU AUTOMOTIVE SERVICE CENTRE", Font.BOLD, 15, textDark));
            receiptPanel.add(rLbl("No. 12, Jalan Teknologi 1, 57000 KL", Font.PLAIN, 10, textGray));
            receiptPanel.add(rLbl("Tel: +603-8888 9999  |  asc@apu.edu.my", Font.PLAIN, 10, textGray));
            receiptPanel.add(Box.createVerticalStrut(8));
            receiptPanel.add(rDivider(accentBlue, 2));
            receiptPanel.add(rLbl("PROVISIONAL RECEIPT", Font.BOLD, 13, accentBlue));
            receiptPanel.add(rDivider(new Color(200, 200, 200), 1));
            receiptPanel.add(Box.createVerticalStrut(6));

            receiptPanel.add(rRow("Appt ID:",       apt.getAppointmentId(),    textDark, textGray));
            receiptPanel.add(rRow("Customer:",       cust != null ? cust.getName() : apt.getCustomerId(), textDark, textGray));
            receiptPanel.add(rRow("Vehicle:",        apt.getVehicleInfo(),      textDark, textGray));
            receiptPanel.add(rRow("Service:",        apt.getServiceType() + " (" + apt.getDurationHours() + " hr)", textDark, textGray));
            receiptPanel.add(rRow("Date / Time:",    apt.getDate() + "  " + apt.getTime(), textDark, textGray));
            if (vip != null) {
                int earnedPts = (!selectedMethod[0].isEmpty() && !"Wallet Balance".equals(selectedMethod[0]))
                        ? (int)(finalAmt * 0.5) : 0;
                receiptPanel.add(rRow("VIP Tier:",   vip.getTier() + "  (" + (vip.getPoints() + earnedPts) + " pts)", textDark, textGray));
            }
            String methodStr = selectedMethod[0].isEmpty() ? "(not selected)" : selectedMethod[0];
            receiptPanel.add(rRow("Payment Method:", methodStr, textDark, textGray));

            receiptPanel.add(Box.createVerticalStrut(10));
            receiptPanel.add(rDivider(new Color(200, 200, 200), 1));
            receiptPanel.add(Box.createVerticalStrut(4));

            receiptPanel.add(rItemHdr(textGray, paperBg));
            receiptPanel.add(rItem("Service: " + apt.getServiceType(), 1, basePrice, textDark, paperBg));
            if (!parts.isEmpty()) {
                receiptPanel.add(rSectionLabel("Additional Parts", textGray, paperBg));
                for (AppointmentPart ap : parts)
                    receiptPanel.add(rItem(ap.getPartName(), ap.getQty(), ap.getUnitPrice(), textDark, paperBg));
            }

            receiptPanel.add(Box.createVerticalStrut(6));
            receiptPanel.add(rDivider(new Color(200, 200, 200), 1));
            receiptPanel.add(rTotalRow("Subtotal:", String.format("RM %.2f", preVipTotal), textGray, paperBg));
            if (vipDisc > 0)
                receiptPanel.add(rTotalRow(String.format("VIP Discount (%.0f%%):", vip.getDiscountPct()),
                        String.format("-RM %.2f", vipDisc), new Color(0, 150, 80), paperBg));
            if (couponDisc > 0)
                receiptPanel.add(rTotalRow("Coupon Discount:", String.format("-RM %.2f", couponDisc),
                        new Color(0, 150, 80), paperBg));
            receiptPanel.add(rDivider(accentBlue, 1));
            receiptPanel.add(rTotalRow("TOTAL:", String.format("RM %.2f", finalAmt), accentBlue, paperBg));
            receiptPanel.add(Box.createVerticalStrut(6));
            receiptPanel.add(rLbl("* This is a provisional receipt only.", Font.ITALIC, 10, textGray));
            receiptPanel.add(rLbl("  Official receipt issued upon payment.", Font.ITALIC, 10, textGray));

            receiptPanel.revalidate();
            receiptPanel.repaint();
        };
        rebuildReceipt[0].run();

        // ── Complete Payment action ───────────────────────────────────────────
        completeBtn.addActionListener(e -> {
            if (selectedMethod[0].isEmpty()) {
                methodWarnLbl.setVisible(true);
                return;
            }
            String method = resolveMethod(selectedMethod[0]);
            Coupon cpn = selectedCoupon[0];
            double afterVip   = preVipTotal - vipDisc;
            double couponDisc = 0;
            if (cpn != null)
                couponDisc = "FIXED".equals(cpn.getDiscountType())
                        ? cpn.getDiscountValue()
                        : afterVip * cpn.getDiscountValue() / 100.0;
            double finalAmt = Math.max(0, afterVip - couponDisc);

            if ("Use Balance".equals(method)
                    && walletBalance(apt.getCustomerId()) < finalAmt) {
                JOptionPane.showMessageDialog(dlg, "余额不足，请充值。",
                        "Error", JOptionPane.ERROR_MESSAGE); return;
            }

            Payment pay = new Payment(
                    FileManager.generatePaymentId(), apt.getAppointmentId(),
                    apt.getCustomerId(), finalAmt, LocalDate.now().toString(),
                    method, FileManager.generateReceiptNumber());
            FileManager.savePayment(pay);

            if (cpn != null) { cpn.setUsed(true); FileManager.updateCoupon(cpn); }
            apt.setStatus(Appointment.STATUS_COMPLETED);
            FileManager.updateAppointment(apt);
            // VIP points: wallet payment = 0 pts (already awarded at top-up);
            // non-wallet payment = 0.5 pts per RM 1
            String oldTier = (vip != null) ? vip.getTier() : null;
            if (vip != null && !"Use Balance".equals(method)) {
                vip.addPointsFromPayment(finalAmt, 0.5);
                FileManager.updateVipAccount(vip);
            }
            // ── Notification: customer gets payment confirmed ──────────────
            String cName = cust != null ? cust.getName() : apt.getCustomerId();
            FileManager.saveNotification(new asc.model.Notification(
                    apt.getCustomerId(), asc.model.Notification.PAYMENT_RECEIVED,
                    "Payment Confirmed",
                    "Payment of RM " + String.format("%.2f", finalAmt)
                    + " for appointment " + apt.getAppointmentId()
                    + " has been confirmed. Thank you, " + cName + "!"));
            // ── Notification: VIP tier upgraded ────────────────────────────
            if (vip != null && oldTier != null && !oldTier.equals(vip.getTier())
                    && !asc.model.VipAccount.TIER_NONE.equals(vip.getTier())) {
                FileManager.saveNotification(new asc.model.Notification(
                        apt.getCustomerId(), asc.model.Notification.VIP_TIER_UPGRADE,
                        "VIP Tier Upgraded",
                        "Congratulations! Your VIP tier has been upgraded to "
                        + vip.getTier() + " (" + vip.getPoints() + " pts)!"));
            }

            dlg.dispose();
            showPostPaymentDialog(pay, apt);
            refreshAll();
        });

        root.add(receiptScroll, BorderLayout.CENTER);
        root.add(rightPanel,    BorderLayout.EAST);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ── Post-payment dialog ───────────────────────────────────────────────────
    private void showPostPaymentDialog(Payment pay, Appointment apt) {
        JDialog d = new JDialog(main, "Payment Complete", true);
        d.setSize(420, 260);
        d.setLocationRelativeTo(main);
        d.setResizable(false);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIUtils.DARK_NAVY);
        p.setBorder(new EmptyBorder(28, 36, 28, 36));

        JLabel icon = new JLabel("\u2714", JLabel.CENTER);
        icon.setFont(UIUtils.uiFont(Font.BOLD, 42));
        icon.setForeground(UIUtils.SUCCESS_GREEN);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Payment Collected Successfully!", JLabel.CENTER);
        title.setFont(UIUtils.uiFont(Font.BOLD, 14));
        title.setForeground(UIUtils.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Receipt: " + pay.getReceiptNumber()
                + "   Amount: RM " + String.format("%.2f", pay.getAmount()), JLabel.CENTER);
        sub.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        sub.setForeground(UIUtils.DIM_TEXT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setBackground(UIUtils.DARK_NAVY);

        UIUtils.RoundedButton printBtn = new UIUtils.RoundedButton(
                "\uD83D\uDDA8  Print Receipt", UIUtils.ACCENT_BLUE);
        UIUtils.RoundedButton doneBtn  = new UIUtils.RoundedButton(
                "Done", UIUtils.SUCCESS_GREEN);
        printBtn.setPreferredSize(new Dimension(150, 36));
        doneBtn .setPreferredSize(new Dimension(100, 36));

        printBtn.addActionListener(e -> {
            main.showReceiptWindow(pay, apt);
            d.dispose();
        });
        doneBtn.addActionListener(e -> d.dispose());

        btnRow.add(printBtn);
        btnRow.add(doneBtn);

        p.add(icon);
        p.add(Box.createVerticalStrut(8));
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        p.add(Box.createVerticalStrut(20));
        p.add(btnRow);

        d.setContentPane(p);
        d.setVisible(true);
    }

    // =========================================================================
    //  RECEIPT RENDERERS  (paper-style)
    // =========================================================================

    private JLabel rLbl(String text, int style, int size, Color fg) {
        JLabel l = new JLabel(text, JLabel.CENTER);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(fg);
        l.setBackground(new Color(252, 252, 250));
        l.setOpaque(true);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, size + 10));
        return l;
    }

    private JSeparator rDivider(Color c, int thick) {
        JSeparator sep = new JSeparator();
        sep.setForeground(c);
        sep.setBackground(new Color(252, 252, 250));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, thick + 4));
        return sep;
    }

    private JPanel rRow(String label, String value, Color valueFg, Color bg) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setBackground(bg);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel k = new JLabel(label);
        k.setFont(new Font("SansSerif", Font.PLAIN, 11));
        k.setForeground(new Color(100, 100, 100));
        JLabel v = new JLabel(value, JLabel.RIGHT);
        v.setFont(new Font("SansSerif", Font.BOLD, 11));
        v.setForeground(valueFg);
        p.add(k, BorderLayout.WEST); p.add(v, BorderLayout.EAST);
        return p;
    }

    private JPanel rItemHdr(Color fg, Color bg) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(235, 235, 235));
        p.setBorder(new EmptyBorder(3, 4, 3, 4));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel d = new JLabel("Description");
        d.setFont(new Font("SansSerif", Font.BOLD, 10)); d.setForeground(fg);
        JLabel q = new JLabel("Qty", JLabel.CENTER);
        q.setFont(new Font("SansSerif", Font.BOLD, 10)); q.setForeground(fg);
        q.setPreferredSize(new Dimension(50, 16));
        q.setMinimumSize(new Dimension(50, 16));
        JLabel a = new JLabel("Amount", JLabel.RIGHT);
        a.setFont(new Font("SansSerif", Font.BOLD, 10)); a.setForeground(fg);
        a.setPreferredSize(new Dimension(85, 16));
        a.setMinimumSize(new Dimension(85, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.insets = new Insets(0, 4, 0, 4);
        gbc.gridx = 0; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(d, gbc);
        gbc.gridx = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        p.add(q, gbc);
        gbc.gridx = 2;
        p.add(a, gbc);
        return p;
    }

    private JPanel rItem(String name, int qty, double price, Color fg, Color bg) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(bg);
        p.setBorder(new EmptyBorder(2, 4, 2, 4));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel n = new JLabel(name);
        n.setFont(new Font("SansSerif", Font.PLAIN, 11)); n.setForeground(fg);
        JLabel q = new JLabel("x" + qty, JLabel.CENTER);
        q.setFont(new Font("SansSerif", Font.PLAIN, 11));
        q.setForeground(new Color(120, 120, 120));
        q.setPreferredSize(new Dimension(50, 18));
        q.setMinimumSize(new Dimension(50, 18));
        JLabel a = new JLabel(String.format("RM %.2f", price * qty), JLabel.RIGHT);
        a.setFont(new Font("SansSerif", Font.BOLD, 11));
        a.setForeground(new Color(30, 80, 160));
        a.setPreferredSize(new Dimension(85, 18));
        a.setMinimumSize(new Dimension(85, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.insets = new Insets(0, 4, 0, 4);
        gbc.gridx = 0; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(n, gbc);
        gbc.gridx = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        p.add(q, gbc);
        gbc.gridx = 2;
        p.add(a, gbc);
        return p;
    }

    private JPanel rSectionLabel(String text, Color fg, Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel l = new JLabel("\u2014 " + text + " \u2014");
        l.setFont(new Font("SansSerif", Font.ITALIC, 10));
        l.setForeground(fg);
        p.add(l); return p;
    }

    private JPanel rTotalRow(String label, String value, Color valueFg, Color bg) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setBackground(bg);
        p.setBorder(new EmptyBorder(3, 4, 3, 4));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel k = new JLabel(label); k.setFont(new Font("SansSerif", Font.BOLD, 12)); k.setForeground(new Color(60, 60, 60));
        JLabel v = new JLabel(value, JLabel.RIGHT); v.setFont(new Font("SansSerif", Font.BOLD, 13)); v.setForeground(valueFg);
        p.add(k, BorderLayout.WEST); p.add(v, BorderLayout.EAST);
        return p;
    }

    // =========================================================================
    //  DATA HELPERS
    // =========================================================================

    void refreshAll() {
        ServicePrice sp = FileManager.readPrices();
        String filter   = filterField == null ? "" : filterField.getText().trim().toLowerCase();

        if (unpaidModel != null) {
            unpaidModel.setRowCount(0);
            for (Appointment a : FileManager.readAllAppointments()) {
                if (!canCollect(a)) continue;
                Customer   c   = a.getCustomer();
                VipAccount vip = FileManager.findVipByCustomerId(a.getCustomerId());
                double     svc = sp.getPriceForService(a.getServiceType());
                double     pts = FileManager.getPartsForAppointment(a.getAppointmentId())
                        .stream().mapToDouble(AppointmentPart::getSubtotal).sum();
                String discStr  = (vip != null && vip.getDiscountPct() > 0)
                        ? String.format("%.0f%%", vip.getDiscountPct()) : "-";
                String custName = c != null ? c.getName() : a.getCustomerId();
                if (!filter.isEmpty()) {
                    String all = (a.getAppointmentId() + " " + custName + " "
                            + a.getVehicleInfo() + " " + a.getDate()).toLowerCase();
                    if (!all.contains(filter)) continue;
                }
                unpaidModel.addRow(new Object[]{
                    a.getAppointmentId(), custName, a.getVehicleInfo(),
                    a.getServiceType(),
                    String.format("%.2f", svc),
                    pts > 0 ? String.format("%.2f", pts) : "-",
                    discStr, a.getDate(), "Load Order"
                });
            }
        }

        if (payModel != null) {
            payModel.setRowCount(0);
            for (Payment p : FileManager.readAllPayments()) {
                if (p.getAppointmentId().startsWith("TOPUP-")) continue;
                Appointment a = p.getAppointment();
                Customer    c = p.getCustomer();
                String custName = c != null ? c.getName() : p.getCustomerId();
                if (!filter.isEmpty()) {
                    String all = (p.getReceiptNumber() + " " + p.getPaymentId() + " "
                            + p.getAppointmentId() + " " + custName + " "
                            + p.getPaymentMethod() + " " + p.getPaymentDate()).toLowerCase();
                    if (!all.contains(filter)) continue;
                }
                payModel.addRow(new Object[]{
                    p.getReceiptNumber(), p.getPaymentId(), p.getAppointmentId(),
                    custName,
                    a != null ? a.getServiceType() : "-",
                    String.format("%.2f", p.getAmount()),
                    p.getPaymentMethod(), p.getPaymentDate()
                });
            }
        }
    }

    private void applyFilter() { refreshAll(); }

    private boolean canCollect(Appointment a) {
        if (a == null) return false;
        if ("CANCELLED".equalsIgnoreCase(a.getStatus())
                || "CANCELED".equalsIgnoreCase(a.getStatus())
                || Appointment.STATUS_MISSED.equals(a.getStatus())) return false;
        return Appointment.STATUS_AWAITING_PAYMENT.equals(a.getStatus())
                && FileManager.findPaymentByAppointmentId(a.getAppointmentId()) == null;
    }

    private void viewReceipt() {
        int row = payTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a payment row first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE); return;
        }
        String payId = (String) payModel.getValueAt(row, 1);
        Payment p = FileManager.readAllPayments().stream()
                .filter(x -> x.getPaymentId().equals(payId)).findFirst().orElse(null);
        if (p != null) main.showReceiptWindow(p,
                p.getAppointment());
    }

    private double walletBalance(String id) {
        double in = 0, out = 0;
        for (Payment p : FileManager.readAllPayments()) {
            if (!p.getCustomerId().equals(id)) continue;
            if (p.getAppointmentId().startsWith("TOPUP-"))                 in  += p.getAmount();
            else if ("Use Balance".equalsIgnoreCase(p.getPaymentMethod())) out += p.getAmount();
        }
        return in - out;
    }

    private String resolveMethod(String s) {
        if ("Wallet Balance".equals(s)) return "Use Balance";
        if ("QR Scan".equals(s))       return "E-Wallet";
        return s;
    }

    // =========================================================================
    //  UI HELPERS
    // =========================================================================

    private JButton payMethodBtn(String text) {
        UIUtils.RoundedButton b = new UIUtils.RoundedButton(text, CounterStaffDashboard.CARD3);
        b.setForeground(UIUtils.LIGHT_GRAY);
        b.setFont(UIUtils.uiFont(Font.BOLD, 11));
        b.setPreferredSize(new Dimension(110, 42));
        return b;
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(55, 80, 130));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        return s;
    }

    // ── Cell renderer for "Load Order" column ─────────────────────────────────
    private static class LoadOrderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = new JLabel("Load Order", JLabel.CENTER);
            l.setFont(UIUtils.uiFont(Font.BOLD, 11));
            l.setOpaque(true);
            if (sel) {
                l.setBackground(new Color(50, 130, 90));
                l.setForeground(Color.WHITE);
            } else {
                l.setBackground(new Color(30, 100, 60));
                l.setForeground(new Color(180, 255, 200));
            }
            l.setBorder(BorderFactory.createCompoundBorder(
                    new UIUtils.RoundedBorder(new Color(50, 160, 100), 6, 1),
                    new EmptyBorder(3, 10, 3, 10)));
            return l;
        }
    }
}
