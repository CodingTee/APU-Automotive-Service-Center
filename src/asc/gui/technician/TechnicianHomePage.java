package asc.gui.technician;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.List;

public class TechnicianHomePage extends JPanel {

    private static final long serialVersionUID = 1L;
    final TechnicianDashboard main;

    public TechnicianHomePage(TechnicianDashboard m) {
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
        JLabel greeting = new JLabel("Welcome back, " + main.currentTech.getName() + "!");
        greeting.setFont(UIUtils.uiFont(Font.BOLD, 20));
        greeting.setForeground(UIUtils.WHITE);
        JLabel idLine = new JLabel(main.currentTech.getName() + "  " + main.currentTech.getEmail());
        idLine.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        idLine.setForeground(new Color(160, 195, 240));
        bannerLeft.add(greeting); bannerLeft.add(Box.createVerticalStrut(5)); bannerLeft.add(idLine);

        UIUtils.RoundedButton viewAptsBtn = new UIUtils.RoundedButton("View My Appointments", new Color(50, 90, 170));
        viewAptsBtn.setFont(UIUtils.uiFont(Font.BOLD, 11));
        viewAptsBtn.setPreferredSize(new Dimension(200, 32));
        viewAptsBtn.addActionListener(e -> main.aptsMenuBtn.doClick());

        UIUtils.RoundedButton changeBgBtn = new UIUtils.RoundedButton("Change Background", new Color(50, 90, 170));
        changeBgBtn.setFont(UIUtils.uiFont(Font.BOLD, 11));
        changeBgBtn.setPreferredSize(new Dimension(168, 32));
        changeBgBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                String saved = UIUtils.saveBgImage(
                        main.currentTech.getUserId(), "technician", fc.getSelectedFile());
                if (saved != null) {
                    main.homeBgImage = UIUtils.loadBgImage(main.currentTech.getUserId(), "technician");
                    main.showHome();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to save background image.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        UIUtils.RoundedButton clearBgBtn = new UIUtils.RoundedButton("Clear", new Color(150, 50, 50));
        clearBgBtn.setFont(UIUtils.uiFont(Font.BOLD, 11));
        clearBgBtn.setPreferredSize(new Dimension(70, 32));
        clearBgBtn.addActionListener(e -> {
            UIUtils.deleteBgImage(main.currentTech.getUserId(), "technician");
            main.homeBgImage = null;
            main.showHome();
        });

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnGroup.setOpaque(false);
        btnGroup.add(changeBgBtn); btnGroup.add(clearBgBtn); btnGroup.add(viewAptsBtn);
        banner.add(bannerLeft, BorderLayout.CENTER);
        banner.add(btnGroup,   BorderLayout.EAST);

        // stat cards
        List<Appointment> myApts = FileManager.readAllAppointments().stream()
                .filter(a -> a.getTechnicianId().equals(main.currentTech.getUserId()))
                .collect(java.util.stream.Collectors.toList());
        long completed = myApts.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
        long pending   = myApts.stream().filter(a -> "PENDING".equalsIgnoreCase(a.getStatus())).count();
        long withFb    = myApts.stream()
                .filter(a -> FileManager.findFeedbackByAppointmentId(a.getAppointmentId()) != null).count();

        JPanel statsWrapper = new JPanel(new BorderLayout());
        statsWrapper.setOpaque(false);
        statsWrapper.setPreferredSize(new Dimension(-1, 98));
        statsWrapper.add(buildStatsRow(myApts.size(), completed, pending, withFb), BorderLayout.CENTER);

        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 16, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(buildPendingCard());
        bottomRow.add(buildQuickActions());

        root.add(banner,       BorderLayout.NORTH);
        root.add(statsWrapper, BorderLayout.CENTER);
        root.add(bottomRow,    BorderLayout.SOUTH);
        return root;
    }

    JPanel buildStatsRow(int total, long comp, long pend, long fb) {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setOpaque(false);
        row.add(statCard("Total Assigned", String.valueOf(total), UIUtils.ACCENT_BLUE, "All appointments"));
        row.add(statCard("Completed",      String.valueOf(comp),  TechnicianDashboard.GREEN_OK,            "Finished jobs"));
        row.add(statCard("Pending",        String.valueOf(pend),  TechnicianDashboard.ACCENT_YELLOW,       "Upcoming work"));
        row.add(statCard("Feedback Given", String.valueOf(fb),    TechnicianDashboard.PURPLE,              "Written feedbacks"));
        return row;
    }

    JPanel statCard(String title, String value, Color accent, String desc) {
        JPanel c = new JPanel(new GridBagLayout()) {
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
        valLbl.setFont(UIUtils.uiFont(Font.BOLD, 22)); valLbl.setForeground(accent); c.add(valLbl, gbc);
        gbc.gridy = 1;
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(UIUtils.uiFont(Font.BOLD, 12)); titleLbl.setForeground(UIUtils.WHITE); c.add(titleLbl, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 0, 0);
        JLabel descLbl = new JLabel(desc, SwingConstants.CENTER);
        descLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10)); descLbl.setForeground(UIUtils.DIM_TEXT); c.add(descLbl, gbc);
        return c;
    }

