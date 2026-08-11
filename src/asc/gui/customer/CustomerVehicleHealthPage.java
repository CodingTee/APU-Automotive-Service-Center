package asc.gui.customer;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class CustomerVehicleHealthPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final CustomerDashboard main;
    private final String highlightRecordId;

    public CustomerVehicleHealthPage(CustomerDashboard m) {
        this(m, null);
    }

    public CustomerVehicleHealthPage(CustomerDashboard m, String highlightRecordId) {
        main = m;
        this.highlightRecordId = highlightRecordId;
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
                CustomerDashboard.ICON_BASE + "VehicleHealth.png"), BorderLayout.NORTH);

        // ── Gather data ──────────────────────────────────────────────────────
        List<VehicleHealthRecord> myRecords =
                FileManager.findHealthRecordsByCustomerId(main.currentCustomer.getUserId());

        // Build a quick lookup: appointmentId → Appointment for service type & vehicle
        Map<String, Appointment> aptMap = new HashMap<>();
        for (Appointment a : main.currentCustomer.getMyAppointments())
            aptMap.put(a.getAppointmentId(), a);

        // ── Table ────────────────────────────────────────────────────────────
        String[] cols = {"Record ID","Date","Vehicle","Service","Mileage",
                         "Next Oil","Next Major","Tyre","Brake","Oil","History"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 10; }
        };
        JTable table = UIUtils.createStyledTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowHeight(30);
        int[] widths = {70, 90, 120, 100, 75, 80, 80, 85, 70, 90, 55};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // ── Renderers ────────────────────────────────────────────────────────
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            { setHorizontalAlignment(CENTER); }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Mileage
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Next Oil
        table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Next Major

        // Service type coloured renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                    boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String s = v == null ? "" : v.toString();
                setForeground(s.contains("Major") ? CustomerDashboard.ACCENT_YELLOW
                        : UIUtils.SUCCESS_GREEN);
                setBackground(sel ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
                setHorizontalAlignment(CENTER);
                return this;
            }
        });

        // ── Apt column: clickable button to jump to Service History ──────────
        table.getColumnModel().getColumn(10).setCellRenderer(new AptButtonRenderer());
        table.getColumnModel().getColumn(10).setCellEditor(new AptButtonEditor());

        // ── Load data ─────────────────────────────────────────────────────────
        Runnable fill = () -> {
            model.setRowCount(0);
            int highlightRow = -1;
            for (VehicleHealthRecord r : myRecords) {
                Appointment apt = aptMap.get(r.getAppointmentId());
                String vehicle = apt != null ? apt.getVehicleInfo() : "\u2014";
                String svcType = apt != null
                        ? ("NORMAL".equalsIgnoreCase(apt.getServiceType())
                                ? "Normal" : "Major")
                        : "\u2014";
                model.addRow(new Object[]{
                    r.getRecordId(),
                    r.getRecordDate(),
                    vehicle,
                    svcType,
                    r.getMileage() > 0 ? r.getMileage() + " km" : "\u2014",
                    r.getNextServiceMileage() > 0 ? r.getNextServiceMileage() + " km" : "\u2014",
                    r.getMajorServiceMileage() > 0 ? r.getMajorServiceMileage() + " km" : "\u2014",
                    r.getTirePressure(),
                    r.getBrakeThickness(),
                    r.getOilType(),
                    r.getAppointmentId()  // stored for Apt column button
                });
                if (highlightRecordId != null && highlightRecordId.equals(r.getRecordId()))
                    highlightRow = model.getRowCount() - 1;
            }
            // Scroll to highlighted row
            if (highlightRow >= 0) {
                final int row = highlightRow;
                SwingUtilities.invokeLater(() -> {
                    table.setRowSelectionInterval(row, row);
                    table.scrollRectToVisible(table.getCellRect(row, 0, true));
                });
            }
        };
        fill.run();

        JScrollPane scroll = new JScrollPane(table);
        UIUtils.styleScrollPane(scroll);

        // ── Summary cards ─────────────────────────────────────────────────────
        int latestMileage = 0, nextOil = 0, nextMajor = 0;
        for (VehicleHealthRecord r : myRecords) {
            if (r.getMileage() > latestMileage) latestMileage = r.getMileage();
            if (r.getNextServiceMileage() > 0 && (nextOil == 0 || r.getNextServiceMileage() < nextOil))
                nextOil = r.getNextServiceMileage();
            if (r.getMajorServiceMileage() > 0 && (nextMajor == 0 || r.getMajorServiceMileage() < nextMajor))
                nextMajor = r.getMajorServiceMileage();
        }

        JPanel summary = new JPanel(new GridLayout(1, 4, 10, 0));
        summary.setBackground(UIUtils.DARK_NAVY);
        summary.setBorder(new EmptyBorder(10, 0, 0, 0));
        summary.add(main.miniCard("Total Records", String.valueOf(myRecords.size()),
                UIUtils.ACCENT_BLUE));
        summary.add(main.miniCard("Latest Mileage",
                latestMileage > 0 ? latestMileage + " km" : "\u2014", CustomerDashboard.GREEN_OK));
        summary.add(main.miniCard("Next Oil Change",
                nextOil > 0 ? nextOil + " km" : "\u2014", CustomerDashboard.ACCENT_YELLOW));
        summary.add(main.miniCard("Next Major Service",
                nextMajor > 0 ? nextMajor + " km" : "\u2014", UIUtils.ERROR_RED));

        JPanel centre = new JPanel(new BorderLayout(0, 8));
        centre.setBackground(UIUtils.DARK_NAVY);
        centre.add(scroll, BorderLayout.CENTER);
        centre.add(summary, BorderLayout.SOUTH);

        panel.add(centre, BorderLayout.CENTER);
        return panel;
    }

    // ── Apt button renderer ──────────────────────────────────────────────────
    private class AptButtonRenderer extends JButton implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        public AptButtonRenderer() {
            setOpaque(true);
        }
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String aptId = (String) value;
            setText("\uD83D\uDCCB");
            setToolTipText("View Service History: " + aptId);
            setEnabled(true);
            setBackground(isSelected ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
            setForeground(UIUtils.WHITE);
            return this;
        }
    }

    // ── Apt button editor ────────────────────────────────────────────────────
    private class AptButtonEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;
        private JButton button;
        private String currentAptId;

        public AptButtonEditor() {
            super(new JCheckBox());
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> {
                if (currentAptId != null && !currentAptId.isEmpty()) {
                    main.showHistory(currentAptId);
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentAptId = (String) value;
            button.setText("\uD83D\uDCCB");
            button.setEnabled(true);
            button.setBackground(UIUtils.TABLE_BG);
            button.setForeground(UIUtils.WHITE);
            return button;
        }

        public Object getCellEditorValue() {
            return currentAptId;
        }
    }
}
