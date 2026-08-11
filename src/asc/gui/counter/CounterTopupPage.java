package asc.gui.counter;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class CounterTopupPage extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CounterStaffDashboard main;
    private DefaultTableModel model;
    private JTable table;
    private JPanel vipProgressPanel;

    public CounterTopupPage(CounterStaffDashboard m) {
        main = m;
        setLayout(new BorderLayout(0, 10));
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(8, 20, 8, 20));
        add(main.sectionTitle("Top-Up Wallet",
                CounterStaffDashboard.ICON_BASE + "Top-Up Wallet(Blue).png"), BorderLayout.NORTH);
        add(makeBody(), BorderLayout.CENTER);
    }

    // =========================================================================
    //  BODY
    // =========================================================================

    private JScrollPane makeBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UIUtils.DARK_NAVY);

        // ── Top-up form card ─────────────────────────────────────────────────
        JPanel top = main.makeCard(null);
        top.setLayout(new BorderLayout(24, 0));

        JPanel left = new JPanel(new GridBagLayout());
        left.setBackground(CounterStaffDashboard.CARD2);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 5, 5, 5);

        JTextField search   = main.tf();
        JComboBox<String> custBox = new JComboBox<>();
        JTextField amountF  = main.tf();
        JComboBox<String> methodCb = new JComboBox<>(
                new String[]{"Cash", "Debit Card", "Credit Card", "E-Wallet", "Online Transfer"});
        UIUtils.styleCombo(custBox);
        UIUtils.styleCombo(methodCb);

        JLabel balLbl = new JLabel("Balance: RM 0.00");
        balLbl.setForeground(UIUtils.LIGHT_GRAY);
        balLbl.setFont(UIUtils.uiFont(Font.BOLD, 14));

        Runnable fill = () -> {
            String old = main.comboId(custBox);
            custBox.removeAllItems();
            custBox.addItem("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"); // separator / null option
            String q = search.getText().trim().toLowerCase();
            for (Customer c : FileManager.readAllCustomers()) {
                if (!q.isEmpty() && !main.customerFind(c, q)) continue;
                custBox.addItem(c.getUserId() + " | " + c.getName() + " | " + c.getPhone());
            }
            main.selectById(custBox, old);
            if (custBox.getSelectedItem() == null && custBox.getItemCount() > 0) {
                custBox.setSelectedIndex(0);
            }
        };
        Runnable setBal = () -> {
            Customer c = main.comboCustomer(custBox);
            double b = c == null ? 0 : walletBalance(c.getUserId());
            balLbl.setText(String.format("Balance: RM %.2f", b));
            balLbl.setForeground(b > 0 ? UIUtils.GREEN_OK : UIUtils.LIGHT_GRAY);
            updateVipProgress(c != null ? c.getUserId() : null);
        };

        fill.run(); setBal.run();
        search.getDocument().addDocumentListener(main.doc(() -> { fill.run(); setBal.run(); }));
        custBox.addActionListener(e -> setBal.run());

        fRow(left, g, 0, "Customer Search:",    search);
        fRow(left, g, 1, "Matched Customer:",   custBox);
        fRow(left, g, 2, "Top-Up Amount (RM):", amountF);

        // Quick amount buttons (left side)
        JPanel quickPanel = new JPanel(new GridLayout(1, 4, 8, 0));
        quickPanel.setBackground(CounterStaffDashboard.CARD2);
        String[] quickAmts = {"50", "100", "200", "500"};
        for (String amt : quickAmts) {
            UIUtils.RoundedButton btn = new UIUtils.RoundedButton("RM " + amt, new Color(55, 80, 135));
            btn.setFont(UIUtils.uiFont(Font.BOLD, 12));
            btn.addActionListener(e -> amountF.setText(amt));
            quickPanel.add(btn);
        }
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2; left.add(quickPanel, g);

        fRow(left, g, 4, "Payment Method:",     methodCb);
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2; left.add(balLbl, g);

        // ── Right: live preview panel ────────────────────────────────────────
        JPanel right = new JPanel();
        right.setMinimumSize(new Dimension(240, 0));
        right.setPreferredSize(new Dimension(240, 0));
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(CounterStaffDashboard.CARD3);
        right.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1),
                new EmptyBorder(18, 26, 18, 26)));

        JLabel bigLbl = new JLabel("Wallet Top-Up");
        bigLbl.setForeground(UIUtils.WHITE); bigLbl.setFont(UIUtils.uiFont(Font.BOLD, 22));
        bigLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel moneyLbl = new JLabel("RM 0.00");
        moneyLbl.setForeground(UIUtils.WARN_YELLOW); moneyLbl.setFont(UIUtils.uiFont(Font.BOLD, 32));
        moneyLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        amountF.getDocument().addDocumentListener(main.doc(() ->
                moneyLbl.setText("RM " + safeAmount(amountF.getText()))));

        UIUtils.RoundedButton saveBtn = new UIUtils.RoundedButton("Save Top-Up", UIUtils.SUCCESS_GREEN);
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.setMaximumSize(new Dimension(180, 24));
        saveBtn.addActionListener(e -> {
            Customer c = main.comboCustomer(custBox);
            if (c == null) { main.msgErr(this, "Select a customer first."); return; }
            double a;
            try { a = Double.parseDouble(amountF.getText().trim()); }
            catch (Exception ex) { main.msgErr(this, "Invalid amount."); return; }
            if (a <= 0) { main.msgErr(this, "Amount must be more than 0."); return; }

            // Use processCustomerTopup to auto-award VIP points
            VipAccount oldVip = FileManager.findVipByCustomerId(c.getUserId());
            String oldTier = oldVip != null ? oldVip.getTier() : null;
            Payment p = FileManager.processCustomerTopup(c.getUserId(), a,
                    methodCb.getSelectedItem().toString());
            // ── Notification: customer gets top-up confirmed ───────────────
            FileManager.saveNotification(new asc.model.Notification(
                    c.getUserId(), asc.model.Notification.PAYMENT_RECEIVED,
                    "Top-Up Confirmed",
                    "Top-up of RM " + String.format("%.2f", a) + " via "
                    + methodCb.getSelectedItem().toString()
                    + " has been credited to your wallet. Receipt: " + p.getReceiptNumber() + "."));
            VipAccount updVip = FileManager.findVipByCustomerId(c.getUserId());
            // ── Notification: VIP tier upgrade ─────────────────────────────
            if (updVip != null && oldTier != null && !oldTier.equals(updVip.getTier())
                    && !asc.model.VipAccount.TIER_NONE.equals(updVip.getTier())) {
                FileManager.saveNotification(new asc.model.Notification(
                        c.getUserId(), asc.model.Notification.VIP_TIER_UPGRADE,
                        "VIP Tier Upgraded",
                        "Congratulations! Your VIP tier has been upgraded to "
                        + updVip.getTier() + " (" + updVip.getPoints() + " pts)!"));
            }
            String vipMsg = updVip != null
                    ? String.format("\nVIP Points: %d pts  (%s)",
                            updVip.getPoints(), updVip.getTier()) : "";
            JOptionPane.showMessageDialog(this,
                    String.format("Top-up saved.\nReceipt: %s\nAmount: RM %.2f%s",
                            p.getReceiptNumber(), a, vipMsg),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            main.showReceiptWindow(p, null);
            amountF.setText("");
            setBal.run();
            refresh();
            updateVipProgress(c.getUserId());
        });

        right.add(bigLbl);
        right.add(Box.createVerticalStrut(14));
        right.add(moneyLbl);
        right.add(Box.createVerticalStrut(8));

        // VIP progress container with fixed height so layout stays consistent
        JPanel vipContainer = new JPanel(new BorderLayout());
        vipContainer.setBackground(CounterStaffDashboard.CARD3);
        vipContainer.setPreferredSize(new Dimension(0, 60));
        vipContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        vipContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        vipProgressPanel = new JPanel();
        vipProgressPanel.setLayout(new BoxLayout(vipProgressPanel, BoxLayout.Y_AXIS));
        vipProgressPanel.setBackground(CounterStaffDashboard.CARD3);
        vipContainer.add(vipProgressPanel, BorderLayout.CENTER);

        right.add(vipContainer);

        right.add(Box.createVerticalStrut(40));
        right.add(saveBtn);

        top.add(left,  BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);

        // ── Top-up history card ──────────────────────────────────────────────
        JPanel history = main.makeCard("Top-Up History");
        model = new DefaultTableModel(
                new String[]{"Receipt No.","Customer","Amount (RM)","Method","Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = UIUtils.createStyledTable(model);
        main.darkTable(table);
        refresh();

        JScrollPane scroll = new JScrollPane(table);
        UIUtils.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(0, 270));
        history.add(scroll);

        body.add(top);
        body.add(Box.createVerticalStrut(8));
        body.add(history);

        JScrollPane all = new JScrollPane(body);
        all.setBorder(null);
        all.getViewport().setBackground(UIUtils.DARK_NAVY);
        return all;
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private void updateVipProgress(String customerId) {
        if (vipProgressPanel == null) return;
        vipProgressPanel.removeAll();
        if (customerId == null) {
            vipProgressPanel.revalidate();
            vipProgressPanel.repaint();
            return;
        }
        VipAccount vip = FileManager.findVipByCustomerId(customerId);
        if (vip == null) {
            vipProgressPanel.revalidate();
            vipProgressPanel.repaint();
            return;
        }

        // Tier badge
        JLabel tierLbl = new JLabel(vip.tierBadge() + "  \u2014  " + String.format("%,d pts", vip.getPoints()));
        tierLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        tierLbl.setForeground(vip.tierColor());
        tierLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        vipProgressPanel.add(tierLbl);
        vipProgressPanel.add(Box.createVerticalStrut(3));

        // Progress bar
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) (vip.tierProgressRatio() * 100));
        bar.setStringPainted(true);
        String next = vip.nextTierName();
        bar.setString(next != null
                ? String.format("%d pts to %s", vip.pointsToNextTier(), next)
                : "MAX TIER");
        bar.setForeground(vip.tierColor());
        bar.setBackground(new Color(40, 50, 80));
        bar.setFont(UIUtils.uiFont(Font.PLAIN, 10));
        bar.setPreferredSize(new Dimension(180, 18));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        vipProgressPanel.add(bar);

        // Discount info
        double disc = vip.getDiscountPct();
        JLabel discLbl = new JLabel(disc > 0
                ? String.format("%.0f%% service discount", disc)
                : "No discount yet");
        discLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        discLbl.setForeground(disc > 0 ? UIUtils.GREEN_OK : UIUtils.DIM_TEXT);
        discLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        vipProgressPanel.add(discLbl);

        vipProgressPanel.revalidate();
        vipProgressPanel.repaint();
    }

    private void refresh() {
        if (model == null) return;
        model.setRowCount(0);
        for (Payment p : FileManager.readAllPayments()) {
            if (!p.getAppointmentId().startsWith("TOPUP-")) continue;
            Customer c = p.getCustomer();
            model.addRow(new Object[]{
                p.getReceiptNumber(),
                c != null ? c.getName() : p.getCustomerId(),
                String.format("%.2f", p.getAmount()),
                p.getPaymentMethod(), p.getPaymentDate()
            });
        }
    }

    private void fRow(JPanel p, GridBagConstraints g, int y, String label, JComponent comp) {
        g.gridy = y; g.gridx = 0; g.gridwidth = 1; g.weightx = 0.32;
        JLabel l = new JLabel(label);
        l.setForeground(UIUtils.WHITE); l.setFont(UIUtils.uiFont(Font.BOLD, 12));
        p.add(l, g);
        g.gridx = 1; g.weightx = 0.68; p.add(comp, g);
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

    private String safeAmount(String s) {
        try { return String.format("%.2f", Double.parseDouble(s.trim())); }
        catch (Exception ex) { return "0.00"; }
    }
}
