package asc.gui.counter;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class CounterHomePage extends JPanel {

    private static final long serialVersionUID = 1L;
    private final CounterStaffDashboard main;

    public CounterHomePage(CounterStaffDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UIUtils.DARK_NAVY);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));
        root.add(buildBanner(),       BorderLayout.NORTH);
        root.add(buildStatsWrapper(), BorderLayout.CENTER);
        root.add(buildBottomRow(),    BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildBanner() {
        JPanel banner = new JPanel(new BorderLayout(0, 10)) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (main.homeBgImage != null) {
                    g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                    g2.drawImage(main.homeBgImage, 0, 0, getWidth(), getHeight(), this);
                    g2.setClip(null);
                    g2.setColor(new Color(10, 20, 50, 165));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                } else {
                    g2.setPaint(new GradientPaint(0, 0, new Color(18, 38, 82, 220),
                            getWidth(), getHeight(), new Color(40, 90, 180, 200)));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                }
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 20, 1),
                new EmptyBorder(100, 32, 100, 32)));

        JPanel bannerLeft = new JPanel();
        bannerLeft.setLayout(new BoxLayout(bannerLeft, BoxLayout.Y_AXIS));
        bannerLeft.setOpaque(false);
        JLabel greeting = new JLabel("Welcome back, " + main.currentStaff.getName() + "!");
        greeting.setFont(UIUtils.uiFont(Font.BOLD, 20)); greeting.setForeground(UIUtils.WHITE);
        JLabel idLine = new JLabel(main.currentStaff.getName() + "  " + main.currentStaff.getEmail());
        idLine.setFont(UIUtils.uiFont(Font.PLAIN, 11)); idLine.setForeground(new Color(160, 195, 240));
        bannerLeft.add(greeting); bannerLeft.add(Box.createVerticalStrut(5)); bannerLeft.add(idLine);

        UIUtils.RoundedButton changeBgBtn = new UIUtils.RoundedButton("Change Background", new Color(50, 90, 170));
        changeBgBtn.setFont(UIUtils.uiFont(Font.BOLD, 11));
        changeBgBtn.setPreferredSize(new Dimension(168, 32));
        changeBgBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(main) == JFileChooser.APPROVE_OPTION) {
                String saved = UIUtils.saveBgImage(main.currentStaff.getUserId(), "counter", fc.getSelectedFile());
                if (saved != null) { main.homeBgImage = UIUtils.loadBgImage(main.currentStaff.getUserId(), "counter"); main.showHome(); }
                else JOptionPane.showMessageDialog(main, "Failed to save background image.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        UIUtils.RoundedButton clearBgBtn = new UIUtils.RoundedButton("Clear", new Color(150, 50, 50));
        clearBgBtn.setFont(UIUtils.uiFont(Font.BOLD, 11));
        clearBgBtn.setPreferredSize(new Dimension(70, 32));
        clearBgBtn.addActionListener(e -> { UIUtils.deleteBgImage(main.currentStaff.getUserId(), "counter"); main.homeBgImage = null; main.showHome(); });

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnGroup.setOpaque(false);
        btnGroup.add(changeBgBtn); btnGroup.add(clearBgBtn);
        banner.add(bannerLeft, BorderLayout.CENTER);
        banner.add(btnGroup,   BorderLayout.EAST);
        return banner;
    }

    private JPanel buildStatsWrapper() {
        int    custCount = FileManager.readAllCustomers().size();
        long   aptCount  = FileManager.readAllAppointments().size();
        long   pending   = FileManager.readAllAppointments().stream()
                .filter(a -> Appointment.STATUS_AWAITING_PAYMENT.equals(a.getStatus())).count();
        double revenue   = FileManager.readAllPayments().stream().mapToDouble(Payment::getAmount).sum();

        JPanel statsWrapper = new JPanel(new BorderLayout());
        statsWrapper.setOpaque(false);
        statsWrapper.setPreferredSize(new Dimension(-1, 98));
        statsWrapper.add(buildStatsRow(custCount, aptCount, pending, revenue), BorderLayout.CENTER);
        return statsWrapper;
    }

    private JPanel buildStatsRow(int custs, long apts, long pending, double revenue) {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setOpaque(false);
        row.add(statCard("Customers",        String.valueOf(custs),             UIUtils.ACCENT_BLUE, "Registered"));
        row.add(statCard("Appointments",     String.valueOf(apts),              CounterStaffDashboard.GREEN_OK,            "All records"));
        row.add(statCard("Awaiting Payment", String.valueOf(pending),           CounterStaffDashboard.ACCENT_YELLOW,       "Need payment"));
        row.add(statCard("Revenue",          String.format("RM %.0f", revenue), CounterStaffDashboard.PURPLE,              "Total collected"));
        return row;
    }

    private JPanel statCard(String title, String value, Color accent, String desc) {
        JPanel c = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIUtils.TABLE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), 10, 12, 12);
                g2.fillRect(0, 5, getWidth(), 5);
                g2.dispose();
            }
        };
        c.setOpaque(false);
        c.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(accent, 12, 1), new EmptyBorder(15, 10, 10, 10)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 2, 0);
        JLabel valLbl = new JLabel(value, SwingConstants.CENTER);
        valLbl.setFont(UIUtils.uiFont(Font.BOLD, 20)); valLbl.setForeground(accent); c.add(valLbl, gbc);
        gbc.gridy = 1;
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(UIUtils.uiFont(Font.BOLD, 11)); titleLbl.setForeground(UIUtils.WHITE); c.add(titleLbl, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 0, 0);
        JLabel descLbl = new JLabel(desc, SwingConstants.CENTER);
        descLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10)); descLbl.setForeground(UIUtils.DIM_TEXT); c.add(descLbl, gbc);
        return c;
    }

    private JPanel buildBottomRow() {
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 16, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(buildPendingAptsCard());
        bottomRow.add(buildQuickActions());
        return bottomRow;
    }

    private JPanel buildPendingAptsCard() {
        List<Appointment> pending = FileManager.readAllAppointments().stream()
                .filter(a -> Appointment.STATUS_AWAITING_PAYMENT.equals(a.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(UIUtils.TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(CounterStaffDashboard.ACCENT_YELLOW, 12, 1), new EmptyBorder(14, 18, 14, 18)));
        JLabel hdr = new JLabel(pending.isEmpty() ? "\u23F0  No Appointments Awaiting Payment"
                : "\u23F0  Awaiting Payment (" + pending.size() + ")");
        hdr.setFont(UIUtils.uiFont(Font.BOLD, 13)); hdr.setForeground(CounterStaffDashboard.ACCENT_YELLOW);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(UIUtils.TABLE_BG);

        if (pending.isEmpty()) {
            JLabel none = new JLabel("All appointments are completed.");
            none.setForeground(UIUtils.LIGHT_GRAY); none.setFont(UIUtils.uiFont(Font.ITALIC, 11));
            rows.add(none);
        } else {
            int max = Math.min(pending.size(), 3);
            for (int i = 0; i < max; i++) {
                Appointment a    = pending.get(i);
                Customer    cust = a.getCustomer();
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setBackground(new Color(44, 68, 120));
                row.setBorder(BorderFactory.createCompoundBorder(
                        new UIUtils.RoundedBorder(new Color(60, 90, 150), 8, 1), new EmptyBorder(5, 8, 5, 8)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
                JLabel lft = new JLabel(a.getAppointmentId() + "  \u2022  " + a.getDate() + " " + a.getTime());
                lft.setFont(UIUtils.uiFont(Font.PLAIN, 11)); lft.setForeground(UIUtils.WHITE);
                JLabel rgt = new JLabel(cust != null ? cust.getName() : a.getCustomerId());
                rgt.setFont(UIUtils.uiFont(Font.BOLD, 11)); rgt.setForeground(CounterStaffDashboard.ACCENT_YELLOW);
                row.add(lft, BorderLayout.WEST); row.add(rgt, BorderLayout.EAST);
                rows.add(row); rows.add(Box.createVerticalStrut(4));
            }
        }
        card.add(hdr,  BorderLayout.NORTH);
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildQuickActions() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(UIUtils.TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1), new EmptyBorder(14, 18, 14, 18)));
        JLabel hdr = new JLabel("\u26A1  Quick Actions");
        hdr.setFont(UIUtils.uiFont(Font.BOLD, 13)); hdr.setForeground(UIUtils.ACCENT_BLUE);
        JPanel btns = new JPanel(new GridLayout(2, 2, 10, 10));
        btns.setBackground(UIUtils.TABLE_BG);
        btns.add(quickBtn("Customer Mgmt",   new Color(60, 125, 200), main.customersMenuBtn));
        btns.add(quickBtn("Appointments",    new Color(55, 165, 120), main.aptsMenuBtn));
        btns.add(quickBtn("Top-Up Wallet",   new Color(195, 115, 55), main.topupMenuBtn));
        btns.add(quickBtn("Collect Payment", new Color(150, 95, 220), main.paymentsMenuBtn));
        card.add(hdr,  BorderLayout.NORTH);
        card.add(btns, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 150));
        return card;
    }

    private JButton quickBtn(String text, Color bg, JButton target) {
        UIUtils.RoundedButton b = new UIUtils.RoundedButton(text, bg);
        b.setFont(UIUtils.uiFont(Font.BOLD, 11));
        b.setPreferredSize(new Dimension(0, 28));
        b.addActionListener(e -> target.doClick());
        return b;
    }
}
