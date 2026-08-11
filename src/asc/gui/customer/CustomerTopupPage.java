package asc.gui.customer;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

public class CustomerTopupPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final CustomerDashboard main;

    // ── Payment method tracking ──────────────────────────────────────────
    private String selectedMethod = "TNG"; // default

    // ── Brand colours ────────────────────────────────────────────────────
    private static final Color TNG_BLUE      = new Color(0,   102, 204);
    private static final Color FPX_RED       = new Color(200, 50,   50);
    private static final Color MAYBANK_YELLOW = new Color(255, 204, 0);
    private static final Color MAYBANK_BG    = new Color(30,  30,  30);
    private static final Color PBB_BLUE      = new Color(13,  59,  135);
    private static final Color PBB_BG        = new Color(245, 245, 245);
    private static final Color CIMB_RED      = new Color(175, 0,   36);
    private static final Color CIMB_BG       = new Color(248, 248, 248);

    // References needed across methods
    private JLabel previewLbl;
    private JLabel balAfterLbl;
    private JPanel tngCard;
    private JPanel fpxCard;

    public CustomerTopupPage(CustomerDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(6, 18, 6, 18));
        add(buildPanel(), BorderLayout.CENTER);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PAGE BUILD
    // ═══════════════════════════════════════════════════════════════════════════

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY);
        panel.setBorder(new EmptyBorder(8, 22, 8, 22));

        // ── Title row ─────────────────────────────────────────────────────
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setBackground(UIUtils.DARK_NAVY);
        titleRow.setBorder(new EmptyBorder(0, 0, 0, 0));

        ImageIcon rawIcon = null;
        try {
            java.io.File iconFile = new java.io.File("data/Picture/Customer/Top-Up Wallet(Blue).png");
            if (iconFile.exists()) {
                Image img = new ImageIcon(iconFile.getAbsolutePath())
                        .getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                rawIcon = new ImageIcon(img);
            }
        } catch (Exception ignored) {}

        JLabel titleLbl = rawIcon != null
                ? new JLabel("Top-Up My Wallet", rawIcon, JLabel.LEFT)
                : new JLabel("Top-Up My Wallet");
        titleLbl.setFont(UIUtils.uiFont(Font.BOLD, 14));
        titleLbl.setForeground(UIUtils.ACCENT_BLUE);
        if (rawIcon != null) titleLbl.setIconTextGap(8);
        titleRow.add(titleLbl);
        panel.add(titleRow, BorderLayout.NORTH);

        // ── Balance banner (compact) ──────────────────────────────────────
        double walletBal = main.computeWalletBalance();

        JPanel banner = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(28, 68, 140),
                        getWidth(), 0, new Color(18, 42, 96)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(8, 20, 8, 20)));

        JLabel balTitleLbl = new JLabel("Current Wallet Balance");
        balTitleLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        balTitleLbl.setForeground(UIUtils.DIM_TEXT);

        JLabel balValLbl = new JLabel(String.format("RM  %.2f", walletBal));
        balValLbl.setFont(UIUtils.uiFont(Font.BOLD, 24));
        balValLbl.setForeground(walletBal > 0 ? UIUtils.GREEN_OK : UIUtils.LIGHT_GRAY);

        JPanel balLeft = new JPanel();
        balLeft.setLayout(new BoxLayout(balLeft, BoxLayout.Y_AXIS));
        balLeft.setOpaque(false);
        balLeft.add(balTitleLbl);
        balLeft.add(Box.createVerticalStrut(2));
        balLeft.add(balValLbl);

        // VIP badge
        VipAccount topupVip = FileManager.findVipByCustomerId(main.currentCustomer.getUserId());
        JPanel balRight = new JPanel();
        balRight.setLayout(new BoxLayout(balRight, BoxLayout.Y_AXIS));
        balRight.setOpaque(false);
        if (topupVip != null) {
            JLabel tierBadge = new JLabel(topupVip.tierBadge(), JLabel.RIGHT);
            tierBadge.setFont(UIUtils.uiFont(Font.BOLD, 14));
            tierBadge.setForeground(topupVip.tierColor());
            tierBadge.setAlignmentX(Component.RIGHT_ALIGNMENT);
            JLabel ptLbl = new JLabel(topupVip.getPoints() + " pts  |  1 pt per RM", JLabel.RIGHT);
            ptLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10));
            ptLbl.setForeground(UIUtils.DIM_TEXT);
            ptLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
            balRight.add(tierBadge);
            balRight.add(Box.createVerticalStrut(2));
            balRight.add(ptLbl);
        } else {
            JLabel noVip = new JLabel("No VIP Tier", JLabel.RIGHT);
            noVip.setFont(UIUtils.uiFont(Font.ITALIC, 11));
            noVip.setForeground(UIUtils.DIM_TEXT);
            noVip.setAlignmentX(Component.RIGHT_ALIGNMENT);
            balRight.add(noVip);
        }
        banner.add(balLeft, BorderLayout.WEST);
        banner.add(balRight, BorderLayout.EAST);

        // ── Body: GridLayout(1,2) left form, right history ────────────────
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setBackground(UIUtils.DARK_NAVY);

        // ══════════════════════════════════════════════════════════════════
        // LEFT CARD  –  Top-Up Form
        // ══════════════════════════════════════════════════════════════════
        JPanel leftCard = new JPanel(new BorderLayout(0, 0));
        leftCard.setBackground(new Color(30, 50, 92));
        leftCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(0, 0, 0, 0)));

        JPanel leftHeader = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(40, 72, 140));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 12, 12, 12);
                g2.dispose();
            }
        };
        leftHeader.setOpaque(false);
        leftHeader.setBorder(new EmptyBorder(6, 14, 6, 14));
        JPanel leftWrapper = new JPanel(new GridBagLayout());
        leftWrapper.setOpaque(false);
        JPanel leftInner = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        leftInner.setOpaque(false);
        JLabel leftHdrLbl2 = new JLabel("New Top-Up");
        leftHdrLbl2.setFont(UIUtils.uiFont(Font.BOLD, 12));
        leftHdrLbl2.setForeground(Color.WHITE);
        JLabel leftDot2 = new JLabel("\u25CF");
        leftDot2.setFont(UIUtils.uiFont(Font.BOLD, 9));
        leftDot2.setForeground(UIUtils.GREEN_OK);
        leftInner.add(leftDot2);
        leftInner.add(leftHdrLbl2);
        leftWrapper.add(leftInner);
        leftHeader.add(leftWrapper, BorderLayout.CENTER);
        leftCard.add(leftHeader, BorderLayout.NORTH);

        // Form body
        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setBackground(new Color(30, 50, 92));

        JPanel formInner = new JPanel();
        formInner.setLayout(new BoxLayout(formInner, BoxLayout.Y_AXIS));
        formInner.setBackground(new Color(30, 50, 92));
        formInner.setMaximumSize(new Dimension(420, Integer.MAX_VALUE));

        // ── Amount input ──────────────────────────────────────────────────
        JLabel amtLbl = new JLabel("Amount to Top-Up (RM)");
        amtLbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
        amtLbl.setForeground(UIUtils.LIGHT_GRAY);
        amtLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField amountF = new JTextField();
        UIUtils.styleTextField(amountF);
        amountF.setFont(UIUtils.uiFont(Font.BOLD, 18));
        amountF.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        amountF.setHorizontalAlignment(JTextField.CENTER);
        amountF.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Live preview ──────────────────────────────────────────────────
        previewLbl = new JLabel("RM  0.00", JLabel.CENTER);
        previewLbl.setFont(UIUtils.uiFont(Font.BOLD, 22));
        previewLbl.setForeground(new Color(120, 140, 180));
        previewLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        previewLbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        amountF.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { update(); }
            public void removeUpdate(DocumentEvent e)  { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
            void update() {
                try {
                    double v = Double.parseDouble(amountF.getText().trim());
                    previewLbl.setText(String.format("RM  %.2f", v));
                    previewLbl.setForeground(UIUtils.WARN_YELLOW);
                } catch (Exception ex) {
                    previewLbl.setText("RM  0.00");
                    previewLbl.setForeground(new Color(120, 140, 180));
                }
            }
        });

        // ── Payment method selection ──────────────────────────────────────
        JLabel methodLbl = new JLabel("Payment Method");
        methodLbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
        methodLbl.setForeground(UIUtils.LIGHT_GRAY);
        methodLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel methodPanel = buildPaymentMethodPanel();
        methodPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Confirm button ────────────────────────────────────────────────
        UIUtils.RoundedButton confirmBtn =
                new UIUtils.RoundedButton("  Confirm Top-Up", UIUtils.SUCCESS_GREEN);
        confirmBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
        confirmBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        confirmBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmBtn.setPreferredSize(new Dimension(300, 34));

        // Balance label
        balAfterLbl = new JLabel(String.format(
                "Current Balance: RM %.2f", walletBal), JLabel.CENTER);
        balAfterLbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
        balAfterLbl.setForeground(walletBal > 0 ? UIUtils.GREEN_OK : UIUtils.DIM_TEXT);
        balAfterLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        confirmBtn.addActionListener(e -> {
            double amt;
            try { amt = Double.parseDouble(amountF.getText().trim()); }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Please enter a valid amount.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE); return;
            }
            if (amt < 1.0) {
                JOptionPane.showMessageDialog(panel, "Minimum top-up is RM 1.00.",
                        "Too Low", JOptionPane.ERROR_MESSAGE); return;
            }

            // Route to the correct payment flow
            boolean paid = false;
            if ("TNG".equals(selectedMethod)) {
                paid = showTngPaymentFlow(amt);
            } else {
                paid = showFpxPaymentFlow(amt);
            }

            if (!paid) return; // user cancelled

            // Process the actual top-up
            VipAccount oldVip = FileManager.findVipByCustomerId(main.currentCustomer.getUserId());
            String oldTier = oldVip != null ? oldVip.getTier() : null;
            Payment pay = FileManager.processCustomerTopup(
                    main.currentCustomer.getUserId(), amt, selectedMethod);
            // ── Notification: customer gets top-up confirmed ───────────────
            FileManager.saveNotification(new asc.model.Notification(
                    main.currentCustomer.getUserId(), asc.model.Notification.PAYMENT_RECEIVED,
                    "Top-Up Confirmed",
                    "Top-up of RM " + String.format("%.2f", amt) + " via " + selectedMethod
                    + " has been credited to your wallet. Receipt: " + pay.getReceiptNumber() + "."));
            // ── Notification: VIP tier upgrade ─────────────────────────────
            VipAccount updVip = FileManager.findVipByCustomerId(main.currentCustomer.getUserId());
            if (updVip != null && oldTier != null && !oldTier.equals(updVip.getTier())
                    && !asc.model.VipAccount.TIER_NONE.equals(updVip.getTier())) {
                FileManager.saveNotification(new asc.model.Notification(
                        main.currentCustomer.getUserId(), asc.model.Notification.VIP_TIER_UPGRADE,
                        "VIP Tier Upgraded",
                        "Congratulations! Your VIP tier has been upgraded to "
                        + updVip.getTier() + " (" + updVip.getPoints() + " pts)!"));
            }
            VipAccount updVip2 = updVip; // reuse
            String vipMsg = updVip2 != null
                    ? String.format("\nVIP Points: %d pts  (%s)",
                            updVip.getPoints(), updVip.getTier()) : "";
            JOptionPane.showMessageDialog(panel,
                    String.format("Top-up successful!\nReceipt: %s\nAmount: RM %.2f%s",
                            pay.getReceiptNumber(), amt, vipMsg),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            amountF.setText("");
            previewLbl.setText("RM  0.00");
            previewLbl.setForeground(new Color(120, 140, 180));
            main.showTopup();
        });

        // ── Quick amount shortcuts ────────────────────────────────────────
        JLabel quickLbl = new JLabel("Quick select:");
        quickLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10));
        quickLbl.setForeground(UIUtils.DIM_TEXT);
        quickLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel quickRow = new JPanel(new GridLayout(2, 3, 8, 6));
        quickRow.setBackground(new Color(30, 50, 92));
        quickRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        quickRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        for (int amt2 : new int[]{10, 20, 50, 100, 200, 500}) {
            UIUtils.RoundedButton qb = new UIUtils.RoundedButton(
                    "RM " + amt2, new Color(40, 68, 130));
            qb.setFont(UIUtils.uiFont(Font.BOLD, 12));
            qb.addActionListener(ev -> amountF.setText(String.valueOf(amt2)));
            quickRow.add(qb);
        }

        // Divider
        JSeparator fSep = new JSeparator();
        fSep.setForeground(new Color(60, 90, 150));
        fSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        fSep.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Assemble formInner
        formInner.add(amtLbl);
        formInner.add(Box.createVerticalStrut(4));
        formInner.add(amountF);
        formInner.add(Box.createVerticalStrut(8));
        formInner.add(previewLbl);
        formInner.add(Box.createVerticalStrut(8));
        formInner.add(fSep);
        formInner.add(Box.createVerticalStrut(10));
        formInner.add(methodLbl);
        formInner.add(Box.createVerticalStrut(4));
        formInner.add(methodPanel);
        formInner.add(Box.createVerticalStrut(12));
        formInner.add(quickLbl);
        formInner.add(Box.createVerticalStrut(4));
        formInner.add(quickRow);
        formInner.add(Box.createVerticalStrut(14));
        formInner.add(confirmBtn);
        formInner.add(Box.createVerticalStrut(6));
        formInner.add(balAfterLbl);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(12, 18, 12, 18);
        formBody.add(formInner, gbc);

        leftCard.add(formBody, BorderLayout.CENTER);

        // ══════════════════════════════════════════════════════════════════
        // RIGHT CARD  –  History
        // ══════════════════════════════════════════════════════════════════
        JPanel rightCard = new JPanel(new BorderLayout(0, 0));
        rightCard.setBackground(new Color(30, 50, 92));
        rightCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(0, 0, 0, 0)));

        JPanel rightHeader = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(40, 72, 140));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 12, 12, 12);
                g2.dispose();
            }
        };
        rightHeader.setOpaque(false);
        rightHeader.setBorder(new EmptyBorder(6, 14, 6, 14));

        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.setOpaque(false);
        JPanel rightInner = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        rightInner.setOpaque(false);
        JLabel rightHdrLbl2 = new JLabel("Transaction History");
        rightHdrLbl2.setFont(UIUtils.uiFont(Font.BOLD, 12));
        rightHdrLbl2.setForeground(Color.WHITE);
        JLabel rightDot2 = new JLabel("\u25CF");
        rightDot2.setFont(UIUtils.uiFont(Font.BOLD, 9));
        rightDot2.setForeground(UIUtils.ACCENT_BLUE);
        rightInner.add(rightDot2);
        rightInner.add(rightHdrLbl2);
        rightWrapper.add(rightInner);
        rightHeader.add(rightWrapper, BorderLayout.CENTER);

        // Stats pills
        List<Payment> topups = new ArrayList<>();
        for (Payment p : FileManager.readAllPayments()) {
            if (!p.getCustomerId().equals(main.currentCustomer.getUserId())) continue;
            if (!p.getAppointmentId().startsWith("TOPUP-")) continue;
            topups.add(p);
        }
        topups.sort((a, b) -> b.getPaymentDate().compareTo(a.getPaymentDate()));
        double totalLoaded = topups.stream().mapToDouble(Payment::getAmount).sum();

        JPanel statPills = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        statPills.setOpaque(false);
        statPills.add(buildHistStatPill(String.valueOf(topups.size()), "records", UIUtils.ACCENT_BLUE));
        statPills.add(buildHistStatPill(String.format("RM %.0f", totalLoaded), "loaded", UIUtils.GREEN_OK));
        rightHeader.add(statPills, BorderLayout.EAST);
        rightCard.add(rightHeader, BorderLayout.NORTH);

        // History table
        DefaultTableModel histModel = new DefaultTableModel(
                new String[]{"Receipt No.", "Amount (RM)", "Method", "Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Payment p : topups) {
            histModel.addRow(new Object[]{
                p.getReceiptNumber(),
                String.format("RM %.2f", p.getAmount()),
                p.getPaymentMethod(),
                p.getPaymentDate()
            });
        }

        JTable histTable = UIUtils.createStyledTable(histModel);
        histTable.setRowHeight(24);
        int[] histWidths = {130, 110, 130, 110};
        for (int i = 0; i < histWidths.length; i++)
            histTable.getColumnModel().getColumn(i).setPreferredWidth(histWidths[i]);

        JScrollPane histScroll = new JScrollPane(histTable);
        UIUtils.styleScrollPane(histScroll);

        JPanel histBody = new JPanel(new BorderLayout());
        histBody.setBackground(new Color(30, 50, 92));
        histBody.setBorder(new EmptyBorder(6, 12, 6, 12));

        if (topups.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(new Color(30, 50, 92));
            JPanel emptyInner = new JPanel();
            emptyInner.setLayout(new BoxLayout(emptyInner, BoxLayout.Y_AXIS));
            emptyInner.setBackground(new Color(30, 50, 92));
            JLabel emptyIcon = new JLabel("\uD83D\uDCB3", JLabel.CENTER);
            emptyIcon.setFont(new Font("Dialog", Font.PLAIN, 36));
            emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel emptyLbl = new JLabel("No transactions yet", JLabel.CENTER);
            emptyLbl.setFont(UIUtils.uiFont(Font.ITALIC, 12));
            emptyLbl.setForeground(UIUtils.DIM_TEXT);
            emptyLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel emptyHint = new JLabel("Top-up to see your history here", JLabel.CENTER);
            emptyHint.setFont(UIUtils.uiFont(Font.PLAIN, 10));
            emptyHint.setForeground(new Color(100, 130, 175));
            emptyHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyInner.add(Box.createVerticalStrut(10));
            emptyInner.add(emptyIcon);
            emptyInner.add(Box.createVerticalStrut(8));
            emptyInner.add(emptyLbl);
            emptyInner.add(Box.createVerticalStrut(3));
            emptyInner.add(emptyHint);
            empty.add(emptyInner);
            histBody.add(empty, BorderLayout.CENTER);
        } else {
            histBody.add(histScroll, BorderLayout.CENTER);
        }
        rightCard.add(histBody, BorderLayout.CENTER);

        // Right card footer
        JPanel rightFooter = new JPanel(new GridLayout(1, 3, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(24, 40, 80));
                g2.fillRoundRect(0, -12, getWidth(), getHeight() + 12, 12, 12);
                g2.dispose();
            }
        };
        rightFooter.setOpaque(false);
        rightFooter.setBorder(new EmptyBorder(6, 12, 6, 12));
        rightFooter.add(main.buildFooterStat("Total Records",
                String.valueOf(topups.size()), UIUtils.ACCENT_BLUE));
        rightFooter.add(main.buildFooterStat("Total Loaded",
                String.format("RM %.2f", totalLoaded), UIUtils.GREEN_OK));
        rightFooter.add(main.buildFooterStat("Wallet Balance",
                String.format("RM %.2f", walletBal), UIUtils.WARN_YELLOW));
        rightCard.add(rightFooter, BorderLayout.SOUTH);

        body.add(leftCard);
        body.add(rightCard);

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBackground(UIUtils.DARK_NAVY);
        content.add(banner, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PAYMENT METHOD SELECTION PANEL
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel buildPaymentMethodPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBackground(new Color(30, 50, 92));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        // TNG card
        tngCard = buildMethodCard("TNG", "Touch 'n Go eWallet", TNG_BLUE, true);
        tngCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tngCard.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { selectMethod("TNG"); }
        });

        // FPX card
        fpxCard = buildMethodCard("FPX", "Online Banking", FPX_RED, false);
        fpxCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fpxCard.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { selectMethod("FPX"); }
        });

        panel.add(tngCard);
        panel.add(fpxCard);
        return panel;
    }

    private JPanel buildMethodCard(String title, String subtitle, Color accent, boolean selected) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        Color bg = selected ? blend(accent, new Color(24, 42, 80), 0.25f) : new Color(30, 50, 92);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(selected ? accent : new Color(50, 80, 140), 10, selected ? 2 : 1),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
        titleLbl.setForeground(selected ? Color.WHITE : UIUtils.LIGHT_GRAY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(UIUtils.uiFont(Font.PLAIN, 9));
        subLbl.setForeground(selected ? new Color(200, 215, 235) : UIUtils.DIM_TEXT);
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(subLbl);

        return card;
    }

    private void selectMethod(String method) {
        selectedMethod = method;
        updateMethodCards();
    }

    private void updateMethodCards() {
        boolean tngSel = "TNG".equals(selectedMethod);

        updateCardAppearance(tngCard, "TNG", "Touch 'n Go eWallet", TNG_BLUE, tngSel);
        updateCardAppearance(fpxCard, "FPX", "Online Banking", FPX_RED, !tngSel);

        tngCard.revalidate(); tngCard.repaint();
        fpxCard.revalidate(); fpxCard.repaint();
    }

    private void updateCardAppearance(JPanel card, String title, String subtitle,
                                       Color accent, boolean selected) {
        card.removeAll();
        Color bg = selected ? blend(accent, new Color(24, 42, 80), 0.25f) : new Color(30, 50, 92);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(selected ? accent : new Color(50, 80, 140), 10, selected ? 2 : 1),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
        titleLbl.setForeground(selected ? Color.WHITE : UIUtils.LIGHT_GRAY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(UIUtils.uiFont(Font.PLAIN, 9));
        subLbl.setForeground(selected ? new Color(200, 215, 235) : UIUtils.DIM_TEXT);
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(subLbl);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TNG PAYMENT FLOW
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean showTngPaymentFlow(double amount) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Touch 'n Go eWallet", true);
        dlg.setSize(420, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 248, 255));

        // ── TNG Header ────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(TNG_BLUE);
        header.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel tngLogo = new JLabel("Touch 'n Go");
        tngLogo.setFont(UIUtils.uiFont(Font.BOLD, 20));
        tngLogo.setForeground(Color.WHITE);

        JLabel ewalletLbl = new JLabel("eWallet", JLabel.RIGHT);
        ewalletLbl.setFont(UIUtils.uiFont(Font.BOLD, 14));
        ewalletLbl.setForeground(new Color(180, 210, 255));

        header.add(tngLogo, BorderLayout.WEST);
        header.add(ewalletLbl, BorderLayout.EAST);

        // ── Body ──────────────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(new Color(245, 248, 255));
        body.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel payToLbl = new JLabel("Payment to");
        payToLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        payToLbl.setForeground(new Color(120, 130, 150));
        payToLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel merchantLbl = new JLabel("APU Automotive Service Centre");
        merchantLbl.setFont(UIUtils.uiFont(Font.BOLD, 14));
        merchantLbl.setForeground(new Color(40, 50, 70));
        merchantLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel amountLbl = new JLabel(String.format("RM %.2f", amount), JLabel.CENTER);
        amountLbl.setFont(UIUtils.uiFont(Font.BOLD, 42));
        amountLbl.setForeground(TNG_BLUE);
        amountLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Reference number
        String refNo = "TNG-" + System.currentTimeMillis() % 100000000;
        JLabel refLbl = new JLabel("Ref: " + refNo, JLabel.CENTER);
        refLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        refLbl.setForeground(new Color(150, 160, 180));
        refLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // PIN entry simulation
        JPanel pinPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pinPanel.setBackground(new Color(245, 248, 255));
        pinPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPasswordField pinField = new JPasswordField(6);
        pinField.setFont(UIUtils.uiFont(Font.BOLD, 18));
        pinField.setHorizontalAlignment(JTextField.CENTER);
        pinField.setPreferredSize(new Dimension(180, 38));
        pinField.setBackground(Color.WHITE);
        pinField.setForeground(new Color(40, 50, 70));
        pinField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1),
                new EmptyBorder(4, 8, 4, 8)));
        JLabel pinLbl = new JLabel("Enter 6-digit PIN");
        pinLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        pinLbl.setForeground(new Color(120, 130, 150));
        pinPanel.add(pinLbl);
        pinPanel.add(pinField);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 220, 235));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setBackground(new Color(245, 248, 255));
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
        cancelBtn.setForeground(new Color(150, 160, 180));
        cancelBtn.setBackground(new Color(230, 235, 245));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 225), 1));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton payBtn = new JButton("Pay Now");
        payBtn.setFont(UIUtils.uiFont(Font.BOLD, 13));
        payBtn.setForeground(Color.WHITE);
        payBtn.setBackground(TNG_BLUE);
        payBtn.setBorder(BorderFactory.createEmptyBorder());
        payBtn.setFocusPainted(false);
        payBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnRow.add(cancelBtn);
        btnRow.add(payBtn);

        body.add(Box.createVerticalStrut(6));
        body.add(payToLbl);
        body.add(Box.createVerticalStrut(4));
        body.add(merchantLbl);
        body.add(Box.createVerticalStrut(14));
        body.add(amountLbl);
        body.add(Box.createVerticalStrut(10));
        body.add(refLbl);
        body.add(Box.createVerticalStrut(14));
        body.add(sep);
        body.add(Box.createVerticalStrut(14));
        body.add(pinPanel);
        body.add(Box.createVerticalStrut(18));
        body.add(btnRow);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        dlg.setContentPane(root);

        final boolean[] result = {false};

        cancelBtn.addActionListener(e -> dlg.dispose());
        payBtn.addActionListener(e -> {
            String pin = new String(pinField.getPassword()).trim();
            if (pin.length() < 6) {
                JOptionPane.showMessageDialog(dlg, "Please enter a 6-digit PIN.",
                        "Invalid PIN", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Simulate processing
            body.removeAll();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.add(Box.createVerticalStrut(20));
            JLabel procLbl = new JLabel("Processing Payment...", JLabel.CENTER);
            procLbl.setFont(UIUtils.uiFont(Font.BOLD, 16));
            procLbl.setForeground(TNG_BLUE);
            procLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);
            progress.setMaximumSize(new Dimension(200, 6));
            progress.setAlignmentX(Component.CENTER_ALIGNMENT);

            body.add(procLbl);
            body.add(Box.createVerticalStrut(12));
            body.add(progress);
            body.revalidate(); body.repaint();

            // Short delay then success
            javax.swing.Timer timer = new javax.swing.Timer(1500, ev -> {
                body.removeAll();
                body.add(Box.createVerticalStrut(20));
                JLabel checkLbl = new JLabel("\u2714", JLabel.CENTER);
                checkLbl.setFont(UIUtils.uiFont(Font.BOLD, 48));
                checkLbl.setForeground(UIUtils.SUCCESS_GREEN);
                checkLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

                JLabel successLbl = new JLabel("Payment Successful!", JLabel.CENTER);
                successLbl.setFont(UIUtils.uiFont(Font.BOLD, 18));
                successLbl.setForeground(UIUtils.SUCCESS_GREEN);
                successLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

                JLabel amtDoneLbl = new JLabel(String.format("RM %.2f has been deducted", amount), JLabel.CENTER);
                amtDoneLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
                amtDoneLbl.setForeground(new Color(120, 130, 150));
                amtDoneLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

                JButton doneBtn = new JButton("Done");
                doneBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
                doneBtn.setForeground(Color.WHITE);
                doneBtn.setBackground(TNG_BLUE);
                doneBtn.setBorder(BorderFactory.createEmptyBorder(8, 40, 8, 40));
                doneBtn.setFocusPainted(false);
                doneBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                doneBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                doneBtn.addActionListener(ev2 -> {
                    result[0] = true;
                    dlg.dispose();
                });

                body.add(checkLbl);
                body.add(Box.createVerticalStrut(10));
                body.add(successLbl);
                body.add(Box.createVerticalStrut(6));
                body.add(amtDoneLbl);
                body.add(Box.createVerticalStrut(16));
                body.add(doneBtn);
                body.revalidate(); body.repaint();
            });
            timer.setRepeats(false);
            timer.start();
        });

        dlg.setVisible(true);
        return result[0];
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FPX PAYMENT FLOW
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean showFpxPaymentFlow(double amount) {
        // Step 1: Bank selection
        String bank = showBankSelectionDialog(amount);
        if (bank == null) return false;

        // Step 2: Bank-specific simulation
        switch (bank) {
            case "Maybank":     return showBankPaymentDialog(amount, "Maybank", "Maybank2u",
                    MAYBANK_YELLOW, MAYBANK_BG, Color.WHITE);
            case "Public Bank": return showBankPaymentDialog(amount, "Public Bank", "PBe",
                    PBB_BLUE, PBB_BG, new Color(40, 50, 70));
            case "CIMB Bank":   return showBankPaymentDialog(amount, "CIMB Bank", "CIMB Clicks",
                    CIMB_RED, CIMB_BG, new Color(40, 50, 70));
            default: return false;
        }
    }

    private String showBankSelectionDialog(double amount) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "FPX - Select Your Bank", true);
        dlg.setSize(420, 380);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 248, 255));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(FPX_RED);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel fpxLogo = new JLabel("FPX  Financial Process Exchange");
        fpxLogo.setFont(UIUtils.uiFont(Font.BOLD, 15));
        fpxLogo.setForeground(Color.WHITE);
        JLabel fpxSub = new JLabel("Online Banking", JLabel.RIGHT);
        fpxSub.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        fpxSub.setForeground(new Color(255, 200, 200));
        header.add(fpxLogo, BorderLayout.WEST);
        header.add(fpxSub, BorderLayout.EAST);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(new Color(245, 248, 255));
        body.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel selectLbl = new JLabel("Select your bank to proceed with payment:");
        selectLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        selectLbl.setForeground(new Color(120, 130, 150));
        selectLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel amtLbl = new JLabel(String.format("Amount: RM %.2f", amount), JLabel.CENTER);
        amtLbl.setFont(UIUtils.uiFont(Font.BOLD, 16));
        amtLbl.setForeground(new Color(40, 50, 70));
        amtLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        body.add(selectLbl);
        body.add(Box.createVerticalStrut(6));
        body.add(amtLbl);
        body.add(Box.createVerticalStrut(16));

        final String[] result = {null};

        body.add(buildBankButton("Maybank", "Maybank2u", MAYBANK_YELLOW, MAYBANK_BG,
                Color.WHITE, dlg, result));
        body.add(Box.createVerticalStrut(8));
        body.add(buildBankButton("Public Bank", "PBe Online Banking", PBB_BLUE, PBB_BG,
                new Color(40, 50, 70), dlg, result));
        body.add(Box.createVerticalStrut(8));
        body.add(buildBankButton("CIMB Bank", "CIMB Clicks", CIMB_RED, CIMB_BG,
                new Color(40, 50, 70), dlg, result));

        body.add(Box.createVerticalStrut(12));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        cancelBtn.setForeground(new Color(150, 160, 180));
        cancelBtn.setBackground(new Color(230, 235, 245));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 225), 1));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.addActionListener(e -> dlg.dispose());
        body.add(cancelBtn);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        dlg.setContentPane(root);
        dlg.setVisible(true);

        return result[0];
    }

    private JPanel buildBankButton(String bankName, String subName, Color accent,
                                    Color bg, Color textColor, JDialog parent,
                                    final String[] resultRef) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1),
                new EmptyBorder(12, 16, 12, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setBackground(bg);

        JLabel nameLbl = new JLabel(bankName);
        nameLbl.setFont(UIUtils.uiFont(Font.BOLD, 14));
        nameLbl.setForeground(textColor);

        JLabel subLbl = new JLabel(subName);
        subLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10));
        subLbl.setForeground(blend(textColor, bg, 0.5f));

        textCol.add(nameLbl);
        textCol.add(Box.createVerticalStrut(1));
        textCol.add(subLbl);

        JLabel arrowLbl = new JLabel("\u276F");
        arrowLbl.setFont(UIUtils.uiFont(Font.BOLD, 14));
        arrowLbl.setForeground(accent);

        card.add(textCol, BorderLayout.CENTER);
        card.add(arrowLbl, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                resultRef[0] = bankName;
                parent.dispose();
            }
            public void mouseEntered(MouseEvent e) {
                card.setBackground(brighten(bg, 10));
                textCol.setBackground(brighten(bg, 10));
            }
            public void mouseExited(MouseEvent e) {
                card.setBackground(bg);
                textCol.setBackground(bg);
            }
        });

        return card;
    }

    private boolean showBankPaymentDialog(double amount, String bankName, String portalName,
                                           Color accent, Color bg, Color textColor) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                bankName + " - " + portalName, true);
        dlg.setSize(440, 500);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Bank header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(accent);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel bankLogo = new JLabel(bankName);
        bankLogo.setFont(UIUtils.uiFont(Font.BOLD, 18));
        // Choose text colour for readability on the accent background
        Color headerText = (accent == MAYBANK_YELLOW) ? Color.BLACK : Color.WHITE;
        bankLogo.setForeground(headerText);

        JLabel portalLbl = new JLabel(portalName, JLabel.RIGHT);
        portalLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        portalLbl.setForeground(brighten(headerText, -80));

        JLabel secureLbl = new JLabel("\uD83D\uDD12  Secure Connection", JLabel.RIGHT);
        secureLbl.setFont(UIUtils.uiFont(Font.PLAIN, 9));
        secureLbl.setForeground(brighten(headerText, -60));

        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setBackground(accent);
        rightCol.setOpaque(false);
        portalLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        secureLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightCol.add(portalLbl);
        rightCol.add(secureLbl);

        header.add(bankLogo, BorderLayout.WEST);
        header.add(rightCol, BorderLayout.EAST);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(22, 28, 22, 28));

        // Merchant info
        JPanel infoCard = new JPanel();
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBackground(new Color(248, 250, 252));
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 228, 238), 1),
                new EmptyBorder(14, 16, 14, 16)));
        infoCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        JLabel merchantLbl = new JLabel("Merchant: APU Automotive Service Centre");
        merchantLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        merchantLbl.setForeground(new Color(40, 50, 70));
        merchantLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel fpxRefLbl = new JLabel("FPX Ref: FPX-" + System.currentTimeMillis() % 100000000);
        fpxRefLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10));
        fpxRefLbl.setForeground(new Color(150, 160, 180));
        fpxRefLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel amtLbl = new JLabel(String.format("Amount:  RM %.2f", amount));
        amtLbl.setFont(UIUtils.uiFont(Font.BOLD, 20));
        amtLbl.setForeground(accent);
        amtLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dateLbl = new JLabel("Date: " + java.time.LocalDate.now().toString());
        dateLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10));
        dateLbl.setForeground(new Color(150, 160, 180));
        dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoCard.add(merchantLbl);
        infoCard.add(Box.createVerticalStrut(2));
        infoCard.add(fpxRefLbl);
        infoCard.add(Box.createVerticalStrut(8));
        infoCard.add(amtLbl);
        infoCard.add(Box.createVerticalStrut(2));
        infoCard.add(dateLbl);

        JLabel verifyLbl = new JLabel("Please verify the details above.", JLabel.CENTER);
        verifyLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        verifyLbl.setForeground(new Color(150, 160, 180));
        verifyLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setBackground(Color.WHITE);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
        cancelBtn.setForeground(new Color(150, 160, 180));
        cancelBtn.setBackground(new Color(230, 235, 245));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 225), 1));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton confirmBtn = new JButton("Confirm & Pay");
        confirmBtn.setFont(UIUtils.uiFont(Font.BOLD, 13));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setBackground(accent);
        confirmBtn.setBorder(BorderFactory.createEmptyBorder());
        confirmBtn.setFocusPainted(false);
        confirmBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnRow.add(cancelBtn);
        btnRow.add(confirmBtn);

        body.add(infoCard);
        body.add(Box.createVerticalStrut(12));
        body.add(verifyLbl);
        body.add(Box.createVerticalStrut(16));
        body.add(btnRow);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        dlg.setContentPane(root);

        final boolean[] result = {false};

        cancelBtn.addActionListener(e -> dlg.dispose());
        confirmBtn.addActionListener(e -> {
            // Show processing
            body.removeAll();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

            JLabel procLbl = new JLabel("Connecting to " + bankName + "...", JLabel.CENTER);
            procLbl.setFont(UIUtils.uiFont(Font.BOLD, 14));
            procLbl.setForeground(new Color(40, 50, 70));
            procLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);
            progress.setMaximumSize(new Dimension(240, 6));
            progress.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel secureCheck = new JLabel("\u2713 Secure connection verified", JLabel.CENTER);
            secureCheck.setFont(UIUtils.uiFont(Font.PLAIN, 10));
            secureCheck.setForeground(UIUtils.SUCCESS_GREEN);
            secureCheck.setAlignmentX(Component.CENTER_ALIGNMENT);

            body.add(Box.createVerticalStrut(30));
            body.add(procLbl);
            body.add(Box.createVerticalStrut(10));
            body.add(progress);
            body.add(Box.createVerticalStrut(8));
            body.add(secureCheck);
            body.revalidate(); body.repaint();

            javax.swing.Timer timer = new javax.swing.Timer(2000, ev -> {
                body.removeAll();

                JLabel checkLbl = new JLabel("\u2714", JLabel.CENTER);
                checkLbl.setFont(UIUtils.uiFont(Font.BOLD, 48));
                checkLbl.setForeground(UIUtils.SUCCESS_GREEN);
                checkLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

                JLabel successLbl = new JLabel("Payment Successful!", JLabel.CENTER);
                successLbl.setFont(UIUtils.uiFont(Font.BOLD, 18));
                successLbl.setForeground(UIUtils.SUCCESS_GREEN);
                successLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

                JLabel amtDoneLbl = new JLabel(String.format("RM %.2f paid via %s", amount, bankName), JLabel.CENTER);
                amtDoneLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
                amtDoneLbl.setForeground(new Color(120, 130, 150));
                amtDoneLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

                JLabel bankRefLbl = new JLabel("Transaction ID: " + bankName.substring(0, 3).toUpperCase()
                        + "-" + System.currentTimeMillis() % 10000000, JLabel.CENTER);
                bankRefLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10));
                bankRefLbl.setForeground(new Color(150, 160, 180));
                bankRefLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

                JButton doneBtn = new JButton("Done");
                doneBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
                doneBtn.setForeground(Color.WHITE);
                doneBtn.setBackground(accent);
                doneBtn.setBorder(BorderFactory.createEmptyBorder(8, 40, 8, 40));
                doneBtn.setFocusPainted(false);
                doneBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                doneBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                doneBtn.addActionListener(ev2 -> {
                    result[0] = true;
                    dlg.dispose();
                });

                body.add(checkLbl);
                body.add(Box.createVerticalStrut(10));
                body.add(successLbl);
                body.add(Box.createVerticalStrut(6));
                body.add(amtDoneLbl);
                body.add(Box.createVerticalStrut(4));
                body.add(bankRefLbl);
                body.add(Box.createVerticalStrut(16));
                body.add(doneBtn);
                body.revalidate(); body.repaint();
            });
            timer.setRepeats(false);
            timer.start();
        });

        dlg.setVisible(true);
        return result[0];
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  COLOUR UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    private static Color blend(Color a, Color b, float ratio) {
        int r = (int) (a.getRed() * (1 - ratio) + b.getRed() * ratio);
        int g = (int) (a.getGreen() * (1 - ratio) + b.getGreen() * ratio);
        int bl = (int) (a.getBlue() * (1 - ratio) + b.getBlue() * ratio);
        return new Color(clamp(r), clamp(g), clamp(bl));
    }

    private static Color brighten(Color c, int d) {
        return new Color(clamp(c.getRed() + d), clamp(c.getGreen() + d), clamp(c.getBlue() + d));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  HISTORY STAT PILL
    // ═══════════════════════════════════════════════════════════════════════════

    JPanel buildHistStatPill(String value, String label, Color accent) {
        JPanel pill = new JPanel(new GridLayout(2, 1, 0, 1));
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1, true),
                new EmptyBorder(2, 8, 2, 8)));
        JLabel vl = new JLabel(value, JLabel.CENTER);
        vl.setFont(UIUtils.uiFont(Font.BOLD, 11)); vl.setForeground(accent);
        JLabel ll = new JLabel(label, JLabel.CENTER);
        ll.setFont(UIUtils.uiFont(Font.PLAIN, 9)); ll.setForeground(UIUtils.DIM_TEXT);
        pill.add(vl); pill.add(ll);
        return pill;
    }
}
