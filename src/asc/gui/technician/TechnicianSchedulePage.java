package asc.gui.technician;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class TechnicianSchedulePage extends JPanel {

    private static final long serialVersionUID = 1L;
    final TechnicianDashboard main;

    public TechnicianSchedulePage(TechnicianDashboard m) {
        main = m;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(18, 18, 14, 18));

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(UIUtils.DARK_NAVY);
        body.add(main.sectionTitle("My Schedule",
                TechnicianDashboard.ICON_BASE + "My Schedule(Blue).png"), BorderLayout.NORTH);
        body.add(buildSchedulePanel(), BorderLayout.CENTER);

        // Wrap in scroll so content is never clipped when window is small
        JScrollPane outerScroll = new JScrollPane(body,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.setBorder(null);
        outerScroll.getViewport().setBackground(UIUtils.DARK_NAVY);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(outerScroll, BorderLayout.CENTER);
    }

    private JPanel buildSchedulePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY);

        final LocalDate[] selectedDate = { LocalDate.now() };

        JLabel monthLabel = new JLabel("", JLabel.LEFT);
        monthLabel.setFont(UIUtils.uiFont(Font.BOLD, 18));
        monthLabel.setForeground(UIUtils.WHITE);

        UIUtils.RoundedButton prevBtn  = new UIUtils.RoundedButton("\u2039",  new Color(50, 70, 120));
        UIUtils.RoundedButton nextBtn  = new UIUtils.RoundedButton("\u203a",  new Color(50, 70, 120));
        UIUtils.RoundedButton todayBtn = new UIUtils.RoundedButton("Today",   UIUtils.ACCENT_BLUE);
        prevBtn .setPreferredSize(new Dimension(40, 32));
        nextBtn .setPreferredSize(new Dimension(40, 32));
        todayBtn.setPreferredSize(new Dimension(72, 32));

        JPanel navLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navLeft.setOpaque(false);
        navLeft.add(prevBtn);
        navLeft.add(todayBtn);
        navLeft.add(nextBtn);
        navLeft.add(Box.createHorizontalStrut(8));
        navLeft.add(monthLabel);

        JPanel navRow = new JPanel(new BorderLayout(10, 0));
        navRow.setOpaque(false);
        navRow.add(navLeft, BorderLayout.WEST);

        JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 4, 4));
        calendarGrid.setOpaque(false);

        JPanel daySchedule = new JPanel(new BorderLayout(0, 6));
        daySchedule.setBackground(UIUtils.TABLE_BG);
        daySchedule.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel dayTitle = new JLabel("", JLabel.CENTER);
        dayTitle.setFont(UIUtils.uiFont(Font.BOLD, 14));
        dayTitle.setForeground(UIUtils.ACCENT_BLUE);

        JPanel timelinePanel = new JPanel();
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.Y_AXIS));
        timelinePanel.setBackground(UIUtils.TABLE_BG);

        JScrollPane timelineScroll = new JScrollPane(timelinePanel);
        timelineScroll.setBorder(null);
        timelineScroll.getViewport().setBackground(UIUtils.TABLE_BG);
        UIUtils.styleScrollPane(timelineScroll);

        daySchedule.add(dayTitle,      BorderLayout.NORTH);
        daySchedule.add(timelineScroll, BorderLayout.CENTER);

        java.util.Map<String, java.util.List<Appointment>> apptMap
                = new java.util.LinkedHashMap<>();
        for (Appointment a : FileManager.readAllAppointments()) {
            if (!a.getTechnicianId().equals(main.currentTech.getUserId())) continue;
            // Only show scheduled / awaiting — skip completed and missed appointments
            if ("COMPLETED".equalsIgnoreCase(a.getStatus())) continue;
            if (Appointment.STATUS_MISSED.equals(a.getStatus())) continue;
            apptMap.computeIfAbsent(a.getDate(), k -> new ArrayList<>()).add(a);
        }

        Runnable[] buildCalendar = { null };
        buildCalendar[0] = () -> {
            calendarGrid.removeAll();
            LocalDate first = selectedDate[0].withDayOfMonth(1);
            monthLabel.setText(first.getMonth().getDisplayName(
                    java.time.format.TextStyle.FULL,
                    java.util.Locale.ENGLISH) + "  " + first.getYear());

            String[] days = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
            for (String d : days) {
                JLabel lbl = new JLabel(d, JLabel.CENTER);
                lbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
                lbl.setForeground(UIUtils.LIGHT_GRAY);
                calendarGrid.add(lbl);
            }

            int startDow = first.getDayOfWeek().getValue() % 7;
            for (int i = 0; i < startDow; i++)
                calendarGrid.add(new JLabel(""));

            int daysInMonth = first.lengthOfMonth();
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date     = first.withDayOfMonth(day);
                String    dateStr  = date.toString();
                boolean hasAppt  = apptMap.containsKey(dateStr);
                boolean isToday  = date.equals(LocalDate.now());
                boolean isSel    = date.equals(selectedDate[0]);
                // Show OFF based on published schedule — consistent with what counter staff sees
                boolean isOffDay = FileManager.isTechnicianOffOnDay(main.currentTech.getUserId(), dateStr);

                // apptMap already excludes COMPLETED — all entries here are active/pending
                int pending = hasAppt ? apptMap.get(dateStr).size() : 0;

                final int finalDay = day;
                JPanel cell = new JPanel(new BorderLayout(0, 2)) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                            RenderingHints.VALUE_ANTIALIAS_ON);
                        Color bg = isSel    ? UIUtils.ACCENT_BLUE
                                 : isOffDay ? new Color(50, 50, 55)
                                 : isToday  ? new Color(40, 80, 150)
                                 : hasAppt  ? new Color(30, 55, 90)
                                 :            UIUtils.TABLE_BG;
                        g2.setColor(bg);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                        if (isToday && !isSel) {
                            g2.setColor(UIUtils.ACCENT_BLUE);
                            g2.setStroke(new BasicStroke(2));
                            g2.drawRoundRect(1, 1, getWidth()-2,
                                    getHeight()-2, 10, 10);
                        }
                        if (isOffDay && !isSel) {
                            g2.setColor(new Color(180, 60, 60));
                            g2.setStroke(new BasicStroke(1));
                            g2.drawRoundRect(1, 1, getWidth()-2,
                                    getHeight()-2, 10, 10);
                        }
                        g2.dispose();
                    }
                };
                cell.setOpaque(false);
                cell.setBorder(new EmptyBorder(4, 4, 4, 4));
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                JLabel dayNum = new JLabel(String.valueOf(day), JLabel.CENTER);
                dayNum.setFont(UIUtils.uiFont(isSel || isToday
                        ? Font.BOLD : Font.PLAIN, 12));
                dayNum.setForeground(isOffDay ? new Color(120, 120, 120)
                        : isSel ? Color.WHITE
                        : isToday ? UIUtils.ACCENT_BLUE : UIUtils.WHITE);
                cell.add(dayNum, BorderLayout.NORTH);

                // Show "OFF" label for off-duty days
                if (isOffDay) {
                    JLabel offLbl = new JLabel("OFF", JLabel.CENTER);
                    offLbl.setFont(UIUtils.uiFont(Font.BOLD, 9));
                    offLbl.setForeground(new Color(180, 70, 70));
                    cell.add(offLbl, BorderLayout.CENTER);
                } else if (hasAppt) {
                    JPanel dots = new JPanel(new FlowLayout(
                            FlowLayout.CENTER, 2, 0));
                    dots.setOpaque(false);
                    if (pending > 0) {
                        JLabel dot = new JLabel("\u25CF");
                        dot.setForeground(TechnicianDashboard.ACCENT_YELLOW);
                        dot.setFont(UIUtils.uiSymbolFont(Font.PLAIN, 8));
                        dots.add(dot);
                    }
                    cell.add(dots, BorderLayout.CENTER);

                    JLabel cnt = new JLabel(pending + " job"
                            + (pending > 1 ? "s" : ""), JLabel.CENTER);
                    cnt.setFont(UIUtils.uiFont(Font.PLAIN, 9));
                    cnt.setForeground(UIUtils.LIGHT_GRAY);
                    cell.add(cnt, BorderLayout.SOUTH);
                }

                cell.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectedDate[0] = first.withDayOfMonth(finalDay);
                        buildCalendar[0].run();
                        showDaySchedule(selectedDate[0], apptMap,
                                dayTitle, timelinePanel, timelineScroll);
                    }
                });

                calendarGrid.add(cell);
            }

            calendarGrid.revalidate();
            calendarGrid.repaint();

            showDaySchedule(selectedDate[0], apptMap,
                    dayTitle, timelinePanel, timelineScroll);
        };

        buildCalendar[0].run();

        prevBtn.addActionListener(e -> {
            selectedDate[0] = selectedDate[0].minusMonths(1)
                    .withDayOfMonth(1);
            buildCalendar[0].run();
        });
        nextBtn.addActionListener(e -> {
            selectedDate[0] = selectedDate[0].plusMonths(1)
                    .withDayOfMonth(1);
            buildCalendar[0].run();
        });
        todayBtn.addActionListener(e -> {
            selectedDate[0] = LocalDate.now();
            buildCalendar[0].run();
        });

        JPanel calWrapper = new JPanel(new BorderLayout(0, 8));
        calWrapper.setOpaque(false);
        calWrapper.add(calendarGrid, BorderLayout.CENTER);

        JScrollPane calScroll = new JScrollPane(calWrapper);
        calScroll.setBorder(null);
        calScroll.getViewport().setBackground(UIUtils.DARK_NAVY);
        calScroll.setBackground(UIUtils.DARK_NAVY);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                calScroll, daySchedule);
        split.setResizeWeight(0.6);
        split.setDividerLocation(0.6);
        split.setDividerSize(6);
        split.setBackground(UIUtils.DARK_NAVY);
        split.setBorder(null);
        split.setOpaque(false);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.6));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(navRow, BorderLayout.CENTER);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        legend.setOpaque(false);
        legend.add(legendItem("● Scheduled / Awaiting", TechnicianDashboard.ACCENT_YELLOW));
        legend.add(legendItem("■ Today",     new Color(40, 80, 150)));
        legend.add(legendItem("■ Selected",  UIUtils.ACCENT_BLUE));
        legend.add(legendItem("■ Day Off",   new Color(180, 70, 70)));

        panel.add(top,    BorderLayout.NORTH);
        panel.add(split,  BorderLayout.CENTER);
        panel.add(legend, BorderLayout.SOUTH);
        return panel;
    }
    
    // ── UI helpers ──────────────────────────────────────────────────────────

    private JLabel legendItem(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(UIUtils.uiSymbolFont(Font.BOLD, 11));
        return l;
    }

    void showDaySchedule(LocalDate date,
            java.util.Map<String, java.util.List<Appointment>> apptMap,
            JLabel dayTitle, JPanel timelinePanel,
            JScrollPane timelineScroll) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy",
                java.util.Locale.ENGLISH);
        dayTitle.setText("  " + date.format(fmt));

        timelinePanel.removeAll();

        String dateStr = date.toString();
        java.util.List<Appointment> apts = apptMap.getOrDefault(
                dateStr, new ArrayList<>());

        if (apts.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(UIUtils.TABLE_BG);
            JLabel none = new JLabel("No appointments on this day");
            none.setFont(UIUtils.uiFont(Font.ITALIC, 13));
            none.setForeground(UIUtils.LIGHT_GRAY);
            empty.add(none);
            timelinePanel.add(empty);
        } else {
            apts.sort((a, b) -> a.getTime().compareTo(b.getTime()));

            for (Appointment a : apts) {
                // All appointments here are non-completed (filtered at load time)
                Color accent = TechnicianDashboard.ACCENT_YELLOW;

                JPanel slot = new JPanel(new BorderLayout(8, 4)) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                            RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(60, 42, 15));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                        g2.setColor(accent);
                        g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                        g2.dispose();
                    }
                };
                slot.setOpaque(false);
                slot.setBorder(BorderFactory.createCompoundBorder(
                        new UIUtils.RoundedBorder(accent, 10, 1),
                        new EmptyBorder(10, 14, 10, 14)));
                slot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

                JLabel timeLbl = new JLabel("🕐  " + a.getTime());
                timeLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
                timeLbl.setForeground(accent);

                JLabel svcLbl = new JLabel(a.getServiceType());
                svcLbl.setFont(UIUtils.uiFont(Font.BOLD, 14));
                svcLbl.setForeground(UIUtils.WHITE);

                JLabel notesLbl = new JLabel(a.getNotes() != null
                        && !a.getNotes().isEmpty()
                        ? "📝  " + a.getNotes() : "");
                notesLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
                notesLbl.setForeground(UIUtils.LIGHT_GRAY);

                Customer c = a.getCustomer();
                JLabel custLbl = new JLabel("👤  "
                        + (c != null ? c.getName() : a.getCustomerId()));
                custLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
                custLbl.setForeground(UIUtils.LIGHT_GRAY);

                JLabel statusLbl = new JLabel("⏳ " + a.getStatus());
                statusLbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
                statusLbl.setForeground(accent);

                JPanel topRow = new JPanel(new BorderLayout());
                topRow.setOpaque(false);
                topRow.add(timeLbl,   BorderLayout.WEST);
                topRow.add(statusLbl, BorderLayout.EAST);

                JPanel info = new JPanel();
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                info.setOpaque(false);
                info.add(svcLbl);
                info.add(Box.createVerticalStrut(3));
                info.add(custLbl);
                if (!notesLbl.getText().isEmpty()) {
                    info.add(Box.createVerticalStrut(2));
                    info.add(notesLbl);
                }

                slot.add(topRow, BorderLayout.NORTH);
                slot.add(info,   BorderLayout.CENTER);

                timelinePanel.add(slot);
                timelinePanel.add(Box.createVerticalStrut(8));
            }
        }

        timelinePanel.revalidate();
        timelinePanel.repaint();
        timelineScroll.getVerticalScrollBar().setValue(0);
    }


}
