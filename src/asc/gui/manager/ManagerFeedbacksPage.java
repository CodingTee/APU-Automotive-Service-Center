package asc.gui.manager;

import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class ManagerFeedbacksPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final ManagerDashboard main;

    public ManagerFeedbacksPage(ManagerDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel=new JPanel(new BorderLayout(0,10));
        panel.setBackground(UIUtils.DARK_NAVY);
        panel.setBorder(new EmptyBorder(18,20,18,20));
        panel.add(main.sectionTitle("Feedbacks & Customer Comments", ManagerDashboard.MGR_ICON_BASE+"Feedbacks & Comments(Blue).png"), BorderLayout.NORTH);

        JTabbedPane tabs=new JTabbedPane(); tabs.setBackground(UIUtils.TABLE_BG); tabs.setForeground(UIUtils.WHITE);

        // Feedbacks
        main.feedbackModel=new DefaultTableModel(new String[]{"FB ID","Apt ID","Tech ID","Technician","Content","Date","View"},0){public boolean isCellEditable(int r,int c){return false;}};
        main.feedbackTable=UIUtils.createStyledTable(main.feedbackModel);
        main.feedbackTable.getColumnModel().getColumn(4).setPreferredWidth(260);
        main.feedbackTable.getColumnModel().getColumn(6).setCellRenderer(new ViewButtonRenderer());
        main.feedbackTable.getColumnModel().getColumn(6).setPreferredWidth(60);
        main.feedbackTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = main.feedbackTable.rowAtPoint(e.getPoint());
                int col = main.feedbackTable.columnAtPoint(e.getPoint());
                if (row < 0 || col != 6) return;
                showFeedbackDetail(row);
            }
        });
        JScrollPane fbSp=new JScrollPane(main.feedbackTable); UIUtils.styleScrollPane(fbSp);
        JPanel fbPanel=new JPanel(new BorderLayout()); fbPanel.setBackground(UIUtils.DARK_NAVY); fbPanel.setBorder(new EmptyBorder(8,8,8,8));
        fbPanel.add(fbSp,BorderLayout.CENTER);
        UIUtils.RoundedButton rfBtn=new UIUtils.RoundedButton("Refresh",UIUtils.MEDIUM_NAVY);
        rfBtn.addActionListener(e->main.refreshFeedbacks()); rfBtn.setPreferredSize(new Dimension(110,30));
        JPanel fbBtnRow=new JPanel(new FlowLayout(FlowLayout.RIGHT)); fbBtnRow.setBackground(UIUtils.DARK_NAVY); fbBtnRow.add(rfBtn);
        fbPanel.add(fbBtnRow,BorderLayout.SOUTH);

        // Comments
        main.commentModel=new DefaultTableModel(new String[]{"CMT ID","Apt ID","Customer","CS Comment","CS ★","Tech Comment","Tech ★","Date","View"},0){public boolean isCellEditable(int r,int c){return false;}};
        main.commentTable=UIUtils.createStyledTable(main.commentModel);
        main.commentTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        main.commentTable.getColumnModel().getColumn(5).setPreferredWidth(160);
        main.commentTable.getColumnModel().getColumn(8).setCellRenderer(new ViewButtonRenderer());
        main.commentTable.getColumnModel().getColumn(8).setPreferredWidth(60);
        main.commentTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = main.commentTable.rowAtPoint(e.getPoint());
                int col = main.commentTable.columnAtPoint(e.getPoint());
                if (row < 0 || col != 8) return;
                showCommentDetail(row);
            }
        });
        JScrollPane cmtSp=new JScrollPane(main.commentTable); UIUtils.styleScrollPane(cmtSp);
        JPanel cmtPanel=new JPanel(new BorderLayout()); cmtPanel.setBackground(UIUtils.DARK_NAVY); cmtPanel.setBorder(new EmptyBorder(8,8,8,8));
        cmtPanel.add(cmtSp,BorderLayout.CENTER);
        UIUtils.RoundedButton rcBtn=new UIUtils.RoundedButton("Refresh",UIUtils.MEDIUM_NAVY);
        rcBtn.addActionListener(e->main.refreshComments()); rcBtn.setPreferredSize(new Dimension(110,30));
        JPanel cmtBtnRow=new JPanel(new FlowLayout(FlowLayout.RIGHT)); cmtBtnRow.setBackground(UIUtils.DARK_NAVY); cmtBtnRow.add(rcBtn);
        cmtPanel.add(cmtBtnRow,BorderLayout.SOUTH);

        main.refreshFeedbacks(); main.refreshComments();
        tabs.addTab("Technician Feedbacks",fbPanel);
        tabs.addTab("Customer Comments",cmtPanel);
        panel.add(tabs,BorderLayout.CENTER); return panel;
    }

    // ── View button renderer ────────────────────────────────────────────────
    private static class ViewButtonRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel lbl = new JLabel("View", JLabel.CENTER);
            lbl.setFont(UIUtils.uiFont(Font.BOLD, 10));
            lbl.setOpaque(true);
            if (sel) {
                lbl.setBackground(new Color(40, 100, 160));
                lbl.setForeground(Color.WHITE);
            } else {
                lbl.setBackground(new Color(30, 80, 140));
                lbl.setForeground(new Color(180, 210, 255));
            }
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    new UIUtils.RoundedBorder(new Color(60, 130, 200), 6, 1),
                    new EmptyBorder(2, 4, 2, 4)));
            return lbl;
        }
    }

    private void showFeedbackDetail(int row) {
        String fbId   = (String) main.feedbackModel.getValueAt(row, 0);
        String aptId  = (String) main.feedbackModel.getValueAt(row, 1);
        String techId = (String) main.feedbackModel.getValueAt(row, 2);
        String techName = (String) main.feedbackModel.getValueAt(row, 3);
        String content  = (String) main.feedbackModel.getValueAt(row, 4);
        String date     = (String) main.feedbackModel.getValueAt(row, 5);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIUtils.TABLE_BG);
        p.setBorder(new EmptyBorder(12, 16, 12, 16));

        p.add(detailRow("Feedback ID:", fbId));
        p.add(detailRow("Appointment ID:", aptId));
        p.add(detailRow("Technician:", techName + "  (" + techId + ")"));
        p.add(detailRow("Date:", date));
        p.add(Box.createVerticalStrut(6));
        p.add(detailRow("Content:", ""));
        JTextArea ta = new JTextArea(content);
        ta.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        ta.setForeground(UIUtils.WHITE);
        ta.setBackground(new Color(22, 36, 70));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setEditable(false);
        ta.setBorder(new EmptyBorder(8, 8, 8, 8));
        JPanel taWrap = new JPanel(new BorderLayout());
        taWrap.setBackground(UIUtils.TABLE_BG);
        taWrap.setPreferredSize(new Dimension(380, 100));
        taWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        taWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        taWrap.add(ta, BorderLayout.CENTER);
        p.add(taWrap);

        JScrollPane sp = new JScrollPane(p);
        sp.setBorder(null); sp.getViewport().setBackground(UIUtils.TABLE_BG);
        sp.setPreferredSize(new Dimension(420, 280));

        JOptionPane.showMessageDialog(this, sp,
                "Feedback Detail — " + fbId, JOptionPane.PLAIN_MESSAGE);
    }

    private void showCommentDetail(int row) {
        String cmtId = (String) main.commentModel.getValueAt(row, 0);
        String aptId = (String) main.commentModel.getValueAt(row, 1);
        String cust  = (String) main.commentModel.getValueAt(row, 2);
        String csCmt = (String) main.commentModel.getValueAt(row, 3);
        String csStar= (String) main.commentModel.getValueAt(row, 4);
        String tCmt  = (String) main.commentModel.getValueAt(row, 5);
        String tStar = (String) main.commentModel.getValueAt(row, 6);
        String date  = (String) main.commentModel.getValueAt(row, 7);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIUtils.TABLE_BG);
        p.setBorder(new EmptyBorder(12, 16, 12, 16));

        p.add(detailRow("Comment ID:", cmtId));
        p.add(detailRow("Appointment ID:", aptId));
        p.add(detailRow("Customer:", cust));
        p.add(detailRow("Date:", date));
        p.add(Box.createVerticalStrut(8));
        p.add(detailRow("Counter Staff Comment:", csStar));
        p.add(wrapTextArea(csCmt));
        p.add(Box.createVerticalStrut(8));
        p.add(detailRow("Technician Comment:", tStar));
        p.add(wrapTextArea(tCmt));

        JScrollPane sp = new JScrollPane(p);
        sp.setBorder(null); sp.getViewport().setBackground(UIUtils.TABLE_BG);
        sp.setPreferredSize(new Dimension(440, 340));

        JOptionPane.showMessageDialog(this, sp,
                "Comment Detail — " + cmtId, JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel detailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(UIUtils.TABLE_BG);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
        lbl.setForeground(UIUtils.ACCENT_BLUE);
        lbl.setPreferredSize(new Dimension(170, 22));
        JLabel val = new JLabel(value);
        val.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        val.setForeground(UIUtils.WHITE);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private JPanel wrapTextArea(String text) {
        JTextArea ta = new JTextArea(text);
        ta.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        ta.setForeground(UIUtils.WHITE);
        ta.setBackground(new Color(22, 36, 70));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setEditable(false);
        ta.setBorder(new EmptyBorder(8, 8, 8, 8));
        JPanel taWrap = new JPanel(new BorderLayout());
        taWrap.setBackground(UIUtils.TABLE_BG);
        taWrap.setPreferredSize(new Dimension(380, 70));
        taWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        taWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        taWrap.add(ta, BorderLayout.CENTER);
        return taWrap;
    }
}
