package asc.util;

import asc.model.User;
import asc.model.VipAccount;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

public class UIUtils {

    // ── Colour palette ────────────────────────────────────────────────────────
    public static final Color DARK_NAVY    = new Color(18,  32,  62);
    public static final Color MEDIUM_NAVY  = new Color(28,  46,  88);
    public static final Color TABLE_BG     = new Color(36,  58, 102);
    public static final Color ACCENT_BLUE  = new Color(70,  140, 215);
    public static final Color ACCENT_HOVER = new Color(90,  160, 235);
    public static final Color SUCCESS_GREEN= new Color(50,  170, 105);
    public static final Color ERROR_RED    = new Color(210,  70,  70);
    public static final Color WARN_YELLOW  = new Color(255, 200,  80);
    public static final Color PURPLE       = new Color(160, 105, 230);
    public static final Color GREEN_OK     = new Color(75,  205, 125);
    public static final Color WHITE        = Color.WHITE;
    public static final Color LIGHT_GRAY   = new Color(210, 220, 235);
    public static final Color DIM_TEXT     = new Color(150, 170, 200);
    public static final Color TABLE_HEADER = new Color(20,  35,  65);

    // VIP tier colours (match VipAccount.tierColor())
    public static final Color VIP_BRONZE    = new Color(205, 127,  50);
    public static final Color VIP_GOLD      = new Color(255, 200,  60);
    public static final Color VIP_BLACKGOLD = new Color(255, 215,   0);

    private static final Color POPUP_BG      = new Color(32,  52,  96);
    private static final Color POPUP_HOVER   = new Color(50,  78, 138);
    private static final Color POPUP_BORDER  = new Color(60,  95, 165);
    private static final Color POPUP_DIVIDER = new Color(50,  80, 140);

    // ── Font ──────────────────────────────────────────────────────────────────
    public static Font uiFont(int style, int size) {
        for (String name : new String[]{"Segoe UI Symbol", "Segoe UI Emoji", "Segoe UI", "SF Pro Display", "Helvetica Neue", "Arial"}) {
            Font f = new Font(name, style, size);
            if (!f.getFamily().equals("Dialog")) return f;
        }
        return new Font(Font.SANS_SERIF, style, size);
    }

    /** Font that can render Unicode symbols — delegates to uiFont which now prefers Segoe UI Symbol. */
    public static Font uiSymbolFont(int style, int size) {
        return uiFont(style, size);
    }

    // ── Logo panel ────────────────────────────────────────────────────────────
    private static Image logoImage = null;
    private static boolean logoLoaded = false;

    private static void loadLogo() {
        if (logoLoaded) return;
        logoLoaded = true;
        try {
            java.io.File f = new java.io.File("data/Picture/NavBar/Java logo.png");
            if (f.exists()) {
                logoImage = ImageIO.read(f);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Creates a bulletproof logo panel that paints the logo image directly via
     * paintComponent and enforces its size through getPreferredSize/getMinimumSize/getMaximumSize.
     * This bypasses all layout manager sizing issues that plague JLabel+ImageIcon.
     */
    public static JPanel createLogoPanel(int w, int h) {
        loadLogo();
        final Image img = logoImage;
        final int pw = w, ph = h;

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (img != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                    g2.dispose();
                }
            }

            @Override
            public Dimension getPreferredSize() { return new Dimension(pw, ph); }
            @Override
            public Dimension getMinimumSize()   { return new Dimension(pw, ph); }
            @Override
            public Dimension getMaximumSize()   { return new Dimension(pw, ph); }
        };
        panel.setOpaque(false);
        return panel;
    }

    /** Creates a header panel with a subtle vertical gradient to visually
     *  separate the top navbar from the identically-coloured sidebar. */
    public static JPanel createGradientHeader() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Left=dark → Right=medium (MEDIUM_NAVY)
                GradientPaint gp = new GradientPaint(0, 0, new Color(18, 34, 68),
                        getWidth(), 0, MEDIUM_NAVY);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    // ── Rounded border ────────────────────────────────────────────────────────
    public static class RoundedBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final Color color; private final int radius, thickness;
        public RoundedBorder(Color color, int radius, int thickness) {
            this.color = color; this.radius = radius; this.thickness = thickness;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.setStroke(new BasicStroke(thickness));
            // offset by 1 so the stroke isn't clipped at the component edge
            int off = Math.max(1, thickness);
            g2.drawRoundRect(x + off, y + off, w - off * 2, h - off * 2, radius, radius);
            g2.dispose();
        }
        // insets must be >= offset used in paintBorder so the border is never clipped
        private int inset() { return Math.max(1, thickness) + 1; }
        @Override public Insets getBorderInsets(Component c) { int i = inset(); return new Insets(i, i, i, i); }
        @Override public Insets getBorderInsets(Component c, Insets ins) { int i = inset(); ins.set(i, i, i, i); return ins; }
    }