    JPanel buildPendingCard() {
        List<Appointment> pending = FileManager.readAllAppointments().stream()
                .filter(a -> a.getTechnicianId().equals(main.currentTech.getUserId())
                        && "PENDING".equalsIgnoreCase(a.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(UIUtils.TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(TechnicianDashboard.ACCENT_YELLOW, 12, 1), new EmptyBorder(14, 18, 14, 18)));
        JLabel hdr = new JLabel(pending.isEmpty() ? "\u23F0  No Pending Appointments"
                : "\u23F0  Pending (" + pending.size() + ")");
        hdr.setFont(UIUtils.uiFont(Font.BOLD, 13)); hdr.setForeground(TechnicianDashboard.ACCENT_YELLOW);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(UIUtils.TABLE_BG);

        if (pending.isEmpty()) {
            JLabel none = new JLabel("All caught up! No pending jobs.");
            none.setForeground(UIUtils.LIGHT_GRAY); none.setFont(UIUtils.uiFont(Font.ITALIC, 11));
            rows.add(none);
        } else {
            for (Appointment a : pending) {
                Customer cust = a.getCustomer();
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setBackground(new Color(44, 68, 120));
                row.setBorder(BorderFactory.createCompoundBorder(
                        new UIUtils.RoundedBorder(new Color(60, 90, 150), 8, 1), new EmptyBorder(6, 10, 6, 10)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                JLabel left  = new JLabel(a.getAppointmentId() + "  \u2022  " + a.getDate() + " " + a.getTime());
                left.setFont(UIUtils.uiFont(Font.PLAIN, 11)); left.setForeground(UIUtils.WHITE);
                JLabel right = new JLabel(cust != null ? cust.getName() : a.getCustomerId());
                right.setFont(UIUtils.uiFont(Font.BOLD, 11)); right.setForeground(TechnicianDashboard.ACCENT_YELLOW);
                row.add(left, BorderLayout.WEST); row.add(right, BorderLayout.EAST);
                rows.add(row); rows.add(Box.createVerticalStrut(5));
            }
        }
        card.add(hdr,  BorderLayout.NORTH);
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    JPanel buildQuickActions() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(UIUtils.TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1), new EmptyBorder(14, 18, 14, 18)));
        JLabel hdr = new JLabel("\u26A1  Quick Actions");
        hdr.setFont(UIUtils.uiFont(Font.BOLD, 13)); hdr.setForeground(UIUtils.ACCENT_BLUE);
        JPanel btns = new JPanel(new GridLayout(2, 2, 10, 10));
        btns.setBackground(UIUtils.TABLE_BG);
        btns.add(quickBtn("My Appointments",   new Color(60, 125, 200), main.aptsMenuBtn));
        btns.add(quickBtn("Health Records",    new Color(55, 165, 120), main.healthMenuBtn));
        btns.add(quickBtn("My Analytics",      new Color(150, 95, 220), main.analyticsMenuBtn));
        btns.add(quickBtn("My Schedule",       new Color(195, 115, 55), main.scheduleMenuBtn));
        card.add(hdr,  BorderLayout.NORTH);
        card.add(btns, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 150));
        return card;
    }

    /** Quick action button — delegates to the corresponding sidebar button. */
    JButton quickBtn(String text, Color bg, JButton sidebarBtn) {
        UIUtils.RoundedButton b = new UIUtils.RoundedButton(text, bg);
        b.setFont(UIUtils.uiFont(Font.BOLD, 11));
        b.setPreferredSize(new Dimension(0, 28));
        b.addActionListener(e -> sidebarBtn.doClick());
        return b;
    }

}
