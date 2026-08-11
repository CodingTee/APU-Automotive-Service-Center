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

public class TechnicianAppointmentPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final TechnicianDashboard main;

    public TechnicianAppointmentPage(TechnicianDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(2, 14, 6, 14));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(UIUtils.DARK_NAVY);
        panel.setBorder(new EmptyBorder(2, 14, 4, 14));
        panel.add(main.sectionTitle("My Appointments", TechnicianDashboard.ICON_BASE + "My Appointments(Blue).png"), BorderLayout.NORTH);

        // ── Table ────────────────────────────────────────────────────────────
        main.appointmentsModel = new DefaultTableModel(
                new String[]{"Apt ID","Customer","Service Type","Status","Date","Time","Vehicle","Notes","Feedback"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        main.appointmentsTable = UIUtils.createStyledTable(main.appointmentsModel);
        main.appointmentsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] widths = {70, 100, 100, 115, 80, 50, 165, 320, 75};
        for (int i = 0; i < widths.length; i++)
            main.appointmentsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // ── Feature 2: Sort columns by clicking header ────────────────────────
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(main.appointmentsModel);
        main.appointmentsTable.setRowSorter(sorter);

        // ── Feature 5: Double-click row to view full details ─────────────────
        main.appointmentsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = main.appointmentsTable.getSelectedRow();
                    if (row >= 0) main.viewAppointmentDetails();
                }
            }
        });

        // ── Feature 5: Color-coded rows by status ────────────────────────────
        main.appointmentsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);

                int modelRow = t.convertRowIndexToModel(row);
                String status = (String) t.getModel().getValueAt(modelRow, 3);
                boolean done    = Appointment.STATUS_COMPLETED.equalsIgnoreCase(status);
                boolean waiting = Appointment.STATUS_AWAITING_PAYMENT.equalsIgnoreCase(status);
                boolean missed  = Appointment.STATUS_MISSED.equalsIgnoreCase(status);

                if (sel) {
                    setBackground(UIUtils.ACCENT_BLUE);
                } else if (done) {
                    setBackground(new Color(25, 60, 40));   // green tint
                } else if (waiting) {
                    setBackground(new Color(50, 40, 10));   // amber tint
                } else if (missed) {
                    setBackground(new Color(60, 20, 20));   // red tint
                } else {
                    setBackground(new Color(45, 30, 60));   // purple tint (scheduled)
                }
                setForeground(UIUtils.WHITE);
                setFont(UIUtils.uiFont(Font.PLAIN, 12));
                setBorder(new EmptyBorder(0, 6, 0, 6));

                // Status column special formatting
                if (col == 3) {
                    if (done) {
                        setText("✅ " + v);
                        setForeground(TechnicianDashboard.GREEN_OK);
                    } else if (waiting) {
                        setText("💳 " + v);
                        setForeground(TechnicianDashboard.ACCENT_YELLOW);
                    } else if (missed) {
                        setText("❌ " + v);
                        setForeground(UIUtils.ERROR_RED);
                    } else {
                        setText("⏳ " + v);
                        setForeground(UIUtils.LIGHT_GRAY);
                    }
                    setFont(UIUtils.uiFont(Font.BOLD, 12));
                    setHorizontalAlignment(CENTER);
                } else {
                    setHorizontalAlignment(LEFT);
                }
                return this;
            }
        });

        main.refreshAppointmentsTable();

        JScrollPane scroll = new JScrollPane(main.appointmentsTable);
        UIUtils.styleScrollPane(scroll);

        // ── Feature 1: Search / Filter bar ──────────────────────────────────
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setForeground(UIUtils.LIGHT_GRAY);
        searchLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));

        JTextField searchField = new JTextField(16);
        UIUtils.styleTextField(searchField);
        searchField.setToolTipText("Search by Appt ID, Customer, Date or Service");

        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setForeground(UIUtils.LIGHT_GRAY);
        statusLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));

        String[] statusOpts = {"All", "SCHEDULED", "COMPLETED"};
        JComboBox<String> statusFilter = new JComboBox<>(statusOpts);
        UIUtils.styleCombo(statusFilter);

        UIUtils.RoundedButton searchBtn = new UIUtils.RoundedButton("🔍 Search", new Color(70, 130, 200));
        UIUtils.RoundedButton clearBtn  = new UIUtils.RoundedButton("✖ Clear",   new Color(180, 60, 60));
        searchBtn.setPreferredSize(new Dimension(110, 30));
        clearBtn .setPreferredSize(new Dimension(90,  30));

        searchBtn.addActionListener(e ->
                main.filterAppointmentsTable(searchField.getText().trim(),
                        (String) statusFilter.getSelectedItem(), sorter));
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            sorter.setRowFilter(null); // clear filter
            main.refreshAppointmentsTable();
        });

        // Also filter live as user types
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                main.filterAppointmentsTable(searchField.getText().trim(),
                        (String) statusFilter.getSelectedItem(), sorter);
            }
        });

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filterRow.setBackground(UIUtils.DARK_NAVY);
        filterRow.add(searchLbl);
        filterRow.add(searchField);
        filterRow.add(statusLbl);
        filterRow.add(statusFilter);
        filterRow.add(searchBtn);
        filterRow.add(clearBtn);

        // ── Stats summary cards ──────────────────────────────────────────────
        List<Appointment> myApts = FileManager.readAllAppointments().stream()
                .filter(a -> a.getTechnicianId().equals(main.currentTech.getUserId()))
                .collect(java.util.stream.Collectors.toList());
        long comp = myApts.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();

        JPanel summary = new JPanel(new GridLayout(1, 3, 6, 0));
        summary.setBackground(UIUtils.DARK_NAVY);
        summary.setBorder(new EmptyBorder(2, 0, 0, 0));
        summary.add(main.miniCard("Total",     String.valueOf(myApts.size()),        UIUtils.ACCENT_BLUE));
        summary.add(main.miniCard("Completed", String.valueOf(comp),                 TechnicianDashboard.GREEN_OK));
        summary.add(main.miniCard("Pending",   String.valueOf(myApts.size() - comp), TechnicianDashboard.ACCENT_YELLOW));

        // ── Action buttons ───────────────────────────────────────────────────
        UIUtils.RoundedButton viewBtn  = new UIUtils.RoundedButton("View Details",      new Color(140, 135, 130));
        UIUtils.RoundedButton complBtn = new UIUtils.RoundedButton("Mark as Completed", UIUtils.SUCCESS_GREEN);
        UIUtils.RoundedButton fbBtn    = new UIUtils.RoundedButton("Provide Feedback",  UIUtils.ACCENT_BLUE);
        UIUtils.RoundedButton refBtn   = new UIUtils.RoundedButton("Refresh",           new Color(60, 90, 150));
        for (JButton b : new JButton[]{viewBtn, complBtn, fbBtn, refBtn})
            b.setPreferredSize(new Dimension(178, 34));

        viewBtn .addActionListener(e -> main.viewAppointmentDetails());
        complBtn.addActionListener(e -> main.markCompleted());
        fbBtn   .addActionListener(e -> showProvideFeedbackDialog());
        refBtn  .addActionListener(e -> main.showAppointments());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setBackground(UIUtils.DARK_NAVY);
        btnRow.add(viewBtn); btnRow.add(complBtn); btnRow.add(fbBtn); btnRow.add(refBtn);

        // ── Centre: filter + table ───────────────────────────────────────────
        JPanel centre = new JPanel(new BorderLayout(0, 2));
        centre.setBackground(UIUtils.DARK_NAVY);
        centre.add(filterRow, BorderLayout.NORTH);
        centre.add(scroll,    BorderLayout.CENTER);
        centre.add(summary,   BorderLayout.SOUTH);

        // ── South: buttons ───────────────────────────────────────────────────
        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(UIUtils.DARK_NAVY);
        south.setBorder(new EmptyBorder(12, 0, 0, 0));
        south.add(btnRow, BorderLayout.CENTER);

        panel.add(centre, BorderLayout.CENTER);
        panel.add(south,  BorderLayout.SOUTH);
        return panel;
    }

    void showProvideFeedbackDialog() {
        int row = main.appointmentsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this,"Select an appointment.","No Selection",JOptionPane.WARNING_MESSAGE); return; }
        String aptId  = (String) main.appointmentsModel.getValueAt(row, 0);
        String status = (String) main.appointmentsModel.getValueAt(row, 3);
        if (!"COMPLETED".equalsIgnoreCase(status)) { JOptionPane.showMessageDialog(this,"Only for COMPLETED appointments.","Not Allowed",JOptionPane.WARNING_MESSAGE); return; }
        Feedback    existing = FileManager.findFeedbackByAppointmentId(aptId);
        Appointment apt      = FileManager.findAppointmentById(aptId);
        Customer    c        = (apt != null) ? apt.getCustomer() : null;

        JDialog d = new JDialog((Dialog) null, "Provide Feedback", true);
        d.setSize(520, 380); d.setLocationRelativeTo(null);
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(UIUtils.DARK_NAVY); p.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel info = new JPanel(new GridLayout(3, 1, 3, 3));
        info.setBackground(UIUtils.TABLE_BG);
        info.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1), new EmptyBorder(8, 12, 8, 12)));
        JLabel al = new JLabel("Appointment: " + aptId); al.setForeground(UIUtils.WHITE); al.setFont(UIUtils.uiFont(Font.BOLD, 12));
        JLabel cl = new JLabel("Customer: "    + (c != null ? c.getName() : (apt != null ? apt.getCustomerId() : "-"))); cl.setForeground(UIUtils.LIGHT_GRAY); cl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        JLabel sl = new JLabel("Service: "     + (apt != null ? apt.getServiceType() + " on " + apt.getDate() : "-")); sl.setForeground(UIUtils.LIGHT_GRAY); sl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        info.add(al); info.add(cl); info.add(sl);

        JLabel lbl = new JLabel(existing != null ? "Edit Your Feedback:" : "Write Your Feedback:");
        lbl.setForeground(UIUtils.WHITE); lbl.setFont(UIUtils.uiFont(Font.BOLD, 13));

        JTextArea fbArea = new JTextArea(6, 38);
        fbArea.setFont(UIUtils.uiFont(Font.PLAIN, 13));
        fbArea.setBackground(new Color(40, 62, 110)); fbArea.setForeground(UIUtils.WHITE);
        fbArea.setCaretColor(UIUtils.WHITE); fbArea.setLineWrap(true); fbArea.setWrapStyleWord(true);
        fbArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        if (existing != null) fbArea.setText(existing.getContent());

        JScrollPane scroll = new JScrollPane(fbArea); UIUtils.styleScrollPane(scroll);

        UIUtils.RoundedButton save = new UIUtils.RoundedButton(existing != null ? "Update Feedback" : "Submit Feedback", UIUtils.SUCCESS_GREEN);
        save.setPreferredSize(new Dimension(200, 36));
        save.addActionListener(e -> {
            String content = fbArea.getText().trim();
            if (content.isEmpty()) { JOptionPane.showMessageDialog(d,"Feedback cannot be empty.","Error",JOptionPane.ERROR_MESSAGE); return; }
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (existing != null) { existing.setContent(content); existing.setFeedbackDate(today); FileManager.updateFeedback(existing);
                JOptionPane.showMessageDialog(d,"Feedback updated!","Success",JOptionPane.INFORMATION_MESSAGE); }
            else { FileManager.saveFeedback(new Feedback(FileManager.generateFeedbackId(), aptId, main.currentTech.getUserId(), content, today));
                JOptionPane.showMessageDialog(d,"Feedback submitted!","Success",JOptionPane.INFORMATION_MESSAGE); }
            main.refreshAppointmentsTable(); d.dispose();
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setBackground(UIUtils.DARK_NAVY); btnRow.add(save);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(UIUtils.DARK_NAVY);
        body.add(lbl, BorderLayout.NORTH); body.add(scroll, BorderLayout.CENTER); body.add(btnRow, BorderLayout.SOUTH);
        p.add(info, BorderLayout.NORTH); p.add(body, BorderLayout.CENTER);
        d.setContentPane(p); d.setVisible(true);
    }


}
