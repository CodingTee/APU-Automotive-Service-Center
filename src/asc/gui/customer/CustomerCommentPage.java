package asc.gui.customer;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

public class CustomerCommentPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final CustomerDashboard main;

    public CustomerCommentPage(CustomerDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY); panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.add(main.sectionTitle("Leave a Comment & Rating", CustomerDashboard.ICON_BASE + "Leave a Comment(Blue).png"), BorderLayout.NORTH);
        List<Appointment> done = main.currentCustomer.getMyCompletedAppointments();
        if (done.isEmpty()) { panel.add(main.emptyState("\uD83D\uDE97", "No completed appointments to comment on yet."), BorderLayout.CENTER); return panel; }
        Map<String, Comment> cmtMap = main.currentCustomer.getMyCommentMap();
        JPanel formCard = new JPanel(new GridBagLayout()); formCard.setBackground(UIUtils.TABLE_BG);
        formCard.setBorder(BorderFactory.createCompoundBorder(new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 14, 1), new EmptyBorder(10, 16, 10, 14)));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(7, 6, 7, 6); g.gridwidth = 2;
        String[] opts = done.stream().map(a -> a.getAppointmentId() + " \u2013 " + a.getDate() + " (" + a.getServiceType() + ") \u2013 " + a.getVehicleInfo()).toArray(String[]::new);
        JComboBox<String> aptCombo = new JComboBox<>(opts); UIUtils.styleCombo(aptCombo);
        JTextArea csArea = main.makeTextArea(); JTextArea techArea = main.makeTextArea();
        int[] csStars = {0}; int[] techStars = {0};
        JLabel[] csStarRow = main.makeStarLabels(); JLabel[] techStarRow = main.makeStarLabels();
        main.attachStarListeners(csStarRow, csStars); main.attachStarListeners(techStarRow, techStars);
        Runnable prefill = () -> {
            int idx = aptCombo.getSelectedIndex(); if (idx < 0) return;
            Comment ex = cmtMap.get(done.get(idx).getAppointmentId());
            if (ex != null) {
                csArea.setText(ex.getCounterStaffComment().replaceAll("\\s*\\[Rating:[^\\]]*\\]", "").trim());
                techArea.setText(ex.getTechnicianComment().replaceAll("\\s*\\[Rating:[^\\]]*\\]", "").trim());
                csStars[0] = ex.getCsStars(); techStars[0] = ex.getTechStars();
                main.paintStars(csStarRow, csStars[0]); main.paintStars(techStarRow, techStars[0]);
            } else { csArea.setText(""); techArea.setText(""); csStars[0] = 0; techStars[0] = 0; main.paintStars(csStarRow, 0); main.paintStars(techStarRow, 0); }
        };
        aptCombo.addActionListener(e -> prefill.run()); prefill.run();
        UIUtils.RoundedButton submit = new UIUtils.RoundedButton("Submit Comment & Rating", UIUtils.ACCENT_BLUE);
        submit.setPreferredSize(new Dimension(260, 38));
        submit.addActionListener(e -> {
            int idx = aptCombo.getSelectedIndex(); if (idx < 0) return;
            Appointment apt = done.get(idx);
            String csText = csArea.getText().trim(), techText = techArea.getText().trim();
            if (csText.isEmpty() && techText.isEmpty()) { JOptionPane.showMessageDialog(this, "Please enter at least one comment.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
            if (csText.isEmpty()) csText = "(No comment provided)"; if (techText.isEmpty()) techText = "(No comment provided)";
            main.currentCustomer.submitComment(apt, csText, techText, csStars[0], techStars[0], cmtMap);
            aptCombo.getActionListeners()[0].actionPerformed(new ActionEvent(aptCombo, ActionEvent.ACTION_PERFORMED, ""));
            JOptionPane.showMessageDialog(this, "Comment submitted!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        int row = 0;
        g.gridy = row++; main.addFormRow(formCard, g, "Select Appointment:", aptCombo);
        g.gridy = row++; g.gridx = 0; g.gridwidth = 2; formCard.add(main.fieldLabel("Comment for Counter Staff:"), g);
        g.gridy = row++; JScrollPane csSp = new JScrollPane(csArea); UIUtils.styleScrollPane(csSp); formCard.add(csSp, g);
        g.gridy = row++; formCard.add(main.fieldLabel("Counter Staff Rating:"), g);
        g.gridy = row++; formCard.add(main.starPanel(csStarRow), g);
        g.gridy = row++; formCard.add(main.fieldLabel("Comment for Technician:"), g);
        g.gridy = row++; JScrollPane techSp = new JScrollPane(techArea); UIUtils.styleScrollPane(techSp); formCard.add(techSp, g);
        g.gridy = row++; formCard.add(main.fieldLabel("Technician Rating:"), g);
        g.gridy = row++; formCard.add(main.starPanel(techStarRow), g);
        g.gridy = row++; g.insets = new Insets(16, 6, 8, 6);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER)); btnRow.setBackground(UIUtils.TABLE_BG); btnRow.add(submit); formCard.add(btnRow, g);
        JPanel wrap = new JPanel(new GridBagLayout()); wrap.setBackground(UIUtils.DARK_NAVY); wrap.add(formCard, new GridBagConstraints());
        JScrollPane outer = new JScrollPane(wrap); UIUtils.styleScrollPane(outer);
        panel.add(outer, BorderLayout.CENTER);
        return panel;
    }


}
