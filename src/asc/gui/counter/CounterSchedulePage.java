package asc.gui.counter;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Calendar-based appointment schedule for Counter Staff.
 * Shows ALL appointments across all technicians, colour-coded by status.
 * Left pane = monthly calendar grid; Right pane = day detail timeline.
 */
public class CounterSchedulePage extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CounterStaffDashboard main;

    public CounterSchedulePage(CounterStaffDashboard m) {
        main = m;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(18, 18, 14, 18));

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(UIUtils.DARK_NAVY);
        body.add(main.sectionTitle("My Schedule",
                CounterStaffDashboard.ICON_BASE + "My Schedule.png"), BorderLayout.NORTH);
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

        // ── Navigation row ──────────────────────────────────────────────────
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

        // ── Calendar grid ───────────────────────────────────────────────────
        JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 4, 4));
        calendarGrid.setOpaque(false);
        calendarGrid.setBackground(UIUtils.DARK_NAVY);

        // ── Day detail panel (right side) ───────────────────────────────────
        JPanel daySchedule = new JPanel(new BorderLayout(0, 6));
        daySchedule.setBackground(UIUtils.TABLE_BG);
        daySchedule.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(14, 16, 14, 16)));
        daySchedule.setMinimumSize(new Dimension(360, 0));

        JLabel dayTitle = new JLabel("", JLabel.LEFT);
        dayTitle.setFont(UIUtils.uiFont(Font.BOLD, 13));
        dayTitle.setForeground(UIUtils.ACCENT_BLUE);
        dayTitle.setBorder(new EmptyBorder(0, 2, 8, 0));

        JPanel timelinePanel = new JPanel();
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.Y_AXIS));
        timelinePanel.setBackground(UIUtils.TABLE_BG);

        JScrollPane timelineScroll = new JScrollPane(timelinePanel);
        timelineScroll.setBorder(null);
        timelineScroll.getViewport().setBackground(UIUtils.TABLE_BG);
        UIUtils.styleScrollPane(timelineScroll);

        daySchedule.add(dayTitle,      BorderLayout.NORTH);
        daySchedule.add(timelineScroll, BorderLayout.CENTER);

        // ── Legend ──────────────────────────────────────────────────────────
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        legend.setOpaque(false);
        legend.add(legendItem("●  Scheduled",          new Color(100, 180, 255)));
        legend.add(legendItem("●  Awaiting Payment",   UIUtils.WARN_YELLOW));
        legend.add(legendItem("■  Today",                   UIUtils.ACCENT_BLUE));
        legend.add(legendItem("■  Day Off",                 new Color(180, 70, 70)));

        // ── Build-calendar lambda ────────────────────────────────────────────
        Runnable[] buildCalendar = { null };
        buildCalendar[0] = () -> {
            java.util.Map<String, java.util.List<Appointment>> apptMap = loadScheduleMap();
            calendarGrid.removeAll();
            LocalDate first = selectedDate[0].withDayOfMonth(1);
            monthLabel.setText(first.getMonth().getDisplayName(
                    java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                    + "  " + first.getYear());

            // Day-of-week headers
            for (String d : new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"}) {
                JLabel lbl = new JLabel(d, JLabel.CENTER);
                lbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
                lbl.setForeground(UIUtils.LIGHT_GRAY);
                calendarGrid.add(lbl);
            }

            // Empty cells before first day
            int startDow = first.getDayOfWeek().getValue() % 7; // Sun=0
            for (int i = 0; i < startDow; i++) calendarGrid.add(new JLabel(""));

            for (int day = 1; day <= first.lengthOfMonth(); day++) {
                LocalDate date    = first.withDayOfMonth(day);
                String    dateStr = date.toString();
                boolean hasAppt  = apptMap.containsKey(dateStr);
                boolean isToday  = date.equals(LocalDate.now());
                boolean isSel    = date.equals(selectedDate[0]);
                boolean isOffDay = main.currentStaff != null
                        && !FileManager.isStaffWorkingOnDay(main.currentStaff.getUserId(), dateStr);

                int open = 0, awaiting = 0, completed = 0;
                if (hasAppt) {
                    for (Appointment a : apptMap.get(dateStr)) {
                        if      (Appointment.STATUS_COMPLETED.equalsIgnoreCase(a.getStatus()))       completed++;
                        else if (Appointment.STATUS_AWAITING_PAYMENT.equalsIgnoreCase(a.getStatus())) awaiting++;
                        else                                                                           open++;
                    }
                }

                final int fDay = day, fOpen = open, fAwaiting = awaiting, fCompleted = completed;

                JPanel cell = new JPanel(new BorderLayout(0, 2)) {
                    private static final long serialVersionUID = 1L;
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        Color bg = isSel    ? UIUtils.ACCENT_BLUE
                                 : isOffDay ? new Color(50, 50, 55)
                                 : isToday  ? new Color(40, 80, 150)
                                 : hasAppt  ? new Color(30, 55, 90)
                                 : UIUtils.TABLE_BG;
                        g2.setColor(bg);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                        if (isToday && !isSel) {
                            g2.setColor(UIUtils.ACCENT_BLUE);
                            g2.setStroke(new BasicStroke(2));
                            g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                        }
                        if (isOffDay && !isSel) {
                            g2.setColor(new Color(180, 60, 60));
                            g2.setStroke(new BasicStroke(1));
                            g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                        }
                        g2.dispose();
                    }
                };
                cell.setOpaque(false);
                cell.setBorder(new EmptyBorder(4, 4, 4, 4));
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                JLabel dayNum = new JLabel(String.valueOf(day), JLabel.CENTER);
                dayNum.setFont(UIUtils.uiFont(isSel || isToday ? Font.BOLD : Font.PLAIN, 12));
                dayNum.setForeground(isOffDay ? new Color(120, 120, 120)
                        : isSel ? Color.WHITE : isToday ? UIUtils.ACCENT_BLUE : UIUtils.WHITE);
                cell.add(dayNum, BorderLayout.NORTH);

                if (isOffDay) {
                    JLabel offLbl = new JLabel("OFF", JLabel.CENTER);
                    offLbl.setFont(UIUtils.uiFont(Font.BOLD, 9));
                    offLbl.setForeground(new Color(180, 70, 70));
                    cell.add(offLbl, BorderLayout.CENTER);
                } else if (hasAppt) {
                    JPanel dots = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
                    dots.setOpaque(false);
                    if (fOpen > 0)      dots.add(dot(new Color(100, 180, 255)));
                    if (fAwaiting > 0)  dots.add(dot(UIUtils.WARN_YELLOW));
                    if (fCompleted > 0) dots.add(dot(UIUtils.GREEN_OK));
                    cell.add(dots, BorderLayout.CENTER);

                    int total = fOpen + fAwaiting + fCompleted;
                    JLabel cnt = new JLabel(total + " appt" + (total > 1 ? "s" : ""), JLabel.CENTER);
                    cnt.setFont(UIUtils.uiFont(Font.PLAIN, 9));
                    cnt.setForeground(UIUtils.LIGHT_GRAY);
                    cell.add(cnt, BorderLayout.SOUTH);
                }

                cell.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        selectedDate[0] = first.withDayOfMonth(fDay);
                        buildCalendar[0].run();
                    }
                });
                calendarGrid.add(cell);
            }

            calendarGrid.revalidate();
            calendarGrid.repaint();
            showDayDetail(selectedDate[0], apptMap, dayTitle, timelinePanel, timelineScroll);
        };

        // Wire buttons
        prevBtn .addActionListener(e -> { selectedDate[0] = selectedDate[0].minusMonths(1).withDayOfMonth(1); buildCalendar[0].run(); });
        nextBtn .addActionListener(e -> { selectedDate[0] = selectedDate[0].plusMonths(1).withDayOfMonth(1);  buildCalendar[0].run(); });
        todayBtn.addActionListener(e -> { selectedDate[0] = LocalDate.now();                                  buildCalendar[0].run(); });

        buildCalendar[0].run(); // initial render

        // ── Layout ──────────────────────────────────────────────────────────
        JScrollPane calScroll = new JScrollPane(calendarGrid);
        calScroll.setBorder(new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1));
        calScroll.getViewport().setBackground(UIUtils.DARK_NAVY);
        calScroll.setBackground(UIUtils.DARK_NAVY);
        calScroll.getVerticalScrollBar().setUnitIncrement(12);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, calScroll, daySchedule);
        split.setResizeWeight(0.6);
        split.setDividerLocation(0.6);
        split.setDividerSize(6);
        split.setBackground(UIUtils.DARK_NAVY);
        split.setBorder(null);
        split.setOpaque(false);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.6));

        panel.add(navRow, BorderLayout.NORTH);
        panel.add(split,  BorderLayout.CENTER);
        panel.add(legend, BorderLayout.SOUTH);
        return panel;
    }

    // ── Data helpers ────────────────────────────────────────────────────────

    private java.util.Map<String, java.util.List<Appointment>> loadScheduleMap() {
        java.util.Map<String, java.util.List<Appointment>> map = new java.util.LinkedHashMap<>();
        for (Appointment a : FileManager.readAllAppointments()) {
            if (Appointment.STATUS_COMPLETED.equalsIgnoreCase(a.getStatus())) continue;
            map.computeIfAbsent(a.getDate(), k -> new java.util.ArrayList<>()).add(a);
        }
        return map;
    }

    private void showDayDetail(LocalDate date,
            java.util.Map<String, java.util.List<Appointment>> apptMap,
            JLabel dayTitle, JPanel timelinePanel, JScrollPane timelineScroll) {

        dayTitle.setText(date.format(
                DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", java.util.Locale.ENGLISH)));
        timelinePanel.removeAll();

        java.util.List<Appointment> apts = new java.util.ArrayList<>(
                apptMap.getOrDefault(date.toString(), java.util.Collections.emptyList()));

        if (apts.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(UIUtils.TABLE_BG);
            JLabel none = new JLabel("No appointments on this day");
            none.setFont(UIUtils.uiFont(Font.ITALIC, 13));
            none.setForeground(UIUtils.LIGHT_GRAY);
            empty.add(none);
            timelinePanel.add(empty);
        } else {
            // Sort by time
            apts.sort((a, b) -> a.getTime().compareTo(b.getTime()));
            for (Appointment a : apts) {
                timelinePanel.add(appointmentSlot(a));
                timelinePanel.add(Box.createVerticalStrut(8));
            }
        }

        timelinePanel.revalidate();
        timelinePanel.repaint();
        timelineScroll.getVerticalScrollBar().setValue(0);
    }

    // ── UI components ────────────────────────────────────────────────────────

    private JPanel appointmentSlot(Appointment a) {
        Color accent = statusColor(a.getStatus());

        JPanel slot = new JPanel(new BorderLayout(8, 4)) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 45, 78));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Accent left bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        slot.setOpaque(false);
        slot.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(accent, 10, 1),
                new EmptyBorder(10, 14, 10, 14)));
        slot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        slot.setAlignmentX(Component.LEFT_ALIGNMENT);

        Customer   c = a.getCustomer();
        Technician t = a.getTechnician();

        JLabel timeLbl = new JLabel(a.getTime() + "  |  " + a.getDurationHours() + " hr");
        timeLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        timeLbl.setForeground(accent);

        JLabel statusLbl = new JLabel(a.getStatusLabel());
        statusLbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
        statusLbl.setForeground(accent);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(timeLbl,   BorderLayout.WEST);
        topRow.add(statusLbl, BorderLayout.EAST);

        JLabel svcLbl  = new JLabel(a.getServiceType() + " Service  —  " + a.getAppointmentId());
        svcLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        svcLbl.setForeground(UIUtils.WHITE);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.add(svcLbl);
        info.add(Box.createVerticalStrut(3));
        info.add(infoLbl("Customer:   " + (c != null ? c.getName() : a.getCustomerId())));
        info.add(infoLbl("Technician: " + (t != null ? t.getName() : a.getTechnicianId())));
        info.add(infoLbl("Vehicle:    " + a.getVehicleInfo()));

        slot.add(topRow, BorderLayout.NORTH);
        slot.add(info,   BorderLayout.CENTER);
        return slot;
    }

    private JLabel infoLbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        l.setForeground(UIUtils.LIGHT_GRAY);
        return l;
    }

    private JLabel dot(Color color) {
        JLabel d = new JLabel("\u25CF");
        d.setForeground(color);
        d.setFont(UIUtils.uiFont(Font.PLAIN, 8));
        return d;
    }

    private JLabel legendItem(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(UIUtils.uiSymbolFont(Font.BOLD, 11));
        return l;
    }

    private Color statusColor(String status) {
        if (Appointment.STATUS_COMPLETED.equalsIgnoreCase(status))        return UIUtils.GREEN_OK;
        if (Appointment.STATUS_AWAITING_PAYMENT.equalsIgnoreCase(status)) return UIUtils.WARN_YELLOW;
        if (Appointment.STATUS_MISSED.equalsIgnoreCase(status))           return UIUtils.ERROR_RED;
        return UIUtils.LIGHT_GRAY;
    }
}
