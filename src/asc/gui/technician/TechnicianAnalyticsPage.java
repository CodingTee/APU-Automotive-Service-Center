package asc.gui.technician;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class TechnicianAnalyticsPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final TechnicianDashboard main;

    public TechnicianAnalyticsPage(TechnicianDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIUtils.DARK_NAVY);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.add(main.sectionTitle("My Analytics", TechnicianDashboard.ICON_BASE + "My Analytics(Blue).png"), BorderLayout.NORTH);

        // ── Load all appointments for this technician ─────────────────────
        java.util.List<Appointment> myApts = new ArrayList<>();
        for (Appointment a : FileManager.readAllAppointments()) {
            if (a.getTechnicianId().equals(main.currentTech.getUserId()))
                myApts.add(a);
        }

        int total     = myApts.size();
        int completed = 0;
        int pending   = 0;
        int normal    = 0;
        int major     = 0;

        String[] monthNames = {"Jan","Feb","Mar","Apr","May","Jun",
                               "Jul","Aug","Sep","Oct","Nov","Dec"};
        int[] monthCounts   = new int[12];
        int   bestMonthIdx  = 0;

        for (Appointment a : myApts) {
            if ("COMPLETED".equalsIgnoreCase(a.getStatus())) completed++;
            else pending++;
            if ("NORMAL".equalsIgnoreCase(a.getServiceType())) normal++;
            else major++;
            try {
                String[] parts = a.getDate().split("-");
                int mIdx = Integer.parseInt(parts[1]) - 1;
                if (mIdx >= 0 && mIdx < 12) monthCounts[mIdx]++;
            } catch (Exception ex) { /* skip */ }
        }

        for (int i = 0; i < 12; i++)
            if (monthCounts[i] > monthCounts[bestMonthIdx])
                bestMonthIdx = i;

        final int TOTAL     = total;
        final int COMPLETED = completed;
        final int NORMAL    = normal;
        final int MAJOR     = major;
        final int[] MONTHS  = monthCounts;
        final String BEST   = total == 0 ? "N/A" : monthNames[bestMonthIdx]
                + " (" + monthCounts[bestMonthIdx] + " jobs)";
        final int COMP_RATE = total == 0 ? 0 : (completed * 100) / total;

        // ── Top summary cards ─────────────────────────────────────────────
        JPanel topCards = new JPanel(new GridLayout(1, 4, 10, 0));
        topCards.setOpaque(false);
        topCards.add(main.statCard("Total Jobs",       String.valueOf(total),     UIUtils.ACCENT_BLUE, "All appointments"));
        topCards.add(main.statCard("Completed",        String.valueOf(completed), TechnicianDashboard.GREEN_OK,            "Finished jobs"));
        topCards.add(main.statCard("Completion Rate",  COMP_RATE + "%",           TechnicianDashboard.PURPLE,              "Jobs completed"));
        topCards.add(main.statCard("Best Month",       BEST,                      TechnicianDashboard.ACCENT_YELLOW,       "Most appointments"));

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setOpaque(false);
        topWrapper.setPreferredSize(new Dimension(0, 110));
        topWrapper.add(topCards, BorderLayout.CENTER);

        // ── Bottom: 3 charts side by side ────────────────────────────────
        JPanel chartsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        chartsRow.setOpaque(false);

        chartsRow.add(buildPieChartPanel(
                "■  Monthly Workload",
                monthNames, MONTHS, new Color[]{
                    new Color(255, 99,  132), new Color(255, 159,  64),
                    new Color(255, 205,  86), new Color( 75, 192, 192),
                    new Color( 54, 162, 235), new Color(153, 102, 255),
                    new Color(220,  80,  80), new Color( 60, 180, 120),
                    new Color(200, 100,  50), new Color( 80, 160, 220),
                    new Color(180,  60, 200), new Color(100, 200, 100)
                }));

        chartsRow.add(buildServiceBreakdownChart(NORMAL, MAJOR, TOTAL));
        chartsRow.add(buildCompletionRateChart(COMPLETED, pending, COMP_RATE));

        panel.add(topWrapper,  BorderLayout.NORTH);
        panel.add(chartsRow,   BorderLayout.CENTER);
        return panel;
    }
    
    JPanel buildPieChartPanel(String titleText,
            String[] labels, int[] values, Color[] colors) {

        int total = 0;
        for (int v : values) total += v;
        final int TOTAL = total;

        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIUtils.TABLE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 14, 1),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel title = new JLabel(titleText, JLabel.CENTER);
        title.setFont(UIUtils.uiFont(Font.BOLD, 13));
        title.setForeground(UIUtils.ACCENT_BLUE);

        JPanel pie = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIUtils.TABLE_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int size = Math.min(getWidth(), getHeight()) - 40;
                int cx   = getWidth()  / 2;
                int cy   = getHeight() / 2;
                int x    = cx - size / 2;
                int y    = cy - size / 2;

                if (TOTAL == 0) {
                    g2.setColor(new Color(60, 85, 130));
                    g2.fillOval(x, y, size, size);
                    g2.setColor(UIUtils.LIGHT_GRAY);
                    g2.setFont(UIUtils.uiFont(Font.BOLD, 12));
                    g2.drawString("No Data", cx - 28, cy + 4);
                    g2.dispose();
                    return;
                }

                double startAngle = 0;
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == 0) continue;
                    double sliceAngle = 360.0 * values[i] / TOTAL;

                    g2.setColor(colors[i % colors.length]);
                    g2.fillArc(x, y, size, size,
                            (int) startAngle, (int) Math.ceil(sliceAngle));

                    g2.setColor(UIUtils.DARK_NAVY);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawArc(x, y, size, size,
                            (int) startAngle, (int) Math.ceil(sliceAngle));

                    // % label inside slice
                    int pct = (int) Math.round(100.0 * values[i] / TOTAL);
                    if (pct >= 8) {
                        double mid = Math.toRadians(startAngle + sliceAngle / 2);
                        int lx = (int) (cx + (size / 3.2) * Math.cos(mid));
                        int ly = (int) (cy - (size / 3.2) * Math.sin(mid));
                        g2.setColor(Color.WHITE);
                        g2.setFont(UIUtils.uiFont(Font.BOLD, 10));
                        String ps = pct + "%";
                        g2.drawString(ps, lx - g2.getFontMetrics()
                                .stringWidth(ps) / 2, ly + 4);
                    }
                    startAngle += sliceAngle;
                }

                // Outer ring
                g2.setColor(new Color(80, 110, 160));
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(x, y, size, size);
                g2.dispose();
            }
        };
        pie.setBackground(UIUtils.TABLE_BG);

        // Legend
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setBackground(UIUtils.TABLE_BG);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) continue;
            int pct = (int) Math.round(100.0 * values[i] / TOTAL);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
            row.setBackground(UIUtils.TABLE_BG);
            JLabel dot = new JLabel("\u25CF");
            dot.setForeground(colors[i % colors.length]);
            dot.setFont(UIUtils.uiSymbolFont(Font.BOLD, 10));
            JLabel lbl = new JLabel(labels[i] + ": " + values[i]
                    + " (" + pct + "%)");
            lbl.setForeground(UIUtils.WHITE);
            lbl.setFont(UIUtils.uiFont(Font.PLAIN, 10));
            row.add(dot); row.add(lbl);
            legend.add(row);
        }

        JScrollPane legendScroll = new JScrollPane(legend);
        legendScroll.setBorder(null);
        legendScroll.setBackground(UIUtils.TABLE_BG);
        legendScroll.getViewport().setBackground(UIUtils.TABLE_BG);
        legendScroll.setPreferredSize(new Dimension(0, 80));

        card.add(title,        BorderLayout.NORTH);
        card.add(pie,          BorderLayout.CENTER);
        card.add(legendScroll, BorderLayout.SOUTH);
        return card;
    }
    
    JPanel buildServiceBreakdownChart(int normal, int major, int total) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIUtils.TABLE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(TechnicianDashboard.ACCENT_YELLOW, 14, 1),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel title = new JLabel("●  Service Type Breakdown", JLabel.CENTER);
        title.setFont(UIUtils.uiFont(Font.BOLD, 13));
        title.setForeground(TechnicianDashboard.ACCENT_YELLOW);

        // Pie chart with just 2 slices
        int[]    vals   = {normal, major};
        Color[]  cols   = {new Color(54, 162, 235), new Color(255, 99, 132)};
        String[] lbls   = {"NORMAL", "MAJOR"};
        final int TOT   = total;

        JPanel pie = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIUtils.TABLE_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int size = Math.min(getWidth(), getHeight()) - 40;
                int cx = getWidth() / 2, cy = getHeight() / 2;
                int x  = cx - size / 2,  y  = cy - size / 2;

                if (TOT == 0) {
                    g2.setColor(new Color(60, 85, 130));
                    g2.fillOval(x, y, size, size);
                    g2.setColor(UIUtils.LIGHT_GRAY);
                    g2.setFont(UIUtils.uiFont(Font.BOLD, 12));
                    g2.drawString("No Data", cx - 28, cy + 4);
                    g2.dispose(); return;
                }

                double start = 0;
                for (int i = 0; i < 2; i++) {
                    if (vals[i] == 0) continue;
                    double angle = 360.0 * vals[i] / TOT;
                    g2.setColor(cols[i]);
                    g2.fillArc(x, y, size, size, (int) start, (int) Math.ceil(angle));
                    g2.setColor(UIUtils.DARK_NAVY);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawArc(x, y, size, size, (int) start, (int) Math.ceil(angle));

                    int pct = (int) Math.round(100.0 * vals[i] / TOT);
                    if (pct >= 8) {
                        double mid = Math.toRadians(start + angle / 2);
                        int lx = (int)(cx + (size/3.2) * Math.cos(mid));
                        int ly = (int)(cy - (size/3.2) * Math.sin(mid));
                        g2.setColor(Color.WHITE);
                        g2.setFont(UIUtils.uiFont(Font.BOLD, 11));
                        String ps = pct + "%";
                        g2.drawString(ps, lx - g2.getFontMetrics()
                                .stringWidth(ps)/2, ly + 4);
                    }
                    start += angle;
                }
                g2.setColor(new Color(80, 110, 160));
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(x, y, size, size);
                g2.dispose();
            }
        };
        pie.setBackground(UIUtils.TABLE_BG);

        // Legend
        JPanel legend = new JPanel(new GridLayout(2, 1, 4, 4));
        legend.setBackground(UIUtils.TABLE_BG);
        for (int i = 0; i < 2; i++) {
            int pct = TOT == 0 ? 0 : (int) Math.round(100.0 * vals[i] / TOT);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
            row.setBackground(UIUtils.TABLE_BG);
            JLabel dot = new JLabel("\u25CF");
            dot.setForeground(cols[i]);
            dot.setFont(UIUtils.uiSymbolFont(Font.BOLD, 14));
            JLabel lbl = new JLabel(lbls[i] + ": " + vals[i] + " (" + pct + "%)");
            lbl.setForeground(UIUtils.WHITE);
            lbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
            row.add(dot); row.add(lbl);
            legend.add(row);
        }

        card.add(title,  BorderLayout.NORTH);
        card.add(pie,    BorderLayout.CENTER);
        card.add(legend, BorderLayout.SOUTH);
        return card;
    }
    
    JPanel buildCompletionRateChart(int completed, int pending, int rate) {
        final int RATE      = rate;
        final int COMPLETED = completed;
        final int PENDING   = pending;

        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIUtils.TABLE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(TechnicianDashboard.GREEN_OK, 14, 1),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel title = new JLabel("✓  Completion Rate", JLabel.CENTER);
        title.setFont(UIUtils.uiFont(Font.BOLD, 13));
        title.setForeground(TechnicianDashboard.GREEN_OK);

        // Arc gauge
        JPanel gauge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIUtils.TABLE_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int size = Math.min(getWidth(), getHeight()) - 40;
                int cx   = getWidth()  / 2;
                int cy   = getHeight() / 2 + 10;
                int x    = cx - size / 2;
                int y    = cy - size / 2;

                // Background arc (grey track)
                g2.setColor(new Color(50, 70, 110));
                g2.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawArc(x + 10, y + 10, size - 20, size - 20, 200, -160);

                // Filled arc (green progress)
                int sweepAngle = (int) (-160.0 * RATE / 100);
                GradientPaint gp = new GradientPaint(
                        x, y,          new Color(60, 200, 120),
                        x + size, y,   new Color(30, 120, 80));
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawArc(x + 10, y + 10, size - 20, size - 20,
                        200, sweepAngle);

                // Center text
                g2.setColor(UIUtils.WHITE);
                g2.setFont(UIUtils.uiFont(Font.BOLD, 28));
                String rateStr = RATE + "%";
                int sw = g2.getFontMetrics().stringWidth(rateStr);
                g2.drawString(rateStr, cx - sw / 2, cy + 10);

                g2.setFont(UIUtils.uiFont(Font.PLAIN, 11));
                g2.setColor(UIUtils.LIGHT_GRAY);
                String sub = "Completion Rate";
                int subW = g2.getFontMetrics().stringWidth(sub);
                g2.drawString(sub, cx - subW / 2, cy + 28);

                g2.dispose();
            }
        };
        gauge.setBackground(UIUtils.TABLE_BG);

        // Legend
        JPanel legend = new JPanel(new GridLayout(2, 1, 4, 4));
        legend.setBackground(UIUtils.TABLE_BG);

        JPanel compRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        compRow.setBackground(UIUtils.TABLE_BG);
        JLabel compDot = new JLabel("\u25CF"); compDot.setForeground(TechnicianDashboard.GREEN_OK);
        compDot.setFont(UIUtils.uiSymbolFont(Font.BOLD, 14));
        JLabel compLbl = new JLabel("Completed: " + COMPLETED);
        compLbl.setForeground(UIUtils.WHITE);
        compLbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
        compRow.add(compDot); compRow.add(compLbl);

        JPanel pendRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        pendRow.setBackground(UIUtils.TABLE_BG);
        JLabel pendDot = new JLabel("\u25CF"); pendDot.setForeground(TechnicianDashboard.ACCENT_YELLOW);
        pendDot.setFont(UIUtils.uiSymbolFont(Font.BOLD, 14));
        JLabel pendLbl = new JLabel("Pending: " + PENDING);
        pendLbl.setForeground(UIUtils.WHITE);
        pendLbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
        pendRow.add(pendDot); pendRow.add(pendLbl);

        legend.add(compRow);
        legend.add(pendRow);

        card.add(title,  BorderLayout.NORTH);
        card.add(gauge,  BorderLayout.CENTER);
        card.add(legend, BorderLayout.SOUTH);
        return card;
    }


}
