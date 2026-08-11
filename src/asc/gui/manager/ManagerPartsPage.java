package asc.gui.manager;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.util.List;

public class ManagerPartsPage extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String PART_IMG_DIR = "data/Picture/Manager/Product Images/";
    final ManagerDashboard main;

    public ManagerPartsPage(ManagerDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));
        panel.add(main.sectionTitle("Car Parts Catalogue", ManagerDashboard.MGR_ICON_BASE + "Car Parts Catalogue(Blue).png"), BorderLayout.NORTH);

        // Clean table: only essential columns visible on dashboard
        String[] cols = {"Part ID", "Part Name", "Category", "Price (RM)", "Qty"};
        DefaultTableModel dm = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIUtils.createStyledTable(dm);
        table.setRowHeight(32);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Column widths
        int[] widths = {70, 280, 130, 100, 120};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Qty column renderer — shows quantity or "Out of Stock"
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                int qty = (v instanceof Integer) ? (Integer) v : 0;
                if (qty > 0) {
                    setForeground(ManagerDashboard.GREEN_OK);
                    setText(qty + " in stock");
                } else {
                    setForeground(new Color(255, 100, 80));
                    setText("Out of Stock");
                }
                setBackground(sel ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
                setFont(UIUtils.uiFont(Font.BOLD, 12));
                setHorizontalAlignment(CENTER);
                return this;
            }
        });

        refreshPartsTable(dm);
        JScrollPane sp = new JScrollPane(table); UIUtils.styleScrollPane(sp);

        // Double-click row to open View Details
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showPartDetails(table, dm, () -> refreshPartsTable(dm));
            }
        });

        // Buttons
        UIUtils.RoundedButton addBtn     = new UIUtils.RoundedButton("Add Part",      UIUtils.SUCCESS_GREEN);
        UIUtils.RoundedButton detailBtn  = new UIUtils.RoundedButton("View Details", UIUtils.ACCENT_BLUE);
        UIUtils.RoundedButton delBtn     = new UIUtils.RoundedButton("Delete",         UIUtils.ERROR_RED);
        UIUtils.RoundedButton goodsInBtn  = new UIUtils.RoundedButton("Goods In",     new Color(100, 80, 200));
        UIUtils.RoundedButton goodsOutBtn = new UIUtils.RoundedButton("Goods Out",    new Color(220, 120, 40));
        UIUtils.RoundedButton refBtn      = new UIUtils.RoundedButton("Refresh",         UIUtils.MEDIUM_NAVY);

        addBtn  .setPreferredSize(new Dimension(130, 34));
        detailBtn.setPreferredSize(new Dimension(148, 34));
        delBtn  .setPreferredSize(new Dimension(110, 34));
        goodsInBtn.setPreferredSize(new Dimension(130, 34));
        goodsOutBtn.setPreferredSize(new Dimension(130, 34));
        refBtn  .setPreferredSize(new Dimension(110, 34));

        addBtn  .addActionListener(e -> { showAddPartDialog(); refreshPartsTable(dm); });
        detailBtn.addActionListener(e -> showPartDetails(table, dm, () -> refreshPartsTable(dm)));
        delBtn  .addActionListener(e -> { deletePartAction(table, dm); refreshPartsTable(dm); });
        goodsInBtn.addActionListener(e -> { showGoodsInDialog(); refreshPartsTable(dm); });
        goodsOutBtn.addActionListener(e -> { showGoodsOutDialog(); refreshPartsTable(dm); });
        refBtn  .addActionListener(e -> refreshPartsTable(dm));

        JLabel hint = new JLabel("Double-click a row to view full details & edit");
        hint.setFont(UIUtils.uiFont(Font.ITALIC, 11)); hint.setForeground(UIUtils.DIM_TEXT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnRow.setBackground(UIUtils.DARK_NAVY);
        btnRow.add(addBtn); btnRow.add(detailBtn); btnRow.add(delBtn); btnRow.add(goodsInBtn); btnRow.add(goodsOutBtn); btnRow.add(refBtn); btnRow.add(hint);

        panel.add(sp,     BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    void refreshPartsTable(DefaultTableModel dm) {
        dm.setRowCount(0);
        for (CarPart cp : FileManager.readAllCarParts()) {
            dm.addRow(new Object[]{
                cp.getPartId(),
                cp.getName(),
                cp.getCategory(),
                String.format("RM %.2f", cp.getPrice()),
                cp.getStockQuantity()
            });
        }
    }

    /** View Details dialog — shows all fields + image, with Edit button inside */
    void showPartDetails(JTable table, DefaultTableModel dm, Runnable onSaved) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a part to view."); return; }
        String partId = (String) dm.getValueAt(row, 0);
        CarPart cp = FileManager.findCarPartById(partId);
        if (cp == null) return;

        JDialog d = new JDialog((Dialog) null, "Part Details  \u2014  " + cp.getName(), true);
        d.setSize(620, 560); d.setLocationRelativeTo(this); d.setResizable(false);

        JPanel outer = new JPanel(new BorderLayout(0, 14));
        outer.setBackground(UIUtils.DARK_NAVY);
        outer.setBorder(new EmptyBorder(18, 22, 18, 22));

        // ── Image panel (left)
        JPanel imgPanel = new JPanel(new BorderLayout());
        imgPanel.setBackground(UIUtils.TABLE_BG);
        imgPanel.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(8, 8, 8, 8)));
        imgPanel.setPreferredSize(new Dimension(200, 200));

        JLabel imgLbl = new JLabel("", JLabel.CENTER);
        imgLbl.setForeground(UIUtils.DIM_TEXT);
        imgLbl.setFont(UIUtils.uiFont(Font.PLAIN, 42));
        main.loadPartImageInto(imgLbl, cp.getImageUrl(), 184, 180);

        UIUtils.RoundedButton changeImgBtn = new UIUtils.RoundedButton("Change Image", new Color(60, 100, 160));
        changeImgBtn.setFont(UIUtils.uiFont(Font.BOLD, 11));
        changeImgBtn.setPreferredSize(new Dimension(184, 30));
        changeImgBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(d) == JFileChooser.APPROVE_OPTION) {
                cp.setImageUrl(savePartImage(fc.getSelectedFile()));
                FileManager.updateCarPart(cp);
                main.loadPartImageInto(imgLbl, cp.getImageUrl(), 184, 180);
                imgLbl.repaint();
            }
        });
        imgPanel.add(imgLbl,        BorderLayout.CENTER);
        imgPanel.add(changeImgBtn,  BorderLayout.SOUTH);

        // ── Info panel (right)
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(UIUtils.TABLE_BG);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(16, 18, 16, 18)));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(5, 4, 5, 4); g.anchor = GridBagConstraints.WEST;

        main.detailRow(infoPanel, g, 0, "Part ID:",   cp.getPartId());
        main.detailRow(infoPanel, g, 1, "Category:",  cp.getCategory());
        main.detailRow(infoPanel, g, 2, "Price:",      String.format("RM %.2f", cp.getPrice()));
        main.detailRow(infoPanel, g, 3, "Quantity:",
                cp.getStockQuantity() + (cp.getStockQuantity() > 0 ? " in stock" : " (Out of Stock)"));
        main.detailRow(infoPanel, g, 4, "Visible:",    cp.isVisible() ? "Yes (shown to customers)" : "No (hidden)");
        main.detailRow(infoPanel, g, 5, "Added By:",   cp.getAddedBy());
        main.detailRow(infoPanel, g, 6, "Date Added:", cp.getAddedDate());

        // Description label
        g.gridy = 7; g.gridx = 0; g.gridwidth = 2; g.insets = new Insets(10, 4, 2, 4);
        JLabel descHdr = new JLabel("Description:");
        descHdr.setFont(UIUtils.uiFont(Font.BOLD, 12)); descHdr.setForeground(UIUtils.ACCENT_BLUE);
        infoPanel.add(descHdr, g);

        g.gridy = 8;
        JTextArea descArea = new JTextArea(cp.getDescription());
        descArea.setEditable(false); descArea.setLineWrap(true); descArea.setWrapStyleWord(true);
        descArea.setBackground(new Color(28, 46, 88)); descArea.setForeground(UIUtils.WHITE);
        descArea.setFont(UIUtils.uiFont(Font.PLAIN, 12)); descArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        descArea.setRows(4);
        JScrollPane dsp = new JScrollPane(descArea); dsp.setPreferredSize(new Dimension(340, 90));
        UIUtils.styleScrollPane(dsp); infoPanel.add(dsp, g);

        // ── Left + Right split
        JPanel splitRow = new JPanel(new BorderLayout(12, 0));
        splitRow.setBackground(UIUtils.DARK_NAVY);
        splitRow.add(imgPanel,  BorderLayout.WEST);
        splitRow.add(infoPanel, BorderLayout.CENTER);

        // ── Action buttons at bottom
        UIUtils.RoundedButton editBtn  = new UIUtils.RoundedButton("Edit This Part",   UIUtils.ACCENT_BLUE);
        UIUtils.RoundedButton closeBtn = new UIUtils.RoundedButton("Close", UIUtils.MEDIUM_NAVY);

        editBtn .setPreferredSize(new Dimension(160, 36));
        closeBtn.setPreferredSize(new Dimension(100, 36));

        editBtn.addActionListener(e -> {
            d.dispose();
            showEditPartDialog(cp, onSaved);
        });
        closeBtn.addActionListener(e -> d.dispose());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setBackground(UIUtils.DARK_NAVY);
        btnRow.add(editBtn); btnRow.add(closeBtn);

        outer.add(splitRow, BorderLayout.CENTER);
        outer.add(btnRow,   BorderLayout.SOUTH);
        d.setContentPane(outer); d.setVisible(true);
    }

    void showAddPartDialog() {
        JDialog d = new JDialog((Dialog) null, "Add Car Part", true);
        d.setSize(500, 480); d.setLocationRelativeTo(null);
        JPanel p = main.dlgPanel(); GridBagConstraints gbc = main.dlgGbc();

        JTextField nameF  = main.tf(), catF  = main.tf(), priceF = main.tf(), imageF = main.tf(), qtyF = main.tf();
        qtyF.setText("0");
        JTextArea  descA  = new JTextArea(4, 30);
        descA.setLineWrap(true); descA.setWrapStyleWord(true);
        descA.setBackground(new Color(40, 62, 110)); descA.setForeground(UIUtils.WHITE);
        descA.setCaretColor(UIUtils.WHITE); descA.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        descA.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane descSp = new JScrollPane(descA); UIUtils.styleScrollPane(descSp);

        UIUtils.RoundedButton browseBtn = new UIUtils.RoundedButton("Browse Image", new Color(60, 100, 160));
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images","png","jpg","jpeg"));
            if (fc.showOpenDialog(d) == JFileChooser.APPROVE_OPTION)
                imageF.setText(savePartImage(fc.getSelectedFile()));
        });

        main.addRow(p, gbc, 0, "Part Name:",   nameF);
        main.addRow(p, gbc, 1, "Category:",    catF);
        main.addRow(p, gbc, 2, "Price (RM):",  priceF);
        main.addRow(p, gbc, 3, "Initial Qty:", qtyF);
        main.addRow(p, gbc, 4, "Image Path:",  imageF);
        gbc.gridx=0; gbc.gridy=5; gbc.gridwidth=2; p.add(browseBtn, gbc);
        gbc.gridy=6;
        JLabel dLbl = new JLabel("Description:"); dLbl.setForeground(UIUtils.WHITE); dLbl.setFont(UIUtils.uiFont(Font.BOLD,12));
        p.add(dLbl, gbc);
        gbc.gridy=7; gbc.fill=GridBagConstraints.BOTH; gbc.weighty=1.0;
        p.add(descSp, gbc);
        gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weighty=0;

        UIUtils.RoundedButton save = new UIUtils.RoundedButton("Save Part", UIUtils.SUCCESS_GREEN);
        save.setPreferredSize(new Dimension(160, 34));
        save.addActionListener(e -> {
            String nm=nameF.getText().trim(), cat=catF.getText().trim(), img=imageF.getText().trim();
            String desc=descA.getText().trim();
            if (nm.isEmpty()||cat.isEmpty()) { main.err(d,"Name and category required."); return; }
            double price=0;
            try { price=Double.parseDouble(priceF.getText().trim()); } catch(Exception ex) { main.err(d,"Invalid price."); return; }
            int initQty=0;
            try { initQty=Integer.parseInt(qtyF.getText().trim()); } catch(Exception ex) { initQty=0; }
            if (initQty<0) initQty=0;
            CarPart cp = new CarPart(FileManager.generateCarPartId(), nm, cat, desc, price, img, true, true,
                    main.currentManager.getUserId(), LocalDate.now().toString(), initQty, 3);
            FileManager.saveCarPart(cp);
            JOptionPane.showMessageDialog(d,"Car part added!","Success",JOptionPane.INFORMATION_MESSAGE);
            d.dispose();
        });
        gbc.gridy=8; p.add(save, gbc);
        d.setContentPane(p); d.setVisible(true);
    }

    /** Edit dialog — opened from View Details dialog */
    void showEditPartDialog(CarPart cp, Runnable onSaved) {
        JDialog d = new JDialog((Dialog) null, "Edit Car Part  \u2014  " + cp.getName(), true);
        d.setSize(500, 420); d.setLocationRelativeTo(null);
        JPanel p = main.dlgPanel(); GridBagConstraints gbc = main.dlgGbc();

        JTextField nameF  = new JTextField(cp.getName());   UIUtils.styleTextField(nameF);
        JTextField catF   = new JTextField(cp.getCategory()); UIUtils.styleTextField(catF);
        JTextField priceF = new JTextField(String.format("%.2f", cp.getPrice())); UIUtils.styleTextField(priceF);
        JTextField imageF = new JTextField(cp.getImageUrl()); UIUtils.styleTextField(imageF);
        JTextArea  descA  = new JTextArea(cp.getDescription(), 4, 30);
        descA.setLineWrap(true); descA.setWrapStyleWord(true);
        descA.setBackground(new Color(40, 62, 110)); descA.setForeground(UIUtils.WHITE);
        descA.setCaretColor(UIUtils.WHITE); descA.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        descA.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane descSp = new JScrollPane(descA); UIUtils.styleScrollPane(descSp);

        UIUtils.RoundedButton browseBtn = new UIUtils.RoundedButton("Browse Image", new Color(60, 100, 160));
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images","png","jpg","jpeg"));
            if (fc.showOpenDialog(d) == JFileChooser.APPROVE_OPTION)
                imageF.setText(savePartImage(fc.getSelectedFile()));
        });

        main.addRow(p, gbc, 0, "Part Name:",  nameF);
        main.addRow(p, gbc, 1, "Category:",   catF);
        main.addRow(p, gbc, 2, "Price (RM):", priceF);
        main.addRow(p, gbc, 3, "Image Path:", imageF);
        gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2; p.add(browseBtn, gbc);
        gbc.gridy=5;
        JLabel dLbl = new JLabel("Description:"); dLbl.setForeground(UIUtils.WHITE); dLbl.setFont(UIUtils.uiFont(Font.BOLD,12));
        p.add(dLbl, gbc);
        gbc.gridy=6; gbc.fill=GridBagConstraints.BOTH; gbc.weighty=1.0;
        p.add(descSp, gbc);
        gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weighty=0;

        UIUtils.RoundedButton save = new UIUtils.RoundedButton("Update Part", UIUtils.ACCENT_BLUE);
        save.setPreferredSize(new Dimension(160, 34));
        save.addActionListener(e -> {
            String nm=nameF.getText().trim(), cat=catF.getText().trim();
            if (nm.isEmpty()||cat.isEmpty()) { main.err(d,"Name and category required."); return; }
            double price=0;
            try { price=Double.parseDouble(priceF.getText().trim()); } catch(Exception ex) { main.err(d,"Invalid price."); return; }
            cp.setName(nm); cp.setCategory(cat);
            cp.setDescription(descA.getText().trim());
            cp.setPrice(price); cp.setImageUrl(imageF.getText().trim());
            FileManager.updateCarPart(cp);
            if (onSaved != null) onSaved.run();
            JOptionPane.showMessageDialog(d,"Updated!","Success",JOptionPane.INFORMATION_MESSAGE);
            d.dispose();
        });
        gbc.gridy=7; p.add(save, gbc);
        d.setContentPane(p); d.setVisible(true);
    }

    /** Copy image into managed product-images directory and return relative path */
    private String savePartImage(java.io.File source) {
        java.io.File dir = new java.io.File(PART_IMG_DIR);
        if (!dir.exists()) dir.mkdirs();
        java.io.File target = new java.io.File(dir, source.getName());
        try {
            java.nio.file.Files.copy(source.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            // Generate 300x300 thumbnail for fast loading on Customer side
            BufferedImage orig = ImageIO.read(target);
            if (orig != null) {
                BufferedImage thumb = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = thumb.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(orig, 0, 0, 300, 300, null);
                g.dispose();
                String ext = source.getName().substring(source.getName().lastIndexOf('.') + 1);
                java.io.File thumbFile = new java.io.File(dir, UIUtils.thumbPath(source.getName()));
                ImageIO.write(thumb, ext, thumbFile);
            }
        } catch (Exception ex) {
            return source.getAbsolutePath(); // fallback
        }
        return PART_IMG_DIR + source.getName();
    }

    void deletePartAction(JTable table, DefaultTableModel dm) {
        int row=table.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(this,"Select a part to delete.");return;}
        String partId=(String)dm.getValueAt(row,0), name=(String)dm.getValueAt(row,1);
        if(JOptionPane.showConfirmDialog(this,"Delete part: "+name+"?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        List<CarPart> parts=FileManager.readAllCarParts();
        parts.removeIf(cp->cp.getPartId().equals(partId));
        java.io.File f=new java.io.File("data/car_parts.txt");
        try(java.io.PrintWriter pw=new java.io.PrintWriter(new java.io.FileWriter(f,false))){
            for(CarPart cp:parts) pw.println(cp.toFileString());
        }catch(java.io.IOException ignored){}
        JOptionPane.showMessageDialog(this,"Deleted.","Success",JOptionPane.INFORMATION_MESSAGE);
    }

    /** Goods In dialog — list all parts, enter qty to add for each one */
    void showGoodsInDialog() {
        List<CarPart> parts = FileManager.readAllCarParts();
        JDialog dlg = new JDialog((Dialog) null, "Goods In — Restock Parts", true);
        dlg.setSize(680, 520);
        dlg.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dlg.setResizable(false);

        JPanel outer = new JPanel(new BorderLayout(0, 14));
        outer.setBackground(UIUtils.DARK_NAVY);
        outer.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Table: Part Name, Current Qty, Add Qty (+/-)
        String[] colNames = {"Part ID", "Part Name", "Current Qty", "Add Qty"};
        DefaultTableModel dm = new DefaultTableModel(colNames, 0) {
            public boolean isCellEditable(int r, int c) { return c == 3; }
            public Class<?> getColumnClass(int c) { return c == 3 ? Integer.class : String.class; }
        };

        for (CarPart cp : parts) {
            dm.addRow(new Object[]{cp.getPartId(), cp.getName(), cp.getStockQuantity(), 0});
        }

        JTable table = UIUtils.createStyledTable(dm);
        table.setRowHeight(34);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] w = {70, 280, 100, 100};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        // Qty column — render current quantity with colour
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                int qty = (v instanceof Integer) ? (Integer) v : 0;
                setText(String.valueOf(qty));
                setForeground(qty > 0 ? ManagerDashboard.GREEN_OK : new Color(255, 100, 80));
                setFont(UIUtils.uiFont(Font.BOLD, 12));
                setHorizontalAlignment(CENTER);
                setBackground(sel ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
                return this;
            }
        });

        // Add Qty column — custom editor with +/- buttons and spinner
        table.getColumnModel().getColumn(3).setCellRenderer(new SpinnerRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new SpinnerEditor());

        JScrollPane sp = new JScrollPane(table);
        UIUtils.styleScrollPane(sp);
        sp.setPreferredSize(new Dimension(640, 360));

        // Buttons
        UIUtils.RoundedButton confirmBtn = new UIUtils.RoundedButton("Confirm Goods In", UIUtils.SUCCESS_GREEN);
        confirmBtn.setPreferredSize(new Dimension(180, 36));
        UIUtils.RoundedButton cancelBtn = new UIUtils.RoundedButton("Cancel", UIUtils.MEDIUM_NAVY);
        cancelBtn.setPreferredSize(new Dimension(100, 36));

        confirmBtn.addActionListener(e -> {
            try {
                boolean anyUpdated = false;
                java.util.List<String> restockedParts = new java.util.ArrayList<>();
                for (int i = 0; i < dm.getRowCount(); i++) {
                    Object val = dm.getValueAt(i, 3);
                    int addQty;
                    if (val instanceof Integer) {
                        addQty = (Integer) val;
                    } else if (val instanceof String) {
                        try { addQty = Integer.parseInt((String) val); }
                        catch (NumberFormatException nfe) { addQty = 0; }
                    } else {
                        addQty = 0;
                    }
                    if (addQty <= 0) continue;
                    String partId = (String) dm.getValueAt(i, 0);
                    CarPart cp = FileManager.findCarPartById(partId);
                    if (cp != null) {
                        boolean wasOut = (cp.getStockQuantity() == 0);
                        cp.setStockQuantity(cp.getStockQuantity() + addQty);
                        FileManager.updateCarPart(cp);
                        if (wasOut) restockedParts.add(cp.getName() + " (Qty: " + cp.getStockQuantity() + ")");
                        anyUpdated = true;
                    }
                }
                // ── Notification: managers notified of restocked parts ─────────
                if (!restockedParts.isEmpty()) {
                    for (asc.model.Manager mgr : FileManager.readAllManagers()) {
                        for (String p : restockedParts) {
                            FileManager.saveNotification(new asc.model.Notification(
                                    mgr.getUserId(), asc.model.Notification.PART_RESTOCKED,
                                    "Part Restocked", p + " has been restocked."));
                        }
                    }
                }
                if (anyUpdated) {
                    JOptionPane.showMessageDialog(dlg, "Stock updated successfully!", "Goods In", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(dlg, "No quantities were added. Please set at least one Add Qty > 0.", "Goods In", JOptionPane.WARNING_MESSAGE);
                    return; // stay open so user can correct
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg,
                        "Failed to update stock:\n" + ex.getMessage(),
                        "Goods In Error", JOptionPane.ERROR_MESSAGE);
                return; // stay open
            }
            dlg.dispose();
        });
        cancelBtn.addActionListener(e -> dlg.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setBackground(UIUtils.DARK_NAVY);
        btnPanel.add(confirmBtn);
        btnPanel.add(cancelBtn);

        outer.add(sp, BorderLayout.CENTER);
        outer.add(btnPanel, BorderLayout.SOUTH);
        dlg.setContentPane(outer);
        dlg.setVisible(true);
    }

    /** Goods Out dialog — deduct stock (damaged / excess / write-off) */
    void showGoodsOutDialog() {
        List<CarPart> parts = FileManager.readAllCarParts();
        JDialog dlg = new JDialog((Dialog) null, "Goods Out — Deduct Stock", true);
        dlg.setSize(680, 520);
        dlg.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dlg.setResizable(false);

        JPanel outer = new JPanel(new BorderLayout(0, 14));
        outer.setBackground(UIUtils.DARK_NAVY);
        outer.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Table: Part Name, Current Qty, Deduct Qty (+/-)
        String[] colNames = {"Part ID", "Part Name", "Current Qty", "Deduct Qty"};
        DefaultTableModel dm = new DefaultTableModel(colNames, 0) {
            public boolean isCellEditable(int r, int c) { return c == 3; }
            public Class<?> getColumnClass(int c) { return c == 3 ? Integer.class : String.class; }
        };

        for (CarPart cp : parts) {
            dm.addRow(new Object[]{cp.getPartId(), cp.getName(), cp.getStockQuantity(), 0});
        }

        JTable table = UIUtils.createStyledTable(dm);
        table.setRowHeight(34);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] w = {70, 280, 100, 100};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        // Qty column — render current quantity with colour
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                int qty = (v instanceof Integer) ? (Integer) v : 0;
                setText(String.valueOf(qty));
                setForeground(qty > 0 ? ManagerDashboard.GREEN_OK : new Color(255, 100, 80));
                setFont(UIUtils.uiFont(Font.BOLD, 12));
                setHorizontalAlignment(CENTER);
                setBackground(sel ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
                return this;
            }
        });

        // Deduct Qty column — custom editor with +/- buttons and spinner (capped at current stock)
        table.getColumnModel().getColumn(3).setCellRenderer(new SpinnerRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new SpinnerEditor(true));

        JScrollPane sp = new JScrollPane(table);
        UIUtils.styleScrollPane(sp);
        sp.setPreferredSize(new Dimension(640, 360));

        // Buttons
        UIUtils.RoundedButton confirmBtn = new UIUtils.RoundedButton("Confirm Goods Out", new Color(220, 120, 40));
        confirmBtn.setPreferredSize(new Dimension(180, 36));
        UIUtils.RoundedButton cancelBtn = new UIUtils.RoundedButton("Cancel", UIUtils.MEDIUM_NAVY);
        cancelBtn.setPreferredSize(new Dimension(100, 36));

        confirmBtn.addActionListener(e -> {
            try {
                boolean anyUpdated = false;
                for (int i = 0; i < dm.getRowCount(); i++) {
                    Object val = dm.getValueAt(i, 3);
                    int deductQty;
                    if (val instanceof Integer) {
                        deductQty = (Integer) val;
                    } else if (val instanceof String) {
                        try { deductQty = Integer.parseInt((String) val); }
                        catch (NumberFormatException nfe) { deductQty = 0; }
                    } else {
                        deductQty = 0;
                    }
                    if (deductQty <= 0) continue;
                    String partId = (String) dm.getValueAt(i, 0);
                    CarPart cp = FileManager.findCarPartById(partId);
                    if (cp != null) {
                        int newQty = Math.max(0, cp.getStockQuantity() - deductQty);
                        cp.setStockQuantity(newQty);
                        FileManager.updateCarPart(cp);
                        anyUpdated = true;
                    }
                }
                if (anyUpdated) {
                    JOptionPane.showMessageDialog(dlg, "Stock deducted successfully!", "Goods Out", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(dlg, "No quantities were deducted. Please set at least one Deduct Qty > 0.", "Goods Out", JOptionPane.WARNING_MESSAGE);
                    return; // stay open so user can correct
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg,
                        "Failed to deduct stock:\n" + ex.getMessage(),
                        "Goods Out Error", JOptionPane.ERROR_MESSAGE);
                return; // stay open
            }
            dlg.dispose();
        });
        cancelBtn.addActionListener(e -> dlg.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setBackground(UIUtils.DARK_NAVY);
        btnPanel.add(confirmBtn);
        btnPanel.add(cancelBtn);

        outer.add(sp, BorderLayout.CENTER);
        outer.add(btnPanel, BorderLayout.SOUTH);
        dlg.setContentPane(outer);
        dlg.setVisible(true);
    }

    // ── Spinner renderer for Goods In Add Qty column ────────────────────────
    private static class SpinnerRenderer extends JPanel implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final JLabel label = new JLabel("", JLabel.CENTER);
        public SpinnerRenderer() {
            setLayout(new BorderLayout());
            setOpaque(true);
            label.setFont(UIUtils.uiFont(Font.BOLD, 13));
            add(label, BorderLayout.CENTER);
        }
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            int qty = (value instanceof Integer) ? (Integer) value : 0;
            label.setText(String.valueOf(qty));
            label.setForeground(qty > 0 ? UIUtils.ACCENT_BLUE : UIUtils.DIM_TEXT);
            setBackground(isSelected ? UIUtils.ACCENT_BLUE : UIUtils.TABLE_BG);
            return this;
        }
    }

    // ── Spinner editor for Goods In/Out Qty column ──────────────────────────
    private class SpinnerEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;
        private final JPanel panel;
        private final JButton minusBtn, plusBtn;
        private final JLabel qtyLabel;
        private int currentQty;
        private JTable editorTable;
        private int editorRow, editorCol;
        private boolean useStockCap;    // true = cap + at current stock qty (Goods Out)

        /** Goods In — unlimited (up to 999). */
        public SpinnerEditor() { this(false); }

        /** @param stockCapped true = cannot exceed current stock qty (Goods Out) */
        public SpinnerEditor(boolean stockCapped) {
            super(new JCheckBox());
            this.useStockCap = stockCapped;
            panel = new JPanel(new BorderLayout(4, 0));
            panel.setBackground(UIUtils.TABLE_BG);

            minusBtn = createSpinBtn("\u2212");
            plusBtn  = createSpinBtn("+");
            qtyLabel = new JLabel("0", JLabel.CENTER);
            qtyLabel.setFont(UIUtils.uiFont(Font.BOLD, 13));
            qtyLabel.setForeground(UIUtils.ACCENT_BLUE);
            qtyLabel.setPreferredSize(new Dimension(30, 24));

            minusBtn.addActionListener(e -> {
                if (currentQty > 0) {
                    currentQty--;
                    qtyLabel.setText(String.valueOf(currentQty));
                    if (editorTable != null) editorTable.setValueAt(currentQty, editorRow, editorCol);
                }
            });
            plusBtn.addActionListener(e -> {
                int max = maxAllowed();
                if (currentQty < max) {
                    currentQty++;
                    qtyLabel.setText(String.valueOf(currentQty));
                    if (editorTable != null) editorTable.setValueAt(currentQty, editorRow, editorCol);
                }
            });

            panel.add(minusBtn, BorderLayout.WEST);
            panel.add(qtyLabel, BorderLayout.CENTER);
            panel.add(plusBtn, BorderLayout.EAST);
        }

        private JButton createSpinBtn(String text) {
            JButton btn = new JButton(text);
            btn.setFont(UIUtils.uiFont(Font.BOLD, 14));
            btn.setForeground(UIUtils.WHITE);
            btn.setBackground(new Color(60, 100, 160));
            btn.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(28, 24));
            return btn;
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.editorTable = table;
            this.editorRow   = row;
            this.editorCol   = column;
            this.currentQty = (value instanceof Integer) ? (Integer) value : 0;
            qtyLabel.setText(String.valueOf(currentQty));
            qtyLabel.setForeground(currentQty > 0 ? UIUtils.ACCENT_BLUE : UIUtils.DIM_TEXT);
            return panel;
        }

        private int maxAllowed() {
            if (!useStockCap || editorTable == null) return 999;
            Object stockVal = editorTable.getValueAt(editorRow, 2); // column 2 = Current Qty
            int stock = 0;
            if (stockVal instanceof Integer) stock = (Integer) stockVal;
            else if (stockVal instanceof String) {
                try { stock = Integer.parseInt((String) stockVal); } catch (NumberFormatException ignored) {}
            }
            return Math.max(stock, 0);
        }

        public Object getCellEditorValue() {
            return currentQty;
        }
    }


}
