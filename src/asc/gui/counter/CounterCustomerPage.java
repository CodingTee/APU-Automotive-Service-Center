package asc.gui.counter;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class CounterCustomerPage extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CounterStaffDashboard main;
    private DefaultTableModel model;
    private JTable table;
    private JTextField genF, carF, phoneF, idF, emailF;

    public CounterCustomerPage(CounterStaffDashboard m) {
        main = m;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(18, 18, 18, 18));

        // Title stays at top, never scrolls away
        add(main.sectionTitle("Customer Management",
                CounterStaffDashboard.ICON_BASE + "Customer Management.png"), BorderLayout.NORTH);
        add(makeBody(), BorderLayout.CENTER);
    }

    // =========================================================================
    //  BODY  – two rows fixed at top, table fills the rest
    // =========================================================================

    private JPanel makeBody() {
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(UIUtils.DARK_NAVY);

        // ── Search card – compact fixed height so table gets most of the space ──
        JPanel searchCard = new JPanel(new BorderLayout(0, 8));
        searchCard.setBackground(CounterStaffDashboard.CARD2);
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1),
                new EmptyBorder(12, 14, 10, 14)));

        JLabel searchTitle = new JLabel("Search Customers");
        searchTitle.setForeground(UIUtils.WHITE);
        searchTitle.setFont(UIUtils.uiFont(Font.BOLD, 13));
        searchCard.add(searchTitle, BorderLayout.NORTH);

        // Fields row
        JPanel fieldsRow = new JPanel(new GridLayout(1, 5, 10, 0));
        fieldsRow.setBackground(CounterStaffDashboard.CARD2);
        genF   = main.tf(); carF   = main.tf();
        phoneF = main.tf(); idF    = main.tf(); emailF = main.tf();
        fieldsRow.add(main.inputBox("General",     genF));
        fieldsRow.add(main.inputBox("Vehicle",     carF));
        fieldsRow.add(main.inputBox("Phone",       phoneF));
        fieldsRow.add(main.inputBox("Customer ID", idF));
        fieldsRow.add(main.inputBox("Email",       emailF));

        UIUtils.RoundedButton addBtn   = new UIUtils.RoundedButton("Add Customer", UIUtils.SUCCESS_GREEN);
        UIUtils.RoundedButton resetBtn = new UIUtils.RoundedButton("Reset",           new Color(65, 95, 155));
        for (JButton b : new JButton[]{addBtn, resetBtn}) b.setPreferredSize(new Dimension(145, 32));

        addBtn.addActionListener(e -> showAdd());
        resetBtn.addActionListener(e -> {
            genF.setText(""); carF.setText(""); phoneF.setText(""); idF.setText(""); emailF.setText("");
            refresh();
        });
        genF.getDocument().addDocumentListener(main.doc(this::refresh));
        carF.getDocument().addDocumentListener(main.doc(this::refresh));
        phoneF.getDocument().addDocumentListener(main.doc(this::refresh));
        idF.getDocument().addDocumentListener(main.doc(this::refresh));
        emailF.getDocument().addDocumentListener(main.doc(this::refresh));

        JPanel btnsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnsRow.setBackground(CounterStaffDashboard.CARD2);
        btnsRow.add(addBtn); btnsRow.add(resetBtn);

        JPanel midSection = new JPanel(new BorderLayout(0, 8));
        midSection.setBackground(CounterStaffDashboard.CARD2);
        midSection.add(fieldsRow, BorderLayout.CENTER);
        midSection.add(btnsRow,   BorderLayout.SOUTH);
        searchCard.add(midSection, BorderLayout.CENTER);

        // ── Table fills remaining height ─────────────────────────────────────
        model = new DefaultTableModel(
                new String[]{"ID","Name","Phone","Email","Vehicle","VIP","Actions"}, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = UIUtils.createStyledTable(model);
        main.darkTable(table);
        TxtLeft ll = new TxtLeft();
        for (int i = 0; i < 5; i++) table.getColumnModel().getColumn(i).setCellRenderer(ll);
        table.getColumnModel().getColumn(5).setCellRenderer(new VipRender());
        table.getColumnModel().getColumn(6).setCellRenderer(new ActionRender());
        table.getColumnModel().getColumn(6).setPreferredWidth(130);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint()); if (row < 0) return;
                Customer c = FileManager.findCustomerById((String) model.getValueAt(row, 0)); if (c == null) return;
                int col = table.columnAtPoint(e.getPoint());
                if (col == 6) {
                    Rectangle r = table.getCellRect(row, col, false);
                    if (e.getX() - r.x < r.width / 2) showEdit(c); else deleteCustomer(c);
                } else if (e.getClickCount() == 2) { showEdit(c); }
            }
        });
        refresh();

        JScrollPane tableScroll = new JScrollPane(table);
        UIUtils.styleScrollPane(tableScroll);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(CounterStaffDashboard.CARD2);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1), new EmptyBorder(12, 14, 12, 14)));
        JLabel tblTitle = new JLabel("Customer List");
        tblTitle.setForeground(UIUtils.WHITE); tblTitle.setFont(UIUtils.uiFont(Font.BOLD, 13));
        tblTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        tableCard.add(tblTitle,     BorderLayout.NORTH);
        tableCard.add(tableScroll,  BorderLayout.CENTER);

        body.add(searchCard, BorderLayout.NORTH);
        body.add(tableCard,  BorderLayout.CENTER);
        return body;
    }

    // =========================================================================
    //  REFRESH
    // =========================================================================

    private void refresh() {
        if (model == null) return;
        model.setRowCount(0);
        String g = low(genF), car = low(carF), ph = low(phoneF), id = low(idF), em = low(emailF);
        for (Customer c : FileManager.readAllCustomers()) {
            if (!g.isEmpty()   && !main.customerFind(c, g))  continue;
            if (!car.isEmpty() && !main.safe(c.getVehicleInfo()).toLowerCase().contains(car)) continue;
            if (!ph.isEmpty()  && !main.safe(c.getPhone()).toLowerCase().contains(ph))        continue;
            if (!id.isEmpty()  && !main.safe(c.getUserId()).toLowerCase().contains(id))       continue;
            if (!em.isEmpty()  && !main.safe(c.getEmail()).toLowerCase().contains(em))        continue;
            VipAccount vip = FileManager.findVipByCustomerId(c.getUserId());
            String vipTier = (vip != null) ? vip.tierBadge() : "\u2014";
            model.addRow(new Object[]{
                c.getUserId(), c.getName(), c.getPhone(), c.getEmail(), fmtVehicles(c),
                vipTier, "Edit | Delete"
            });
        }
    }

    private String low(JTextField f) {
        return f == null || f.getText() == null ? "" : f.getText().trim().toLowerCase();
    }

    // =========================================================================
    //  ADD CUSTOMER
    // =========================================================================

    private void showAdd() {
        JDialog d = new JDialog(main, "Add Customer", true);
        d.setSize(500, 580); d.setLocationRelativeTo(main);
        JPanel p = formPanel(); GridBagConstraints g = baseG();
        String newId = FileManager.generateCustomerId();
        JTextField cidF  = new JTextField(newId); cidF.setEditable(false); UIUtils.styleTextField(cidF);
        JTextField userF = main.tf(), passF = main.tf(), nameF = main.tf(), emailF = main.tf(), phoneF = main.tf();
        JTextField v1F   = main.tf(), v2F   = main.tf();
        JComboBox<String> genderCb = UIUtils.createGenderCombo("");
        passF.setText(autoPw(newId, ""));
        fRow(p, g, 0, "Customer ID",  cidF,     false);
        fRow(p, g, 1, "Username",     userF,    true);
        fRow(p, g, 2, "Password",     passF,    true);
        fRow(p, g, 3, "Full Name",    nameF,    true);
        fRow(p, g, 4, "Email",        emailF,   true);
        fRow(p, g, 5, "Phone",        phoneF,   true);
        fRow(p, g, 6, "Gender",       genderCb, false);
        fRow(p, g, 7, "Vehicle Info", v1F,      true);
        JPanel car2 = new JPanel(new BorderLayout(6, 0)); car2.setBackground(UIUtils.DARK_NAVY);
        UIUtils.RoundedButton addCar = new UIUtils.RoundedButton("+", UIUtils.ACCENT_BLUE);
        addCar.setPreferredSize(new Dimension(40, 32)); v2F.setVisible(false);
        car2.add(v2F, BorderLayout.CENTER); car2.add(addCar, BorderLayout.EAST);
        fRow(p, g, 8, "2nd Vehicle", car2, false);
        addCar.addActionListener(e -> { v2F.setVisible(true); d.revalidate(); });
        phoneF.getDocument().addDocumentListener(main.doc(() -> {
            String next = autoPw(newId, phoneF.getText());
            if (passF.getText().trim().isEmpty() || passF.getText().startsWith(newId)) passF.setText(next);
        }));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); btns.setBackground(UIUtils.DARK_NAVY);
        UIUtils.RoundedButton cancel = new UIUtils.RoundedButton("Cancel", new Color(65, 95, 155));
        UIUtils.RoundedButton save   = new UIUtils.RoundedButton("Create Customer", UIUtils.SUCCESS_GREEN);
        cancel.setPreferredSize(new Dimension(110, 32)); save.setPreferredSize(new Dimension(160, 32));
        cancel.addActionListener(e -> d.dispose());
        save.addActionListener(e -> {
            String u = userF.getText().trim(), pw = passF.getText().trim(), n = nameF.getText().trim();
            String mail = emailF.getText().trim(), hp = phoneF.getText().trim();
            String vehicle = carsText(v1F.getText(), v2F.getText());
            if (u.isEmpty()||pw.isEmpty()||n.isEmpty()||mail.isEmpty()||hp.isEmpty()||vehicle.isEmpty()) { main.msgErr(d,"All required fields must be filled."); return; }
            if (!ValidationUtils.isValidEmail(mail)) { main.msgErr(d,"Invalid email."); return; }
            if (!ValidationUtils.isValidPhone(hp))   { main.msgErr(d,"Phone must be 10-11 digits."); return; }
            if (FileManager.isUsernameExists(u))     { main.msgErr(d,"Username already exists."); return; }
            FileManager.saveCustomer(new Customer(newId, u, pw, n, mail, hp, vehicle, UIUtils.resolveGender(genderCb)));
            // Auto-create VIP account at NONE tier
            if (FileManager.findVipByCustomerId(newId) == null) {
                VipAccount autoVip = VipAccount.enrol(FileManager.generateVipId(), newId, hp);
                FileManager.saveVipAccount(autoVip);
            }
            JOptionPane.showMessageDialog(d,"Customer added!\nID: "+newId,"Success",JOptionPane.INFORMATION_MESSAGE);
            d.dispose(); refresh();
        });
        btns.add(cancel); btns.add(save);
        g.gridx=0; g.gridy=9; g.gridwidth=2; p.add(btns,g);
        d.setContentPane(p); d.setVisible(true);
    }

    // =========================================================================
    //  EDIT CUSTOMER
    // =========================================================================

    private void showEdit(Customer c) {
        JDialog d = new JDialog(main, "Edit Customer", true);
        d.setSize(480, 520); d.setLocationRelativeTo(main);
        JPanel p = formPanel(); GridBagConstraints g = baseG();
        JTextField userF  = new JTextField(c.getUsername());       userF.setEditable(false); UIUtils.styleTextField(userF);
        JTextField passF  = new JTextField(c.getPassword());         UIUtils.styleTextField(passF);
        JTextField nameF  = new JTextField(c.getName());             UIUtils.styleTextField(nameF);
        JTextField emailF = new JTextField(c.getEmail());            UIUtils.styleTextField(emailF);
        JTextField phoneF = new JTextField(c.getPhone());            UIUtils.styleTextField(phoneF);
        JTextField v1F    = new JTextField(firstCar(c.getVehicleInfo(),0)); UIUtils.styleTextField(v1F);
        JTextField v2F    = new JTextField(firstCar(c.getVehicleInfo(),1)); UIUtils.styleTextField(v2F);
        JComboBox<String> genderCb = UIUtils.createGenderCombo(c.getGender());
        fRow(p,g,0,"Username",    userF,   false);
        fRow(p,g,1,"Password",    passF,   true);
        fRow(p,g,2,"Full Name",   nameF,   true);
        fRow(p,g,3,"Gender",      genderCb,false);
        fRow(p,g,4,"Email",       emailF,  true);
        fRow(p,g,5,"Phone",       phoneF,  true);
        fRow(p,g,6,"Vehicle Info",v1F,     true);
        fRow(p,g,7,"2nd Vehicle", v2F,     false);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); btns.setBackground(UIUtils.DARK_NAVY);
        UIUtils.RoundedButton clear = new UIUtils.RoundedButton("Clear", new Color(65,95,155));
        UIUtils.RoundedButton save  = new UIUtils.RoundedButton("Save Changes", UIUtils.ACCENT_BLUE);
        clear.setPreferredSize(new Dimension(100,32)); save.setPreferredSize(new Dimension(140,32));
        clear.addActionListener(e -> { passF.setText(""); nameF.setText(""); emailF.setText(""); phoneF.setText(""); v1F.setText(""); v2F.setText(""); });
        save.addActionListener(e -> {
            String pw=passF.getText().trim(), n=nameF.getText().trim(), mail=emailF.getText().trim();
            String hp=phoneF.getText().trim(), vehicle=carsText(v1F.getText(),v2F.getText());
            if (pw.isEmpty()||n.isEmpty()||mail.isEmpty()||hp.isEmpty()||vehicle.isEmpty()) { main.msgErr(d,"Required fields cannot be empty."); return; }
            if (!ValidationUtils.isValidEmail(mail)) { main.msgErr(d,"Invalid email."); return; }
            if (!ValidationUtils.isValidPhone(hp))   { main.msgErr(d,"Phone must be 10-11 digits."); return; }
            c.setPassword(pw); c.setName(n); c.setEmail(mail); c.setPhone(hp);
            c.setVehicleInfo(vehicle); c.setGender(UIUtils.resolveGender(genderCb));
            FileManager.updateCustomer(c);
            JOptionPane.showMessageDialog(d,"Customer updated!","Success",JOptionPane.INFORMATION_MESSAGE);
            d.dispose(); refresh();
        });
        btns.add(clear); btns.add(save);
        g.gridx=0; g.gridy=8; g.gridwidth=2; p.add(btns,g);
        d.setContentPane(p); d.setVisible(true);
    }

    private void deleteCustomer(Customer c) {
        if (JOptionPane.showConfirmDialog(main, "Are you sure you want to DELETE \"" + c.getName() + "\"?\nThis action cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        FileManager.deleteCustomer(c.getUserId());
        refresh();
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIUtils.DARK_NAVY); p.setBorder(new EmptyBorder(14,26,14,26));
        return p;
    }

    private GridBagConstraints baseG() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(6,5,6,5);
        return g;
    }

    private void fRow(JPanel p, GridBagConstraints g, int y, String label, JComponent comp, boolean req) {
        g.gridy=y; g.gridx=0; g.gridwidth=1; g.weightx=0.36;
        JLabel l = new JLabel(req ? "<html>"+label+" <font color='#ff6969'>*</font>:</html>" : label+":");
        l.setForeground(UIUtils.WHITE); l.setFont(UIUtils.uiFont(Font.BOLD,12)); p.add(l,g);
        g.gridx=1; g.weightx=0.64; p.add(comp,g);
    }

    private String carsText(String a, String b) {
        List<String> cars = new ArrayList<>();
        if (a!=null&&!a.trim().isEmpty()) cars.add(a.trim());
        if (b!=null&&!b.trim().isEmpty()) cars.add(b.trim());
        return String.join("||", cars);
    }

    private String firstCar(String v, int i) {
        String[] parts = main.safe(v).split("\\|\\|", -1);
        return i < parts.length ? parts[i].trim() : "";
    }

    /** Format a customer's vehicle list as a clean comma-separated display string. */
    static String fmtVehicles(Customer c) {
        java.util.List<String> v = c.getVehicleList();
        return v.isEmpty() ? "\u2014" : String.join(", ", v);
    }

    private String autoPw(String id, String phone) {
        String d = main.safe(phone).replaceAll("\\D","");
        return id + (d.length()>=4 ? d.substring(d.length()-4) : "0000");
    }

    // =========================================================================
    //  CELL RENDERERS
    // =========================================================================

    private class VipRender extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, s, f, r, c);
            setHorizontalAlignment(CENTER); setFont(UIUtils.uiFont(Font.BOLD, 11));
            String tier = String.valueOf(v);
            if (tier.contains("BLACKGOLD"))      setForeground(new Color(255, 215, 0));
            else if (tier.contains("GOLD"))      setForeground(new Color(255, 200, 60));
            else if (tier.contains("BRONZE"))    setForeground(new Color(205, 127, 50));
            else if (tier.contains("NONE"))      setForeground(UIUtils.DIM_TEXT);
            else                                 setForeground(UIUtils.DIM_TEXT);
            setBackground(s ? UIUtils.ACCENT_BLUE : CounterStaffDashboard.CARD2);
            return this;
        }
    }

    private class TxtLeft extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c) {
            super.getTableCellRendererComponent(t,v,s,f,r,c);
            setHorizontalAlignment(LEFT); setBorder(new EmptyBorder(0,8,0,0));
            setForeground(UIUtils.WHITE);
            setBackground(s ? UIUtils.ACCENT_BLUE : CounterStaffDashboard.CARD2); return this;
        }
    }

    private class ActionRender extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c) {
            super.getTableCellRendererComponent(t,v,s,f,r,c);
            setHorizontalAlignment(CENTER); setFont(UIUtils.uiFont(Font.BOLD,12));
            setForeground(UIUtils.ACCENT_BLUE);
            setBackground(s ? UIUtils.MEDIUM_NAVY : CounterStaffDashboard.CARD2); return this;
        }
    }
}
