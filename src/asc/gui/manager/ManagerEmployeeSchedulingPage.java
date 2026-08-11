package asc.gui.manager;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * ManagerEmployeeSchedulingPage — Manager sets employee working days
 * (Mon-Sun) for each technician and counter staff.
 *
 * Features:
 *  - Role tabs: Technicians | Counter Staff
 *  - Week navigation: Prev Week / This Week / Next Week
 *  - Pagination: 10 staff per page
 *  - 7-day grid: rows = staff, columns = Mon-Sun
 *  - ON (green) / OFF (red) per cell
 *  - Read-only by default; Edit Timetable enters edit mode
 *  - In edit mode: click cell to toggle, batch buttons (Set Mon-Fri, Select All, Deselect All)
 *  - Copy Last Week with pre-validation (disabled if no data exists)
 *  - Publish / Unpublish for staff visibility
 *  - Cancel with discard confirmation
 *  - Save Changes: first save asks "Publish?"
 *  - No scroll pane — single-screen layout fits max window
 *  - Published timetable syncs to Technician & Counter Staff My Schedule pages
 */
public class ManagerEmployeeSchedulingPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final ManagerDashboard main;

    private static final String[] DAY_NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.ENGLISH);
    private static final int PAGE_SIZE = 10;

    public ManagerEmployeeSchedulingPage(ManagerDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(12, 18, 10, 18));
        add(buildPanel(), BorderLayout.CENTER);
    }

    private LocalDate getWeekStart(LocalDate date) {
        return date.with(ChronoField.DAY_OF_WEEK, 1);
    }

    // =========================================================================
    //  Main panel
    // =========================================================================
    private JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(UIUtils.DARK_NAVY);
        panel.setBorder(new EmptyBorder(10, 20, 10, 20));

        panel.add(main.sectionTitle("Employee Scheduling Management",
                ManagerDashboard.MGR_ICON_BASE + "Employee Scheduling.png"),
                BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UIUtils.TABLE_BG);
        tabs.setForeground(UIUtils.WHITE);
        tabs.setFont(UIUtils.uiFont(Font.BOLD, 12));
        tabs.addTab("Technicians",   buildRolePanel("TECHNICIAN"));
        tabs.addTab("Counter Staff", buildRolePanel("COUNTER_STAFF"));

        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    //  Role panel (fully self-contained — all state is local, no shared fields)
    // =========================================================================
    private JPanel buildRolePanel(String role) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(UIUtils.DARK_NAVY);

        // ── Per-tab state (fully isolated) ──────────────────────
        final LocalDate[] weekStart  = { getWeekStart(LocalDate.now()) };
        final List<String> allStaffIds   = new ArrayList<>();
        final Map<String, boolean[]> draftDays    = new LinkedHashMap<>();
        final Map<String, boolean[]> originalDays = new LinkedHashMap<>();
        final List<String> pageStaffIds  = new ArrayList<>();
        final int[] currentPage    = {0};
        final int[] totalPages     = {1};
        final boolean[] isEditMode = {false};
        final boolean[] hasBeenPublished = {false};

        // ── UI Components ───────────────────────────────────────
        JLabel weekLabel = new JLabel("", JLabel.CENTER);
        weekLabel.setFont(UIUtils.uiFont(Font.BOLD, 16));
        weekLabel.setForeground(UIUtils.ACCENT_BLUE);

        UIUtils.RoundedButton prevWeekBtn = new UIUtils.RoundedButton("\u2039  Prev Week",
                new Color(60, 90, 150));
        UIUtils.RoundedButton thisWeekBtn = new UIUtils.RoundedButton("This Week",
                UIUtils.SUCCESS_GREEN);
        UIUtils.RoundedButton nextWeekBtn = new UIUtils.RoundedButton("Next Week  \u203a",
                new Color(60, 90, 150));
        prevWeekBtn.setPreferredSize(new Dimension(130, 30));
        thisWeekBtn.setPreferredSize(new Dimension(110, 30));
        nextWeekBtn.setPreferredSize(new Dimension(130, 30));

        JPanel navLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        navLeft.setOpaque(false);
        navLeft.add(prevWeekBtn); navLeft.add(thisWeekBtn); navLeft.add(nextWeekBtn);

        JLabel summaryLbl = new JLabel("", SwingConstants.RIGHT);
        summaryLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        summaryLbl.setForeground(UIUtils.LIGHT_GRAY);

        // 3-column grid: left (buttons) | center (date) | right (summary)
        JPanel navRow = new JPanel(new GridLayout(1, 3, 10, 0));
        navRow.setOpaque(false);
        navRow.add(navLeft);
        navRow.add(weekLabel);
        navRow.add(summaryLbl);

        JLabel statusLbl = new JLabel("", SwingConstants.LEFT);
        statusLbl.setFont(UIUtils.uiSymbolFont(Font.BOLD, 11));
        statusLbl.setForeground(UIUtils.WARN_YELLOW);

        // ── Week Grid (NO scroll — fixed height, fits on one screen) ──
        JPanel weekGrid = new JPanel(new GridLayout(0, 8, 1, 1)) {
            private static final long serialVersionUID = 1L;
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(d.width, Math.max(d.height, 200));
            }
            @Override public Dimension getMaximumSize() {
                Dimension d = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, d.height);
            }
        };
        weekGrid.setOpaque(false);
        weekGrid.setBackground(UIUtils.DARK_NAVY);
        weekGrid.setBorder(BorderFactory.createLineBorder(new Color(80, 110, 160), 1));

        // Legend
        JPanel weekLegend = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        weekLegend.setOpaque(false);
        weekLegend.add(legendItem("\u2713 ON",  UIUtils.GREEN_OK));
        weekLegend.add(legendItem("\u2717 OFF", UIUtils.ERROR_RED));
        weekLegend.add(legendItem("\u25A0 Today", UIUtils.ACCENT_BLUE));

        // ── Pagination Bar ─────────────────────────────────────
        JPanel pageBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        pageBar.setOpaque(false);

        UIUtils.RoundedButton prevPageBtn = new UIUtils.RoundedButton("\u2039 Prev",
                new Color(60, 90, 150));
        JLabel pageInfoLbl = new JLabel("Page 1 of 1", JLabel.CENTER);
        pageInfoLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
        pageInfoLbl.setForeground(UIUtils.LIGHT_GRAY);
        UIUtils.RoundedButton nextPageBtn = new UIUtils.RoundedButton("Next \u203a",
                new Color(60, 90, 150));
        prevPageBtn.setPreferredSize(new Dimension(90, 28));
        nextPageBtn.setPreferredSize(new Dimension(90, 28));

        pageBar.add(prevPageBtn);
        pageBar.add(pageInfoLbl);
        pageBar.add(nextPageBtn);

        // ── Action Buttons ─────────────────────────────────────
        JPanel btnLeft  = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnLeft.setOpaque(false);
        JPanel btnRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRight.setOpaque(false);

        JPanel btnRow = new JPanel(new BorderLayout(0, 0));
        btnRow.setBackground(UIUtils.DARK_NAVY);
        btnRow.add(btnLeft,  BorderLayout.WEST);
        btnRow.add(btnRight, BorderLayout.EAST);

        UIUtils.RoundedButton editBtn      = new UIUtils.RoundedButton("Edit Timetable",
                UIUtils.ACCENT_BLUE);
        UIUtils.RoundedButton copyPrevBtn  = new UIUtils.RoundedButton("Copy Last Week",
                new Color(60, 90, 150));
        UIUtils.RoundedButton publishBtn   = new UIUtils.RoundedButton("Publish",
                UIUtils.ERROR_RED);
        UIUtils.RoundedButton unpublishBtn = new UIUtils.RoundedButton("Unpublish",
                new Color(200, 120, 50));
        UIUtils.RoundedButton weekdaysBtn  = new UIUtils.RoundedButton("Set Mon-Fri Only",
                new Color(60, 90, 150));
        UIUtils.RoundedButton allDaysBtn   = new UIUtils.RoundedButton("Select All Days",
                new Color(60, 90, 150));
        UIUtils.RoundedButton noDaysBtn    = new UIUtils.RoundedButton("Deselect All",
                new Color(120, 60, 60));
        UIUtils.RoundedButton cancelBtn    = new UIUtils.RoundedButton("Cancel",
                UIUtils.ERROR_RED);
        UIUtils.RoundedButton saveBtn      = new UIUtils.RoundedButton("Save Changes",
                UIUtils.SUCCESS_GREEN);

        editBtn.setPreferredSize(      new Dimension(140, 32));
        copyPrevBtn.setPreferredSize(  new Dimension(150, 32));
        publishBtn.setPreferredSize(   new Dimension(110, 32));
        unpublishBtn.setPreferredSize( new Dimension(120, 32));
        weekdaysBtn.setPreferredSize(  new Dimension(150, 30));
        allDaysBtn.setPreferredSize(   new Dimension(150, 30));
        noDaysBtn.setPreferredSize(    new Dimension(120, 30));
        cancelBtn.setPreferredSize(    new Dimension(100, 30));
        saveBtn.setPreferredSize(      new Dimension(140, 30));

        // ── Pre-check: does the previous week have data? ──────
        Runnable updateCopyBtnState = () -> {
            if (isEditMode[0]) return;
            copyPrevBtn.setEnabled(true);
            copyPrevBtn.setToolTipText(null);
        };

        // ── Build logic ────────────────────────────────────────
        Runnable[] buildAll = { null };
        buildAll[0] = () -> {
            LocalDate start = weekStart[0];
            LocalDate end   = start.plusDays(6);
            weekLabel.setText(start.format(DATE_FMT) + "  \u2014  " + end.format(DATE_FMT));

            List<? extends User> staffList = "TECHNICIAN".equals(role)
                    ? FileManager.readAllTechnicians()
                    : FileManager.readAllCounterStaff();

            allStaffIds.clear();
            draftDays.clear();
            originalDays.clear();
            pageStaffIds.clear();

            for (User staff : staffList) {
                allStaffIds.add(staff.getUserId());
                boolean[] days = FileManager.getStaffEmployeeScheduling(
                        staff.getUserId(), weekStart[0].toString());
                draftDays.put(staff.getUserId(), days.clone());
                originalDays.put(staff.getUserId(), days.clone());
            }

            totalPages[0] = Math.max(1, (allStaffIds.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (currentPage[0] >= totalPages[0]) currentPage[0] = totalPages[0] - 1;
            if (currentPage[0] < 0) currentPage[0] = 0;

            int from = currentPage[0] * PAGE_SIZE;
            int to   = Math.min(from + PAGE_SIZE, allStaffIds.size());
            for (int i = from; i < to; i++) {
                pageStaffIds.add(allStaffIds.get(i));
            }

            hasBeenPublished[0] = FileManager.isWeekPublished(weekStart[0].toString());
            updateStatusLabel(statusLbl, isEditMode[0], hasBeenPublished[0]);

            rebuildWeekGrid(weekGrid, weekStart[0], role, summaryLbl, staffList,
                    draftDays, pageStaffIds, allStaffIds.size(), isEditMode[0]);

            pageInfoLbl.setText("Page " + (currentPage[0] + 1) + " of " + totalPages[0]
                    + "  (" + allStaffIds.size() + " total)");
            prevPageBtn.setEnabled(currentPage[0] > 0);
            nextPageBtn.setEnabled(currentPage[0] < totalPages[0] - 1);

            btnLeft.removeAll();
            btnRight.removeAll();
            if (isEditMode[0]) {
                btnLeft.add(weekdaysBtn);
                btnLeft.add(allDaysBtn);
                btnLeft.add(noDaysBtn);
                btnRight.add(cancelBtn);
                btnRight.add(saveBtn);
            } else {
                btnLeft.add(editBtn);
                btnLeft.add(copyPrevBtn);
                if (hasBeenPublished[0]) {
                    btnLeft.add(unpublishBtn);
                } else {
                    btnLeft.add(publishBtn);
                }
            }
            btnLeft.revalidate();
            btnLeft.repaint();
            btnRight.revalidate();
            btnRight.repaint();

            if (!isEditMode[0]) updateCopyBtnState.run();
        };

        // ── Week Nav Actions ───────────────────────────────────
        prevWeekBtn.addActionListener(e -> {
            weekStart[0] = weekStart[0].minusWeeks(1); buildAll[0].run(); });
        thisWeekBtn.addActionListener(e -> {
            weekStart[0] = getWeekStart(LocalDate.now()); buildAll[0].run(); });
        nextWeekBtn.addActionListener(e -> {
            weekStart[0] = weekStart[0].plusWeeks(1);  buildAll[0].run(); });

        // ── Page Nav Actions ───────────────────────────────────
        prevPageBtn.addActionListener(e -> {
            if (currentPage[0] > 0) { currentPage[0]--; buildAll[0].run(); } });
        nextPageBtn.addActionListener(e -> {
            if (currentPage[0] < totalPages[0] - 1) { currentPage[0]++; buildAll[0].run(); } });

        // ── Button Actions ─────────────────────────────────────
        editBtn.addActionListener(e -> { isEditMode[0] = true;  buildAll[0].run(); });

        // Copy Last Week — pre-validated: disabled if no records; popup if all-off
        copyPrevBtn.addActionListener(e -> {
            LocalDate prevWeek = weekStart[0].minusWeeks(1);

            // Pre-check: does the previous week have any ON days?
            if (!FileManager.hasAnyWorkingDaysInWeek(prevWeek.toString())) {
                JOptionPane.showMessageDialog(this,
                        "The previous week has no working days set.\n\n"
                        + "Previous week: " + prevWeek.format(DATE_FMT)
                        + " \u2014 " + prevWeek.plusDays(6).format(DATE_FMT)
                        + "\n\nNothing to copy.",
                        "No Working Days", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Copy the timetable from the previous week?\n\n"
                            + "This will overwrite the current week's draft schedule.",
                    "Copy Last Week", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean copied = FileManager.copyFromPreviousEmployeeSchedulingWeek(
                        weekStart[0].toString());
                if (copied) {
                    hasBeenPublished[0] = false;
                    buildAll[0].run();
                    JOptionPane.showMessageDialog(this,
                            "Previous week's schedule has been copied.",
                            "Copied", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "No schedule data exists from the previous week.\n\n"
                            + "Previous week: " + prevWeek.format(DATE_FMT)
                            + " \u2014 " + prevWeek.plusDays(6).format(DATE_FMT),
                            "No Data", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        // Publish current week
        publishBtn.addActionListener(e -> {
            // Save current drafts first
            for (String sid : allStaffIds) {
                boolean[] days = draftDays.get(sid);
                if (days != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 7; i++) sb.append(days[i] ? "1" : "0");
                    FileManager.saveStaffEmployeeScheduling(sid, role,
                            weekStart[0].toString(), sb.toString());
                }
            }
            FileManager.publishWeek(weekStart[0].toString());
            // ── Notification: all staff get schedule published ─────────────
            String weekStr = weekStart[0].toString();
            for (asc.model.Technician t : FileManager.readAllTechnicians()) {
                FileManager.saveNotification(new asc.model.Notification(
                        t.getUserId(), asc.model.Notification.SCHEDULE_PUBLISHED,
                        "Schedule Published",
                        "The work schedule for week " + weekStr + " has been published. Check My Schedule for details."));
            }
            for (asc.model.CounterStaff cs : FileManager.readAllCounterStaff()) {
                FileManager.saveNotification(new asc.model.Notification(
                        cs.getUserId(), asc.model.Notification.SCHEDULE_PUBLISHED,
                        "Schedule Published",
                        "The work schedule for week " + weekStr + " has been published. Check My Schedule for details."));
            }
            hasBeenPublished[0] = true;
            buildAll[0].run();
            JOptionPane.showMessageDialog(this,
                    "This week's timetable has been published!\n"
                    + "Technicians and Counter Staff can now see their schedules.",
                    "Published", JOptionPane.INFORMATION_MESSAGE);
        });

        // Unpublish current week
        unpublishBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Unpublish this week's timetable?\n\n"
                            + "Staff will no longer see this week's schedule.\n"
                            + "The draft will be kept and can be re-published later.",
                    "Confirm Unpublish", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                FileManager.unpublishWeek(weekStart[0].toString());
                hasBeenPublished[0] = false;
                buildAll[0].run();
                JOptionPane.showMessageDialog(this,
                        "This week's timetable has been unpublished.",
                        "Unpublished", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        weekdaysBtn.addActionListener(e -> {
            for (String sid : allStaffIds) {
                boolean[] days = draftDays.get(sid);
                if (days != null) {
                    days[0]=true; days[1]=true; days[2]=true;
                    days[3]=true; days[4]=true; days[5]=false; days[6]=false;
                }
            }
            rebuildWeekGrid(weekGrid, weekStart[0], role, summaryLbl,
                    "TECHNICIAN".equals(role) ? FileManager.readAllTechnicians()
                                              : FileManager.readAllCounterStaff(),
                    draftDays, pageStaffIds, allStaffIds.size(), isEditMode[0]);
        });
        allDaysBtn.addActionListener(e -> {
            for (String sid : allStaffIds) {
                boolean[] days = draftDays.get(sid);
                if (days != null) for (int i = 0; i < 7; i++) days[i] = true;
            }
            rebuildWeekGrid(weekGrid, weekStart[0], role, summaryLbl,
                    "TECHNICIAN".equals(role) ? FileManager.readAllTechnicians()
                                              : FileManager.readAllCounterStaff(),
                    draftDays, pageStaffIds, allStaffIds.size(), isEditMode[0]);
        });
        noDaysBtn.addActionListener(e -> {
            for (String sid : allStaffIds) {
                boolean[] days = draftDays.get(sid);
                if (days != null) for (int i = 0; i < 7; i++) days[i] = false;
            }
            rebuildWeekGrid(weekGrid, weekStart[0], role, summaryLbl,
                    "TECHNICIAN".equals(role) ? FileManager.readAllTechnicians()
                                              : FileManager.readAllCounterStaff(),
                    draftDays, pageStaffIds, allStaffIds.size(), isEditMode[0]);
        });

        cancelBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Discard all changes?\n\nAny modifications you made will be lost.",
                    "Confirm Cancel", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                for (String sid : allStaffIds) {
                    boolean[] orig = originalDays.get(sid);
                    if (orig != null) draftDays.put(sid, orig.clone());
                }
                isEditMode[0] = false;
                buildAll[0].run();
            }
        });

        saveBtn.addActionListener(e -> {
            String ws = weekStart[0].toString();
            for (String sid : allStaffIds) {
                boolean[] days = draftDays.get(sid);
                if (days != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 7; i++) sb.append(days[i] ? "1" : "0");
                    FileManager.saveStaffEmployeeScheduling(sid, role, ws, sb.toString());
                }
            }
            for (String sid : allStaffIds) {
                boolean[] days = draftDays.get(sid);
                if (days != null) originalDays.put(sid, days.clone());
            }

            if (hasBeenPublished[0]) {
                // Already published — auto-sync the published file with updated draft
                FileManager.publishWeek(ws);
                // ── Notification: inform all staff that schedule was updated ──
                for (asc.model.Technician t : FileManager.readAllTechnicians()) {
                    FileManager.saveNotification(new asc.model.Notification(
                            t.getUserId(), asc.model.Notification.SCHEDULE_PUBLISHED,
                            "Schedule Updated",
                            "The work schedule for week " + ws + " has been updated. Check My Schedule for the latest details."));
                }
                for (asc.model.CounterStaff cs : FileManager.readAllCounterStaff()) {
                    FileManager.saveNotification(new asc.model.Notification(
                            cs.getUserId(), asc.model.Notification.SCHEDULE_PUBLISHED,
                            "Schedule Updated",
                            "The work schedule for week " + ws + " has been updated. Check My Schedule for the latest details."));
                }
                isEditMode[0] = false;
                buildAll[0].run();
                JOptionPane.showMessageDialog(this,
                        "Changes saved and published schedule updated!\n"
                        + "Staff can now see the latest schedule.",
                        "Saved & Updated", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Not yet published — ask whether to publish now
            int choice = JOptionPane.showOptionDialog(this,
                    "This timetable is not published.\n"
                    + "Technicians and Counter Staff cannot see it yet. Publish now?",
                    "Publish?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Yes, Publish", "Save Only"},
                    "Yes, Publish");
            if (choice == JOptionPane.YES_OPTION) {
                FileManager.publishWeek(ws);
                // ── Notification: all staff get schedule published ─────
                for (asc.model.Technician t : FileManager.readAllTechnicians()) {
                    FileManager.saveNotification(new asc.model.Notification(
                            t.getUserId(), asc.model.Notification.SCHEDULE_PUBLISHED,
                            "Schedule Published",
                            "The work schedule for week " + ws + " has been published. Check My Schedule for details."));
                }
                for (asc.model.CounterStaff cs : FileManager.readAllCounterStaff()) {
                    FileManager.saveNotification(new asc.model.Notification(
                            cs.getUserId(), asc.model.Notification.SCHEDULE_PUBLISHED,
                            "Schedule Published",
                            "The work schedule for week " + ws + " has been published. Check My Schedule for details."));
                }
                hasBeenPublished[0] = true;
                isEditMode[0] = false;
                buildAll[0].run();
                JOptionPane.showMessageDialog(this,
                        "Timetable saved and published!\n"
                        + "Staff can now see the latest schedule.",
                        "Published", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            isEditMode[0] = false;
            buildAll[0].run();
            JOptionPane.showMessageDialog(this,
                    "Changes saved for this week.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        });

        // Initial build
        buildAll[0].run();

        // ── Layout (NO scroll pane — grid fits in one screen) ──
        JPanel centerPanel = new JPanel(new BorderLayout(0, 2));
        centerPanel.setBackground(UIUtils.DARK_NAVY);
        centerPanel.setBorder(new EmptyBorder(2, 0, 1, 0));
        centerPanel.add(weekGrid,  BorderLayout.CENTER);
        centerPanel.add(weekLegend, BorderLayout.SOUTH);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusRow.setOpaque(false);
        statusRow.setBorder(new EmptyBorder(6, 0, 0, 0));
        statusRow.add(statusLbl);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.add(navRow);
        topPanel.add(Box.createVerticalStrut(2));
        topPanel.add(statusRow);

        JPanel southPanel = new JPanel(new BorderLayout(0, 2));
        southPanel.setOpaque(false);
        southPanel.add(pageBar, BorderLayout.NORTH);
        southPanel.add(btnRow,  BorderLayout.CENTER);

        wrapper.add(topPanel);
        wrapper.add(centerPanel);
        wrapper.add(southPanel);

        return wrapper;
    }

    // =========================================================================
    //  Week Grid
    // =========================================================================
    private void rebuildWeekGrid(JPanel weekGrid, LocalDate weekStart, String role,
                                JLabel summaryLbl, List<? extends User> allStaffList,
                                Map<String, boolean[]> draftDays, List<String> pageStaffIds,
                                int totalStaff, boolean editMode) {
        weekGrid.removeAll();

        Map<String, User> staffMap = new LinkedHashMap<>();
        for (User u : allStaffList) staffMap.put(u.getUserId(), u);

        // Header
        JLabel cornerLbl = new JLabel("Staff", JLabel.CENTER);
        cornerLbl.setFont(UIUtils.uiFont(Font.BOLD, 11));
        cornerLbl.setForeground(UIUtils.ACCENT_BLUE);
        cornerLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(80, 110, 160)),
                new EmptyBorder(2, 8, 2, 8)));
        weekGrid.add(cornerLbl);

        LocalDate today = LocalDate.now();
        String[] dayShort = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            boolean isToday = d.equals(today);
            boolean isWeekend = (i >= 5);

            JPanel hdrCell = new JPanel(new BorderLayout());
            hdrCell.setOpaque(false);
            hdrCell.setBackground(isToday ? new Color(40, 80, 150) : UIUtils.TABLE_BG);
            hdrCell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(80, 110, 160)),
                    new EmptyBorder(2, 2, 2, 2)));

            JLabel dayLbl = new JLabel(dayShort[i], JLabel.CENTER);
            dayLbl.setFont(UIUtils.uiFont(Font.BOLD, 10));
            dayLbl.setForeground(isToday ? Color.WHITE
                    : (isWeekend ? UIUtils.WARN_YELLOW : UIUtils.LIGHT_GRAY));

            JLabel dateLbl = new JLabel(d.format(DATE_FMT), JLabel.CENTER);
            dateLbl.setFont(UIUtils.uiFont(Font.PLAIN, 9));
            dateLbl.setForeground(isToday ? new Color(200, 220, 255) : UIUtils.LIGHT_GRAY);

            hdrCell.add(dayLbl, BorderLayout.NORTH);
            hdrCell.add(dateLbl, BorderLayout.SOUTH);
            weekGrid.add(hdrCell);
        }

        int totalOnDuty = 0;
        for (String staffId : pageStaffIds) {
            User staff = staffMap.get(staffId);
            if (staff == null) continue;

            boolean[] days = draftDays.get(staffId);
            if (days == null) days = new boolean[]{false,false,false,false,false,false,false};

            // Staff name cell
            JPanel nameCell = new JPanel(new BorderLayout());
            nameCell.setBackground(UIUtils.TABLE_BG);
            nameCell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(80, 110, 160)),
                    new EmptyBorder(2, 8, 2, 8)));
            JLabel nameLbl = new JLabel(staff.getName(), JLabel.LEFT);
            nameLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
            nameLbl.setForeground(UIUtils.WHITE);
            nameCell.add(nameLbl, BorderLayout.CENTER);
            weekGrid.add(nameCell);

            for (int i = 0; i < 7; i++) {
                LocalDate d = weekStart.plusDays(i);
                boolean isWorking = days[i];
                boolean isToday = d.equals(today);
                if (isWorking && isToday) totalOnDuty++;
                weekGrid.add(createDayCell(staffId, i, isWorking, isToday,
                        weekGrid, weekStart, role, summaryLbl, allStaffList,
                        draftDays, pageStaffIds, totalStaff, editMode));
            }
        }

        // Fill remaining rows with empty cells so grid always shows PAGE_SIZE rows
        int rowsShown = pageStaffIds.size();
        for (int r = rowsShown; r < PAGE_SIZE; r++) {
            JPanel emptyName = new JPanel(new BorderLayout());
            emptyName.setBackground(UIUtils.TABLE_BG);
            emptyName.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(80, 110, 160)),
                    new EmptyBorder(2, 8, 2, 8)));
            weekGrid.add(emptyName);
            for (int i = 0; i < 7; i++) {
                JPanel emptyCell = new JPanel(new BorderLayout());
                emptyCell.setOpaque(false);
                emptyCell.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(80, 110, 160)),
                        new EmptyBorder(2, 2, 2, 2)));
                weekGrid.add(emptyCell);
            }
        }

        int dayOfWeekToday = today.getDayOfWeek().getValue();
        String todayName = DAY_NAMES[dayOfWeekToday - 1];
        String roleName = "TECHNICIAN".equals(role) ? "Technicians" : "Counter Staff";
        summaryLbl.setText(String.format("Total %s: %d  |  On duty %s: %d",
                roleName, totalStaff, todayName, totalOnDuty));

        weekGrid.revalidate();
        weekGrid.repaint();
    }

    private JPanel createDayCell(String staffId, int dayIdx, boolean isWorking,
                                  boolean isToday, JPanel weekGrid, LocalDate weekStart,
                                  String role, JLabel summaryLbl,
                                  List<? extends User> allStaffList,
                                  Map<String, boolean[]> draftDays,
                                  List<String> pageStaffIds,
                                  int totalStaff, boolean editMode) {
        JPanel dayCell = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                if (isToday) {
                    g2.setColor(new Color(40, 80, 150));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                }
                g2.dispose();
            }
        };
        dayCell.setOpaque(false);
        dayCell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(80, 110, 160)),
                new EmptyBorder(4, 2, 4, 2)));

        if (editMode) {
            dayCell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            final LocalDate fWeekStart = weekStart;
            final String fRole = role;
            final JLabel fSummaryLbl = summaryLbl;
            dayCell.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    boolean[] days = draftDays.get(staffId);
                    if (days != null) {
                        days[dayIdx] = !days[dayIdx];
                        rebuildWeekGrid(weekGrid, fWeekStart, fRole, fSummaryLbl,
                                allStaffList, draftDays, pageStaffIds, totalStaff, editMode);
                    }
                }
            });
        }

        JLabel statusLbl = new JLabel(isWorking ? "\u2713" : "\u2717", JLabel.CENTER);
        statusLbl.setFont(UIUtils.uiSymbolFont(Font.BOLD, 20));
        statusLbl.setForeground(isWorking ? UIUtils.GREEN_OK : UIUtils.ERROR_RED);
        dayCell.add(statusLbl, BorderLayout.CENTER);
        return dayCell;
    }

    // =========================================================================
    //  Status label
    // =========================================================================
    private void updateStatusLabel(JLabel statusLbl, boolean editMode,
                                    boolean weekPublished) {
        if (editMode) {
            statusLbl.setText("  \u270E EDIT MODE  —  Click cells to toggle ON/OFF");
            statusLbl.setForeground(new Color(100, 180, 255));
        } else if (!weekPublished) {
            statusLbl.setText("  \u26A0  This week has not been published yet. "
                    + "Staff cannot see this schedule.");
            statusLbl.setForeground(UIUtils.WARN_YELLOW);
        } else {
            statusLbl.setText("  \u2713 This week is published. "
                    + "Technicians and Counter Staff can see their schedules.");
            statusLbl.setForeground(UIUtils.GREEN_OK);
        }
    }

    // =========================================================================
    //  Legend helper
    // =========================================================================
    private JLabel legendItem(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(UIUtils.uiSymbolFont(Font.BOLD, 11));
        return l;
    }
}