    // ── Rounded button ────────────────────────────────────────────────────────
    public static class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color base; private Color current; private static final int ARC = 5;
        public RoundedButton(String text, Color bg) {
            super(text); this.base = bg; this.current = bg;
            setFont(uiFont(Font.BOLD, 12)); setForeground(WHITE);
            setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
            setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e)  { current = brighten(base, 25);  repaint(); }
                public void mouseExited (MouseEvent e)  { current = base;                 repaint(); }
                public void mousePressed(MouseEvent e)  { current = brighten(base, -20); repaint(); }
                public void mouseReleased(MouseEvent e) { current = base;                 repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(current); g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
            g2.dispose(); super.paintComponent(g);
        }
        private static Color brighten(Color c, int d) { return new Color(clamp(c.getRed()+d),clamp(c.getGreen()+d),clamp(c.getBlue()+d)); }
        private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    }

    // ── Component styling ─────────────────────────────────────────────────────
    public static void styleButton(JButton btn, Color color) {
        btn.setBackground(color); btn.setForeground(WHITE); btn.setFont(uiFont(Font.BOLD, 12));
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 32));
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.addMouseListener(new MouseAdapter() {
            Color base = color;
            public void mouseEntered(MouseEvent e) { btn.setBackground(brighten(base, 20)); btn.repaint(); }
            public void mouseExited (MouseEvent e) { btn.setBackground(base);               btn.repaint(); }
        });
    }

    /** Overload: style with default accent colour. */
    public static void styleButton(JButton btn) {
        styleButton(btn, ACCENT_BLUE);
    }

    private static Color brighten(Color c, int d) { return new Color(clamp(c.getRed()+d),clamp(c.getGreen()+d),clamp(c.getBlue()+d)); }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    // Row colours (alternating)
    public static final Color ROW_ODD  = new Color(240, 245, 255); // very light blue-white
    public static final Color ROW_EVEN = new Color(225, 233, 248); // slightly deeper
    public static final Color ROW_TEXT = new Color(15,  25,  50);  // near-black for readability

    public static JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (c instanceof JLabel) ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? ROW_ODD : ROW_EVEN);
                    c.setForeground(ROW_TEXT);
                } else {
                    c.setBackground(ACCENT_BLUE);
                    c.setForeground(WHITE);
                }
                return c;
            }
        };
        table.setBackground(ROW_ODD); table.setForeground(ROW_TEXT); table.setFont(uiFont(Font.PLAIN, 12));
        table.setRowHeight(28); table.setSelectionBackground(ACCENT_BLUE); table.setSelectionForeground(WHITE);
        table.setGridColor(new Color(190, 205, 230)); table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(true); table.setShowVerticalLines(false);
        // Default cell renderer – black text, alternating rows handled by prepareRenderer
        javax.swing.table.DefaultTableCellRenderer center = new javax.swing.table.DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        center.setBackground(ROW_ODD); center.setForeground(ROW_TEXT);
        for (int i = 0; i < model.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(center);
        // Header – fully custom renderer, no super call, works on all L&Fs
        javax.swing.table.TableCellRenderer headerRenderer = new javax.swing.table.TableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel lbl = new JLabel(value == null ? "" : value.toString(), SwingConstants.CENTER);
                lbl.setBackground(TABLE_HEADER);
                lbl.setForeground(WHITE);
                lbl.setFont(uiFont(Font.BOLD, 12));
                lbl.setOpaque(true);
                return lbl;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);
        table.getTableHeader().setBackground(TABLE_HEADER);
        table.getTableHeader().setOpaque(true);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_BLUE));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        return table;
    }

    public static void styleScrollPane(JScrollPane sp) {
        sp.getViewport().setBackground(TABLE_BG);
        sp.setBorder(new RoundedBorder(ACCENT_BLUE, 10, 1));
        sp.getVerticalScrollBar().setBackground(MEDIUM_NAVY);
        sp.getHorizontalScrollBar().setBackground(MEDIUM_NAVY);
    }

    public static void styleTextField(JTextField field) {
        field.setFont(uiFont(Font.PLAIN, 12));
        field.setBackground(new Color(46, 68, 118)); field.setForeground(WHITE); field.setCaretColor(WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(ACCENT_BLUE, 8, 1), new EmptyBorder(5, 9, 5, 9)));
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setBackground(new Color(46, 68, 118)); combo.setForeground(WHITE);
        combo.setFont(uiFont(Font.PLAIN, 12)); combo.setBorder(new RoundedBorder(ACCENT_BLUE, 8, 1));
    }

    public static JComboBox<String> createGenderCombo(String currentGender) {
        JComboBox<String> cb = new JComboBox<>(new String[]{"-- Choose --", "Male", "Female"});
        styleCombo(cb);
        if ("Male".equalsIgnoreCase(currentGender))        cb.setSelectedItem("Male");
        else if ("Female".equalsIgnoreCase(currentGender)) cb.setSelectedItem("Female");
        else                                                cb.setSelectedIndex(0);
        return cb;
    }

    public static String resolveGender(JComboBox<String> cb) {
        String sel = (String) cb.getSelectedItem();
        return ("-- Choose --".equals(sel) || sel == null) ? "" : sel;
    }

    // ── Background image helpers ───────────────────────────────────────────────
    public static String saveBgImage(String userId, String role, File srcFile) {
        String dir = "data/Picture/" + role + "/";
        new File(dir).mkdirs();
        String lower = srcFile.getName().toLowerCase();
        String ext = (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ? "jpg" : "png";
        for (String e : new String[]{"png","jpg","jpeg"}) {
            File f = new File(dir + "bg_" + userId + "." + e); if (f.exists()) f.delete();
            for (int n = 1; n <= 50; n++) { File sf = new File(dir+"bg_"+userId+"_"+n+"."+e); if (sf.exists()) sf.delete(); else break; }
        }
        File dest = new File(dir + "bg_" + userId + "." + ext);
        if (dest.exists()) { int n=1; while (dest.exists()) { dest=new File(dir+"bg_"+userId+"_"+n++ +"."+ext); } }
        try { Files.copy(srcFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING); return dest.getPath().replace("\\","/"); }
        catch (IOException e) { System.err.println("Background save error: "+e.getMessage()); return null; }
    }

    public static void deleteBgImage(String userId, String role) {
        String dir = "data/Picture/" + role + "/";
        for (String e : new String[]{"png","jpg","jpeg"}) {
            File f = new File(dir+"bg_"+userId+"."+e); if (f.exists()) f.delete();
            for (int n=1; n<=50; n++) { File sf=new File(dir+"bg_"+userId+"_"+n+"."+e); if (sf.exists()) sf.delete(); else break; }
        }
    }

    public static Image loadBgImage(String userId, String role) {
        String path = getBgImagePath(userId, role);
        if (path == null) return null;
        try { return ImageIO.read(new File(path)); } catch (Exception ex) { return null; }
    }

    public static String getBgImagePath(String userId, String role) {
        String dir = "data/Picture/" + role + "/", base = "bg_" + userId;
        for (String e : new String[]{"png","jpg","jpeg"}) {
            File f = new File(dir+base+"."+e); if (f.exists()) return f.getPath().replace("\\","/");
            for (int n=1; n<=50; n++) { File sf=new File(dir+base+"_"+n+"."+e); if (sf.exists()) return sf.getPath().replace("\\","/"); }
        }
        return null;
    }

    // ── Avatar helpers ────────────────────────────────────────────────────────
    private static final String AVATAR_DIR            = "data/avatars/";
    private static final String DEFAULT_AVATAR_MALE   = "data/avatars/boy.jpeg";
    private static final String DEFAULT_AVATAR_FEMALE = "data/avatars/girl.jpeg";
    private static final String DEFAULT_AVATAR_ANON   = "data/avatars/anonymous.jpg";
    private static final int    AVATAR_SIZE            = 48;

    public static File getAvatarFile(String userId) {
        if (userId != null) {
            for (String ext : new String[]{"png","jpg","jpeg"}) {
                File f = new File(AVATAR_DIR + userId + "." + ext); if (f.exists()) return f;
            }
        }
        return null;
    }

    public static boolean saveAvatar(String userId, File srcFile) {
        try {
            new File(AVATAR_DIR).mkdirs();
            for (String ext : new String[]{"png","jpg","jpeg"}) { File old=new File(AVATAR_DIR+userId+"."+ext); if (old.exists()) old.delete(); }
            String name = srcFile.getName().toLowerCase();
            String ext  = (name.endsWith(".jpg")||name.endsWith(".jpeg")) ? "jpg" : "png";
            Files.copy(srcFile.toPath(), new File(AVATAR_DIR+userId+"."+ext).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) { System.err.println("Avatar save error: "+e.getMessage()); return false; }
    }

    public static ImageIcon loadCircularAvatar(String userId, String gender, int diameter) {
        File f = getAvatarFile(userId);
        if (f == null) {
            if ("Male".equalsIgnoreCase(gender))   { File d=new File(DEFAULT_AVATAR_MALE);   if (d.exists()) f=d; }
            else if ("Female".equalsIgnoreCase(gender)) { File d=new File(DEFAULT_AVATAR_FEMALE); if (d.exists()) f=d; }
            else { File d=new File(DEFAULT_AVATAR_ANON); if (d.exists()) f=d; }
        }
        BufferedImage src = null;
        if (f != null) { try { src = ImageIO.read(f); } catch (IOException ignored) {} }
        BufferedImage result = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setClip(new Ellipse2D.Float(0, 0, diameter, diameter));
        if (src != null) { g2.drawImage(src, 0, 0, diameter, diameter, null); }
        else {
            g2.setPaint(new GradientPaint(0,0,new Color(80,130,200),diameter,diameter,new Color(35,65,135)));
            g2.fillOval(0,0,diameter,diameter);
            g2.setColor(new Color(200,215,235));
            int hd=diameter*2/5, hx=(diameter-hd)/2, hy=diameter/8;
            g2.fillOval(hx,hy,hd,hd); g2.fillOval((diameter-diameter*3/5)/2,hy+hd-2,diameter*3/5,diameter*2/5);
        }
        g2.dispose();
        Graphics2D g3 = result.createGraphics();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.setColor(new Color(100,160,235,200)); g3.setStroke(new BasicStroke(2f));
        g3.drawOval(1,1,diameter-2,diameter-2); g3.dispose();
        return new ImageIcon(result);
    }

    /** Create a circular avatar directly from an in-memory BufferedImage (no disk read). */
    public static ImageIcon loadCircularAvatarFromImage(BufferedImage src, int diameter) {
        BufferedImage result = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setClip(new Ellipse2D.Float(0, 0, diameter, diameter));
        g2.drawImage(src, 0, 0, diameter, diameter, null);
        g2.dispose();
        Graphics2D g3 = result.createGraphics();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.setColor(new Color(100, 160, 235, 200)); g3.setStroke(new BasicStroke(2f));
        g3.drawOval(1, 1, diameter - 2, diameter - 2); g3.dispose();
        return new ImageIcon(result);
    }

    public static ImageIcon loadCircularAvatar(String userId, int diameter) { return loadCircularAvatar(userId, "", diameter); }
    public static ImageIcon loadCircularAvatar(String userId) { return loadCircularAvatar(userId, "", AVATAR_SIZE); }

    /** Crops any Image into a circular icon of the given diameter. */
    public static ImageIcon cropToCircle(Image src, int diameter) {
        BufferedImage result = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setClip(new Ellipse2D.Float(0, 0, diameter, diameter));
        g2.drawImage(src, 0, 0, diameter, diameter, null);
        g2.dispose();
        return new ImageIcon(result);
    }

    // ── Thumbnail path helper ──────────────────────────────────────────────────
    /** Convert original path to _thumb version, e.g. "foo/bar.png" → "foo/bar_thumb.png" */
    public static String thumbPath(String originalPath) {
        if (originalPath == null || originalPath.isEmpty()) return originalPath;
        int dot = originalPath.lastIndexOf('.');
        if (dot > 0) return originalPath.substring(0, dot) + "_thumb" + originalPath.substring(dot);
        return originalPath + "_thumb";
    }

    // ── Notification bell ─────────────────────────────────────────────────────
    private static final String BELL_ICON_DEFAULT = "data/Picture/NavBar/Notifications.png";
    private static final String BELL_ICON_ACTIVE  = "data/Picture/NavBar/Notifications + star.png";

    /**
     * Creates a notification bell label using image icons.
     * Default icon when no unread notifications; active icon (with star) when unread > 0.
     * Call refreshBellIcon() after notification state changes (e.g. popup closes).
     */
    public static JLabel createNotificationBell(Supplier<Integer> badgeSupplier, Runnable onClick) {
        JLabel bell = new JLabel();
        bell.setPreferredSize(new Dimension(24, 24));
        bell.setHorizontalAlignment(JLabel.CENTER);
        bell.setVerticalAlignment(JLabel.CENTER);
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBellIcon(bell, badgeSupplier);
        if (onClick != null) bell.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        return bell;
    }

    /** Refresh the bell icon and tooltip based on current unread count. */
    public static void refreshBellIcon(JLabel bell, Supplier<Integer> badgeSupplier) {
        int count = badgeSupplier != null ? badgeSupplier.get() : 0;
        String path = count > 0 ? BELL_ICON_ACTIVE : BELL_ICON_DEFAULT;
        ImageIcon icon = loadMenuIcon(path, 24, 24);
        if (icon != null) bell.setIcon(icon);
        bell.setToolTipText(count > 0 ? count + " unread notification(s)" : "No new notifications");
    }

    /**
     * Builds the full header-right widget:
     *   [🔔 bell]  [username / (optional VIP)]  [avatar]
     * All in a horizontal FlowLayout panel, ready to add to EAST of the header.
     */
    public static JPanel createHeaderRightPanel(
            String userId, String gender, String username,
            String userEmail, Supplier<Integer> notifSupplier,
            JFrame parentFrame,
            Runnable onMyProfile, Runnable onMyVehicle, Runnable onLogout,
            boolean showVip) {
        return createHeaderRightPanel(userId, gender, username, userEmail, notifSupplier,
                parentFrame, onMyProfile, onMyVehicle, onLogout, showVip, null);
    }

    /** Extended variant with onCustomerSupport callback (Customer role only). */
    public static JPanel createHeaderRightPanel(
            String userId, String gender, String username,
            String userEmail, Supplier<Integer> notifSupplier,
            JFrame parentFrame,
            Runnable onMyProfile, Runnable onMyVehicle, Runnable onLogout,
            boolean showVip,
            Runnable onCustomerSupport) {

        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        // ── Live clock (top row, right-aligned) ──────────────────────────
        JLabel clockLabel = new JLabel();
        clockLabel.setFont(uiFont(Font.PLAIN, 11));
        clockLabel.setForeground(DIM_TEXT);
        clockLabel.setHorizontalAlignment(JLabel.RIGHT);
        DateTimeFormatter clockFmt = DateTimeFormatter.ofPattern(
                "EEEE, dd MMMM yyyy  |  hh:mm:ss a", Locale.ENGLISH);
        javax.swing.Timer clockTimer = new javax.swing.Timer(1000, e ->
                clockLabel.setText(LocalDateTime.now().format(clockFmt)));
        clockTimer.setInitialDelay(0);
        clockTimer.start();

        // ── Bell + userInfo + avatar row ──────────────────────────────────
        final JLabel[] bellRef = new JLabel[1];
        bellRef[0] = createNotificationBell(notifSupplier, () -> {
            showNotificationPopup(parentFrame, userId, () -> {
                refreshBellIcon(bellRef[0], notifSupplier); // refresh icon after popup closes
            });
        });
        JLabel bell = bellRef[0];

        JPanel userInfo = new JPanel();
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
        userInfo.setOpaque(false);

        JLabel usernameLbl = new JLabel(username);
        usernameLbl.setFont(uiFont(Font.BOLD, 14));
        usernameLbl.setForeground(WHITE);
        usernameLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);

        userInfo.add(usernameLbl);

        if (showVip) {
            VipAccount vip = asc.util.FileManager.findVipByCustomerId(userId);
            JLabel vipLbl;
            if (vip != null && !VipAccount.TIER_NONE.equals(vip.getTier())) {
                vipLbl = new JLabel(vip.tierBadge());
                vipLbl.setFont(uiFont(Font.BOLD, 11));
                vipLbl.setForeground(vip.tierColor());
            } else {
                vipLbl = new JLabel(vip != null ? "VIP: NONE" : "No VIP");
                vipLbl.setFont(uiFont(Font.PLAIN, 11));
                vipLbl.setForeground(DIM_TEXT);
            }
            vipLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
            userInfo.add(Box.createVerticalStrut(1));
            userInfo.add(vipLbl);
        }

        JLabel avatar = createAvatarMenuLabel(userId, gender, username, userEmail,
                parentFrame, onMyProfile, onMyVehicle, onLogout, showVip, onCustomerSupport);

        JPanel avatarRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        avatarRow.setOpaque(false);
        avatarRow.add(bell);
        avatarRow.add(userInfo);
        avatarRow.add(avatar);

        p.add(clockLabel, BorderLayout.NORTH);
        p.add(Box.createVerticalStrut(6), BorderLayout.CENTER);
        p.add(avatarRow,  BorderLayout.SOUTH);
        return p;
    }

    /** Staff/technician variant (no vehicle, no VIP). */
    public static JPanel createHeaderRightPanel(
            String userId, String gender, String username,
            String userEmail, Supplier<Integer> notifSupplier,
            JFrame parentFrame,
            Runnable onMyProfile, Runnable onLogout) {
        return createHeaderRightPanel(userId, gender, username, userEmail, notifSupplier,
                parentFrame, onMyProfile, null, onLogout, false);
    }

    // ── Avatar menu popup ─────────────────────────────────────────────────────
    private static final AtomicReference<JWindow> activePopup = new AtomicReference<>(null);
    private static AWTEventListener globalClickListener = null;

    /** Standard 2-action variant (no My Vehicle). */
    public static JLabel createAvatarMenuLabel(String userId, String gender,
                                                String userName, String userEmail,
                                                JFrame parentFrame,
                                                Runnable onMyProfile, Runnable onLogout) {
        return createAvatarMenuLabel(userId, gender, userName, userEmail, parentFrame, onMyProfile, null, onLogout);
    }

    /**
     * Full variant: the popup shows the user's name, VIP tier badge (only for
     * customers), an optional My Vehicle item, optional Customer Support,
     * and Log out.
     * Pass showVip=true only for CustomerDashboard.
     */
    public static JLabel createAvatarMenuLabel(String userId, String gender,
                                                String userName, String userEmail,
                                                JFrame parentFrame,
                                                Runnable onMyProfile,
                                                Runnable onMyVehicle,
                                                Runnable onLogout,
                                                boolean showVip) {
        return createAvatarMenuLabel(userId, gender, userName, userEmail,
                parentFrame, onMyProfile, onMyVehicle, onLogout, showVip, null);
    }

    /**
     * Extended variant with onCustomerSupport callback.
     * When non-null, a "Customer Support" menu item is shown above "Log out".
     */
    public static JLabel createAvatarMenuLabel(String userId, String gender,
                                                String userName, String userEmail,
                                                JFrame parentFrame,
                                                Runnable onMyProfile,
                                                Runnable onMyVehicle,
                                                Runnable onLogout,
                                                boolean showVip,
                                                Runnable onCustomerSupport) {
        JLabel label = new JLabel(loadCircularAvatar(userId, gender, AVATAR_SIZE));
        label.setToolTipText("Click to open menu");
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        label.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JWindow prev = activePopup.get();
                if (prev != null) { closePopup(); return; }
                showAvatarPopup(label, userId, gender, userName, userEmail,
                        parentFrame, onMyProfile, onMyVehicle, onLogout, showVip,
                        () -> label.setIcon(loadCircularAvatar(userId, gender, AVATAR_SIZE)),
                        onCustomerSupport);
            }
        });
        return label;
    }

    /** Backward-compat: no vehicle, no VIP (staff roles). */
    public static JLabel createAvatarMenuLabel(String userId, String gender,
                                                String userName, String userEmail,
                                                JFrame parentFrame,
                                                Runnable onMyProfile,
                                                Runnable onMyVehicle,
                                                Runnable onLogout) {
        return createAvatarMenuLabel(userId, gender, userName, userEmail,
                parentFrame, onMyProfile, onMyVehicle, onLogout, false);
    }

    public static JLabel createAvatarMenuLabel(String userId, String userName,
                                                String userEmail, JFrame parentFrame,
                                                Runnable onMyProfile, Runnable onLogout) {
        return createAvatarMenuLabel(userId, "", userName, userEmail, parentFrame, onMyProfile, onLogout);
    }

    private static void showAvatarPopup(JLabel avatarLabel,
                                         String userId, String gender,
                                         String userName, String userEmail,
                                         JFrame parentFrame,
                                         Runnable onMyProfile, Runnable onMyVehicle, Runnable onLogout,
                                         boolean showVip,
                                         Runnable onAvatarChanged,
                                         Runnable onCustomerSupport) {
        closePopup();
        JWindow popup = new JWindow(parentFrame);
        popup.setBackground(new Color(0, 0, 0, 0));
        activePopup.set(popup);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(POPUP_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(POPUP_BORDER, 12, 1), new EmptyBorder(0, 0, 6, 0)));

        // ── Info section ─────────────────────────────────────────────────────
        JPanel infoSection = new JPanel(new BorderLayout(12, 0));
        infoSection.setBackground(POPUP_BG);
        infoSection.setBorder(new EmptyBorder(14, 16, 12, 16));
        infoSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel popupAvatar = new JLabel(loadCircularAvatar(userId, gender, 46));
        popupAvatar.setPreferredSize(new Dimension(46, 46));
        popupAvatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        popupAvatar.setToolTipText("Click to change photo");
        popupAvatar.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Choose Profile Picture");
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files", "png","jpg","jpeg"));
                if (fc.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
                    File srcFile = fc.getSelectedFile();
                    try {
                        BufferedImage src = ImageIO.read(srcFile);
                        if (src == null) {
                            JOptionPane.showMessageDialog(parentFrame, "Cannot read image file.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        // Update immediately from in-memory image
                        popupAvatar.setIcon(loadCircularAvatarFromImage(src, 46));
                        popupAvatar.revalidate(); popupAvatar.repaint();
                        if (saveAvatar(userId, srcFile)) {
                            if (onAvatarChanged != null) onAvatarChanged.run();
                        } else {
                            JOptionPane.showMessageDialog(parentFrame, "Failed to save photo.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(parentFrame, "Cannot read image file.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        JPanel textInfo = new JPanel();
        textInfo.setLayout(new BoxLayout(textInfo, BoxLayout.Y_AXIS));
        textInfo.setBackground(POPUP_BG);

        JLabel nameLbl  = new JLabel(userName);
        nameLbl.setFont(uiFont(Font.BOLD, 14)); nameLbl.setForeground(WHITE);
        JLabel emailLbl = new JLabel(userEmail);
        emailLbl.setFont(uiFont(Font.PLAIN, 11)); emailLbl.setForeground(new Color(165, 190, 225));

        textInfo.add(nameLbl);
        textInfo.add(Box.createVerticalStrut(2));
        textInfo.add(emailLbl);

        // ── VIP badge (only for Customer role) ────────────────────────────
        if (showVip) {
            VipAccount vip = asc.util.FileManager.findVipByCustomerId(userId);
            JLabel vipLbl;
            if (vip != null && !VipAccount.TIER_NONE.equals(vip.getTier())) {
                vipLbl = new JLabel(vip.tierBadge());
                vipLbl.setFont(uiFont(Font.BOLD, 11));
                vipLbl.setForeground(vip.tierColor());
            } else {
                vipLbl = new JLabel("No VIP Tier");
                vipLbl.setFont(uiFont(Font.ITALIC, 10));
                vipLbl.setForeground(DIM_TEXT);
            }
            textInfo.add(Box.createVerticalStrut(2));
            textInfo.add(vipLbl);
        }

        infoSection.add(popupAvatar, BorderLayout.WEST);
        infoSection.add(textInfo,    BorderLayout.CENTER);
        card.add(infoSection);
        card.add(makeDivider());

        // ── My Profile ───────────────────────────────────────────────────────
        JPanel profileRow = makeMenuRow("My Profile", false,
                loadMenuIcon("data/Picture/ProfileSystem/My Profile.png", 16, 16));
        profileRow.addMouseListener(new MenuRowHover(profileRow) {
            @Override public void mouseClicked(MouseEvent e) { closePopup(); if (onMyProfile != null) onMyProfile.run(); }
        });
        card.add(profileRow);

        // ── My Vehicle (optional) ─────────────────────────────────────────────
        if (onMyVehicle != null) {
            card.add(makeDivider());
            JPanel vehicleRow = makeMenuRow("My Vehicle", false,
                    loadMenuIcon("data/Picture/NavBar/Vehicle Logo.png", 16, 16));
            vehicleRow.addMouseListener(new MenuRowHover(vehicleRow) {
                @Override public void mouseClicked(MouseEvent e) { closePopup(); onMyVehicle.run(); }
            });
            card.add(vehicleRow);
        }

        card.add(makeDivider());

        // ── Customer Support (optional, above Log out) ──────────────────────
        if (onCustomerSupport != null) {
            JPanel supportRow = makeMenuRow("Customer Support", false,
                    loadMenuIcon("data/Picture/NavBar/customer-service.png", 16, 16));
            supportRow.addMouseListener(new MenuRowHover(supportRow) {
                @Override public void mouseClicked(MouseEvent e) { closePopup(); onCustomerSupport.run(); }
            });
            card.add(supportRow);
            card.add(makeDivider());
        }

        // ── Log out ───────────────────────────────────────────────────────────
        JPanel logoutRow = makeMenuRow("Log out", true,
                loadMenuIcon("data/Picture/ProfileSystem/Log out.png", 16, 16));
        logoutRow.addMouseListener(new MenuRowHover(logoutRow) {
            @Override public void mouseClicked(MouseEvent e) { closePopup(); if (onLogout != null) onLogout.run(); }
        });
        card.add(logoutRow);

        popup.getContentPane().add(card);
        popup.pack();
        Point p = avatarLabel.getLocationOnScreen();
        popup.setLocation(p.x + avatarLabel.getWidth() - popup.getWidth(), p.y + avatarLabel.getHeight() + 6);
        popup.setVisible(true);

        if (globalClickListener != null) { Toolkit.getDefaultToolkit().removeAWTEventListener(globalClickListener); globalClickListener = null; }
        globalClickListener = new AWTEventListener() {
            @Override public void eventDispatched(AWTEvent event) {
                if (event instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) event;
                    if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                        JWindow current = activePopup.get();
                        if (current == null) { Toolkit.getDefaultToolkit().removeAWTEventListener(this); globalClickListener = null; return; }
                        Component src = me.getComponent();
                        if (src != null) { Window w = SwingUtilities.getWindowAncestor(src); if (w == current) return; }
                        Toolkit.getDefaultToolkit().removeAWTEventListener(this); globalClickListener = null;
                        SwingUtilities.invokeLater(() -> closePopup());
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(globalClickListener, AWTEvent.MOUSE_EVENT_MASK);
        popup.addWindowFocusListener(new WindowFocusListener() {
            @Override public void windowGainedFocus(WindowEvent e) {}
            @Override public void windowLostFocus(WindowEvent e)  { SwingUtilities.invokeLater(() -> closePopup()); }
        });
    }

    private static void closePopup() {
        JWindow w = activePopup.getAndSet(null);
        if (w != null) { w.setVisible(false); w.dispose(); }
        if (globalClickListener != null) { Toolkit.getDefaultToolkit().removeAWTEventListener(globalClickListener); globalClickListener = null; }
    }

    public static ImageIcon loadMenuIcon(String path, int w, int h) {
        try {
            File f = new File(path); if (!f.exists()) return null;
            Image img = new ImageIcon(f.getAbsolutePath()).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception ex) { return null; }
    }

    private static JPanel makeMenuRow(String text, boolean isDanger, ImageIcon icon) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(POPUP_BG); row.setBorder(new EmptyBorder(10, 16, 10, 20));
        row.setAlignmentX(Component.LEFT_ALIGNMENT); row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel lbl = new JLabel(text); lbl.setFont(uiFont(Font.PLAIN, 13));
        lbl.setForeground(isDanger ? new Color(230, 100, 100) : WHITE);
        if (icon != null) { JLabel iconLbl = new JLabel(icon); iconLbl.setPreferredSize(new Dimension(16,16)); row.add(iconLbl, BorderLayout.WEST); }
        row.add(lbl, BorderLayout.CENTER);
        return row;
    }

    private static JSeparator makeDivider() {
        JSeparator sep = new JSeparator(); sep.setForeground(POPUP_DIVIDER); sep.setBackground(POPUP_DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); return sep;
    }

    private abstract static class MenuRowHover extends MouseAdapter {
        private final JPanel row; MenuRowHover(JPanel r) { this.row = r; }
        @Override public void mouseEntered(MouseEvent e) { row.setBackground(POPUP_HOVER); row.repaint(); }
        @Override public void mouseExited (MouseEvent e) { row.setBackground(POPUP_BG);    row.repaint(); }
    }

    // ── Profile dialog avatar ─────────────────────────────────────────────────
    public static JLabel createProfileAvatarLabel(String userId, String gender, int diameter,
                                                   Component parent, Runnable onChanged) {
        JLabel[] ref = {null};
        ref[0] = new JLabel(loadCircularAvatar(userId, gender, diameter));
        ref[0].setPreferredSize(new Dimension(diameter, diameter));
        ref[0].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ref[0].setToolTipText("Click to change profile picture");
        ref[0].addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Choose Profile Picture");
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files","png","jpg","jpeg"));
                if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    File srcFile = fc.getSelectedFile();
                    // Read into memory first — avoids disk-cache / Windows write-lag issues
                    try {
                        BufferedImage src = ImageIO.read(srcFile);
                        if (src == null) {
                            JOptionPane.showMessageDialog(parent, "Cannot read image file.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        // Update dialog avatar immediately from in-memory image
                        ref[0].setIcon(loadCircularAvatarFromImage(src, diameter));
                        ref[0].revalidate(); ref[0].repaint();
                        // Save to disk
                        if (saveAvatar(userId, srcFile)) {
                            if (onChanged != null) onChanged.run();
                        } else {
                            JOptionPane.showMessageDialog(parent, "Failed to save photo.","Error",JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(parent, "Cannot read image file.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        return ref[0];
    }

    public static JLabel createProfileAvatarLabel(String userId, int diameter, Component parent, Runnable onChanged) { return createProfileAvatarLabel(userId,"",diameter,parent,onChanged); }
    public static JLabel createClickableAvatarLabel(String userId, int diameter, Component parent, Runnable onChanged) { return createProfileAvatarLabel(userId,"",diameter,parent,onChanged); }
    public static JPanel buildBgButtons(String userId, String role, Image[] bgRef, JFrame frame, Runnable onChanged) {
        RoundedButton changeBgBtn=new RoundedButton("Change Background",new Color(50,90,170));
        changeBgBtn.setFont(uiFont(Font.BOLD,11)); changeBgBtn.setPreferredSize(new Dimension(168,32));
        changeBgBtn.addActionListener(e -> {
            JFileChooser fc=new JFileChooser(); fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images","png","jpg","jpeg"));
            if (fc.showOpenDialog(frame)==JFileChooser.APPROVE_OPTION) {
                String saved=saveBgImage(userId,role,fc.getSelectedFile());
                if (saved!=null) { bgRef[0]=loadBgImage(userId,role); onChanged.run(); }
                else JOptionPane.showMessageDialog(frame,"Failed to save background image.","Error",JOptionPane.ERROR_MESSAGE);
            }
        });
        RoundedButton clearBgBtn=new RoundedButton("Clear",new Color(150,50,50));
        clearBgBtn.setFont(uiFont(Font.BOLD,11)); clearBgBtn.setPreferredSize(new Dimension(70,32));
        clearBgBtn.addActionListener(e -> { deleteBgImage(userId,role); bgRef[0]=null; onChanged.run(); });
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); p.setOpaque(false); p.add(changeBgBtn); p.add(clearBgBtn);
        return p;
    }

    // ── Notification popup ────────────────────────────────────────────────────

    /** Formats a stored timestamp string into a relative time label. */
    private static String relativeTime(String timestampStr) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dt = LocalDateTime.parse(timestampStr, fmt);
            LocalDateTime now = LocalDateTime.now();
            long minutes = java.time.Duration.between(dt, now).toMinutes();
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + " min ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            long days = hours / 24;
            if (days == 1) return "Yesterday";
            if (days < 7) return days + " days ago";
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return timestampStr;
        }
    }

    /**
     * Shows a notification popup dialog for a user.
     * Lists all notifications (newest first), with per-item delete and Clear All.
     * Calls onClose when the dialog is dismissed, so the caller can refresh the bell badge.
     */
    public static void showNotificationPopup(JFrame parent, String userId, Runnable onClose) {
        java.util.List<asc.model.Notification> notifs =
                asc.util.FileManager.getNotificationsForUser(userId);

        JDialog dialog = new JDialog(parent, "Notifications", true);
        dialog.setSize(480, Math.min(500, 180 + notifs.size() * 90));
        dialog.setLocationRelativeTo(parent);
        dialog.getContentPane().setBackground(POPUP_BG);

        JPanel container = new JPanel(new BorderLayout(0, 0));
        container.setBackground(POPUP_BG);
        container.setBorder(BorderFactory.createLineBorder(POPUP_BORDER, 1));

        // ── Header ─────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(POPUP_BG);
        header.setBorder(new EmptyBorder(14, 18, 10, 14));
        JLabel titleLbl = new JLabel("\uD83D\uDD14  Notifications");
        titleLbl.setFont(uiFont(Font.BOLD, 16));
        titleLbl.setForeground(WHITE);
        header.add(titleLbl, BorderLayout.WEST);

        JButton clearAllBtn = new RoundedButton("Clear All", new Color(180, 60, 60));
        clearAllBtn.setFont(uiFont(Font.PLAIN, 11));
        clearAllBtn.setPreferredSize(new Dimension(90, 26));
        clearAllBtn.addActionListener(e -> {
            if (!notifs.isEmpty()) {
                int res = JOptionPane.showConfirmDialog(dialog,
                        "Delete all " + notifs.size() + " notification(s)?",
                        "Clear All Notifications", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (res == JOptionPane.YES_OPTION) {
                    asc.util.FileManager.clearAllNotifications(userId);
                    dialog.dispose();
                    if (onClose != null) onClose.run();
                }
            }
        });
        header.add(clearAllBtn, BorderLayout.EAST);
        container.add(header, BorderLayout.NORTH);

        // ── Notification list ──────────────────────────────────────────────
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(POPUP_BG);

        if (notifs.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            emptyPanel.setBackground(POPUP_BG);
            JLabel emptyLbl = new JLabel("\uD83D\uDD14  No notifications");
            emptyLbl.setFont(uiFont(Font.PLAIN, 14));
            emptyLbl.setForeground(DIM_TEXT);
            emptyPanel.add(emptyLbl);
            listPanel.add(emptyPanel);
        } else {
            for (asc.model.Notification n : notifs) {
                JPanel card = new JPanel(new BorderLayout(8, 2));
                card.setBackground(POPUP_BG);
                card.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 0, POPUP_DIVIDER),
                        new EmptyBorder(8, 6, 8, 5)));
                card.setMaximumSize(new Dimension(390, Integer.MAX_VALUE));

                // Left: icon + title + message
                JPanel content = new JPanel();
                content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
                content.setBackground(POPUP_BG);

                String icon = asc.model.Notification.iconForType(n.getType());
                JLabel titleLbl2 = new JLabel("<html><div style='width:265px'>" + icon + "  " + n.getTitle() + "</div></html>");
                titleLbl2.setFont(uiFont(Font.BOLD, 12));
                titleLbl2.setForeground(WHITE);
                titleLbl2.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel msgLbl = new JLabel("<html><div style='width:265px'>" + n.getMessage() + "</div></html>");
                msgLbl.setFont(uiFont(Font.PLAIN, 11));
                msgLbl.setForeground(DIM_TEXT);
                msgLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel timeLbl = new JLabel(relativeTime(n.getTimestamp()));
                timeLbl.setFont(uiFont(Font.PLAIN, 10));
                timeLbl.setForeground(new Color(100, 120, 160));
                timeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

                content.add(titleLbl2);
                content.add(Box.createVerticalStrut(2));
                content.add(msgLbl);
                content.add(Box.createVerticalStrut(1));
                content.add(timeLbl);

                card.add(content, BorderLayout.CENTER);

                // Right: delete button
                javax.swing.ImageIcon rawIcon =
                        new javax.swing.ImageIcon("data/Picture/NavBar/Red_Cross.png");
                java.awt.Image scaledImg = rawIcon.getImage().getScaledInstance(
                        16, 16, java.awt.Image.SCALE_SMOOTH);
                javax.swing.ImageIcon delIcon = new javax.swing.ImageIcon(scaledImg);
                JButton delBtn = new JButton(delIcon);
                delBtn.setContentAreaFilled(false);
                delBtn.setBorderPainted(false);
                delBtn.setFocusPainted(false);
                delBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                delBtn.setPreferredSize(new Dimension(24, 24));
                delBtn.setToolTipText("Delete this notification");
                delBtn.addActionListener(e -> {
                    int res = JOptionPane.showConfirmDialog(dialog,
                            "Delete this notification?",
                            "Delete", JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (res == JOptionPane.YES_OPTION) {
                        asc.util.FileManager.deleteNotification(n.getNotificationId());
                        dialog.dispose();
                        if (onClose != null) onClose.run();
                    }
                });
                JPanel btnWrap = new JPanel(new BorderLayout());
                btnWrap.setBackground(POPUP_BG);
                btnWrap.add(delBtn, BorderLayout.NORTH);
                card.add(btnWrap, BorderLayout.EAST);

                listPanel.add(card);
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(POPUP_BG);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        styleScrollPane(scrollPane);
        container.add(scrollPane, BorderLayout.CENTER);

        // ── Footer ─────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(POPUP_BG);
        footer.setBorder(new EmptyBorder(8, 18, 12, 14));
        JButton closeBtn = new RoundedButton("Close", new Color(60, 90, 150));
        closeBtn.setFont(uiFont(Font.PLAIN, 11));
        closeBtn.setPreferredSize(new Dimension(80, 28));
        closeBtn.addActionListener(e -> {
            dialog.dispose();
            if (onClose != null) onClose.run();
        });
        footer.add(closeBtn);
        container.add(footer, BorderLayout.SOUTH);

        dialog.add(container);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (onClose != null) onClose.run();
            }
        });
        dialog.setVisible(true);
    }

    // ── Shared staff profile dialog ───────────────────────────────────────────
    /**
     * Shows a reusable profile-edit dialog for staff roles (Manager, CounterStaff, Technician).
     * The caller supplies a Runnable that persists the user (e.g. FileManager.updateXxx).
     */
    public static void showStaffProfileDialog(JFrame parent, User user, JLabel headerAvatarLabel,
                                               Runnable onSave, String dashboardTitlePrefix) {
        JDialog dlg = new JDialog(parent, "My Profile", true);
        dlg.setSize(490, 560);
        dlg.setLocationRelativeTo(parent);
        dlg.setResizable(false);

        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(DARK_NAVY);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(TABLE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(ACCENT_BLUE, 14, 1), new EmptyBorder(20, 50, 25, 50)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 5, 7, 5);

        JLabel titleLbl = new JLabel("Edit My Profile", JLabel.CENTER);
        titleLbl.setFont(uiFont(Font.BOLD, 16));
        titleLbl.setForeground(ACCENT_BLUE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        card.add(titleLbl, gbc);

        JLabel avatar = createProfileAvatarLabel(user.getUserId(), user.getGender(), 90, dlg,
                () -> headerAvatarLabel.setIcon(loadCircularAvatar(user.getUserId(), user.getGender(), 48)));
        JLabel hint = new JLabel("Click photo to change", JLabel.CENTER);
        hint.setForeground(DIM_TEXT);
        hint.setFont(uiFont(Font.ITALIC, 11));
        JPanel avSec = new JPanel(new BorderLayout(0, 4));
        avSec.setBackground(TABLE_BG);
        avSec.add(avatar, BorderLayout.CENTER);
        avSec.add(hint, BorderLayout.SOUTH);
        gbc.gridy = 1;
        card.add(avSec, gbc);
        gbc.gridwidth = 1;

        JTextField nameF = new JTextField(user.getName());
        styleTextField(nameF);
        JComboBox<String> genderCb = createGenderCombo(user.getGender());
        JTextField emailF = new JTextField(user.getEmail());
        styleTextField(emailF);
        JTextField phoneF = new JTextField(user.getPhone());
        styleTextField(phoneF);
        JPasswordField pwF = new JPasswordField();
        styleTextField(pwF);
        JPasswordField cpF = new JPasswordField();
        styleTextField(cpF);

        addProfileRow(card, gbc, 2, "Full Name:", nameF);
        addProfileRow(card, gbc, 3, "Gender:", genderCb);
        addProfileRow(card, gbc, 4, "Email:", emailF);
        addProfileRow(card, gbc, 5, "Phone:", phoneF);
        addProfileRow(card, gbc, 6, "New Password:", pwF);
        addProfileRow(card, gbc, 7, "Confirm Password:", cpF);

        RoundedButton save = new RoundedButton("Save Profile", SUCCESS_GREEN);
        save.setPreferredSize(new Dimension(200, 36));
        save.addActionListener(e -> {
            String n = nameF.getText().trim();
            String g = resolveGender(genderCb);
            String em = emailF.getText().trim();
            String ph = phoneF.getText().trim();
            String pw = new String(pwF.getPassword());
            String cf = new String(cpF.getPassword());
            if (n.isEmpty() || em.isEmpty() || ph.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Name, email and phone required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidEmail(em)) {
                JOptionPane.showMessageDialog(dlg, "Invalid email.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ValidationUtils.isValidPhone(ph)) {
                JOptionPane.showMessageDialog(dlg, "Phone must be 10-11 digits.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!pw.isEmpty() && !pw.equals(cf)) {
                JOptionPane.showMessageDialog(dlg, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            user.setName(n);
            user.setGender(g);
            user.setEmail(em);
            user.setPhone(ph);
            if (!pw.isEmpty()) user.setPassword(pw);
            onSave.run();
            headerAvatarLabel.setIcon(loadCircularAvatar(user.getUserId(), g, 48));
            parent.setTitle(dashboardTitlePrefix + user.getName());
            JOptionPane.showMessageDialog(dlg, "Profile updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dlg.dispose();
        });
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        card.add(save, gbc);
        outer.add(card, new GridBagConstraints());
        dlg.setContentPane(outer);
        dlg.setVisible(true);
    }

    private static void addProfileRow(JPanel card, GridBagConstraints g, int row, String labelText, JComponent comp) {
        g.gridy = row; g.gridx = 0; g.gridwidth = 1; g.weightx = 0.35;
        JLabel l = new JLabel(labelText);
        l.setForeground(WHITE);
        l.setFont(uiFont(Font.BOLD, 12));
        card.add(l, g);
        g.gridx = 1; g.weightx = 0.65;
        card.add(comp, g);
    }

    // ── Sidebar helpers ───────────────────────────────────────────────────────
    public static void sideLabel(JPanel bar, String text) {
        JLabel l = new JLabel("  " + text);
        l.setForeground(Color.WHITE);
        l.setFont(uiFont(Font.BOLD, 15));
        l.setBorder(new EmptyBorder(0, 0, 8, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.add(l);
    }

    public static JButton menuBtn(JPanel bar, String iconPath, String text, Runnable action,
                                  Supplier<JButton> activeBtnGetter) {
        ImageIcon icon = loadMenuIcon(iconPath, 20, 20);
        JButton btn = new JButton(text, icon);
        btn.setForeground(WHITE);
        btn.setBackground(MEDIUM_NAVY);
        btn.setFont(uiFont(Font.PLAIN, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(10);
        btn.setBorder(new EmptyBorder(11, 14, 11, 10));
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn != activeBtnGetter.get()) btn.setBackground(new Color(38, 60, 108));
            }
            public void mouseExited(MouseEvent e) {
                if (btn != activeBtnGetter.get()) btn.setBackground(MEDIUM_NAVY);
            }
        });

        btn.addActionListener(e -> action.run());
        bar.add(btn);
        return btn;
    }

}
