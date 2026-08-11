package asc.gui.customer;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class CustomerHistoryPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final CustomerDashboard main;
    private final String highlightAptId;

    public CustomerHistoryPage(CustomerDashboard m) {
        this(m, null);
    }

    public CustomerHistoryPage(CustomerDashboard m, String highlightAptId) {
        main = m;
        this.highlightAptId = highlightAptId;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY); panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.add(main.sectionTitle("Service & Payment History", CustomerDashboard.ICON_BASE + "Service History(Blue).png"), BorderLayout.NORTH);
        List<Appointment>    myApts = main.currentCustomer.getMyAppointments();
        Map<String, Payment> payMap = main.currentCustomer.getMyPaymentMap();
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); filterBar.setBackground(UIUtils.DARK_NAVY);
        JLabel fl = new JLabel("Filter:"); fl.setForeground(UIUtils.WHITE); fl.setFont(UIUtils.uiFont(Font.BOLD, 12));
        JComboBox<String> statusCb = new JComboBox<>(new String[]{"All Status","SCHEDULED","AWAITING_PAYMENT","COMPLETED","MISSED"});
        JComboBox<String> typeCb   = new JComboBox<>(new String[]{"All Types","NORMAL","MAJOR"});
        UIUtils.styleCombo(statusCb); UIUtils.styleCombo(typeCb);
        JTextField searchField = new JTextField(12); UIUtils.styleTextField(searchField);
        JLabel searchLbl = new JLabel("\uD83D\uDD0D"); searchLbl.setForeground(UIUtils.ACCENT_BLUE); searchLbl.setFont(UIUtils.uiSymbolFont(Font.PLAIN, 14));
        JButton applyBtn = main.makeBtn("Apply", UIUtils.ACCENT_BLUE, 88, 28);
        JButton resetBtn = main.makeBtn("Reset", UIUtils.MEDIUM_NAVY, 88, 28);
        filterBar.add(fl); filterBar.add(statusCb); filterBar.add(typeCb); filterBar.add(searchLbl); filterBar.add(searchField); filterBar.add(applyBtn); filterBar.add(resetBtn);
        String[] cols = {"Apt ID","Date","Time","Type","Status","Car","Amount (RM)","Pay Method","Details","Health Record"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return c == 8 || c == 9; } };
        JTable table = UIUtils.createStyledTable(model);
        table.setRowHeight(30); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {80, 105, 70, 130, 130, 150, 120, 135, 80, 90};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String s = v == null ? "" : v.toString();
                setBackground(sel ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
                setFont(UIUtils.uiFont(Font.BOLD, 12)); setHorizontalAlignment(CENTER);
                if ("COMPLETED".equalsIgnoreCase(s)) {
                    setText("\u2705 " + s); setForeground(CustomerDashboard.GREEN_OK);
                } else if ("MISSED".equalsIgnoreCase(s)) {
                    setText("\u274C " + s); setForeground(UIUtils.ERROR_RED);
                } else if ("AWAITING_PAYMENT".equalsIgnoreCase(s)) {
                    setText("\uD83D\uDCB0 " + s); setForeground(CustomerDashboard.PURPLE);
                } else {
                    setText("\u23F3 " + s); setForeground(CustomerDashboard.ACCENT_YELLOW);
                }
                return this;
            }
        });

        // ── Details column: button renderer + editor ─────────────────────────
        table.getColumnModel().getColumn(8).setCellRenderer(new DetailsButtonRenderer());
        table.getColumnModel().getColumn(8).setCellEditor(new DetailsButtonEditor());

        // ── Health Record column: button renderer + editor ────────────────────
        table.getColumnModel().getColumn(9).setCellRenderer(new HealthButtonRenderer());
        table.getColumnModel().getColumn(9).setCellEditor(new HealthButtonEditor());

        Runnable fill = () -> {
            model.setRowCount(0);
            String sf = (String) statusCb.getSelectedItem(), tf = (String) typeCb.getSelectedItem();
            String kw = searchField.getText().trim().toLowerCase();
            int highlightRow = -1;
            for (int i = 0; i < myApts.size(); i++) {
                Appointment a = myApts.get(i);
                if (!"All Status".equals(sf) && !sf.equalsIgnoreCase(a.getStatus())) continue;
                if (!"All Types".equals(tf)  && !tf.equalsIgnoreCase(a.getServiceType())) continue;
                if (!kw.isEmpty() && !a.getAppointmentId().toLowerCase().contains(kw) && !a.getVehicleInfo().toLowerCase().contains(kw)) continue;
                Payment pay = payMap.get(a.getAppointmentId());
                VehicleHealthRecord vhr = FileManager.findHealthRecordByAppointmentId(a.getAppointmentId());
                model.addRow(new Object[]{ a.getAppointmentId(), a.getDate(), a.getTime(),
                    "NORMAL".equalsIgnoreCase(a.getServiceType()) ? "Normal (1hr)" : "Major (3hr)",
                    a.getStatus(), a.getVehicleInfo(),
                    pay != null ? String.format("RM %.2f", pay.getAmount()) : "\u2013",
                    pay != null ? pay.getPaymentMethod() : "\u2013",
                    a,    // Details column: pass the appointment object
                    vhr });
                if (highlightAptId != null && highlightAptId.equals(a.getAppointmentId()))
                    highlightRow = model.getRowCount() - 1;
            }
            // Scroll to highlighted row if any
            if (highlightRow >= 0) {
                final int row = highlightRow;
                SwingUtilities.invokeLater(() -> {
                    table.setRowSelectionInterval(row, row);
                    table.scrollRectToVisible(table.getCellRect(row, 0, true));
                });
            }
        };
        fill.run();
        applyBtn.addActionListener(e -> fill.run());
        resetBtn.addActionListener(e -> { statusCb.setSelectedIndex(0); typeCb.setSelectedIndex(0); searchField.setText(""); fill.run(); });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { fill.run(); }
            public void removeUpdate(DocumentEvent e) { fill.run(); }
            public void changedUpdate(DocumentEvent e) { fill.run(); }
        });
        JScrollPane scroll = new JScrollPane(table); UIUtils.styleScrollPane(scroll);
        long completed2 = main.currentCustomer.getCompletedCount();
        double totalPaid = payMap.values().stream().mapToDouble(Payment::getAmount).sum();
        JPanel summary = new JPanel(new GridLayout(1, 4, 10, 0));
        summary.setBackground(UIUtils.DARK_NAVY); summary.setBorder(new EmptyBorder(10, 0, 0, 0));
        summary.add(main.miniCard("Total Appts", String.valueOf(myApts.size()), UIUtils.ACCENT_BLUE));
        summary.add(main.miniCard("Completed",   String.valueOf(completed2),    CustomerDashboard.GREEN_OK));
        summary.add(main.miniCard("Pending",     String.valueOf(myApts.size()-completed2), CustomerDashboard.ACCENT_YELLOW));
        summary.add(main.miniCard("Total Spent", String.format("RM %.2f", totalPaid), CustomerDashboard.PURPLE));
        JPanel centre = new JPanel(new BorderLayout(0, 8)); centre.setBackground(UIUtils.DARK_NAVY);
        centre.add(filterBar, BorderLayout.NORTH); centre.add(scroll, BorderLayout.CENTER); centre.add(summary, BorderLayout.SOUTH);
        panel.add(centre, BorderLayout.CENTER);
        return panel;
    }

    // ── Health button renderer ───────────────────────────────────────────────
    private class HealthButtonRenderer extends JButton implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        public HealthButtonRenderer() {
            setOpaque(true);
        }
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            VehicleHealthRecord vhr = (VehicleHealthRecord) value;
            if (vhr != null) {
                setText("\uD83C\uDFE5");
                setToolTipText("View Health Record: " + vhr.getRecordId());
                setEnabled(true);
                setBackground(isSelected ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
                setForeground(UIUtils.WHITE);
            } else {
                setText("\u2013");
                setToolTipText("No health record for this appointment");
                setEnabled(false);
                setBackground(isSelected ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
                setForeground(Color.GRAY);
            }
            return this;
        }
    }

    // ── Details button renderer ──────────────────────────────────────────────
    private class DetailsButtonRenderer extends JButton implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        public DetailsButtonRenderer() {
            setOpaque(true);
        }
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("\uD83D\uDCC4");
            setToolTipText("View appointment details");
            setEnabled(true);
            setBackground(isSelected ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
            setForeground(UIUtils.WHITE);
            return this;
        }
    }

    // ── Details button editor ────────────────────────────────────────────────
    private class DetailsButtonEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;
        private JButton button;
        private Appointment currentApt;

        public DetailsButtonEditor() {
            super(new JCheckBox());
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> {
                if (currentApt != null) {
                    showAppointmentDetailDialog(currentApt);
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentApt = (Appointment) value;
            button.setText("\uD83D\uDCC4");
            button.setEnabled(true);
            button.setBackground(UIUtils.TABLE_BG);
            button.setForeground(UIUtils.WHITE);
            return button;
        }

        public Object getCellEditorValue() {
            return currentApt;
        }
    }

    private void showAppointmentDetailDialog(Appointment a) {
        Payment pay = main.currentCustomer.getMyPaymentMap().get(a.getAppointmentId());
        JDialog dlg = new JDialog((Dialog) null, "Appointment Detail — " + a.getAppointmentId(), true);
        dlg.setSize(500, 480);
        dlg.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dlg.setResizable(false);

        JPanel outer = new JPanel(new BorderLayout(0, 14));
        outer.setBackground(UIUtils.DARK_NAVY);
        outer.setBorder(new EmptyBorder(18, 22, 18, 22));

        // info card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UIUtils.TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(14, 18, 14, 18)));

        String[] labels = {"Appointment ID", "Date & Time", "Service Type", "Status",
                           "Vehicle", "Payment", "Description"};
        boolean done = "COMPLETED".equalsIgnoreCase(a.getStatus());
        String statusStr = (done ? "\u2705 " : "\u23F3 ") + a.getStatus();
        String payStr = pay != null
                ? String.format("RM %.2f — %s", pay.getAmount(), pay.getPaymentMethod())
                : "\u2013";
        String descStr = a.getNotes() != null && !a.getNotes().trim().isEmpty()
                ? a.getNotes().trim() : "(No description)";

        String[] values = {a.getAppointmentId(), a.getDate() + "  " + a.getTime(),
                           a.getServiceType(), statusStr, a.getVehicleInfo(),
                           payStr, descStr};

        for (int i = 0; i < labels.length; i++) {
            JPanel row = new JPanel(new BorderLayout(0, 0));
            row.setBackground(UIUtils.TABLE_BG);
            row.setMaximumSize(new Dimension(460, i == 6 ? 120 : 28));

            JLabel lbl = new JLabel(labels[i] + ":");
            lbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
            lbl.setForeground(UIUtils.ACCENT_BLUE);
            lbl.setPreferredSize(new Dimension(120, 24));

            JLabel val = new JLabel("<html>" + values[i].replace("\n", "<br>") + "</html>");
            val.setFont(i == 6 ? UIUtils.uiFont(Font.PLAIN, 11) : UIUtils.uiFont(Font.PLAIN, 12));
            val.setForeground(UIUtils.WHITE);
            val.setVerticalAlignment(SwingConstants.TOP);

            row.add(lbl, BorderLayout.WEST);
            row.add(val, BorderLayout.CENTER);
            card.add(row);
            if (i < labels.length - 1) card.add(Box.createVerticalStrut(4));
        }

        outer.add(card, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
        closeBtn.setForeground(UIUtils.WHITE);
        closeBtn.setBackground(UIUtils.ACCENT_BLUE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 8, 1));
        closeBtn.setPreferredSize(new Dimension(100, 32));
        closeBtn.addActionListener(e -> dlg.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(UIUtils.DARK_NAVY);
        btnPanel.add(closeBtn);
        outer.add(btnPanel, BorderLayout.SOUTH);

        dlg.setContentPane(outer);
        dlg.setVisible(true);
    }

    // ── Health Record button editor ─────────────────────────────────────────
    private class HealthButtonEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;
        private JButton button;
        private VehicleHealthRecord currentVhr;

        public HealthButtonEditor() {
            super(new JCheckBox());
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> {
                if (currentVhr != null) {
                    main.showVehicleHealth(currentVhr.getRecordId());
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentVhr = (VehicleHealthRecord) value;
            if (currentVhr != null) {
                button.setText("\uD83C\uDFE5");
                button.setEnabled(true);
                button.setBackground(UIUtils.TABLE_BG);
                button.setForeground(UIUtils.WHITE);
            } else {
                button.setText("\u2013");
                button.setEnabled(false);
                button.setBackground(UIUtils.TABLE_BG);
                button.setForeground(Color.GRAY);
            }
            return button;
        }

        public Object getCellEditorValue() {
            return currentVhr;
        }
    }
}
