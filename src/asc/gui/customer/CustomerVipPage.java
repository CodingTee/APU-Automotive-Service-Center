package asc.gui.customer;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class CustomerVipPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final CustomerDashboard main;

    public CustomerVipPage(CustomerDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.add(main.sectionTitle("My VIP Status & Coupons", CustomerDashboard.ICON_BASE + "VIP & Coupons(Blue).png"), BorderLayout.NORTH);
        

        VipAccount vip = FileManager.findVipByCustomerId(main.currentCustomer.getUserId());

        // Auto-create VIP for legacy customers who registered before auto-VIP was introduced
        if (vip == null) {
            String phone = main.currentCustomer.getPhone();
            vip = VipAccount.enrol(FileManager.generateVipId(), main.currentCustomer.getUserId(),
                    phone != null ? phone : "");
            FileManager.saveVipAccount(vip);
        }

        // Auto-issue monthly coupon(s) if eligible and notify user
        if (vip != null) {
            java.util.List<Coupon> issued = FileManager.issueMonthlyCouponIfNeeded(vip);
            if (!issued.isEmpty()) {
                StringBuilder sb = new StringBuilder("Your monthly VIP coupon(s) have been issued!\n\n");
                for (Coupon c : issued) {
                    sb.append(String.format("Code: %s\nValue: RM %.2f off\n\n",
                            c.getCode(), c.getDiscountValue()));
                }
                sb.append("Expires: ").append(issued.get(0).getExpiryDate());
                JOptionPane.showMessageDialog(this, sb.toString(),
                    "Monthly Coupon Issued", JOptionPane.INFORMATION_MESSAGE);
                vip = FileManager.findVipByCustomerId(main.currentCustomer.getUserId());
            }
        }

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIUtils.DARK_NAVY);

        // â”€â”€ Main VIP banner card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        final VipAccount fVip = vip;
        JPanel vipCard = new JPanel(new BorderLayout(16, 8)) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1, c2;
                if (fVip == null) { c1 = new Color(40,60,110); c2 = new Color(25,40,80); }
                else switch (fVip.getTier()) {
                    case VipAccount.TIER_BLACKGOLD: c1=new Color(120,90,0);  c2=new Color(60,40,0);  break;
                    case VipAccount.TIER_GOLD:      c1=new Color(140,100,10);c2=new Color(80,55,0);  break;
                    case VipAccount.TIER_BRONZE:    c1=new Color(110,65,20); c2=new Color(65,35,10); break;
                    default:                        c1=new Color(40,65,120); c2=new Color(25,45,90); break;
                }
                g2.setPaint(new GradientPaint(0,0,c1,getWidth(),getHeight(),c2));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                g2.dispose();
            }
        };
        vipCard.setOpaque(false);
        vipCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));
        vipCard.setBorder(BorderFactory.createCompoundBorder(
            new UIUtils.RoundedBorder(vip != null ? vip.tierColor() : UIUtils.ACCENT_BLUE, 18, 1),
            new EmptyBorder(22, 28, 18, 28)));

        JPanel leftInfo = new JPanel();
        leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
        leftInfo.setOpaque(false);

        if (vip != null) {
            Color tc = vip.tierColor();
            JLabel tierLbl = new JLabel(vip.tierBadge());
            tierLbl.setFont(UIUtils.uiFont(Font.BOLD, 26)); tierLbl.setForeground(tc);

            JLabel ptsLbl = new JLabel(String.format("%,d pts  \u2014  Lifetime spend: RM %.0f",
                    vip.getPoints(), vip.getTotalSpent()));
            ptsLbl.setFont(UIUtils.uiFont(Font.BOLD, 13)); ptsLbl.setForeground(UIUtils.WHITE);

            double disc = vip.getDiscountPct();
            JLabel discLbl = new JLabel(disc > 0
                    ? String.format("Service Discount: %.0f%% off all services", disc)
                    : "No service discount  (reach GOLD for 2% off)");
            discLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
            discLbl.setForeground(disc > 0 ? UIUtils.GREEN_OK : UIUtils.DIM_TEXT);

            String cpnTxt;
            if (VipAccount.TIER_BLACKGOLD.equals(vip.getTier()))  cpnTxt = "Monthly Coupon: RM 50.00 / month";
            else if (VipAccount.TIER_NONE.equals(vip.getTier()))  cpnTxt = "No monthly coupon  (reach BRONZE for RM 20/month)";
            else                                                    cpnTxt = "Monthly Coupon: RM 20.00 / month";
            JLabel cpnLbl = new JLabel(cpnTxt);
            cpnLbl.setFont(UIUtils.uiFont(Font.PLAIN, 12));
            cpnLbl.setForeground(VipAccount.TIER_NONE.equals(vip.getTier()) ? UIUtils.DIM_TEXT : CustomerDashboard.ACCENT_YELLOW);

            JLabel phoneLbl = new JLabel("VIP Phone: " + vip.getPhone()
                    + "   |   Member since: " + vip.getCreatedDate());
            phoneLbl.setFont(UIUtils.uiFont(Font.PLAIN, 11));
            phoneLbl.setForeground(UIUtils.DIM_TEXT);

            leftInfo.add(tierLbl); leftInfo.add(Box.createVerticalStrut(6));
            leftInfo.add(ptsLbl); leftInfo.add(Box.createVerticalStrut(3));
            leftInfo.add(discLbl); leftInfo.add(Box.createVerticalStrut(3));
            leftInfo.add(cpnLbl); leftInfo.add(Box.createVerticalStrut(3));
            leftInfo.add(phoneLbl); leftInfo.add(Box.createVerticalStrut(10));
            leftInfo.add(main.buildTierProgressBar(vip));
        } else {
            JLabel noVip = new JLabel("Not a VIP Member");
            noVip.setFont(UIUtils.uiFont(Font.BOLD, 18)); noVip.setForeground(UIUtils.LIGHT_GRAY);
            JLabel hint  = new JLabel("VIP account is being set up. Please refresh or contact Counter Staff.");
            hint.setFont(UIUtils.uiFont(Font.PLAIN, 12)); hint.setForeground(UIUtils.DIM_TEXT);
            leftInfo.add(noVip); leftInfo.add(Box.createVerticalStrut(6)); leftInfo.add(hint);
        }
        vipCard.add(leftInfo, BorderLayout.CENTER);
        content.add(vipCard);
        content.add(Box.createVerticalStrut(12));

        // â”€â”€ Tier benefits comparison row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (vip != null) {
            JPanel benefitsRow = main.buildTierBenefitsRow(vip);
            benefitsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            content.add(benefitsRow);
            content.add(Box.createVerticalStrut(12));
        }

        // â”€â”€ Coupons table â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JLabel coupHdr = new JLabel("My Coupons");
        coupHdr.setFont(UIUtils.uiFont(Font.BOLD, 14));
        coupHdr.setForeground(CustomerDashboard.ACCENT_YELLOW);
        coupHdr.setBorder(new EmptyBorder(4, 0, 6, 0));
        content.add(coupHdr);

        // ── Coupon card grid ─────────────────────────────────────────────
        List<Coupon> coupons = FileManager.findCouponsByCustomerId(main.currentCustomer.getUserId());

        // Filter out expired and used coupons — expired ones are cleaned up at startup,
        // and used ones are no longer relevant to the customer
        coupons.removeIf(cp -> cp.isExpired() || cp.isUsed());

        if (coupons.isEmpty()) {
            JLabel empty = new JLabel("No coupons yet. Reach BRONZE tier to receive monthly coupons!");
            empty.setFont(UIUtils.uiFont(Font.ITALIC, 12));
            empty.setForeground(UIUtils.DIM_TEXT);
            content.add(empty);
        } else {
            JPanel cardGrid = new JPanel(new GridLayout(0, 4, 12, 12));
            cardGrid.setBackground(UIUtils.DARK_NAVY);
            for (Coupon cp : coupons) {
                cardGrid.add(buildCouponCard(cp));
            }
            JScrollPane couponScroll = new JScrollPane(cardGrid,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            couponScroll.setPreferredSize(new Dimension(-1, 220));
            couponScroll.setBorder(null);
            couponScroll.getViewport().setBackground(UIUtils.DARK_NAVY);
            UIUtils.styleScrollPane(couponScroll);
            content.add(couponScroll);
        }

        JScrollPane outer = new JScrollPane(content);
        outer.getVerticalScrollBar().setUnitIncrement(16);
        UIUtils.styleScrollPane(outer);
        panel.add(outer, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    //  Coupon card (matches Products page tile style)
    // =========================================================================
    private JPanel buildCouponCard(Coupon cp) {
        boolean used    = cp.isUsed();
        boolean expired = cp.getExpiryDate() != null
                && cp.getExpiryDate().compareTo(java.time.LocalDate.now().toString()) < 0;
        boolean invalid = used || expired;
        String  status  = used ? "USED" : (expired ? "EXPIRED" : "AVAILABLE");

        Color borderCol  = invalid ? new Color(80, 40, 40) : new Color(50, 85, 150);
        Color cardBg     = new Color(30, 48, 88);
        Color hoverBg    = new Color(38, 62, 112);

        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(cardBg);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(borderCol, 12, 1),
                new EmptyBorder(0, 0, 0, 0)));

        // ── Top visual area: gradient + value ─────────────────────────────
        JPanel top = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                Color g1 = invalid ? new Color(60, 30, 30) : new Color(28, 55, 110);
                Color g2b = invalid ? new Color(30, 20, 20) : new Color(18, 35, 72);
                g2.setPaint(new GradientPaint(0, 0, g1, 0, h, g2b));
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        top.setOpaque(false);
        top.setPreferredSize(new Dimension(0, 100));

        // Coupon value (big)
        String valStr = "FIXED".equals(cp.getDiscountType())
                ? "RM " + String.format("%.0f", cp.getDiscountValue())
                : cp.getDiscountValue() + "% OFF";
        JLabel valLbl = new JLabel(valStr, JLabel.CENTER);
        valLbl.setFont(UIUtils.uiFont(Font.BOLD, 18));
        valLbl.setForeground(invalid ? new Color(140, 100, 100) : CustomerDashboard.ACCENT_YELLOW);

        // Status badge (top-right overlay)
        Color badgeBg = "AVAILABLE".equals(status) ? new Color(15, 60, 30)
                : (used ? new Color(60, 30, 30) : new Color(70, 40, 10));
        Color badgeFg = "AVAILABLE".equals(status) ? new Color(70, 210, 110)
                : (used ? new Color(200, 80, 80) : new Color(230, 150, 60));
        JLabel badge = new JLabel(status);
        badge.setFont(UIUtils.uiFont(Font.BOLD, 9));
        badge.setOpaque(true);
        badge.setBackground(badgeBg);
        badge.setForeground(badgeFg);
        badge.setBorder(new EmptyBorder(2, 7, 2, 7));

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);
        top.add(badgeWrap, new GridBagConstraints() {{
            gridx = 0; gridy = 0; anchor = GridBagConstraints.NORTHEAST;
            weightx = 1; weighty = 1; fill = GridBagConstraints.HORIZONTAL;
        }});

        top.add(valLbl, new GridBagConstraints() {{
            gridx = 0; gridy = 0; anchor = GridBagConstraints.CENTER;
        }});

        // ── Body: code, expiry, issued ────────────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(cardBg);
        body.setBorder(new EmptyBorder(10, 12, 12, 12));

        JLabel codeLbl = new JLabel(cp.getCode());
        codeLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        codeLbl.setForeground(invalid ? UIUtils.DIM_TEXT : UIUtils.WHITE);
        codeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel expLbl = new JLabel("Expires: " + (cp.getExpiryDate() != null ? cp.getExpiryDate() : "—"));
        expLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10));
        expLbl.setForeground(UIUtils.DIM_TEXT);
        expLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        expLbl.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel dateLbl = new JLabel("Issued: " + cp.getIssuedDate() + " · " + cp.getIssuedBy());
        dateLbl.setFont(UIUtils.uiFont(Font.PLAIN, 10));
        dateLbl.setForeground(UIUtils.DIM_TEXT);
        dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateLbl.setBorder(new EmptyBorder(2, 0, 0, 0));

        body.add(codeLbl);
        body.add(expLbl);
        body.add(dateLbl);

        card.add(top,  BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverBg);
                body.setBackground(hoverBg);
                card.setBorder(BorderFactory.createCompoundBorder(
                        new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 2),
                        new EmptyBorder(0, 0, 0, 0)));
                card.repaint();
            }
            public void mouseExited(MouseEvent e) {
                card.setBackground(cardBg);
                body.setBackground(cardBg);
                card.setBorder(BorderFactory.createCompoundBorder(
                        new UIUtils.RoundedBorder(borderCol, 12, 1),
                        new EmptyBorder(0, 0, 0, 0)));
                card.repaint();
            }
        });

        return card;
    }

}
