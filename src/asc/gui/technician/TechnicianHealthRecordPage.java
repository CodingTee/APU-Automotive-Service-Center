package asc.gui.technician;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.util.List;

public class TechnicianHealthRecordPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final TechnicianDashboard main;

    // ── Column index constants (keep in sync with the table header) ───────────
    private static final int COL_REC_ID    = 0;
    private static final int COL_DATE      = 1;
    private static final int COL_CUSTOMER  = 2;
    private static final int COL_TECHNICIAN = 3;
    private static final int COL_MILEAGE   = 4;
    private static final int COL_OIL       = 7;

    public TechnicianHealthRecordPage(TechnicianDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.add(main.sectionTitle("Vehicle Health Records",
                TechnicianDashboard.ICON_BASE + "Vehicle Health Records(Blue).png"),
                BorderLayout.NORTH);

        // ── Table (12 columns) ───────────────────────────────────────────────
        String[] cols = {"Record ID","Date","Customer","Technician",
                "Latest Mileage","Tyre Tread Depth","Brake Thickness","Oil Type",
                "Battery","Trans Fluid","Coolant","Spark Plug"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIUtils.createStyledTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] widths = {72, 90, 120, 100, 100, 110, 110, 80, 85, 110, 100, 90};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.setRowHeight(28);

        // Mileage column — centre-aligned
        DefaultTableCellRenderer centerR = new DefaultTableCellRenderer();
        centerR.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(COL_MILEAGE).setCellRenderer(centerR);

        // ── Sort ──────────────────────────────────────────────────────────────
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // ── Load data ─────────────────────────────────────────────────────────
        Runnable refresh = () -> {
            model.setRowCount(0);
            for (VehicleHealthRecord r : FileManager.readAllHealthRecords()) {
                Customer c = r.getCustomer();
                // Resolve technician name for display
                String techDisplay = r.getTechnicianId();
                asc.model.Technician t = FileManager.findTechnicianById(r.getTechnicianId());
                if (t != null) techDisplay = t.getName();
                model.addRow(new Object[]{
                    r.getRecordId(),
                    r.getRecordDate(),
                    c != null ? c.getName() : r.getCustomerId(),
                    techDisplay,
                    r.getMileage() > 0 ? r.getMileage() + " km" : "—",
                    notEmpty(r.getTyreTreadDepth(), "—"),
                    notEmpty(r.getBrakeThickness(), "—"),
                    r.getOilType(),
                    notEmpty(r.getBatteryHealth(), "—"),
                    notEmpty(r.getTransmissionFluid(), "—"),
                    notEmpty(r.getCoolantCondition(), "—"),
                    r.getSparkPlugStatus()
                });
            }
        };
        refresh.run();

        JScrollPane scroll = new JScrollPane(table);
        UIUtils.styleScrollPane(scroll);

        // ── Search / Filter bar ───────────────────────────────────────────────
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setForeground(UIUtils.LIGHT_GRAY);
        searchLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));

        JTextField searchField = new JTextField(16);
        UIUtils.styleTextField(searchField);
        searchField.setToolTipText("Search by Record ID, Customer or Date");

        JLabel oilLbl = new JLabel("Oil Type:");
        oilLbl.setForeground(UIUtils.LIGHT_GRAY);
        oilLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));

        java.util.Set<String> oilSet = new java.util.LinkedHashSet<>();
        oilSet.add("All");
        for (VehicleHealthRecord r : FileManager.readAllHealthRecords()) {
            if (r.getOilType() != null && !r.getOilType().isBlank())
                oilSet.add(r.getOilType());
        }
        JComboBox<String> oilFilter = new JComboBox<>(oilSet.toArray(new String[0]));
        UIUtils.styleCombo(oilFilter);

        UIUtils.RoundedButton searchBtn = new UIUtils.RoundedButton("Search", new Color(70, 130, 200));
        UIUtils.RoundedButton clearBtn  = new UIUtils.RoundedButton("Clear",   new Color(180, 60, 60));
        searchBtn.setPreferredSize(new Dimension(110, 30));
        clearBtn .setPreferredSize(new Dimension(90,  30));

        searchBtn.addActionListener(e ->
                filterRecords(searchField.getText().trim(),
                        (String) oilFilter.getSelectedItem(), sorter));
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            oilFilter.setSelectedIndex(0);
            sorter.setRowFilter(null);
            refresh.run();
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                filterRecords(searchField.getText().trim(),
                        (String) oilFilter.getSelectedItem(), sorter);
            }
        });

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filterRow.setBackground(UIUtils.DARK_NAVY);
        filterRow.add(searchLbl); filterRow.add(searchField);
        filterRow.add(oilLbl);    filterRow.add(oilFilter);
        filterRow.add(searchBtn); filterRow.add(clearBtn);

        // ── Buttons ───────────────────────────────────────────────────────────
        UIUtils.RoundedButton addBtn  = new UIUtils.RoundedButton("Add Health Record", UIUtils.SUCCESS_GREEN);
        UIUtils.RoundedButton editBtn = new UIUtils.RoundedButton("Edit Record",       UIUtils.ACCENT_BLUE);
        UIUtils.RoundedButton viewBtn = new UIUtils.RoundedButton("View Details",      new Color(70, 190, 185));
        UIUtils.RoundedButton refBtn  = new UIUtils.RoundedButton("Refresh",           new Color(60, 90, 150));
        for (JButton b : new JButton[]{addBtn, editBtn, viewBtn, refBtn})
            b.setPreferredSize(new Dimension(180, 34));

        addBtn.addActionListener(e -> { showAddHealthRecordDialog(); refresh.run(); });
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a record.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int mr = table.convertRowIndexToModel(row);
            String rid = (String) model.getValueAt(mr, COL_REC_ID);
            VehicleHealthRecord rec = FileManager.readAllHealthRecords().stream()
                    .filter(r -> r.getRecordId().equals(rid)).findFirst().orElse(null);
            if (rec != null) { showEditHealthRecordDialog(rec); refresh.run(); }
        });
        refBtn.addActionListener(e -> {
            sorter.setRowFilter(null);
            searchField.setText("");
            oilFilter.setSelectedIndex(0);
            refresh.run();
        });
        viewBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a record.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int mr = table.convertRowIndexToModel(row);
            String rid = (String) model.getValueAt(mr, COL_REC_ID);
            VehicleHealthRecord rec = FileManager.readAllHealthRecords().stream()
                    .filter(r -> r.getRecordId().equals(rid)).findFirst().orElse(null);
            if (rec != null) showViewDetailsDialog(rec);
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setBackground(UIUtils.DARK_NAVY);
        btnRow.add(addBtn); btnRow.add(editBtn); btnRow.add(viewBtn); btnRow.add(refBtn);

        JPanel centre = new JPanel(new BorderLayout(0, 6));
        centre.setBackground(UIUtils.DARK_NAVY);
        centre.add(filterRow, BorderLayout.NORTH);
        centre.add(scroll,    BorderLayout.CENTER);

        panel.add(centre, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    // ── Custom filter (column-aware) ──────────────────────────────────────────
    private void filterRecords(String keyword, String oilFilter,
            TableRowSorter<DefaultTableModel> sorter) {
        if (keyword.isEmpty() && "All".equals(oilFilter)) {
            sorter.setRowFilter(null);
            return;
        }
        java.util.List<RowFilter<Object, Object>> andFilters = new java.util.ArrayList<>();
        if (!"All".equals(oilFilter)) {
            try {
                andFilters.add(RowFilter.regexFilter(
                        "(?i)^" + java.util.regex.Pattern.quote(oilFilter) + "$", COL_OIL));
            } catch (Exception ex) { /* ignore */ }
        }
        if (!keyword.isEmpty()) {
            try {
                java.util.List<RowFilter<Object, Object>> orFilters = new java.util.ArrayList<>();
                orFilters.add(RowFilter.regexFilter("(?i)" + keyword, COL_REC_ID));
                orFilters.add(RowFilter.regexFilter("(?i)" + keyword, COL_DATE));
                orFilters.add(RowFilter.regexFilter("(?i)" + keyword, COL_CUSTOMER));
                orFilters.add(RowFilter.regexFilter("(?i)" + keyword, COL_TECHNICIAN));
                andFilters.add(RowFilter.orFilter(orFilters));
            } catch (java.util.regex.PatternSyntaxException ex) {
                sorter.setRowFilter(null);
                return;
            }
        }
        sorter.setRowFilter(andFilters.size() == 1
                ? andFilters.get(0) : RowFilter.andFilter(andFilters));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String notEmpty(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }

    /** Auto-calculate next service mileage from oil type.
     *  Semi-synthetic (5W, 10W-30/40) → +5000 km; Full-synthetic (0W, 5W-30, 10W-30) → +10000 km. */
    private static int calcNextServiceMileage(int currentKm, String oilType) {
        if (currentKm <= 0) return 0;
        if (oilType == null) return currentKm + 5000;
        String ot = oilType.toUpperCase().trim();
        // Full-synthetic: typically 0W-xx or 5W-30 / 10W-30 with "FULL" in name
        if (ot.contains("FULL") || ot.startsWith("0W"))
            return currentKm + 10000;
        // Semi-synthetic or conventional: default to 5000
        return currentKm + 5000;
    }

    // =========================================================================
    //  DIALOG: Add for a specific appointment (called from Appointments page)
    // =========================================================================
    void showAddForAppointment(Appointment apt) {
        if (apt == null) return;
        Customer cust = apt.getCustomer();

        JDialog d = new JDialog(main, "Add Vehicle Health Record — " + apt.getAppointmentId(), true);
        d.setSize(550, 700); d.setLocationRelativeTo(main);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIUtils.DARK_NAVY); p.setBorder(new EmptyBorder(16, 28, 16, 28));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(5, 5, 5, 5);

        int r = 0;
        JLabel aptInfo = new JLabel("<html><b>" + apt.getAppointmentId() + "</b>  |  "
                + apt.getServiceType() + "  |  " + apt.getDate()
                + "  |  " + (cust != null ? cust.getName() : apt.getCustomerId()) + "</html>");
        aptInfo.setForeground(UIUtils.ACCENT_BLUE);
        aptInfo.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        gbc.gridx=0; gbc.gridy=r++; gbc.gridwidth=2; p.add(aptInfo, gbc); gbc.gridwidth=1;

        // ── Basic fields ──────────────────────────────────────────────────────
        JTextField tyreF  = new JTextField("32");     UIUtils.styleTextField(tyreF);
        JTextField brakeF = new JTextField("8");      UIUtils.styleTextField(brakeF);
        JTextField oilF   = new JTextField("5W-30");  UIUtils.styleTextField(oilF);
        JTextField sparkF = new JTextField("Good");   UIUtils.styleTextField(sparkF);
        JTextField mileF  = new JTextField("");        UIUtils.styleTextField(mileF);
        JTextField notesF = new JTextField();          UIUtils.styleTextField(notesF);

        // ── New: Sticker mileage fields ───────────────────────────────────────
        JTextField nextSvcF = new JTextField(); UIUtils.styleTextField(nextSvcF);
        nextSvcF.setToolTipText("Auto-calculated from mileage + oil type interval");

        JTextField majorF = new JTextField(); UIUtils.styleTextField(majorF);
        majorF.setToolTipText("Next major inspection mileage (timing belt, spark plugs, ATF, etc.)");

        // ── New: Advanced inspection fields ───────────────────────────────────
        JTextField batteryF  = new JTextField("Good");  UIUtils.styleTextField(batteryF);
        JTextField treadF    = new JTextField("8.0");   UIUtils.styleTextField(treadF);
        JComboBox<String> transCb = new JComboBox<>(
                new String[]{"Red / Clean", "Brown", "Black / Burnt"});
        UIUtils.styleCombo(transCb);
        JComboBox<String> coolantCb = new JComboBox<>(
                new String[]{"Green / Clean", "Rusty / Brown", "Low Level"});
        UIUtils.styleCombo(coolantCb);

        // Auto-calc next service when mileage or oil changes
        Runnable autoCalcNext = () -> {
            try {
                int km = Integer.parseInt(mileF.getText().trim());
                int next = calcNextServiceMileage(km, oilF.getText().trim());
                if (next > 0) nextSvcF.setText(String.valueOf(next));
            } catch (NumberFormatException ex) { /* leave as-is */ }
        };
        mileF.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { autoCalcNext.run(); }
        });
        oilF.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { autoCalcNext.run(); }
        });

        main.addDRow(p, gbc, r++, "Tyre Pressure (psi):",    tyreF);
        main.addDRow(p, gbc, r++, "Brake Thickness (mm):",  brakeF);
        main.addDRow(p, gbc, r++, "Oil Type:",          oilF);
        main.addDRow(p, gbc, r++, "Spark Plug:",        sparkF);
        main.addDRow(p, gbc, r++, "Latest Mileage (km):",      mileF);
        main.addDRow(p, gbc, r++, "Next Service Due (km):",  nextSvcF);
        main.addDRow(p, gbc, r++, "Major Inspection Due (km):", majorF);
        main.addDRow(p, gbc, r++, "Battery Health:",    batteryF);
        main.addDRow(p, gbc, r++, "Tyre Tread Depth (mm):",  treadF);
        main.addDRow(p, gbc, r++, "Transmission Fluid:",transCb);
        main.addDRow(p, gbc, r++, "Coolant Condition:", coolantCb);
        main.addDRow(p, gbc, r++, "Notes:",             notesF);

        UIUtils.RoundedButton save = new UIUtils.RoundedButton("Save Health Record", UIUtils.SUCCESS_GREEN);
        UIUtils.RoundedButton skip = new UIUtils.RoundedButton("Skip", new Color(80, 80, 100));
        save.setPreferredSize(new Dimension(180, 34));
        skip.setPreferredSize(new Dimension(100, 34));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setBackground(UIUtils.DARK_NAVY);
        btnRow.add(save); btnRow.add(skip);
        gbc.gridx=0; gbc.gridy=r; gbc.gridwidth=2; p.add(btnRow, gbc);

        save.addActionListener(e -> {
            int km = 0, nextKm = 0, majorKm = 0;
            try { km = Integer.parseInt(mileF.getText().trim()); } catch (Exception ex) {}
            try { nextKm = Integer.parseInt(nextSvcF.getText().trim()); } catch (Exception ex) {}
            try { majorKm = Integer.parseInt(majorF.getText().trim()); } catch (Exception ex) {}
            VehicleHealthRecord rec = new VehicleHealthRecord(
                    FileManager.generateHealthRecordId(),
                    apt.getAppointmentId(), apt.getCustomerId(),
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    tyreF.getText().trim(), brakeF.getText().trim(),
                    oilF.getText().trim(), sparkF.getText().trim(),
                    km, notesF.getText().trim(), main.currentTech.getUserId(),
                    nextKm, majorKm,
                    batteryF.getText().trim(), treadF.getText().trim(),
                    ((String)transCb.getSelectedItem()).trim(),
                    ((String)coolantCb.getSelectedItem()).trim());
            FileManager.saveHealthRecord(rec);
            // ── Notification: customer gets health record ─────────────
            FileManager.saveNotification(new asc.model.Notification(
                    apt.getCustomerId(), asc.model.Notification.HEALTH_RECORD_ADDED,
                    "Vehicle Health Record Added",
                    "A new vehicle health record has been added for your vehicle ("
                    + apt.getVehicleInfo() + ") after appointment "
                    + apt.getAppointmentId() + "."));
            JOptionPane.showMessageDialog(d, "Health record saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            d.dispose();
        });
        skip.addActionListener(e -> d.dispose());

        d.setContentPane(new JScrollPane(p));
        d.setVisible(true);
    }

    // =========================================================================
    //  DIALOG: Add from scratch (technician picks appointment)
    // =========================================================================
    void showAddHealthRecordDialog() {
        List<Appointment> eligible = FileManager.readAllAppointments().stream()
                .filter(a -> a.getTechnicianId().equals(main.currentTech.getUserId())
                        && "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        if (eligible.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No completed appointments to add a record for.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog d = new JDialog((Dialog) null, "Add Vehicle Health Record", true);
        d.setSize(550, 700); d.setLocationRelativeTo(null);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIUtils.DARK_NAVY); p.setBorder(new EmptyBorder(16, 28, 16, 28));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(5, 5, 5, 5);

        String[] aptOpts = eligible.stream().map(a -> {
            Customer c = a.getCustomer();
            return a.getAppointmentId() + " | " + a.getDate() + " | "
                    + ((c != null) ? c.getName() : a.getCustomerId());
        }).toArray(String[]::new);
        JComboBox<String> aptCb = new JComboBox<>(aptOpts); UIUtils.styleCombo(aptCb);

        JTextField tyreF   = new JTextField("32");     UIUtils.styleTextField(tyreF);
        JTextField brakeF  = new JTextField("8");      UIUtils.styleTextField(brakeF);
        JTextField oilF    = new JTextField("5W-30");  UIUtils.styleTextField(oilF);
        JTextField sparkF  = new JTextField("Good");   UIUtils.styleTextField(sparkF);
        JTextField mileF   = new JTextField("");        UIUtils.styleTextField(mileF);
        JTextField notesF  = new JTextField();          UIUtils.styleTextField(notesF);

        JTextField nextSvcF = new JTextField(); UIUtils.styleTextField(nextSvcF);
        nextSvcF.setToolTipText("Auto-calculated from mileage + oil type interval");

        JTextField majorF = new JTextField(); UIUtils.styleTextField(majorF);
        majorF.setToolTipText("Next major inspection mileage (timing belt, spark plugs, ATF, etc.)");

        JTextField batteryF  = new JTextField("Good");  UIUtils.styleTextField(batteryF);
        JTextField treadF    = new JTextField("8.0");   UIUtils.styleTextField(treadF);
        JComboBox<String> transCb = new JComboBox<>(
                new String[]{"Red / Clean", "Brown", "Black / Burnt"});
        UIUtils.styleCombo(transCb);
        JComboBox<String> coolantCb = new JComboBox<>(
                new String[]{"Green / Clean", "Rusty / Brown", "Low Level"});
        UIUtils.styleCombo(coolantCb);

        Runnable autoCalcNext = () -> {
            try {
                int km = Integer.parseInt(mileF.getText().trim());
                int next = calcNextServiceMileage(km, oilF.getText().trim());
                if (next > 0) nextSvcF.setText(String.valueOf(next));
            } catch (NumberFormatException ex) {}
        };
        mileF.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { autoCalcNext.run(); }
        });
        oilF.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { autoCalcNext.run(); }
        });

        int r = 0;
        main.addDRow(p, gbc, r++, "Appointment:",      aptCb);
        main.addDRow(p, gbc, r++, "Tyre Pressure (psi):",    tyreF);
        main.addDRow(p, gbc, r++, "Brake Thickness (mm):",  brakeF);
        main.addDRow(p, gbc, r++, "Oil Type:",          oilF);
        main.addDRow(p, gbc, r++, "Spark Plug:",        sparkF);
        main.addDRow(p, gbc, r++, "Latest Mileage (km):",      mileF);
        main.addDRow(p, gbc, r++, "Next Service Due (km):",  nextSvcF);
        main.addDRow(p, gbc, r++, "Major Inspection Due (km):", majorF);
        main.addDRow(p, gbc, r++, "Battery Health:",    batteryF);
        main.addDRow(p, gbc, r++, "Tyre Tread Depth (mm):",  treadF);
        main.addDRow(p, gbc, r++, "Transmission Fluid:",transCb);
        main.addDRow(p, gbc, r++, "Coolant Condition:", coolantCb);
        main.addDRow(p, gbc, r++, "Notes:",             notesF);

        UIUtils.RoundedButton save = new UIUtils.RoundedButton("Save Record", UIUtils.SUCCESS_GREEN);
        save.setPreferredSize(new Dimension(180, 34));
        save.addActionListener(e -> {
            int idx = aptCb.getSelectedIndex(); if (idx < 0) return;
            Appointment apt = eligible.get(idx);
            int km = 0, nextKm = 0, majorKm = 0;
            try { km = Integer.parseInt(mileF.getText().trim()); } catch (Exception ex) {}
            try { nextKm = Integer.parseInt(nextSvcF.getText().trim()); } catch (Exception ex) {}
            try { majorKm = Integer.parseInt(majorF.getText().trim()); } catch (Exception ex) {}
            VehicleHealthRecord rec = new VehicleHealthRecord(
                    FileManager.generateHealthRecordId(), apt.getAppointmentId(), apt.getCustomerId(),
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    tyreF.getText().trim(), brakeF.getText().trim(),
                    oilF.getText().trim(), sparkF.getText().trim(),
                    km, notesF.getText().trim(), main.currentTech.getUserId(),
                    nextKm, majorKm,
                    batteryF.getText().trim(), treadF.getText().trim(),
                    ((String)transCb.getSelectedItem()).trim(),
                    ((String)coolantCb.getSelectedItem()).trim());
            FileManager.saveHealthRecord(rec);
            // ── Notification: customer gets health record ─────────────
            FileManager.saveNotification(new asc.model.Notification(
                    apt.getCustomerId(), asc.model.Notification.HEALTH_RECORD_ADDED,
                    "Vehicle Health Record Added",
                    "A new vehicle health record has been added for your vehicle ("
                    + apt.getVehicleInfo() + ") after appointment "
                    + apt.getAppointmentId() + "."));

            // Award VIP points
            VipAccount vip = FileManager.findVipByCustomerId(apt.getCustomerId());
            if (vip != null) {
                Payment pay = FileManager.findPaymentByAppointmentId(apt.getAppointmentId());
                if (pay != null) {
                    String pm = pay.getPaymentMethod() != null ? pay.getPaymentMethod() : "";
                    if (!"Use Balance".equalsIgnoreCase(pm)) {
                        vip.addPointsFromPayment(pay.getAmount(), 0.5);
                        FileManager.updateVipAccount(vip);
                    }
                }
            }

            JOptionPane.showMessageDialog(d, "Health record saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            d.dispose();
        });
        gbc.gridx=0; gbc.gridy=r; gbc.gridwidth=2; p.add(save, gbc);
        d.setContentPane(new JScrollPane(p)); d.setVisible(true);
    }

    // =========================================================================
    //  DIALOG: Edit existing record
    // =========================================================================
    void showEditHealthRecordDialog(VehicleHealthRecord rec) {
        JDialog d = new JDialog((Dialog) null, "Edit Health Record - " + rec.getRecordId(), true);
        d.setSize(550, 700); d.setLocationRelativeTo(null);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIUtils.DARK_NAVY); p.setBorder(new EmptyBorder(16, 28, 16, 28));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(5, 5, 5, 5);

        JTextField tyreF   = new JTextField(rec.getTirePressure());    UIUtils.styleTextField(tyreF);
        JTextField brakeF  = new JTextField(rec.getBrakeThickness());   UIUtils.styleTextField(brakeF);
        JTextField oilF    = new JTextField(rec.getOilType());          UIUtils.styleTextField(oilF);
        JTextField sparkF  = new JTextField(rec.getSparkPlugStatus());  UIUtils.styleTextField(sparkF);
        JTextField mileF   = new JTextField(String.valueOf(rec.getMileage())); UIUtils.styleTextField(mileF);
        JTextField notesF  = new JTextField(rec.getNotes());             UIUtils.styleTextField(notesF);

        JTextField nextSvcF = new JTextField(String.valueOf(rec.getNextServiceMileage()));
        UIUtils.styleTextField(nextSvcF);

        JTextField majorF = new JTextField(String.valueOf(rec.getMajorServiceMileage()));
        UIUtils.styleTextField(majorF);
        majorF.setToolTipText("Next major inspection mileage");

        JTextField batteryF = new JTextField(rec.getBatteryHealth());     UIUtils.styleTextField(batteryF);
        JTextField treadF   = new JTextField(rec.getTyreTreadDepth());    UIUtils.styleTextField(treadF);

        JComboBox<String> transCb = new JComboBox<>(
                new String[]{"", "Red / Clean", "Brown", "Black / Burnt"});
        UIUtils.styleCombo(transCb);
        transCb.setSelectedItem(rec.getTransmissionFluid());

        JComboBox<String> coolantCb = new JComboBox<>(
                new String[]{"", "Green / Clean", "Rusty / Brown", "Low Level"});
        UIUtils.styleCombo(coolantCb);
        coolantCb.setSelectedItem(rec.getCoolantCondition());

        Runnable autoCalcNext = () -> {
            try {
                int km = Integer.parseInt(mileF.getText().trim());
                int next = calcNextServiceMileage(km, oilF.getText().trim());
                if (next > 0) nextSvcF.setText(String.valueOf(next));
            } catch (NumberFormatException ex) {}
        };
        mileF.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { autoCalcNext.run(); }
        });

        int r = 0;
        main.addDRow(p, gbc, r++, "Tyre Pressure (psi):",    tyreF);
        main.addDRow(p, gbc, r++, "Brake Thickness (mm):",  brakeF);
        main.addDRow(p, gbc, r++, "Oil Type:",          oilF);
        main.addDRow(p, gbc, r++, "Spark Plug:",        sparkF);
        main.addDRow(p, gbc, r++, "Latest Mileage (km):",      mileF);
        main.addDRow(p, gbc, r++, "Next Service Due (km):",  nextSvcF);
        main.addDRow(p, gbc, r++, "Major Inspection Due (km):", majorF);
        main.addDRow(p, gbc, r++, "Battery Health:",    batteryF);
        main.addDRow(p, gbc, r++, "Tyre Tread Depth (mm):",  treadF);
        main.addDRow(p, gbc, r++, "Transmission Fluid:",transCb);
        main.addDRow(p, gbc, r++, "Coolant Condition:", coolantCb);
        main.addDRow(p, gbc, r++, "Notes:",             notesF);

        UIUtils.RoundedButton save = new UIUtils.RoundedButton("Update Record", UIUtils.ACCENT_BLUE);
        save.setPreferredSize(new Dimension(180, 34));
        save.addActionListener(e -> {
            int km = 0, nextKm = 0, majorKm = 0;
            try { km = Integer.parseInt(mileF.getText().trim()); } catch (Exception ex) {}
            try { nextKm = Integer.parseInt(nextSvcF.getText().trim()); } catch (Exception ex) {}
            try { majorKm = Integer.parseInt(majorF.getText().trim()); } catch (Exception ex) {}
            rec.setTirePressure(tyreF.getText().trim());
            rec.setBrakeThickness(brakeF.getText().trim());
            rec.setOilType(oilF.getText().trim());
            rec.setSparkPlugStatus(sparkF.getText().trim());
            rec.setMileage(km);
            rec.setNotes(notesF.getText().trim());
            rec.setNextServiceMileage(nextKm);
            rec.setMajorServiceMileage(majorKm);
            rec.setBatteryHealth(batteryF.getText().trim());
            rec.setTyreTreadDepth(treadF.getText().trim());
            rec.setTransmissionFluid(((String)transCb.getSelectedItem()).trim());
            rec.setCoolantCondition(((String)coolantCb.getSelectedItem()).trim());
            FileManager.updateHealthRecord(rec);
            JOptionPane.showMessageDialog(d, "Record updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
            d.dispose();
        });
        gbc.gridx=0; gbc.gridy=r; gbc.gridwidth=2; p.add(save, gbc);
        d.setContentPane(new JScrollPane(p)); d.setVisible(true);
    }

    // =========================================================================
    //  DIALOG: View full record details
    // =========================================================================
    void showViewDetailsDialog(VehicleHealthRecord rec) {
        Customer c = FileManager.findCustomerById(rec.getCustomerId());
        JDialog d = new JDialog((Dialog) null, "Health Record — " + rec.getRecordId(), true);
        d.setSize(480, 600); d.setLocationRelativeTo(null);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIUtils.DARK_NAVY); p.setBorder(new EmptyBorder(16, 28, 16, 28));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(4, 5, 4, 5);

        String[][] fields = {
            {"Record ID:",       rec.getRecordId()},
            {"Date:",            rec.getRecordDate()},
            {"Appointment ID:", rec.getAppointmentId()},
            {"Customer:",        c != null ? c.getName() : rec.getCustomerId()},
            {"Latest Mileage:",  rec.getMileage() > 0 ? rec.getMileage() + " km" : "—"},
            {"Next Service Due:", rec.getNextServiceMileage() > 0 ? rec.getNextServiceMileage() + " km" : "—"},
            {"Major Inspection Due:", rec.getMajorServiceMileage() > 0 ? rec.getMajorServiceMileage() + " km" : "—"},
            {"Tyre Pressure:",     notEmpty(rec.getTirePressure(), "—")},
            {"Brake Thickness:",   notEmpty(rec.getBrakeThickness(), "—")},
            {"Tyre Tread Depth:",  notEmpty(rec.getTyreTreadDepth(), "—")},
            {"Oil Type:",          rec.getOilType()},
            {"Spark Plug:",        rec.getSparkPlugStatus()},
            {"Battery Health:",    notEmpty(rec.getBatteryHealth(), "—")},
            {"Transmission Fluid:", notEmpty(rec.getTransmissionFluid(), "—")},
            {"Coolant Condition:",  notEmpty(rec.getCoolantCondition(), "—")},
            {"Notes:",             notEmpty(rec.getNotes(), "—")},
        };

        for (int i = 0; i < fields.length; i++) {
            gbc.gridy = i;
            gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 0.35;
            JLabel lbl = new JLabel(fields[i][0]);
            lbl.setForeground(UIUtils.WHITE); lbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
            p.add(lbl, gbc);

            gbc.gridx = 1; gbc.weightx = 0.65;
            JLabel val = new JLabel(fields[i][1]);
            val.setForeground(UIUtils.LIGHT_GRAY); val.setFont(UIUtils.uiFont(Font.PLAIN, 12));
            p.add(val, gbc);
        }

        UIUtils.RoundedButton close = new UIUtils.RoundedButton("Close", new Color(80, 80, 100));
        close.setPreferredSize(new Dimension(100, 34));
        close.addActionListener(e -> d.dispose());
        gbc.gridy = fields.length; gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1;
        gbc.insets = new Insets(14, 5, 4, 5);
        p.add(close, gbc);

        d.setContentPane(new JScrollPane(p));
        d.setVisible(true);
    }
}
