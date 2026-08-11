package asc.gui.manager;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class ManagerUserMgmtPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final ManagerDashboard main;

    public ManagerUserMgmtPage(ManagerDashboard m) {
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
        panel.add(main.sectionTitle("User Management", ManagerDashboard.MGR_ICON_BASE + "User Management(Blue).png"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UIUtils.TABLE_BG); tabs.setForeground(UIUtils.WHITE);
        tabs.setFont(UIUtils.uiFont(Font.BOLD, 12));
        tabs.addTab("Managers",      buildUserSubPanel("MANAGER"));
        tabs.addTab("Counter Staff", buildUserSubPanel("COUNTER_STAFF"));
        tabs.addTab("Technicians",   buildUserSubPanel("TECHNICIAN"));
        tabs.addTab("Customers",     buildUserSubPanel("CUSTOMER"));
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    JPanel buildUserSubPanel(String role) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UIUtils.DARK_NAVY);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] cols = role.equals("CUSTOMER")
                ? new String[]{"User ID","Username","Name","Gender","Email","Phone","Vehicle"}
                : new String[]{"User ID","Username","Name","Gender","Email","Phone"};
        DefaultTableModel dm = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIUtils.createStyledTable(dm);
        JScrollPane sp = new JScrollPane(table); UIUtils.styleScrollPane(sp);

        main.fillUserTable(dm, role);

        UIUtils.RoundedButton addBtn    = new UIUtils.RoundedButton("Add",    UIUtils.SUCCESS_GREEN);
        UIUtils.RoundedButton editBtn   = new UIUtils.RoundedButton("Edit",   UIUtils.ACCENT_BLUE);
        UIUtils.RoundedButton deleteBtn = new UIUtils.RoundedButton("Delete", UIUtils.ERROR_RED);
        UIUtils.RoundedButton refreshBtn= new UIUtils.RoundedButton("Refresh",UIUtils.MEDIUM_NAVY);
        editBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
        deleteBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));
        refreshBtn.setFont(UIUtils.uiFont(Font.BOLD, 12));

        for (JButton b : new JButton[]{addBtn, editBtn, deleteBtn, refreshBtn})
            b.setPreferredSize(new Dimension(110, 32));

        addBtn   .addActionListener(e -> { showAddUserDialog(role);              main.fillUserTable(dm, role); });
        editBtn  .addActionListener(e -> { showEditUserDialog(table, dm, role);  main.fillUserTable(dm, role); });
        deleteBtn.addActionListener(e -> { deleteUser(table, dm, role);          main.fillUserTable(dm, role); });
        refreshBtn.addActionListener(e -> main.fillUserTable(dm, role));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnRow.setBackground(UIUtils.DARK_NAVY);
        btnRow.add(addBtn); btnRow.add(editBtn); btnRow.add(deleteBtn); btnRow.add(refreshBtn);

        p.add(sp,     BorderLayout.CENTER);
        p.add(btnRow, BorderLayout.SOUTH);
        return p;
    }

    void showAddUserDialog(String role) {
        JDialog d = new JDialog((Dialog) null, "Add " + main.fmtRole(role), true);
        d.setSize(420, 420); d.setLocationRelativeTo(null);
        JPanel p = main.dlgPanel();
        GridBagConstraints gbc = main.dlgGbc();

        JTextField uF=main.tf(), nF=main.tf(), eF=main.tf(), phF=main.tf(), vF=main.tf();
        JPasswordField pwF=new JPasswordField(); UIUtils.styleTextField(pwF);
        JComboBox<String> gCb = UIUtils.createGenderCombo("");

        main.addRow(p, gbc, 0, "Username:", uF);
        main.addRow(p, gbc, 1, "Password:", pwF);
        main.addRow(p, gbc, 2, "Full Name:", nF);
        main.addRow(p, gbc, 3, "Gender:", gCb);
        main.addRow(p, gbc, 4, "Email:", eF);
        main.addRow(p, gbc, 5, "Phone:", phF);
        if ("CUSTOMER".equals(role)) main.addRow(p, gbc, 6, "Vehicle:", vF);

        UIUtils.RoundedButton save = new UIUtils.RoundedButton("Save", UIUtils.SUCCESS_GREEN);
        save.setPreferredSize(new Dimension(140, 34));
        save.addActionListener(e -> {
            String u=uF.getText().trim(), pw=new String(pwF.getPassword()), n=nF.getText().trim();
            String g=UIUtils.resolveGender(gCb), em=eF.getText().trim(), ph=phF.getText().trim();
            if (u.isEmpty()||pw.isEmpty()||n.isEmpty()){main.err(d,"Username, password and name required.");return;}
            if (!ValidationUtils.isValidEmail(em)){main.err(d,"Invalid email.");return;}
            if (!ValidationUtils.isValidPhone(ph)){main.err(d,"Phone must be 10-11 digits.");return;}
            if (FileManager.isUsernameExists(u)){main.err(d,"Username already exists.");return;}
            switch(role){
                case"MANAGER":      FileManager.saveManager(new Manager(FileManager.generateManagerId(),u,pw,n,em,ph,g));break;
                case"COUNTER_STAFF":FileManager.saveCounterStaff(new CounterStaff(FileManager.generateCounterStaffId(),u,pw,n,em,ph,g));break;
                case"TECHNICIAN":   FileManager.saveTechnician(new Technician(FileManager.generateTechnicianId(),u,pw,n,em,ph,g));break;
                case"CUSTOMER":     FileManager.saveCustomer(new Customer(FileManager.generateCustomerId(),u,pw,n,em,ph,vF.getText().trim(),g));break;
            }
            JOptionPane.showMessageDialog(d, main.fmtRole(role)+" added!","Success",JOptionPane.INFORMATION_MESSAGE);
            d.dispose();
        });
        gbc.gridx=0; gbc.gridy=7; gbc.gridwidth=2; p.add(save, gbc);
        d.setContentPane(p); d.setVisible(true);
    }

    void showEditUserDialog(JTable table, DefaultTableModel dm, String role) {
        int row = table.getSelectedRow();
        if (row < 0){JOptionPane.showMessageDialog(this,"Select a "+main.fmtRole(role)+" to edit.","No Selection",JOptionPane.WARNING_MESSAGE);return;}
        String uid = (String)dm.getValueAt(row, 0);

        JDialog d = new JDialog((Dialog) null, "Edit "+main.fmtRole(role), true);
        d.setSize(420, "CUSTOMER".equals(role) ? 440 : 380); d.setLocationRelativeTo(null);
        JPanel p = main.dlgPanel(); GridBagConstraints gbc = main.dlgGbc();

        JTextField nF=new JTextField((String)dm.getValueAt(row,2)); UIUtils.styleTextField(nF);
        JComboBox<String> gCb = UIUtils.createGenderCombo((String)dm.getValueAt(row,3));
        JTextField eF=new JTextField((String)dm.getValueAt(row,4)); UIUtils.styleTextField(eF);
        JTextField phF=new JTextField((String)dm.getValueAt(row,5)); UIUtils.styleTextField(phF);
        final JTextField vField = new JTextField();
        UIUtils.styleTextField(vField);
        if ("CUSTOMER".equals(role)) {
            Customer cu = FileManager.findCustomerById(uid);
            vField.setText(cu != null ? cu.getVehicleInfo() : "");
        }
        JPasswordField pwF=new JPasswordField(); UIUtils.styleTextField(pwF);

        main.addRow(p,gbc,0,"Full Name:",nF); main.addRow(p,gbc,1,"Gender:",gCb);
        main.addRow(p,gbc,2,"Email:",eF);     main.addRow(p,gbc,3,"Phone:",phF);
        int pwRow = 4;
        if ("CUSTOMER".equals(role)) { main.addRow(p, gbc, 4, "Vehicle:", vField); pwRow = 5; }
        main.addRow(p,gbc,pwRow,"New Password:",pwF);
        JLabel note=new JLabel("(Leave blank to keep current)");
        note.setForeground(UIUtils.DIM_TEXT); note.setFont(UIUtils.uiFont(Font.ITALIC,11));
        int noteRow = pwRow + 1;
        gbc.gridx=0;gbc.gridy=noteRow;gbc.gridwidth=2;p.add(note,gbc);

        UIUtils.RoundedButton save=new UIUtils.RoundedButton("Update",UIUtils.ACCENT_BLUE);
        save.setPreferredSize(new Dimension(140,34));
        save.addActionListener(e->{
            String n=nF.getText().trim(),g=UIUtils.resolveGender(gCb),em=eF.getText().trim(),ph=phF.getText().trim(),pw=new String(pwF.getPassword());
            if(n.isEmpty()){main.err(d,"Name required.");return;}
            if(!ValidationUtils.isValidEmail(em)){main.err(d,"Invalid email.");return;}
            if(!ValidationUtils.isValidPhone(ph)){main.err(d,"Phone must be 10-11 digits.");return;}
            switch(role){
                case"MANAGER":{Manager m=FileManager.findManagerById(uid);if(m!=null){m.setName(n);m.setGender(g);m.setEmail(em);m.setPhone(ph);if(!pw.isEmpty())m.setPassword(pw);FileManager.updateManager(m);}break;}
                case"COUNTER_STAFF":{CounterStaff cs=FileManager.findCounterStaffById(uid);if(cs!=null){cs.setName(n);cs.setGender(g);cs.setEmail(em);cs.setPhone(ph);if(!pw.isEmpty())cs.setPassword(pw);FileManager.updateCounterStaff(cs);}break;}
                case"TECHNICIAN":{Technician t=FileManager.findTechnicianById(uid);if(t!=null){t.setName(n);t.setGender(g);t.setEmail(em);t.setPhone(ph);if(!pw.isEmpty())t.setPassword(pw);FileManager.updateTechnician(t);}break;}
                case"CUSTOMER":{Customer c=FileManager.findCustomerById(uid);if(c!=null){c.setName(n);c.setGender(g);c.setEmail(em);c.setPhone(ph);c.setVehicleInfo("CUSTOMER".equals(role) ? vField.getText().trim() : c.getVehicleInfo());if(!pw.isEmpty())c.setPassword(pw);FileManager.updateCustomer(c);}break;}
            }
            JOptionPane.showMessageDialog(d,"Updated!","Success",JOptionPane.INFORMATION_MESSAGE); d.dispose();
        });
        int saveRow = noteRow + 1;
        gbc.gridy=saveRow;p.add(save,gbc); d.setContentPane(p); d.setVisible(true);
    }

    void deleteUser(JTable table, DefaultTableModel dm, String role) {
        int row=table.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(this,"Select a "+main.fmtRole(role)+" to delete.","No Selection",JOptionPane.WARNING_MESSAGE);return;}
        String uid=(String)dm.getValueAt(row,0), name=(String)dm.getValueAt(row,2);
        if("MANAGER".equals(role)&&uid.equals(main.currentManager.getUserId())){JOptionPane.showMessageDialog(this,"Cannot delete your own account.","Error",JOptionPane.ERROR_MESSAGE);return;}
        if(JOptionPane.showConfirmDialog(this,"Delete "+name+"?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        switch(role){
            case"MANAGER":      FileManager.deleteManager(uid);      break;
            case"COUNTER_STAFF":FileManager.deleteCounterStaff(uid); break;
            case"TECHNICIAN":   FileManager.deleteTechnician(uid);   break;
            case"CUSTOMER":     FileManager.deleteCustomer(uid);      break;
        }
        JOptionPane.showMessageDialog(this,"Deleted.","Success",JOptionPane.INFORMATION_MESSAGE);
    }


}
