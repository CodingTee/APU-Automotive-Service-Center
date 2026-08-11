package asc.gui.manager;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

public class ManagerReportsPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final ManagerDashboard main;

    public ManagerReportsPage(ManagerDashboard m) {
        main = m;
        setLayout(new BorderLayout(0, 8));
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY);

        panel.add(main.sectionTitle("Management Reports",
                ManagerDashboard.MGR_ICON_BASE + "Reports(Blue).png"), BorderLayout.NORTH);

        String[] months = {
            "2024-01","2024-02","2024-03","2024-04","2024-05","2024-06",
            "2024-07","2024-08","2024-09","2024-10","2024-11","2024-12",
            "2025-01","2025-02","2025-03","2025-04","2025-05","2025-06",
            "2025-07","2025-08","2025-09","2025-10","2025-11","2025-12",
            "2026-01","2026-02","2026-03","2026-04","2026-05","2026-06",
            "2026-07","2026-08","2026-09","2026-10","2026-11","2026-12"
        };
        JComboBox<String> fromCb = new JComboBox<>(months);
        JComboBox<String> toCb   = new JComboBox<>(months);
        UIUtils.styleCombo(fromCb); UIUtils.styleCombo(toCb);

        String curMonth = LocalDate.now().toString().substring(0, 7);
        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(curMonth)) {
                fromCb.setSelectedIndex(Math.max(0, i - 5));
                toCb.setSelectedIndex(i);
                break;
            }
        }

        JLabel fromLbl = label("From:"); JLabel toLbl = label("To:");
        UIUtils.RoundedButton genBtn  = btn("By Period",  UIUtils.SUCCESS_GREEN,   120);
        UIUtils.RoundedButton fullBtn = btn("All Time",   UIUtils.ACCENT_BLUE,     100);
        UIUtils.RoundedButton pdfBtn  = btn("Export PDF", new Color(160, 60, 200), 120);
        UIUtils.RoundedButton clrBtn  = btn("Clear",      UIUtils.MEDIUM_NAVY,      80);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.setBackground(new Color(24, 40, 82));
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1),
                new EmptyBorder(6, 10, 6, 10)));
        filterBar.add(fromLbl); filterBar.add(fromCb);
        filterBar.add(toLbl);   filterBar.add(toCb);
        filterBar.add(genBtn);  filterBar.add(fullBtn);
        filterBar.add(pdfBtn);  filterBar.add(clrBtn);

        // TAB 1: Text report — fixed-width card centered on dark background
        main.reportsArea = new JTextPane();
        main.reportsArea.setEditable(false);
        main.reportsArea.setBackground(UIUtils.TABLE_BG);
        main.reportsArea.setForeground(UIUtils.WHITE);
        Font mono = new Font("Monospaced", Font.PLAIN, 13);
        main.reportsArea.setFont(mono);
        main.reportsArea.setBorder(new EmptyBorder(12, 18, 12, 18));

        // Compute exact pixel width for 83 monospaced chars
        Canvas fakeCanvas = new Canvas();
        FontMetrics fm = fakeCanvas.getFontMetrics(mono);
        int charW = fm.charWidth('W');          // monospaced: any char = same width
        int textW = charW * 83 + 18 + 18;   // 83 chars + left/right border padding
        main.reportsArea.setPreferredSize(new Dimension(textW, 200));

        JScrollPane textScroll = new JScrollPane(main.reportsArea);
        textScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        textScroll.setBorder(null);
        textScroll.getViewport().setBackground(UIUtils.DARK_NAVY);
        UIUtils.styleScrollPane(textScroll);
        textScroll.getVerticalScrollBar().setUnitIncrement(16);
        textScroll.getHorizontalScrollBar().setUnitIncrement(16);

        // Wrap scrollpane in a card sized exactly to content width
        int cardW = textW + 8 + 8 + 17;  // text area + card padding + scrollbar
        JPanel cardWrapper = new JPanel(new BorderLayout());
        cardWrapper.setBackground(UIUtils.TABLE_BG);
        cardWrapper.setBorder(new EmptyBorder(8, 8, 8, 8));
        cardWrapper.setPreferredSize(new Dimension(cardW, 200));
        cardWrapper.add(textScroll, BorderLayout.CENTER);

        // Center the card in the tab page — fixed width, fill full height
        JPanel textSection = new JPanel(new GridBagLayout());
        textSection.setBackground(UIUtils.DARK_NAVY);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill   = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 40, 10, 40);
        textSection.add(cardWrapper, gbc);

        // TAB 2: Charts
        ChartPanel[] chartHolder = {null};
        JScrollPane chartScroll = new JScrollPane();
        chartScroll.setBorder(null);
        chartScroll.getViewport().setBackground(UIUtils.DARK_NAVY);
        UIUtils.styleScrollPane(chartScroll);
        chartScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UIUtils.MEDIUM_NAVY);
        tabs.setForeground(UIUtils.WHITE);
        tabs.setFont(UIUtils.uiFont(Font.BOLD, 12));
        tabs.addTab("\uD83D\uDCC4  Text Report",   textSection);
        tabs.addTab("\uD83D\uDCCA  Visual Charts", chartScroll);

        // Wire buttons
        Runnable refreshCharts = () -> {
            String from = (String) fromCb.getSelectedItem();
            String to   = (String) toCb.getSelectedItem();
            if (from == null || to == null) return;
            ChartPanel cp = new ChartPanel(main, from, to);
            chartHolder[0] = cp;
            chartScroll.setViewportView(cp);
            chartScroll.revalidate();
        };

        genBtn.addActionListener(e -> {
            String from = (String) fromCb.getSelectedItem();
            String to   = (String) toCb.getSelectedItem();
            if (from == null || to == null) return;
            if (from.compareTo(to) > 0) {
                Window w = SwingUtilities.getWindowAncestor(this);
                JOptionPane.showMessageDialog(w,
                        "'From' month cannot be later than 'To'.",
                        "Invalid Range", JOptionPane.WARNING_MESSAGE); return;
            }
            generateFilteredReport(from, to);
            refreshCharts.run();
        });

        fullBtn.addActionListener(e -> {
            generateFullReport();
            List<Appointment> allA = FileManager.readAllAppointments();
            List<Payment>     allP = FileManager.readAllPayments();
            String minMonth = allA.stream()
                    .map(Appointment::getDate).filter(d -> d != null && d.length() >= 7)
                    .map(d -> d.substring(0, 7)).min(String::compareTo)
                    .orElse((String) fromCb.getSelectedItem());
            String maxMonth = allP.stream()
                    .map(Payment::getPaymentDate).filter(d -> d != null && d.length() >= 7)
                    .map(d -> d.substring(0, 7)).max(String::compareTo)
                    .orElse((String) toCb.getSelectedItem());
            String maxApt = allA.stream()
                    .map(Appointment::getDate).filter(d -> d != null && d.length() >= 7)
                    .map(d -> d.substring(0, 7)).max(String::compareTo).orElse(maxMonth);
            if (maxApt.compareTo(maxMonth) > 0) maxMonth = maxApt;
            ChartPanel cp = new ChartPanel(main, minMonth, maxMonth);
            chartHolder[0] = cp;
            chartScroll.setViewportView(cp);
            chartScroll.revalidate();
        });

        clrBtn.addActionListener(e -> main.reportsArea.setText(""));
        pdfBtn.addActionListener(e -> exportReportToPdf());

        // Auto-generate on load
        generateFilteredReport((String) fromCb.getSelectedItem(), (String) toCb.getSelectedItem());
        refreshCharts.run();

        // Fixed top: section title + filter bar
        // Scrollable bottom: tabs (Text Report + Visual Charts)
        JScrollPane tabScrollPane = new JScrollPane(tabs);
        tabScrollPane.setBorder(null);
        tabScrollPane.getViewport().setBackground(UIUtils.DARK_NAVY);
        tabScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tabScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        UIUtils.styleScrollPane(tabScrollPane);
        tabScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel centre = new JPanel(new BorderLayout(0, 8));
        centre.setBackground(UIUtils.DARK_NAVY);
        centre.add(filterBar, BorderLayout.NORTH);
        centre.add(tabScrollPane, BorderLayout.CENTER);

        panel.add(centre, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    //  CHART PANEL
    // =========================================================================

    static class ChartPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        @SuppressWarnings("unused")
        private final ManagerDashboard main;
        private final String fromMonth, toMonth;

        private final TreeMap<String, Double>  monthlyRevenue = new TreeMap<>();
        private final TreeMap<String, Integer> monthlyApts    = new TreeMap<>();
        private final Map<String, Long>        techApts       = new LinkedHashMap<>();
        private final Map<String, Long>        techCompleted  = new LinkedHashMap<>();
        private final long totalApts, completedApts, scheduledApts, cancelledApts;
        private final long normalApts, majorApts;
        private final double totalRevenue, normalRevenue, majorRevenue;

        ChartPanel(ManagerDashboard m, String from, String to) {
            main = m; fromMonth = from; toMonth = to;
            setBackground(UIUtils.DARK_NAVY);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(16, 20, 16, 20));

            List<Appointment> allApts = FileManager.readAllAppointments();
            List<Payment>     allPays = FileManager.readAllPayments();

            List<Appointment> apts = allApts.stream().filter(a -> {
                if (a.getDate() == null || a.getDate().length() < 7) return false;
                String mo = a.getDate().substring(0, 7);
                return mo.compareTo(from) >= 0 && mo.compareTo(to) <= 0;
            }).collect(Collectors.toList());

            List<Payment> pays = allPays.stream().filter(p -> {
                if (p.getPaymentDate() == null || p.getPaymentDate().length() < 7) return false;
                String mo = p.getPaymentDate().substring(0, 7);
                return mo.compareTo(from) >= 0 && mo.compareTo(to) <= 0;
            }).collect(Collectors.toList());

            totalApts     = apts.size();
            completedApts = apts.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
            scheduledApts = apts.stream().filter(a -> Appointment.STATUS_SCHEDULED.equalsIgnoreCase(a.getStatus())).count();
            cancelledApts = apts.stream().filter(a -> "CANCELLED".equalsIgnoreCase(a.getStatus()) || "CANCELED".equalsIgnoreCase(a.getStatus())).count();
            normalApts    = apts.stream().filter(a -> "NORMAL".equalsIgnoreCase(a.getServiceType())).count();
            majorApts     = apts.stream().filter(a -> "MAJOR".equalsIgnoreCase(a.getServiceType())).count();

            totalRevenue  = pays.stream().mapToDouble(Payment::getAmount).sum();
            normalRevenue = pays.stream().filter(p -> {
                Appointment a = p.getAppointment();
                return a != null && "NORMAL".equalsIgnoreCase(a.getServiceType());
            }).mapToDouble(Payment::getAmount).sum();
            majorRevenue  = totalRevenue - normalRevenue;

            for (Payment p : pays) {
                String mo = p.getPaymentDate().substring(0, 7);
                monthlyRevenue.merge(mo, p.getAmount(), Double::sum);
            }
            for (Appointment a : apts) {
                String mo = a.getDate().substring(0, 7);
                monthlyApts.merge(mo, 1, Integer::sum);
            }
            for (Technician t : FileManager.readAllTechnicians()) {
                long assigned  = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId())).count();
                long completed = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId())
                        && "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
                techApts.put(t.getName(), assigned);
                techCompleted.put(t.getName(), completed);
            }
            buildCharts();
        }

        private void buildCharts() {
            String range = fromMonth.equals(toMonth) ? fromMonth : fromMonth + " \u2192 " + toMonth;

            JPanel row1 = new JPanel(new GridLayout(1, 2, 16, 0));
            row1.setBackground(UIUtils.DARK_NAVY);
            row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
            row1.add(new BarChart("Monthly Revenue (RM)", monthlyRevenue, new Color(30, 140, 255)));
            row1.add(buildRevenueCard());

            JPanel row2 = new JPanel(new GridLayout(1, 2, 16, 0));
            row2.setBackground(UIUtils.DARK_NAVY);
            row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
            row2.add(new BarChart("Monthly Appointments", toDoubleMap(monthlyApts), new Color(100, 200, 255)));
            row2.add(new PieChart("Appointment Status",
                    new String[]{"Completed","Scheduled","Cancelled"},
                    new long[]{completedApts, scheduledApts, cancelledApts},
                    new Color[]{new Color(30,180,100), new Color(50,130,255), new Color(220,60,60)}));

            JPanel row3 = new JPanel(new GridLayout(1, 2, 16, 0));
            row3.setBackground(UIUtils.DARK_NAVY);
            row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
            row3.add(new GroupedBarChart("Technician Performance", techApts, techCompleted));
            row3.add(new PieChart("Service Type Mix",
                    new String[]{"Normal","Major","Other"},
                    new long[]{normalApts, majorApts, totalApts - normalApts - majorApts},
                    new Color[]{new Color(30,180,100), new Color(220,120,30), new Color(120,80,200)}));

            JLabel periodLbl = new JLabel("Period: " + range, JLabel.CENTER);
            periodLbl.setForeground(UIUtils.DIM_TEXT);
            periodLbl.setFont(UIUtils.uiFont(Font.ITALIC, 11));
            periodLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            periodLbl.setBorder(new EmptyBorder(0, 0, 8, 0));

            add(periodLbl);
            add(row1); add(Box.createVerticalStrut(14));
            add(row2); add(Box.createVerticalStrut(14));
            add(row3);
        }

        private JPanel buildRevenueCard() {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(UIUtils.TABLE_BG);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                    new EmptyBorder(20, 28, 20, 28)));
            JLabel title = new JLabel("Revenue Summary");
            title.setForeground(UIUtils.ACCENT_BLUE);
            title.setFont(UIUtils.uiFont(Font.BOLD, 14));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(16));
            card.add(revenueRow("Total Revenue",  String.format("RM %.2f", totalRevenue),  UIUtils.WARN_YELLOW));
            card.add(Box.createVerticalStrut(10));
            card.add(revenueRow("Normal Service", String.format("RM %.2f", normalRevenue), new Color(30,180,100)));
            card.add(Box.createVerticalStrut(10));
            card.add(revenueRow("Major Service",  String.format("RM %.2f", majorRevenue),  new Color(220,120,30)));
            card.add(Box.createVerticalStrut(20));
            if (totalRevenue > 0) card.add(new RevenueBar(normalRevenue, majorRevenue, totalRevenue));
            card.add(Box.createVerticalGlue());
            card.add(Box.createVerticalStrut(12));
            JLabel aptsLbl = new JLabel(String.format("Total Appointments: %d  |  Completed: %d", totalApts, completedApts));
            aptsLbl.setForeground(UIUtils.DIM_TEXT);
            aptsLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
            aptsLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(aptsLbl);
            return card;
        }

        private JPanel revenueRow(String label, String value, Color valueColor) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(UIUtils.TABLE_BG);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            JLabel k = new JLabel(label); k.setFont(UIUtils.uiFont(Font.PLAIN, 12)); k.setForeground(UIUtils.LIGHT_GRAY);
            JLabel v = new JLabel(value, JLabel.RIGHT); v.setFont(UIUtils.uiFont(Font.BOLD, 14)); v.setForeground(valueColor);
            p.add(k, BorderLayout.WEST); p.add(v, BorderLayout.EAST);
            return p;
        }

        private TreeMap<String, Double> toDoubleMap(TreeMap<String, Integer> m) {
            TreeMap<String, Double> r = new TreeMap<>();
            m.forEach((k, v) -> r.put(k, (double) v));
            return r;
        }
    }

    // =========================================================================
    //  BAR CHART
    // =========================================================================

    static class BarChart extends JPanel {
        private static final long serialVersionUID = 1L;
        private final String title;
        private final TreeMap<String, Double> data;
        private final Color barColor;

        BarChart(String title, TreeMap<String, Double> data, Color barColor) {
            this.title = title; this.data = data; this.barColor = barColor;
            setBackground(UIUtils.TABLE_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new UIUtils.RoundedBorder(new Color(50, 80, 140), 10, 1),
                    new EmptyBorder(12, 14, 12, 14)));
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int padL = 58, padR = 16, padT = 36, padB = 46;
            int chartW = w - padL - padR, chartH = h - padT - padB;
            if (chartW <= 0 || chartH <= 0) return;
            g.setFont(new Font("SansSerif", Font.BOLD, 13)); g.setColor(UIUtils.ACCENT_BLUE); g.drawString(title, padL, 22);
            if (data.isEmpty()) { g.setColor(UIUtils.DIM_TEXT); g.setFont(new Font("SansSerif", Font.ITALIC, 11)); g.drawString("No data", padL + chartW/2 - 25, padT + chartH/2); return; }
            double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1); if (maxVal == 0) maxVal = 1;
            List<String> keys = new ArrayList<>(data.keySet()); int n = keys.size();
            int barW = Math.max(6, (chartW - (n+1)*4) / n), gap = Math.max(2, (chartW - n*barW) / (n+1));
            g.setColor(new Color(60,80,130)); g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{4,4},0));
            for (int i = 0; i <= 4; i++) { int y = padT+(int)(chartH*(1.0-(double)i/4)); g.drawLine(padL,y,padL+chartW,y); g.setColor(UIUtils.DIM_TEXT); g.setFont(new Font("SansSerif",Font.PLAIN,9)); String lbl=maxVal>=1000?String.format("%.0fk",maxVal*i/4/1000):String.format("%.0f",maxVal*i/4); g.drawString(lbl,padL-36,y+4); g.setColor(new Color(60,80,130)); }
            g.setStroke(new BasicStroke(1)); g.setColor(new Color(80,110,180)); g.drawLine(padL,padT,padL,padT+chartH); g.drawLine(padL,padT+chartH,padL+chartW,padT+chartH);
            for (int i = 0; i < n; i++) { String key=keys.get(i); double val=data.getOrDefault(key,0.0); int bh=(int)(chartH*val/maxVal),x=padL+gap+i*(barW+gap),y=padT+chartH-bh; if(bh>0){GradientPaint gp=new GradientPaint(x,y,barColor.brighter(),x,y+bh,barColor.darker());g.setPaint(gp);g.fillRoundRect(x,y,barW,bh,4,4);} g.setColor(UIUtils.LIGHT_GRAY);g.setFont(new Font("SansSerif",Font.PLAIN,9));String sk=key.length()>7?key.substring(5):key;g.drawString(sk,x+barW/2-g.getFontMetrics().stringWidth(sk)/2,padT+chartH+14); if(bh>14){g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,9));String vs=val>=1000?String.format("%.1fk",val/1000):String.format("%.0f",val);g.drawString(vs,x+barW/2-g.getFontMetrics().stringWidth(vs)/2,y-3);} }
        }
    }

    // =========================================================================
    //  PIE CHART
    // =========================================================================

    static class PieChart extends JPanel {
        private static final long serialVersionUID = 1L;
        private final String title; private final String[] labels; private final long[] values; private final Color[] colors;
        PieChart(String title, String[] labels, long[] values, Color[] colors) {
            this.title=title; this.labels=labels; this.values=values; this.colors=colors;
            setBackground(UIUtils.TABLE_BG);
            setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(new Color(50,80,140),10,1),new EmptyBorder(12,14,12,14)));
        }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0); Graphics2D g=(Graphics2D)g0; g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(); g.setFont(new Font("SansSerif",Font.BOLD,13)); g.setColor(UIUtils.ACCENT_BLUE); g.drawString(title,14,22);
            long total=Arrays.stream(values).sum(); if(total==0){g.setColor(UIUtils.DIM_TEXT);g.setFont(new Font("SansSerif",Font.ITALIC,11));g.drawString("No data",w/2-25,h/2);return;}
            int diam=Math.max(60,Math.min(w-40,h-labels.length*20-60)),px=(w-diam)/2,py=34; double startAngle=-90;
            for(int i=0;i<labels.length;i++){if(values[i]==0)continue;double sweep=360.0*values[i]/total;g.setColor(colors[i]);g.fillArc(px,py,diam,diam,(int)startAngle,(int)Math.ceil(sweep));g.setColor(UIUtils.DARK_NAVY);g.setStroke(new BasicStroke(2));g.drawArc(px,py,diam,diam,(int)startAngle,(int)Math.ceil(sweep));g.setStroke(new BasicStroke(1));startAngle+=sweep;}
            int ly=py+diam+14,lx=14; g.setFont(new Font("SansSerif",Font.PLAIN,11));
            for(int i=0;i<labels.length;i++){if(i>0&&lx>w/2){lx=14;ly+=20;}g.setColor(colors[i]);g.fillRoundRect(lx,ly,12,12,4,4);g.setColor(UIUtils.WHITE);String txt=labels[i]+" ("+values[i]+")";g.drawString(txt,lx+16,ly+11);lx+=g.getFontMetrics().stringWidth(txt)+32;}
        }
    }

    // =========================================================================
    //  GROUPED BAR CHART (horizontal)
    // =========================================================================

    static class GroupedBarChart extends JPanel {
        private static final long serialVersionUID = 1L;
        private final String title;
        private final Map<String, Long> assigned, completed;

        GroupedBarChart(String title, Map<String, Long> assigned, Map<String, Long> completed) {
            this.title=title; this.assigned=assigned; this.completed=completed;
            setBackground(UIUtils.TABLE_BG);
            setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(new Color(50,80,140),10,1),new EmptyBorder(12,14,12,14)));
            int n=Math.max(1,assigned.size()), needed=38+n*52+20;
            setPreferredSize(new Dimension(100,Math.max(200,needed)));
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0); Graphics2D g=(Graphics2D)g0; g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(); int padL=110,padR=50,padT=38,padB=16; int chartW=w-padL-padR,chartH=h-padT-padB;
            g.setFont(new Font("SansSerif",Font.BOLD,13)); g.setColor(UIUtils.ACCENT_BLUE); g.drawString(title,padL,22);
            Color ac=new Color(80,160,255),cc=new Color(30,200,100); int legX=w-170; g.setFont(new Font("SansSerif",Font.PLAIN,10));
            g.setColor(ac);g.fillRect(legX,12,10,10);g.setColor(UIUtils.WHITE);g.drawString("Assigned",legX+14,21);
            g.setColor(cc);g.fillRect(legX+80,12,10,10);g.setColor(UIUtils.WHITE);g.drawString("Completed",legX+94,21);
            if(assigned.isEmpty()){g.setColor(UIUtils.DIM_TEXT);g.setFont(new Font("SansSerif",Font.ITALIC,11));g.drawString("No data",padL+chartW/2-30,padT+chartH/2);return;}
            long maxVal=Math.max(assigned.values().stream().mapToLong(Long::longValue).max().orElse(1),completed.values().stream().mapToLong(Long::longValue).max().orElse(1)); if(maxVal==0)maxVal=1;
            g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{3,3},0));
            for(int i=0;i<=4;i++){int x=padL+(int)(chartW*(double)i/4);g.setColor(new Color(60,80,130));g.drawLine(x,padT,x,padT+chartH);g.setColor(UIUtils.DIM_TEXT);g.setFont(new Font("SansSerif",Font.PLAIN,9));String lbl=String.valueOf(maxVal*i/4);g.drawString(lbl,x-g.getFontMetrics().stringWidth(lbl)/2,padT+chartH+12);}
            g.setStroke(new BasicStroke(1)); g.setColor(new Color(80,110,180)); g.drawLine(padL,padT,padL,padT+chartH); g.drawLine(padL,padT+chartH,padL+chartW,padT+chartH);
            List<String> names=new ArrayList<>(assigned.keySet()); int n=names.size(); int groupH=chartH/n; int barH=Math.max(8,Math.min(18,groupH/3)),barGap=4;
            for(int i=0;i<n;i++){String name=names.get(i);long aVal=assigned.getOrDefault(name,0L),cVal=completed.getOrDefault(name,0L);
                int cy2=padT+i*groupH+groupH/2,ay=cy2-barH-barGap,cy=cy2+barGap,aw=(int)(chartW*aVal/maxVal),cw=(int)(chartW*cVal/maxVal);
                if(aw>0){g.setPaint(new GradientPaint(padL,ay,ac.brighter(),padL+aw,ay,ac.darker()));g.fillRoundRect(padL,ay,aw,barH,4,4);}
                if(cw>0){g.setPaint(new GradientPaint(padL,cy,cc.brighter(),padL+cw,cy,cc.darker()));g.fillRoundRect(padL,cy,cw,barH,4,4);}
                String sn=name.length()>14?name.substring(0,13)+".":name; g.setFont(new Font("SansSerif",Font.PLAIN,11)); g.setColor(UIUtils.LIGHT_GRAY); g.drawString(sn,padL-g.getFontMetrics().stringWidth(sn)-8,cy2+4);
                g.setFont(new Font("SansSerif",Font.BOLD,10));
                if(aw>0){g.setColor(ac);g.drawString(String.valueOf(aVal),padL+aw+4,ay+barH-2);}
                if(cw>0){g.setColor(cc);g.drawString(String.valueOf(cVal),padL+cw+4,cy+barH-2);}
            }
        }
    }

    // =========================================================================
    //  REVENUE BAR
    // =========================================================================

    static class RevenueBar extends JPanel {
        private static final long serialVersionUID = 1L;
        private final double normal, major, total;
        RevenueBar(double normal, double major, double total) {
            this.normal=normal; this.major=major; this.total=total;
            setBackground(UIUtils.TABLE_BG); setMaximumSize(new Dimension(Integer.MAX_VALUE,42)); setPreferredSize(new Dimension(100,42));
        }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0); Graphics2D g=(Graphics2D)g0; g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth()-8,bh=20,x=4,y=10; g.setColor(new Color(40,55,90)); g.fillRoundRect(x,y,w,bh,8,8);
            if(total>0){int nw=(int)(w*normal/total);g.setPaint(new GradientPaint(x,y,new Color(30,200,100),x+nw,y,new Color(20,160,80)));g.fillRoundRect(x,y,nw,bh,8,8);if(nw<w){g.setPaint(new GradientPaint(x+nw,y,new Color(220,120,30),x+w,y,new Color(180,90,20)));g.fillRoundRect(x+nw,y,w-nw,bh,8,8);}}
            g.setFont(new Font("SansSerif",Font.PLAIN,9)); g.setColor(Color.WHITE); g.drawString(String.format("Normal %.0f%%",total>0?normal*100/total:0),x+4,y+14); String ms=String.format("Major %.0f%%",total>0?major*100/total:0); g.drawString(ms,x+w-g.getFontMetrics().stringWidth(ms)-4,y+14);
        }
    }

    // =========================================================================
    //  TEXT REPORT GENERATION
    // =========================================================================

    void generateFilteredReport(String fromMonth, String toMonth) {
        List<Appointment> allApts = FileManager.readAllAppointments();
        List<Payment>     allPays = FileManager.readAllPayments();
        List<Technician>  techs   = FileManager.readAllTechnicians();
        ServicePrice      pr      = FileManager.readPrices();

        List<Appointment> apts = allApts.stream().filter(a -> {
            if (a.getDate() == null || a.getDate().length() < 7) return false;
            String m = a.getDate().substring(0, 7);
            return m.compareTo(fromMonth) >= 0 && m.compareTo(toMonth) <= 0;
        }).collect(Collectors.toList());

        List<Payment> pays = allPays.stream().filter(p -> {
            if (p.getPaymentDate() == null || p.getPaymentDate().length() < 7) return false;
            String m = p.getPaymentDate().substring(0, 7);
            return m.compareTo(fromMonth) >= 0 && m.compareTo(toMonth) <= 0;
        }).collect(Collectors.toList());

        long scheduled = apts.stream().filter(a -> Appointment.STATUS_SCHEDULED.equalsIgnoreCase(a.getStatus())).count();
        long completed = apts.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
        long nc        = apts.stream().filter(a -> "NORMAL".equalsIgnoreCase(a.getServiceType())).count();
        long mc        = apts.stream().filter(a -> "MAJOR".equalsIgnoreCase(a.getServiceType())).count();
        double total   = pays.stream().mapToDouble(Payment::getAmount).sum();
        double nr      = pays.stream().filter(p -> {
            Appointment a = FileManager.findAppointmentById(p.getAppointmentId());
            return a != null && "NORMAL".equalsIgnoreCase(a.getServiceType());
        }).mapToDouble(Payment::getAmount).sum();

        String rangeLabel = fromMonth.equals(toMonth) ? fromMonth : fromMonth + "  to  " + toMonth;
        int lw = 83; String border = "=".repeat(lw), dash = "-".repeat(lw);
        StringBuilder sb = new StringBuilder();
        sb.append(border).append("\n").append(centre("APU AUTOMOTIVE SERVICE CENTRE (APU-ASC)", lw)).append("\n");
        sb.append(centre("MANAGEMENT REPORT", lw)).append("\n").append(centre("Period: " + rangeLabel, lw)).append("\n").append(border).append("\n\n");
        sb.append(dash).append("\n APPOINTMENT STATISTICS\n").append(dash).append("\n");
        sb.append(String.format("  Total: %d  |  Scheduled: %d  |  Completed: %d%n", apts.size(), scheduled, completed));
        sb.append(String.format("  Normal (1hr): %d  |  Major (3hr): %d%n%n", nc, mc));
        sb.append(dash).append("\n FINANCIAL SUMMARY\n").append(dash).append("\n");
        sb.append(String.format("  Prices: Normal RM %.2f  |  Major RM %.2f%n", pr.getNormalServicePrice(), pr.getMajorServicePrice()));
        sb.append(String.format("  Revenue Normal: RM %.2f  |  Major: RM %.2f%n", nr, total - nr));
        sb.append(String.format("  TOTAL REVENUE: RM %.2f%n%n", total));
        if (!fromMonth.equals(toMonth)) {
            sb.append(dash).append("\n MONTHLY REVENUE BREAKDOWN\n").append(dash).append("\n");
            TreeMap<String, Double> monthly = new TreeMap<>();
            for (Payment p : pays) monthly.merge(p.getPaymentDate().substring(0, 7), p.getAmount(), Double::sum);
            if (monthly.isEmpty()) sb.append("  (No payments)\n");
            else monthly.forEach((k, v) -> sb.append(String.format("  %s  :  RM %.2f%n", k, v)));
            sb.append("\n");
        }
        sb.append(dash).append("\n TECHNICIAN PERFORMANCE\n").append(dash).append("\n");
        for (Technician t : techs) {
            long tt = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId())).count();
            long tc = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId()) && "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
            sb.append(String.format("  %-22s : %d assigned, %d completed%n", t.getName(), tt, tc));
        }
        sb.append("\n").append(border).append("\n");
        sb.append(String.format("  Generated: %s  |  By: %s (%s)%n", LocalDate.now(), main.currentManager.getName(), main.currentManager.getUserId()));
        sb.append(border).append("\n");
        main.reportsArea.setText(sb.toString());
        main.reportsArea.setCaretPosition(0);
    }

    void generateFullReport() {
        List<Appointment>  apts   = FileManager.readAllAppointments();
        List<Payment>      pays   = FileManager.readAllPayments();
        List<Technician>   techs  = FileManager.readAllTechnicians();
        List<CounterStaff> staffs = FileManager.readAllCounterStaff();
        List<Customer>     custs  = FileManager.readAllCustomers();
        ServicePrice       pr     = FileManager.readPrices();

        long scheduled = apts.stream().filter(a -> Appointment.STATUS_SCHEDULED.equalsIgnoreCase(a.getStatus())).count();
        long completed = apts.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
        long nc        = apts.stream().filter(a -> "NORMAL".equalsIgnoreCase(a.getServiceType())).count();
        long mc        = apts.stream().filter(a -> "MAJOR".equalsIgnoreCase(a.getServiceType())).count();
        double total   = pays.stream().mapToDouble(Payment::getAmount).sum();
        double nr      = pays.stream().filter(p -> {
            Appointment a = FileManager.findAppointmentById(p.getAppointmentId());
            return a != null && "NORMAL".equalsIgnoreCase(a.getServiceType());
        }).mapToDouble(Payment::getAmount).sum();

        int lw = 83; String border = "=".repeat(lw), dash = "-".repeat(lw);
        StringBuilder sb = new StringBuilder();
        sb.append(border).append("\n").append(centre("APU AUTOMOTIVE SERVICE CENTRE (APU-ASC)", lw)).append("\n");
        sb.append(centre("MANAGEMENT REPORT  \u2014  ALL TIME", lw)).append("\n").append(border).append("\n\n");
        sb.append(dash).append("\n SYSTEM OVERVIEW\n").append(dash).append("\n");
        sb.append(String.format("  Managers: %d  |  Staff: %d  |  Technicians: %d  |  Customers: %d%n",
                FileManager.readAllManagers().size(), staffs.size(), techs.size(), custs.size()));
        sb.append(String.format("  Car Parts: %d%n%n", FileManager.readAllCarParts().size()));
        sb.append(dash).append("\n APPOINTMENT STATISTICS\n").append(dash).append("\n");
        sb.append(String.format("  Total: %d  |  Scheduled: %d  |  Completed: %d%n", apts.size(), scheduled, completed));
        sb.append(String.format("  Normal: %d  |  Major: %d%n%n", nc, mc));
        sb.append(dash).append("\n FINANCIAL SUMMARY\n").append(dash).append("\n");
        sb.append(String.format("  Prices: Normal RM %.2f  |  Major RM %.2f%n", pr.getNormalServicePrice(), pr.getMajorServicePrice()));
        sb.append(String.format("  Revenue Normal: RM %.2f  |  Major: RM %.2f%n  TOTAL: RM %.2f%n%n", nr, total - nr, total));
        sb.append(dash).append("\n TECHNICIAN PERFORMANCE\n").append(dash).append("\n");
        for (Technician t : techs) {
            long tt = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId())).count();
            long tc = apts.stream().filter(a -> a.getTechnicianId().equals(t.getUserId()) && "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
            sb.append(String.format("  %-22s : %d assigned, %d completed%n", t.getName(), tt, tc));
        }
        sb.append(String.format("%n  Feedbacks: %d  |  Comments: %d%n", FileManager.readAllFeedbacks().size(), FileManager.readAllComments().size()));
        sb.append("\n").append(border).append("\n");
        sb.append(String.format("  Generated: %s  |  By: %s (%s)%n", LocalDate.now(), main.currentManager.getName(), main.currentManager.getUserId()));
        sb.append(border).append("\n");
        main.reportsArea.setText(sb.toString());
        main.reportsArea.setCaretPosition(0);
    }

    String centre(String text, int width) {
        if (text.length() >= width) return text;
        return " ".repeat((width - text.length()) / 2) + text;
    }

    /** True if {@code ln} looks like a report section header
     *  (indented, all-uppercase body with at least one letter, 5–40 chars). */
    private static boolean isReportSectionLine(String ln) {
        if (!ln.startsWith(" ")) return false;
        String body = ln.trim();
        if (body.isEmpty() || body.length() < 5 || body.length() > 40) return false;
        if (body.chars().noneMatch(Character::isLetter)) return false; // must contain letters
        return body.equals(body.toUpperCase());
    }

    /** True if {@code stripped} is a monthly data line ("YYYY-MM  :  RM XX.XX"). */
    private static boolean isMonthlyDataLine(String stripped) {
        return stripped.matches("\\d{4}-\\d{2}\\s+:\\s+RM\\s+[\\d,]+\\.\\d{2}");
    }

    void exportReportToPdf() {
        Window win = SwingUtilities.getWindowAncestor(this);
        if (main.reportsArea == null || main.reportsArea.getText().isBlank()) {
            JOptionPane.showMessageDialog(win, "Generate a report first.", "No Report", JOptionPane.WARNING_MESSAGE); return;
        }
        String text = main.reportsArea.getText().replace("\r", "");
        String[] lines = text.split("\n", -1);

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Report as PDF");
        String suffix = text.contains("ALL TIME") ? "_AllTime" : "";
        fc.setSelectedFile(new java.io.File("APU_ASC_Report" + suffix + "_" + LocalDate.now() + ".pdf"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
        if (fc.showSaveDialog(win) != JFileChooser.APPROVE_OPTION) return;
        java.io.File dest = fc.getSelectedFile();
        if (!dest.getName().toLowerCase().endsWith(".pdf")) dest = new java.io.File(dest.getAbsolutePath() + ".pdf");

        try (PDDocument doc = new PDDocument()) {
            // ── PART 1: Text pages (portrait A4) ──────────────────────────
            float margin = 50;
            PDRectangle pageSize = PDRectangle.A4;
            float pageW = pageSize.getWidth();
            float usableH = pageSize.getHeight() - margin * 2;
            float lineH = 14.5f;
            int linesPerPage = (int)(usableH / lineH);

            Color clrBorder    = new Color(0, 100, 210);    // blue borders
            Color clrTitle     = new Color(0, 80, 180);     // blue titles
            Color clrSection   = new Color(200, 150, 0);    // gold section headers
            Color clrText      = new Color(40, 40, 40);     // dark gray body
            Color clrHighlight = new Color(180, 40, 40);    // red for TOTAL / key values
            Color clrFooter    = new Color(130, 130, 130);  // gray footer

            for (int start = 0; start < lines.length; start += linesPerPage) {
                PDPage page = new PDPage(pageSize);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float y = pageSize.getHeight() - margin;
                    for (int i = start; i < Math.min(start + linesPerPage, lines.length); i++) {
                        String ln = lines[i];
                        String stripped = ln.strip();
                        String upper    = stripped.toUpperCase();

                        // ── Border lines: draw as solid vector lines ──
                        if (!stripped.isEmpty() && stripped.chars().allMatch(c -> c == '=')) {
                            cs.setStrokingColor(clrBorder);
                            cs.setLineWidth(1.5f);
                            cs.moveTo(margin, y + 3.5f);
                            cs.lineTo(pageW - margin, y + 3.5f);
                            cs.stroke();
                        } else if (!stripped.isEmpty() && stripped.chars().allMatch(c -> c == '-')) {
                            cs.setStrokingColor(clrBorder);
                            cs.setLineWidth(0.8f);
                            cs.moveTo(margin, y + 3.5f);
                            cs.lineTo(pageW - margin, y + 3.5f);
                            cs.stroke();
                        }
                        // ── Normal text lines ──
                        else {
                            PDType1Font font;
                            float fontSize;
                            Color color;

                            if (upper.contains("APU AUTOMOTIVE") || upper.contains("MANAGEMENT REPORT")) {
                                font = PDType1Font.HELVETICA_BOLD; fontSize = 12;  color = clrTitle;
                            } else if (upper.contains("TOTAL") || isMonthlyDataLine(stripped)) {
                                font = PDType1Font.COURIER_BOLD;     fontSize = 10;  color = clrHighlight;
                            } else if (isReportSectionLine(ln)) {
                                font = PDType1Font.HELVETICA_BOLD; fontSize = 11;  color = clrSection;
                            } else if (ln.isBlank()) {
                                y -= lineH;  // blank line: skip but still consume height
                                continue;
                            } else {
                                font = PDType1Font.COURIER;           fontSize = 10;  color = clrText;
                            }

                            String out = ln.length() > 110 ? ln.substring(0, 110) : ln;
                            cs.setNonStrokingColor(color);
                            cs.setFont(font, fontSize);

                            // ── Centre titles, period line, and footer-line ──
                            boolean centre = upper.contains("APU AUTOMOTIVE")
                                          || upper.contains("MANAGEMENT REPORT")
                                          || upper.startsWith("PERIOD:")
                                          || upper.startsWith("GENERATED:");
                            if (centre) {
                                out = stripped;  // discard leading spaces from JTextPane
                                float textW = font.getStringWidth(out) / 1000f * fontSize;
                                float cx = (pageW - textW) / 2f;
                                cs.beginText();
                                cs.newLineAtOffset(cx, y);
                                cs.showText(out);
                                cs.endText();
                            } else {
                                cs.beginText();
                                cs.newLineAtOffset(margin, y);
                                cs.showText(out);
                                cs.endText();
                            }
                        }
                        y -= lineH;
                    }

                    // footer (centred)
                    cs.setNonStrokingColor(clrFooter);
                    cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                    String footerText = "Generated: " + LocalDate.now()
                            + "  |  By: " + main.currentManager.getName()
                            + " (" + main.currentManager.getUserId() + ")";
                    float fw = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(footerText) / 1000f * 8f;
                    float cx = (pageW - fw) / 2f;
                    cs.beginText();
                    cs.newLineAtOffset(cx, 30);
                    cs.showText(footerText);
                    cs.endText();
                }
            }

            // ── PART 2: Chart pages (landscape A4) ────────────────────────
            // Determine the date range from the report text
            String fromMonth = "2024-01", toMonth = LocalDate.now().toString().substring(0, 7);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Period:\\s*(\\S+)\\s*(?:to|→)\\s*(\\S+)")
                    .matcher(text);
            if (m.find()) { fromMonth = m.group(1); toMonth = m.group(2); }
            else {
                java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("Period:\\s*(\\S{7})")
                        .matcher(text);
                if (m2.find()) fromMonth = toMonth = m2.group(1);
            }

            // Build chart panel off-screen
            JFrame hiddenFrame = new JFrame();
            hiddenFrame.setUndecorated(true);
            hiddenFrame.setLocation(-2000, -2000);
            ChartPanel cp = new ChartPanel(main, fromMonth, toMonth);
            cp.setPreferredSize(new Dimension(1100, 920));
            hiddenFrame.getContentPane().add(cp);
            hiddenFrame.pack();
            hiddenFrame.setVisible(true);

            // Force FULL layout cascade (validate: BoxLayout → rows → GridLayout → charts)
            cp.validate();
            // Also explicitly validate each row to ensure GridLayout children are laid out
            for (Component c : cp.getComponents()) {
                if (c instanceof JPanel && ((JPanel) c).getMaximumSize().height == 280) {
                    ((JPanel) c).validate();
                }
            }
            cp.repaint();
            Thread.sleep(300);

            // Find the 3 row panels
            JPanel row1 = null, row2 = null, row3 = null;
            for (Component c : cp.getComponents()) {
                if (c instanceof JPanel && ((JPanel) c).getMaximumSize().height == 280) {
                    if (row1 == null)      row1 = (JPanel) c;
                    else if (row2 == null)  row2 = (JPanel) c;
                    else if (row3 == null)  row3 = (JPanel) c;
                }
            }
            JPanel[] rows = {row1, row2, row3};

            // DEBUG: log the actual sizes BoxLayout gave each row
            for (int ri = 0; ri < 3; ri++) {
                JPanel r = rows[ri];
                if (r != null) {
                    System.out.println("[PDF DEBUG] row" + (ri+1) + " getWidth=" + r.getWidth() + " getHeight=" + r.getHeight()
                        + " prefSize=" + r.getPreferredSize().width + "x" + r.getPreferredSize().height);
                } else {
                    System.out.println("[PDF DEBUG] row" + (ri+1) + " is NULL!");
                }
            }

            // Force every row to its intended 280px height.
            // BoxLayout sometimes compresses a row when other rows request more space,
            // and row2's content may internally measure shorter than row3's GroupedBarChart.
            final int ROW_W = 1100, ROW_H = 280;

            for (int rowIdx = 0; rowIdx < 3; rowIdx++) {
                JPanel row = rows[rowIdx];
                if (row == null) {
                    System.out.println("[PDF DEBUG] SKIPPING row" + (rowIdx+1) + " — null");
                    continue;
                }
                // Force size — bypass the BoxLayout's potentially unfair allocation
                row.setSize(ROW_W, ROW_H);
                row.validate();    // cascade to GridLayout → charts
                row.doLayout();

                BufferedImage img = new BufferedImage(ROW_W, ROW_H, BufferedImage.TYPE_INT_ARGB);
                Graphics2D rg = img.createGraphics();
                rg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                rg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                row.paintAll(rg);
                rg.dispose();

                System.out.println("[PDF DEBUG] row" + (rowIdx+1) + " image=" + ROW_W + "x" + ROW_H);

                PDPage chartPage = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                doc.addPage(chartPage);
                try (PDPageContentStream cs = new PDPageContentStream(doc, chartPage)) {
                    float cMargin = 40;
                    float cW = chartPage.getMediaBox().getWidth() - cMargin * 2;
                    float cH = chartPage.getMediaBox().getHeight() - cMargin * 2 - 60;
                    float scale = Math.min(cW / ROW_W, cH / ROW_H);
                    float drawW = ROW_W * scale;
                    float drawH = ROW_H * scale;
                    float drawX = (chartPage.getMediaBox().getWidth() - drawW) / 2;
                    float drawY = chartPage.getMediaBox().getHeight() - cMargin - 30 - drawH;

                    // Title
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    cs.beginText();
                    cs.newLineAtOffset(cMargin, chartPage.getMediaBox().getHeight() - cMargin);
                    String[] titles = {"Revenue & Monthly Appointments", "Appointment Status & Service Type", "Technician Performance"};
                    cs.showText("APU-ASC Report \u2014 " + titles[rowIdx]);
                    cs.endText();

                    cs.drawImage(LosslessFactory.createFromImage(doc, img), drawX, drawY, drawW, drawH);

                    // Footer
                    cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                    cs.beginText();
                    cs.newLineAtOffset(cMargin, 20);
                    cs.showText("APU-ASC Management Report  |  Period: " + fromMonth + " to " + toMonth
                            + "  |  Page " + (lines.length / linesPerPage + 1 + rowIdx + 1));
                    cs.endText();
                }
            }

            hiddenFrame.dispose();
            doc.save(dest);
            JOptionPane.showMessageDialog(win, "Exported to:\n" + dest.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(win, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JLabel label(String text) { JLabel l=new JLabel(text); l.setForeground(UIUtils.WHITE); l.setFont(UIUtils.uiFont(Font.BOLD,12)); return l; }
    private static UIUtils.RoundedButton btn(String text, Color bg, int w) { UIUtils.RoundedButton b=new UIUtils.RoundedButton(text,bg); b.setPreferredSize(new Dimension(w,34)); return b; }
}
