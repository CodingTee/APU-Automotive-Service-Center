package asc.gui.technician;

import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class TechnicianInventoryPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final TechnicianDashboard main;

    public TechnicianInventoryPage(TechnicianDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(8, 12, 6, 12));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(UIUtils.DARK_NAVY);
        panel.setBorder(new EmptyBorder(8, 10, 4, 10));
        panel.add(main.sectionTitle("Parts Inventory", TechnicianDashboard.ICON_BASE + "Parts Inventory(Blue).png"), BorderLayout.NORTH);

        JPanel infoBanner = new JPanel(new BorderLayout());
        infoBanner.setBackground(new Color(30, 55, 90));
        infoBanner.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1),
                new EmptyBorder(6, 14, 6, 14)));
        JLabel infoLbl = new JLabel(
                "ℹ  Read-only view. Stock is automatically deducted when you mark appointments as Completed. Contact your manager to restock.");
        infoLbl.setForeground(new Color(160, 200, 255));
        infoLbl.setFont(UIUtils.uiFont(Font.ITALIC, 11));
        infoBanner.add(infoLbl, BorderLayout.CENTER);

        String[] cols = {"Part ID", "Name", "Category", "In Stock", "Threshold", "Status", "Price (RM)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIUtils.createStyledTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowHeight(28);

        table.getColumnModel().getColumn(5).setCellRenderer(
            new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v,
                        boolean sel, boolean foc, int row, int col) {
                    super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                    String s = v == null ? "" : v.toString();
                    boolean out = s.startsWith("OUT");
                    boolean low = s.startsWith("LOW");
                    if (out) {
                        setText("\u274C " + s);
                        setForeground(new Color(255, 80, 80));
                    } else if (low) {
                        setText("\u26A0 " + s);
                        setForeground(TechnicianDashboard.ACCENT_YELLOW);
                    } else {
                        setText("\u2705 " + s);
                        setForeground(TechnicianDashboard.GREEN_OK);
                    }
                    setBackground(sel ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
                    setFont(UIUtils.uiSymbolFont(Font.BOLD, 11));
                    setHorizontalAlignment(CENTER);
                    return this;
                }
            });

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        Runnable refresh = () -> {
            model.setRowCount(0);
            for (asc.model.CarPart p : FileManager.readAllCarParts()) {
                int qty = p.getStockQuantity();
                String status;
                if (qty == 0)
                    status = "OUT OF STOCK";
                else if (qty <= p.getLowStockThreshold())
                    status = "LOW STOCK (" + qty + ")";
                else
                    status = "In Stock";
                model.addRow(new Object[]{
                    p.getPartId(), p.getName(), p.getCategory(),
                    p.getStockQuantity(), p.getLowStockThreshold(),
                    status,
                    String.format("%.2f", p.getPrice())
                });
            }
        };
        refresh.run();

        JScrollPane scroll = new JScrollPane(table);
        UIUtils.styleScrollPane(scroll);

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setForeground(UIUtils.LIGHT_GRAY);
        searchLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
        JTextField searchField = new JTextField(16);
        UIUtils.styleTextField(searchField);
        searchField.setToolTipText("Search by part name, ID or category");

        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setForeground(UIUtils.LIGHT_GRAY);
        statusLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All", "Out of Stock", "Low Stock", "In Stock"});
        UIUtils.styleCombo(statusFilter);

        UIUtils.RoundedButton clearBtn = new UIUtils.RoundedButton("Clear", new Color(180, 60, 60));
        clearBtn.setPreferredSize(new Dimension(80, 30));
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            sorter.setRowFilter(null);
        });

        Runnable applyFilter = () -> {
            String kw = searchField.getText().trim();
            String sf = (String) statusFilter.getSelectedItem();
            java.util.List<RowFilter<Object,Object>> filters = new ArrayList<>();
            if (!kw.isEmpty()) {
                try {
                    java.util.List<RowFilter<Object,Object>> or = new ArrayList<>();
                    or.add(RowFilter.regexFilter("(?i)" + kw, 0));
                    or.add(RowFilter.regexFilter("(?i)" + kw, 1));
                    or.add(RowFilter.regexFilter("(?i)" + kw, 2));
                    filters.add(RowFilter.orFilter(or));
                } catch (java.util.regex.PatternSyntaxException ignored) {}
            }
            if ("Out of Stock".equals(sf))
                filters.add(RowFilter.regexFilter("(?i)^OUT", 5));
            else if ("Low Stock".equals(sf))
                filters.add(RowFilter.regexFilter("(?i)^LOW", 5));
            else if ("In Stock".equals(sf))
                filters.add(RowFilter.regexFilter("(?i)^In Stock$", 5));
            sorter.setRowFilter(filters.isEmpty() ? null
                    : filters.size() == 1 ? filters.get(0)
                    : RowFilter.andFilter(filters));
        };

        statusFilter.addActionListener(e -> applyFilter.run());
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { applyFilter.run(); }
        });

        UIUtils.RoundedButton outStockBtn = new UIUtils.RoundedButton("View Out of Stock", new Color(200, 50, 50));
        UIUtils.RoundedButton alertBtn    = new UIUtils.RoundedButton("View Low Stock",     new Color(200, 120, 40));
        UIUtils.RoundedButton refreshBtn  = new UIUtils.RoundedButton("Refresh",            new Color(60, 90, 150));
        outStockBtn.setPreferredSize(new Dimension(145, 30));
        alertBtn   .setPreferredSize(new Dimension(130, 30));
        refreshBtn .setPreferredSize(new Dimension(80,  30));

        outStockBtn.addActionListener(e -> {
            java.util.List<asc.model.CarPart> allP = FileManager.readAllCarParts();
            java.util.List<asc.model.CarPart> outParts = allP.stream()
                    .filter(p -> p.getStockQuantity() == 0)
                    .collect(java.util.stream.Collectors.toList());
            if (outParts.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No parts are currently out of stock.",
                        "Out of Stock", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Out of Stock:\n\n");
            for (asc.model.CarPart p : outParts)
                sb.append("  \u274C  ").append(p.getName())
                  .append("  (Part ID: ").append(p.getPartId()).append(")\n");
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
            ta.setBackground(new Color(50, 20, 20));
            ta.setForeground(new Color(255, 100, 100));
            ta.setBorder(new EmptyBorder(10, 14, 10, 14));
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(460, 220));
            UIUtils.styleScrollPane(sp);
            JOptionPane.showMessageDialog(this, sp,
                    "\u274C  Out of Stock", JOptionPane.ERROR_MESSAGE);
        });

        alertBtn.addActionListener(e -> {
            java.util.List<asc.model.CarPart> allP = FileManager.readAllCarParts();
            java.util.List<asc.model.CarPart> lowParts = allP.stream()
                    .filter(p -> p.getStockQuantity() > 0 && p.isLowStock())
                    .collect(java.util.stream.Collectors.toList());
            if (lowParts.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No parts are currently low on stock.",
                        "Low Stock", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Low Stock:\n\n");
            for (asc.model.CarPart p : lowParts)
                sb.append("  \u26A0  ").append(p.getName())
                  .append("  (Qty: ").append(p.getStockQuantity())
                  .append(", Threshold: ").append(p.getLowStockThreshold()).append(")\n");
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
            ta.setBackground(new Color(50, 35, 20));
            ta.setForeground(new Color(255, 180, 60));
            ta.setBorder(new EmptyBorder(10, 14, 10, 14));
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(460, 220));
            UIUtils.styleScrollPane(sp);
            JOptionPane.showMessageDialog(this, sp,
                    "\u26A0  Low Stock Alert", JOptionPane.WARNING_MESSAGE);
        });

        refreshBtn.addActionListener(e -> {
            refresh.run();
            sorter.setRowFilter(null);
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
        });

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filterRow.setBackground(UIUtils.DARK_NAVY);
        filterRow.add(searchLbl); filterRow.add(searchField);
        filterRow.add(statusLbl); filterRow.add(statusFilter);
        filterRow.add(clearBtn);
        filterRow.add(Box.createHorizontalStrut(24));
        filterRow.add(outStockBtn); filterRow.add(alertBtn); filterRow.add(refreshBtn);

        java.util.List<asc.model.CarPart> allParts = FileManager.readAllCarParts();
        long outCount = allParts.stream().filter(p -> p.getStockQuantity() == 0).count();
        long lowCount = allParts.stream().filter(p -> p.getStockQuantity() > 0 && p.isLowStock()).count();
        long inCount  = allParts.size() - outCount - lowCount;
        JPanel summary = new JPanel(new GridLayout(1, 4, 10, 0));
        summary.setOpaque(false);
        summary.setBorder(new EmptyBorder(4, 0, 0, 0));
        summary.add(main.miniCard("Total Parts",   String.valueOf(allParts.size()), UIUtils.ACCENT_BLUE));
        summary.add(main.miniCard("Out of Stock",  String.valueOf(outCount),         new Color(255, 80, 80)));
        summary.add(main.miniCard("Low Stock",     String.valueOf(lowCount),         TechnicianDashboard.ACCENT_YELLOW));
        summary.add(main.miniCard("In Stock",      String.valueOf(inCount),          TechnicianDashboard.GREEN_OK));

        JPanel topSection = new JPanel(new BorderLayout(0, 4));
        topSection.setOpaque(false);
        topSection.add(infoBanner, BorderLayout.NORTH);
        topSection.add(filterRow,  BorderLayout.SOUTH);

        JPanel centre = new JPanel(new BorderLayout(0, 4));
        centre.setBackground(UIUtils.DARK_NAVY);
        centre.add(topSection, BorderLayout.NORTH);
        centre.add(scroll,     BorderLayout.CENTER);
        centre.add(summary,    BorderLayout.SOUTH);

        panel.add(centre, BorderLayout.CENTER);
        return panel;
    }


}
