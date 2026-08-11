package asc.gui.customer;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.List;

public class CustomerHomePage extends JPanel {

    private static final long serialVersionUID = 1L;
    final CustomerDashboard main;

    public CustomerHomePage(CustomerDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UIUtils.DARK_NAVY);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));

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
                    g2.setPaint(new GradientPaint(0, 0, new Color(18, 38, 82, 220), getWidth(), getHeight(), new Color(40, 90, 180, 200)));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                }
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 20, 1), new EmptyBorder(100, 32, 100, 32)));

        JPanel bannerLeft = new JPanel();
        bannerLeft.setLayout(new BoxLayout(bannerLeft, BoxLayout.Y_AXIS));
        bannerLeft.setOpaque(false);
        JLabel greeting = new JLabel("Welcome back, " + main.currentCustomer.getName() + "!");
        greeting.setFont(UIUtils.uiFont(Font.BOLD, 20)); greeting.setForeground(UIUtils.WHITE);
        JLabel vehicle = new JLabel(main.currentCustomer.getPrimaryVehicle()
                + (main.currentCustomer.hasMultipleVehicles() ? " (+" + (main.currentCustomer.getVehicleList().size()-1) + " more)" : ""));
        vehicle.setFont(UIUtils.uiFont(Font.PLAIN, 12)); vehicle.setForeground(UIUtils.LIGHT_GRAY);
        JLabel idLine = new JLabel(main.currentCustomer.getName() + "  " + main.currentCustomer.getEmail());
        idLine.setFont(UIUtils.uiFont(Font.PLAIN, 11)); idLine.setForeground(new Color(160, 195, 240));
        bannerLeft.add(greeting); bannerLeft.add(Box.createVerticalStrut(5));
        bannerLeft.add(vehicle);  bannerLeft.add(Box.createVerticalStrut(3));
        bannerLeft.add(idLine);

        UIUtils.RoundedButton changeBgBtn = new UIUtils.RoundedButton("Change Background", new Color(50, 90, 170));
        changeBgBtn.setFont(UIUtils.uiFont(Font.BOLD, 11)); changeBgBtn.setPreferredSize(new Dimension(168, 32));
        changeBgBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                String saved = UIUtils.saveBgImage(main.currentCustomer.getUserId(), "customer", fc.getSelectedFile());
                if (saved != null) { main.homeBgImage = UIUtils.loadBgImage(main.currentCustomer.getUserId(), "customer"); main.showHome(); }
                else JOptionPane.showMessageDialog(this, "Failed to save background image.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        UIUtils.RoundedButton clearBgBtn = new UIUtils.RoundedButton("Clear", new Color(150, 50, 50));
        clearBgBtn.setFont(UIUtils.uiFont(Font.BOLD, 11)); clearBgBtn.setPreferredSize(new Dimension(80, 32));
        clearBgBtn.addActionListener(e -> { UIUtils.deleteBgImage(main.currentCustomer.getUserId(), "customer"); main.homeBgImage = null; main.showHome(); });

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnGroup.setOpaque(false);
        btnGroup.add(changeBgBtn); btnGroup.add(clearBgBtn);
        banner.add(bannerLeft, BorderLayout.CENTER);
        banner.add(btnGroup, BorderLayout.EAST);

        List<Appointment> myApts = main.currentCustomer.getMyAppointments();
        long completed  = main.currentCustomer.getCompletedCount();
        long pending    = main.currentCustomer.getPendingCount();
        double totalSpent = main.currentCustomer.getTotalSpent();

        JPanel statsWrapper = new JPanel(new BorderLayout());
        statsWrapper.setOpaque(false);
        statsWrapper.setPreferredSize(new Dimension(-1, 98));
        JPanel[] statsHolder = {buildStatsRow(myApts.size(), completed, pending, totalSpent, false)};
        statsWrapper.add(statsHolder[0], BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                boolean narrow = getWidth() < 780;
                boolean is2x2  = statsHolder[0].getLayout() instanceof GridLayout && ((GridLayout) statsHolder[0].getLayout()).getRows() == 2;
                if (narrow != is2x2) {
                    statsWrapper.remove(statsHolder[0]);
                    statsHolder[0] = buildStatsRow(myApts.size(), completed, pending, totalSpent, narrow);
                    statsWrapper.add(statsHolder[0], BorderLayout.CENTER);
                    statsWrapper.setPreferredSize(new Dimension(-1, narrow ? 180 : 98));
                    statsWrapper.revalidate(); statsWrapper.repaint();
                }
            }
        });

        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 16, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(buildReminderCard());
        bottomRow.add(buildQuickActions());

        root.add(banner,       BorderLayout.NORTH);
        root.add(statsWrapper, BorderLayout.CENTER);
        root.add(bottomRow,    BorderLayout.SOUTH);
        return root;
    }

    JPanel statCard(String title, String value, Color accent, String desc) {
        JPanel c = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIUtils.TABLE_BG); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accent); g2.fillRoundRect(0, 0, getWidth(), 10, 12, 12); g2.fillRect(0, 5, getWidth(), 5);
                g2.dispose();
            }
        };
        c.setOpaque(false);
        c.setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(accent, 12, 1), new EmptyBorder(15, 10, 10, 10)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 2, 0);
        JLabel valLbl = new JLabel(value, SwingConstants.CENTER); valLbl.setFont(UIUtils.uiFont(Font.BOLD, 22)); valLbl.setForeground(accent); c.add(valLbl, gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 2, 0);
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER); titleLbl.setFont(UIUtils.uiFont(Font.BOLD, 12)); titleLbl.setForeground(UIUtils.WHITE); c.add(titleLbl, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 0, 0);
        JLabel descLbl = new JLabel(desc, SwingConstants.CENTER); descLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10)); descLbl.setForeground(UIUtils.DIM_TEXT); c.add(descLbl, gbc);
        return c;
    }

    JPanel buildStatsRow(int total, long completed, long pending, double spent, boolean narrow) {
        JPanel row = new JPanel(narrow ? new GridLayout(2, 2, 10, 10) : new GridLayout(1, 4, 10, 0));
        row.setOpaque(false);
        row.add(statCard("Total Visits", String.valueOf(total),            UIUtils.ACCENT_BLUE, "All appointments"));
        row.add(statCard("Completed",    String.valueOf(completed),        CustomerDashboard.GREEN_OK,            "Services done"));
        row.add(statCard("Upcoming",     String.valueOf(pending),          CustomerDashboard.ACCENT_YELLOW,       "Pending services"));
        row.add(statCard("Total Spent",  String.format("RM %.2f", spent), CustomerDashboard.PURPLE,              "Lifetime payments"));
        return row;
    }

    JPanel buildReminderCard() {
        List<Appointment> pending = main.currentCustomer.getMyPendingAppointments();
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(UIUtils.TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(CustomerDashboard.ACCENT_YELLOW, 12, 1), new EmptyBorder(14, 18, 14, 18)));
        JLabel hdr = new JLabel(pending.isEmpty() ? "\u23F0  No Upcoming Appointments" : "\u23F0  Upcoming (" + pending.size() + ")");
        //panel.add(main.sectionTitle("Service & Payment History", CustomerDashboard.ICON_BASE + "Service History(Blue).png"), BorderLayout.NORTH);
        hdr.setFont(UIUtils.uiFont(Font.BOLD, 13)); hdr.setForeground(CustomerDashboard.ACCENT_YELLOW);
        JPanel rows = new JPanel(); rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS)); rows.setBackground(UIUtils.TABLE_BG);
        if (pending.isEmpty()) {
            JLabel none = new JLabel("All done! Book your next service at the counter.");
            none.setForeground(UIUtils.LIGHT_GRAY); none.setFont(UIUtils.uiFont(Font.ITALIC, 11)); rows.add(none);
        } else {
            for (Appointment a : pending) {
                String t = "NORMAL".equalsIgnoreCase(a.getServiceType()) ? "Normal" : "Major";
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setBackground(new Color(44, 68, 120));
                row.setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(new Color(60, 90, 150), 8, 1), new EmptyBorder(6, 10, 6, 10)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                JLabel left  = new JLabel(a.getAppointmentId() + "  \u2022  " + a.getDate() + " " + a.getTime());
                left.setFont(UIUtils.uiFont(Font.PLAIN, 11)); left.setForeground(UIUtils.WHITE);
                JLabel right = new JLabel(t + " Service");
                right.setFont(UIUtils.uiFont(Font.BOLD, 11)); right.setForeground(CustomerDashboard.ACCENT_YELLOW);
                row.add(left, BorderLayout.WEST); row.add(right, BorderLayout.EAST);
                rows.add(row); rows.add(Box.createVerticalStrut(5));
            }
        }
        card.add(hdr, BorderLayout.NORTH); card.add(rows, BorderLayout.CENTER);
        return card;
    }

    JPanel buildQuickActions() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(UIUtils.TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1), new EmptyBorder(14, 18, 14, 18)));
        JLabel hdr = new JLabel("\u26A1  Quick Actions"); hdr.setFont(UIUtils.uiSymbolFont(Font.BOLD, 13)); hdr.setForeground(UIUtils.ACCENT_BLUE);
        JPanel btns = new JPanel(new GridLayout(2, 2, 10, 10)); btns.setBackground(UIUtils.TABLE_BG);
        btns.add(quickBtn("Service History", new Color(60, 125, 200), main.historyMenuBtn));
        btns.add(quickBtn("Vehicle Health",  new Color(55, 165, 120), main.healthMenuBtn));
        btns.add(quickBtn("Leave Comment",   new Color(150, 95, 220), main.commentMenuBtn));
        btns.add(quickBtn("Receipt & Print", new Color(195, 115, 55), main.receiptMenuBtn));
        card.add(hdr, BorderLayout.NORTH); card.add(btns, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 150));
        return card;
    }

    JButton quickBtn(String text, Color bg, JButton sidebarBtn) {
        UIUtils.RoundedButton b = new UIUtils.RoundedButton(text, bg);
        b.setFont(UIUtils.uiFont(Font.BOLD, 11));
        b.setPreferredSize(new Dimension(0, 28));
        b.addActionListener(e -> sidebarBtn.doClick());
        return b;
    }

}
