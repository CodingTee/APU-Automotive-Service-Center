package asc.gui.customer;

import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

public class CustomerChartPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final CustomerDashboard main;

    public CustomerChartPage(CustomerDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY); panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.add(main.sectionTitle("Monthly Spending Summary", CustomerDashboard.ICON_BASE + "Spending Chart(Blue).png"), BorderLayout.NORTH);
        Map<String, Double> monthly = main.currentCustomer.getMonthlySpending();
        if (monthly.isEmpty()) { panel.add(main.emptyState("\uD83D\uDCB8", "No payment data available yet."), BorderLayout.CENTER); return panel; }
        List<String> months  = new ArrayList<>(monthly.keySet());
        List<Double> amounts = new ArrayList<>();
        for (String m : months) amounts.add(monthly.get(m));
        double maxVal = amounts.stream().mapToDouble(d -> d).max().orElse(1);
        double total  = amounts.stream().mapToDouble(d -> d).sum();
        Color[] palette = {UIUtils.ACCENT_BLUE, CustomerDashboard.GREEN_OK, CustomerDashboard.ACCENT_YELLOW, CustomerDashboard.PURPLE, new Color(255, 115, 95), new Color(85, 215, 215)};
        int[] barX = new int[months.size()], barW = new int[months.size()], barY = new int[months.size()], barH = new int[months.size()];
        int[] padLR = {75, 24, 38, 52};
        JPanel chart = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), padL = padLR[0], padR = padLR[1], padT = padLR[2], padB = padLR[3];
                int cw = w - padL - padR, ch = h - padT - padB;
                g2.setColor(UIUtils.TABLE_BG); g2.fillRoundRect(0, 0, w, h, 16, 16);
                for (int i = 0; i <= 5; i++) { int y = padT + ch - ch * i / 5; g2.setColor(new Color(58, 88, 145)); g2.drawLine(padL, y, padL + cw, y); g2.setColor(UIUtils.DIM_TEXT); g2.setFont(UIUtils.uiFont(Font.PLAIN, 10)); g2.drawString(String.format("RM%.0f", maxVal * i / 5), 2, y + 4); }
                g2.setColor(UIUtils.LIGHT_GRAY); g2.setStroke(new BasicStroke(1.5f)); g2.drawLine(padL, padT, padL, padT + ch); g2.drawLine(padL, padT + ch, padL + cw, padT + ch);
                int n = months.size(), bw = Math.max(18, cw / n - 14), gap = (cw - bw * n) / (n + 1);
                for (int i = 0; i < n; i++) {
                    double amt = amounts.get(i); int bh = (int)(ch * amt / maxVal), x = padL + gap + i * (bw + gap), y = padT + ch - bh;
                    barX[i] = x; barW[i] = bw; barY[i] = y; barH[i] = bh;
                    g2.setColor(palette[i % palette.length]); g2.fillRoundRect(x, y, bw, bh, 8, 8);
                    g2.setColor(Color.WHITE); g2.setFont(UIUtils.uiFont(Font.BOLD, 10)); String amtS = String.format("RM%.0f", amt); FontMetrics fm = g2.getFontMetrics(); g2.drawString(amtS, x + (bw - fm.stringWidth(amtS)) / 2, y - 5);
                    g2.setColor(UIUtils.LIGHT_GRAY); g2.setFont(UIUtils.uiFont(Font.PLAIN, 10)); fm = g2.getFontMetrics(); g2.drawString(months.get(i), x + (bw - fm.stringWidth(months.get(i))) / 2, padT + ch + 16);
                }
                g2.setColor(UIUtils.ACCENT_BLUE); g2.setFont(UIUtils.uiFont(Font.BOLD, 13)); String ct = "Monthly Spending (RM)"; FontMetrics fm = g2.getFontMetrics(); g2.drawString(ct, (w - fm.stringWidth(ct)) / 2, padT - 14);
            }
        };
        chart.setBackground(UIUtils.TABLE_BG); chart.setToolTipText("");
        chart.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                String tip = null;
                for (int i = 0; i < months.size(); i++) { if (e.getX() >= barX[i] && e.getX() <= barX[i] + barW[i] && e.getY() >= barY[i] && e.getY() <= barY[i] + barH[i]) { tip = months.get(i) + ":  RM " + String.format("%.2f", amounts.get(i)); break; } }
                chart.setToolTipText(tip);
            }
        });
        JPanel info = new JPanel(new GridLayout(1, 3, 12, 0)); info.setBackground(UIUtils.DARK_NAVY); info.setBorder(new EmptyBorder(12, 0, 0, 0));
        info.add(main.miniCard("Total Paid",    String.format("RM %.2f", total),               CustomerDashboard.PURPLE));
        info.add(main.miniCard("Months Active", String.valueOf(months.size()),                  UIUtils.ACCENT_BLUE));
        info.add(main.miniCard("Avg / Month",   String.format("RM %.2f", total/months.size()), CustomerDashboard.GREEN_OK));
        panel.add(chart, BorderLayout.CENTER); panel.add(info, BorderLayout.SOUTH);
        return panel;
    }


}
