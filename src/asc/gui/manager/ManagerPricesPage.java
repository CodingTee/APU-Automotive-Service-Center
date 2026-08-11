package asc.gui.manager;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class ManagerPricesPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final ManagerDashboard main;

    public ManagerPricesPage(ManagerDashboard m) {
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
        panel.add(main.sectionTitle("Service Pricing & Configuration", ManagerDashboard.MGR_ICON_BASE + "Set Prices(Blue).png"), BorderLayout.NORTH);

        ServicePrice sp = FileManager.readPrices();
        List<CarPart> allParts = FileManager.readAllCarParts();

        // ── Fields ──────────────────────────────────────────────────────────
        JTextField normalPriceF  = new JTextField(String.format("%.2f", sp.getNormalServicePrice())); UIUtils.styleTextField(normalPriceF);
        JTextField majorPriceF   = new JTextField(String.format("%.2f", sp.getMajorServicePrice()));  UIUtils.styleTextField(majorPriceF);
        JTextArea  normalDescTA  = main.styledTextArea(sp.getNormalDescription(), 3);
        JTextArea  majorDescTA   = main.styledTextArea(sp.getMajorDescription(),  3);

        // ── Checkboxes for included parts ───────────────────────────────────
        List<JCheckBox> normalCbs = new ArrayList<>();
        List<JCheckBox> majorCbs  = new ArrayList<>();
        java.util.Set<String> normalSet = new java.util.HashSet<>(sp.getNormalIncludedPartIds());
        java.util.Set<String> majorSet  = new java.util.HashSet<>(sp.getMajorIncludedPartIds());
        for (CarPart cp : allParts) {
            String label = "[" + cp.getPartId() + "]  " + cp.getName() + "  (RM " + String.format("%.2f", cp.getPrice()) + ")";
            JCheckBox nCb = main.styledCheckBox(label, normalSet.contains(cp.getPartId()));
            JCheckBox mCb = main.styledCheckBox(label, majorSet.contains(cp.getPartId()));
            normalCbs.add(nCb); majorCbs.add(mCb);
        }

        // ── Build two service cards side by side ────────────────────────────
        JPanel cardsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        cardsRow.setOpaque(false);
        cardsRow.add(buildServiceConfigCard(
                "Normal Service  (1 hr)", UIUtils.ACCENT_BLUE,
                normalPriceF, normalDescTA, allParts, normalCbs));
        cardsRow.add(buildServiceConfigCard(
                "Major Service  (3 hrs)", ManagerDashboard.GREEN_OK,
                majorPriceF, majorDescTA, allParts, majorCbs));

        // ── Save button ─────────────────────────────────────────────────────
        UIUtils.RoundedButton saveBtn = new UIUtils.RoundedButton("\u2714  Save All Settings", UIUtils.SUCCESS_GREEN);
        saveBtn.setPreferredSize(new Dimension(220, 38));
        saveBtn.addActionListener(e -> {
            if (!ValidationUtils.isPositiveDouble(normalPriceF.getText())) { main.err(this,"Normal price must be a positive number."); return; }
            if (!ValidationUtils.isPositiveDouble(majorPriceF.getText()))  { main.err(this,"Major price must be a positive number.");  return; }
            double nPrice = Double.parseDouble(normalPriceF.getText().trim());
            double mPrice = Double.parseDouble(majorPriceF.getText().trim());
            String nDesc  = normalDescTA.getText().trim();
            String mDesc  = majorDescTA.getText().trim();
            List<String> nIds = new ArrayList<>();
            List<String> mIds = new ArrayList<>();
            for (int i = 0; i < allParts.size(); i++) {
                if (normalCbs.get(i).isSelected()) nIds.add(allParts.get(i).getPartId());
                if (majorCbs.get(i).isSelected())  mIds.add(allParts.get(i).getPartId());
            }
            FileManager.savePrices(new ServicePrice(nPrice, mPrice, nDesc, mDesc, nIds, mIds));
            JOptionPane.showMessageDialog(this,
                    "Settings saved!\nNormal: RM" + String.format("%.2f", nPrice)
                    + "  |  Major: RM" + String.format("%.2f", mPrice)
                    + "\nNormal includes " + nIds.size() + " part(s), Major includes " + mIds.size() + " part(s).",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnRow.setBackground(UIUtils.DARK_NAVY);
        btnRow.add(saveBtn);

        panel.add(cardsRow, BorderLayout.CENTER);
        panel.add(btnRow,   BorderLayout.SOUTH);
        return panel;
    }

    /** One service config card (Normal or Major). */
    JPanel buildServiceConfigCard(String title, Color accent,
            JTextField priceField, JTextArea descArea,
            List<CarPart> allParts, List<JCheckBox> checkboxes) {

        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(UIUtils.TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(accent, 14, 2),
                new EmptyBorder(16, 18, 16, 18)));

        // Header
        JLabel hdr = new JLabel(title);
        hdr.setFont(UIUtils.uiFont(Font.BOLD, 15)); hdr.setForeground(accent);
        hdr.setBorder(new EmptyBorder(0, 0, 12, 0));

        // Price row
        JPanel priceRow = new JPanel(new BorderLayout(10, 0));
        priceRow.setBackground(UIUtils.TABLE_BG);
        JLabel priceLbl = new JLabel("Price (RM):");
        priceLbl.setFont(UIUtils.uiFont(Font.BOLD, 12)); priceLbl.setForeground(UIUtils.WHITE);
        priceLbl.setPreferredSize(new Dimension(90, 28));
        priceRow.add(priceLbl,  BorderLayout.WEST);
        priceRow.add(priceField, BorderLayout.CENTER);
        priceRow.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Description
        JLabel descLbl = new JLabel("Description:");
        descLbl.setFont(UIUtils.uiFont(Font.BOLD, 12)); descLbl.setForeground(UIUtils.WHITE);
        descLbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        JScrollPane descSp = new JScrollPane(descArea); UIUtils.styleScrollPane(descSp);
        descSp.setPreferredSize(new Dimension(-1, 70));

        // Included parts header
        JLabel partsHdr = new JLabel("Included Parts / Products:");
        partsHdr.setFont(UIUtils.uiFont(Font.BOLD, 12)); partsHdr.setForeground(accent);
        partsHdr.setBorder(new EmptyBorder(10, 0, 6, 0));

        JLabel hint = new JLabel("Checked = included in service price (no extra charge to customer)");
        hint.setFont(UIUtils.uiFont(Font.ITALIC, 10)); hint.setForeground(UIUtils.DIM_TEXT);
        hint.setBorder(new EmptyBorder(0, 0, 6, 0));

        // Select All / None shortcuts
        UIUtils.RoundedButton allBtn  = new UIUtils.RoundedButton("All",  new Color(50, 100, 50));
        UIUtils.RoundedButton noneBtn = new UIUtils.RoundedButton("None", new Color(100, 50, 50));
        allBtn .setPreferredSize(new Dimension(60, 24)); allBtn .setFont(UIUtils.uiFont(Font.BOLD, 10));
        noneBtn.setPreferredSize(new Dimension(60, 24)); noneBtn.setFont(UIUtils.uiFont(Font.BOLD, 10));
        allBtn .addActionListener(e -> checkboxes.forEach(cb -> cb.setSelected(true)));
        noneBtn.addActionListener(e -> checkboxes.forEach(cb -> cb.setSelected(false)));
        JPanel shortcutRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        shortcutRow.setBackground(UIUtils.TABLE_BG);
        shortcutRow.add(new JLabel("Quick select:") {{ setForeground(UIUtils.DIM_TEXT); setFont(UIUtils.uiFont(Font.PLAIN, 10)); }});
        shortcutRow.add(allBtn); shortcutRow.add(noneBtn);

        // Checkboxes panel (scrollable)
        JPanel cbPanel = new JPanel();
        cbPanel.setLayout(new BoxLayout(cbPanel, BoxLayout.Y_AXIS));
        cbPanel.setBackground(new Color(28, 44, 80));
        cbPanel.setBorder(new EmptyBorder(6, 8, 6, 8));
        for (JCheckBox cb : checkboxes) cbPanel.add(cb);
        JScrollPane cbScroll = new JScrollPane(cbPanel);
        cbScroll.setPreferredSize(new Dimension(-1, 210));
        UIUtils.styleScrollPane(cbScroll);

        // Assemble
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UIUtils.TABLE_BG);
        body.add(priceRow);
        body.add(descLbl);
        body.add(descSp);
        body.add(partsHdr);
        body.add(hint);
        body.add(shortcutRow);
        body.add(Box.createVerticalStrut(4));
        body.add(cbScroll);

        card.add(hdr,  BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }


}
