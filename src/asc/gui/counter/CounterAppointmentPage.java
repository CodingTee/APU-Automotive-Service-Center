package asc.gui.counter;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class CounterAppointmentPage extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CounterStaffDashboard main;
    private DefaultTableModel model;
    private JTable table;

    public CounterAppointmentPage(CounterStaffDashboard m) {
        main = m;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(18, 18, 18, 18));
        add(main.sectionTitle("Appointments",
                CounterStaffDashboard.ICON_BASE + "Appointments(Blue).png"), BorderLayout.NORTH);
        add(makeBody(), BorderLayout.CENTER);
    }

    // =========================================================================
    //  BODY  – form card fixed at top, table fills remainder
    // =========================================================================

    private JPanel makeBody() {
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(UIUtils.DARK_NAVY);

        // ── Create-appointment card ──────────────────────────────────────────
        JPanel card = main.makeCard(null);
        card.add(sectionLabel("Create & Assign Appointment"));
        card.add(Box.createVerticalStrut(10));

        // Row 1: customer search | matched customer | vehicle | service type
        JPanel row1 = new JPanel(new GridLayout(1, 4, 10, 0));
        row1.setBackground(CounterStaffDashboard.CARD2);
        // Row 2: date | time slot | technician | (blank)
        JPanel row2 = new JPanel(new GridLayout(1, 4, 10, 0));
        row2.setBackground(CounterStaffDashboard.CARD2);

        JTextField        search  = main.tf();
        JComboBox<String> custBox = new JComboBox<>();
        JComboBox<String> carBox  = new JComboBox<>();
        JComboBox<String> svcBox  = new JComboBox<>(new String[]{"NORMAL (1 hour)", "MAJOR (3 hours)"});
        JTextField        dateF   = new JTextField(LocalDate.now().toString());
        JComboBox<String> timeBox = new JComboBox<>(CounterStaffDashboard.TIME_SLOTS);
        JComboBox<String> techBox = new JComboBox<>();

        UIUtils.styleTextField(dateF);
        UIUtils.styleCombo(custBox); UIUtils.styleCombo(carBox);
        UIUtils.styleCombo(svcBox);  UIUtils.styleCombo(timeBox);
        UIUtils.styleCombo(techBox); techBox.setRenderer(new TechLook());

        // Vehicle auto-fill when customer changes
        Runnable fillCar = () -> {
            carBox.removeAllItems();
            Customer c = main.comboCustomer(custBox);
            if (c != null) for (String s : c.getVehicleList())
                if (!s.trim().isEmpty()) carBox.addItem(s.trim());
        };

        // Customer combo fill (filtered by search)
        Runnable fillCust = () -> {
            String old = main.comboId(custBox); custBox.removeAllItems();
            String q = search.getText().trim().toLowerCase();
            for (Customer c : FileManager.readAllCustomers()) {
                if (!q.isEmpty() && !main.customerFind(c, q)) continue;
                custBox.addItem(c.getUserId() + " | " + c.getName());
            }
            main.selectById(custBox, old); fillCar.run();
        };

        // Technician combo fill with availability colour
        // Three states:
        //   "● Available - "  → scheduled by manager AND time slot is free
        //   "● Busy - "       → scheduled by manager BUT time slot is taken
        //   "● Off - "        → NOT scheduled by manager (day not published/assigned) — cannot be selected
        Runnable fillTech = () -> {
            String old = techId(techBox); techBox.removeAllItems();
            String svc  = svcBox.getSelectedIndex() == 0 ? "NORMAL" : "MAJOR";
            String date = dateF.getText().trim();
            String time = timeBox.getSelectedItem() == null ? "" : timeBox.getSelectedItem().toString();
            for (Technician t : FileManager.readAllTechnicians()) {
                String prefix;
                if (FileManager.isTechnicianOffOnDay(t.getUserId(), date)) {
                    prefix = "\u25CF Off - ";
                } else if (FileManager.isTechnicianAvailable(t.getUserId(), date, time, svc)) {
                    prefix = "\u25CF Available - ";
                } else {
                    prefix = "\u25CF Busy - ";
                }
                techBox.addItem(prefix + t.getUserId() + " | " + t.getName());
            }
            pickTech(techBox, old);
        };

        fillCust.run(); fillTech.run();
        search.getDocument().addDocumentListener(main.doc(fillCust));
        custBox.addActionListener(e -> fillCar.run());
        svcBox.addActionListener(e -> fillTech.run());
        timeBox.addActionListener(e -> fillTech.run());
        dateF.addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent e) { fillTech.run(); } });
        techBox.addActionListener(e -> { if (busy(techBox)) pickTech(techBox, ""); });

        row1.add(main.inputBox("Customer Search",  search));
        row1.add(main.inputBox("Matched Customer", custBox));
        row1.add(main.inputBox("Vehicle",          carBox));
        row1.add(main.inputBox("Service Type",     svcBox));

        JPanel blankCell = new JPanel(); blankCell.setBackground(CounterStaffDashboard.CARD2);
        row2.add(main.inputBox("Date (yyyy-MM-dd)", dateF));
        row2.add(main.inputBox("Time Slot",         timeBox));
        row2.add(main.inputBox("Technician",        techBox));
        row2.add(blankCell);

        ServicePrice price = FileManager.readPrices();
        UIUtils.RoundedButton makeBtn = new UIUtils.RoundedButton(
                "Create Appointment  –  RM " + String.format("%.2f", price.getNormalServicePrice()),
                UIUtils.SUCCESS_GREEN);
        makeBtn.setPreferredSize(new Dimension(340, 36));
        svcBox.addActionListener(e -> makeBtn.setText("Create Appointment  –  RM " + String.format("%.2f",
                svcBox.getSelectedIndex() == 0 ? price.getNormalServicePrice() : price.getMajorServicePrice())));

        makeBtn.addActionListener(e -> {
            Customer c    = main.comboCustomer(custBox);
            String   tech = techId(techBox);
            String   car  = carBox.getSelectedItem() == null ? "" : carBox.getSelectedItem().toString();
            String   date = dateF.getText().trim();
            String   time = timeBox.getSelectedItem() == null ? "" : timeBox.getSelectedItem().toString();
            String   svc  = svcBox.getSelectedIndex() == 0 ? "NORMAL" : "MAJOR";
            if (c == null || tech.isEmpty() || car.isEmpty()) { main.msgErr(this, "Please complete all fields."); return; }
            if (!ValidationUtils.isValidDate(date))           { main.msgErr(this, "Invalid date."); return; }
            if (isOff(techBox)) {
                main.msgErr(this, "This technician is Off / Not Available on the selected date.\nManager has not scheduled them to work on this day.");
                fillTech.run(); return;
            }
            if (busy(techBox) || !FileManager.isTechnicianAvailable(tech, date, time, svc)) {
                main.msgErr(this, "This technician is not available at the selected time."); fillTech.run(); return;
            }
            Appointment apt = new Appointment(FileManager.generateAppointmentId(), c.getUserId(), tech,
                    main.currentStaff.getUserId(), svc, Appointment.STATUS_SCHEDULED, date, time, car, "");
            FileManager.saveAppointment(apt);
            // ── Notification: technician gets new appointment ──────────────
            FileManager.saveNotification(new asc.model.Notification(
                    tech, asc.model.Notification.APPOINTMENT_ASSIGNED,
                    "New Appointment",
                    "A new " + (svc.equals("MAJOR") ? "Major Service" : "Normal Service")
                    + " appointment (" + apt.getAppointmentId() + ") on " + date + " at " + time
                    + " for " + c.getName() + " (" + car + ")."));
            // ── Notification: customer gets reminder if appointment is today ──
            String today = java.time.LocalDate.now().toString();
            if (date.equals(today)) {
                FileManager.saveNotification(new asc.model.Notification(
                        c.getUserId(), asc.model.Notification.APPOINTMENT_REMINDER,
                        "Appointment Today",
                        "You have a " + (svc.equals("MAJOR") ? "Major Service" : "Normal Service")
                        + " appointment today at " + time + " (ID: " + apt.getAppointmentId() + ")."));
            }
            JOptionPane.showMessageDialog(this,
                    "Appointment created!\nID: " + apt.getAppointmentId() + "\nCustomer: " + c.getName() + "\nVehicle: " + car,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            refresh(); fillTech.run();
        });

        JPanel makeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        makeRow.setBackground(CounterStaffDashboard.CARD2);
        makeRow.add(makeBtn);

        card.add(row1);
        card.add(Box.createVerticalStrut(8));
        card.add(row2);
        card.add(Box.createVerticalStrut(10));
        card.add(makeRow);

        // ── Table fills remaining height ─────────────────────────────────────
        model = new DefaultTableModel(
                new String[]{"Apt ID","Customer","Vehicle","Technician","Type","Date","Time","Status"}, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = UIUtils.createStyledTable(model);
        main.darkTable(table);
        table.getColumnModel().getColumn(7).setCellRenderer(new StatusLook());

        // Double-click a MISSED appointment to reschedule
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                String status = (String) model.getValueAt(row, 7);
                if (!Appointment.STATUS_MISSED.equals(status)) return;
                String aptId = (String) model.getValueAt(row, 0);
                Appointment apt = FileManager.findAppointmentById(aptId);
                if (apt != null) rescheduleAppointment(apt);
            }
        });

        refresh();

        JScrollPane scroll = new JScrollPane(table);
        UIUtils.styleScrollPane(scroll);

        JPanel listCard = new JPanel(new BorderLayout());
        listCard.setBackground(CounterStaffDashboard.CARD2);
        listCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1), new EmptyBorder(12, 14, 12, 14)));
        JLabel tblHdr = new JLabel("All Appointments");
        tblHdr.setForeground(UIUtils.WHITE); tblHdr.setFont(UIUtils.uiFont(Font.BOLD, 13));
        tblHdr.setBorder(new EmptyBorder(0, 0, 8, 0));
        listCard.add(tblHdr,  BorderLayout.NORTH);
        listCard.add(scroll,  BorderLayout.CENTER);

        body.add(card,     BorderLayout.NORTH);
        body.add(listCard, BorderLayout.CENTER);
        return body;
    }

    /** Opens a reschedule dialog for a MISSED appointment. */
    private void rescheduleAppointment(Appointment apt) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Appointment " + apt.getAppointmentId() + " was missed.\n"
                        + "Customer: " + (apt.getCustomer() != null
                            ? apt.getCustomer().getName() : apt.getCustomerId()) + "\n"
                        + "Service: " + apt.getServiceType() + " | Vehicle: " + apt.getVehicleInfo() + "\n\n"
                        + "Would you like to reschedule it?",
                "Missed Appointment", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        // Build reschedule dialog
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Reschedule " + apt.getAppointmentId(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 320);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIUtils.DARK_NAVY);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Date field (default to tomorrow)
        JTextField dateF = new JTextField(LocalDate.now().plusDays(1).toString(), 12);
        UIUtils.styleTextField(dateF);
        addRow(p, gbc, 0, "Date:", dateF);

        // Time combo
        JComboBox<String> timeBox = new JComboBox<>(CounterStaffDashboard.TIME_SLOTS);
        UIUtils.styleCombo(timeBox);
        timeBox.setSelectedItem(apt.getTime());
        addRow(p, gbc, 1, "Time:", timeBox);

        // Service type (pre-filled, read-only)
        JLabel svcLbl = new JLabel(apt.getServiceType());
        svcLbl.setForeground(UIUtils.WHITE);
        svcLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        addRow(p, gbc, 2, "Service:", svcLbl);

        // Technician combo (live availability)
        JComboBox<String> techBox = new JComboBox<>();
        UIUtils.styleCombo(techBox);

        Runnable fillTechRsvp = () -> {
            String old = techId(techBox);
            techBox.removeAllItems();
            String date = dateF.getText().trim();
            String time = timeBox.getSelectedItem() == null ? "" : timeBox.getSelectedItem().toString();
            for (Technician t : FileManager.readAllTechnicians()) {
                String prefix;
                if (FileManager.isTechnicianOffOnDay(t.getUserId(), date)) {
                    prefix = "\u25CF Off - ";
                } else if (FileManager.isTechnicianAvailable(t.getUserId(), date, time, apt.getServiceType())) {
                    prefix = "\u25CF Available - ";
                } else {
                    prefix = "\u25CF Busy - ";
                }
                techBox.addItem(prefix + t.getUserId() + " | " + t.getName());
            }
            pickTech(techBox, old);
        };

        techBox.setRenderer(new TechLook());
        fillTechRsvp.run();
        dateF.addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent e) { fillTechRsvp.run(); } });
        timeBox.addActionListener(e -> fillTechRsvp.run());
        addRow(p, gbc, 3, "Technician:", techBox);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dlg.dispose());
        JButton confirmBtn = new JButton("Reschedule");
        confirmBtn.setBackground(UIUtils.SUCCESS_GREEN);
        confirmBtn.setForeground(UIUtils.WHITE);
        confirmBtn.addActionListener(e -> {
            String newDate = dateF.getText().trim();
            String newTime = timeBox.getSelectedItem() == null ? "" : timeBox.getSelectedItem().toString();
            String newTech = techId(techBox);

            if (newDate.isEmpty() || newTime.isEmpty() || newTech.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(dlg, "Invalid date format (YYYY-MM-DD).", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (isOff(techBox)) {
                JOptionPane.showMessageDialog(dlg, "Selected technician is Off on this day.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (busy(techBox)) {
                JOptionPane.showMessageDialog(dlg, "Selected technician is busy at this time.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            apt.setDate(newDate);
            apt.setTime(newTime);
            String oldTech = apt.getTechnicianId();
            apt.setTechnicianId(newTech);
            apt.setStatus(Appointment.STATUS_SCHEDULED);
            FileManager.updateAppointment(apt);
            // ── Notification: rescheduled ──────────────────────────────────
            FileManager.saveNotification(new asc.model.Notification(
                    newTech, asc.model.Notification.APPOINTMENT_RESCHEDULED,
                    "Appointment Rescheduled",
                    "Appointment " + apt.getAppointmentId() + " has been rescheduled to "
                    + newDate + " at " + newTime + "."));
            if (!newTech.equals(oldTech)) {
                FileManager.saveNotification(new asc.model.Notification(
                        oldTech, asc.model.Notification.APPOINTMENT_RESCHEDULED,
                        "Appointment Reassigned",
                        "Appointment " + apt.getAppointmentId() + " has been reassigned to another technician."));
            }
            dlg.dispose();
            refresh();
            JOptionPane.showMessageDialog(this, "Appointment " + apt.getAppointmentId() + " rescheduled.", "Done", JOptionPane.INFORMATION_MESSAGE);
        });
        btnRow.add(cancelBtn);
        btnRow.add(confirmBtn);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 6, 0, 6);
        p.add(btnRow, gbc);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int y, String label, JComponent comp) {
        JLabel lbl = new JLabel(label);
        lbl.setForeground(UIUtils.WHITE);
        lbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0.3;
        p.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        p.add(comp, gbc);
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private void refresh() {
        if (model == null) return;
        model.setRowCount(0);
        for (Appointment a : FileManager.readAllAppointments()) {
            Customer   c = a.getCustomer();
            Technician t = a.getTechnician();
            model.addRow(new Object[]{
                a.getAppointmentId(), c != null ? c.getName() : a.getCustomerId(),
                a.getVehicleInfo(), t != null ? t.getName() : a.getTechnicianId(),
                a.getServiceType(), a.getDate(), a.getTime(), a.getStatus()
            });
        }
    }

    private JComponent sectionLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.LEFT);
        l.setForeground(UIUtils.WHITE); l.setFont(UIUtils.uiFont(Font.BOLD, 14));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return l;
    }

    private String techId(JComboBox<String> box) {
        Object o = box.getSelectedItem(); if (o == null) return "";
        String s = o.toString();
        int dash = s.indexOf(" - "); if (dash >= 0) s = s.substring(dash + 3);
        int bar  = s.indexOf('|');
        return (bar >= 0 ? s.substring(0, bar) : s).trim();
    }

    private boolean busy(JComboBox<String> box) {
        Object o = box.getSelectedItem();
        return o != null && (o.toString().contains("Busy - ") || o.toString().contains("Off - "));
    }

    /** Returns true only if the selected technician is Off (not scheduled by manager). */
    private boolean isOff(JComboBox<String> box) {
        Object o = box.getSelectedItem();
        return o != null && o.toString().contains("Off - ");
    }

    private void pickTech(JComboBox<String> box, String id) {
        if (id != null && !id.isEmpty()) {
            for (int i = 0; i < box.getItemCount(); i++)
                if (box.getItemAt(i).contains("Available - " + id + " ")) { box.setSelectedIndex(i); return; }
        }
        for (int i = 0; i < box.getItemCount(); i++)
            if (box.getItemAt(i).contains("Available - ")) { box.setSelectedIndex(i); return; }
        if (box.getItemCount() > 0) box.setSelectedIndex(0);
    }

    // =========================================================================
    //  CELL RENDERERS
    // =========================================================================

    private class TechLook extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getListCellRendererComponent(JList<?> list, Object v, int i, boolean s, boolean f) {
            super.getListCellRendererComponent(list, v, i, s, f);
            String text = String.valueOf(v);
            // When rendering the selected item on the combo box (i == -1), use dark text colour
            // so it is readable on the combo box background. Only use status colours in the dropdown (i >= 0).
            boolean inDropdown = (i >= 0);
            if (text.contains("Off - ")) {
                setForeground(inDropdown ? new Color(200, 130, 50) : UIUtils.DARK_NAVY);
                setBackground(s ? new Color(80, 55, 20) : CounterStaffDashboard.CARD2);
            } else if (text.contains("Busy - ")) {
                setForeground(inDropdown ? UIUtils.ERROR_RED : UIUtils.DARK_NAVY);
                setBackground(s ? new Color(100, 30, 30) : CounterStaffDashboard.CARD2);
            } else {
                // Available — green in dropdown, dark/black on combo box bar
                setForeground(inDropdown ? UIUtils.GREEN_OK : UIUtils.DARK_NAVY);
                setBackground(s ? UIUtils.ACCENT_BLUE : CounterStaffDashboard.CARD2);
            }
            return this;
        }
    }

    private class StatusLook extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, s, f, r, c);
            String text = v == null ? "" : v.toString();
            setHorizontalAlignment(CENTER); setFont(UIUtils.uiFont(Font.BOLD, 11));
            switch (text) {
                case Appointment.STATUS_SCHEDULED:        setText("\uD83D\uDCC5 Scheduled");       setForeground(UIUtils.LIGHT_GRAY); break;
                case Appointment.STATUS_AWAITING_PAYMENT: setText("\uD83D\uDCB3 Awaiting Payment"); setForeground(UIUtils.WARN_YELLOW); break;
                case Appointment.STATUS_COMPLETED:        setText("\u2705 Completed");              setForeground(UIUtils.GREEN_OK); break;
                case Appointment.STATUS_MISSED:           setText("\u274C Missed");                 setForeground(UIUtils.ERROR_RED); break;
                default: setText(text); setForeground(UIUtils.DIM_TEXT); break;
            }
            setBackground(s ? UIUtils.ACCENT_BLUE : CounterStaffDashboard.CARD2);
            return this;
        }
    }
}
