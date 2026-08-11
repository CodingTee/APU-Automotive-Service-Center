package asc.gui.customer;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class CustomerProductsPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final CustomerDashboard main;

    public CustomerProductsPage(CustomerDashboard m) {
        main = m;
        setLayout(new BorderLayout());
        setBackground(asc.util.UIUtils.DARK_NAVY);
        setBorder(new javax.swing.border.EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), java.awt.BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        main.productsActiveTab      = 0;
        main.productsActiveCategory = "All";
        main.productsSearchKw       = "";
        main.productsSortMode       = 0;

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UIUtils.DARK_NAVY);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));

        // ── Page header ────────────────────────────────────────────────────────
        JLabel pageTitle = new JLabel("Products & Services");
        pageTitle.setFont(UIUtils.uiFont(Font.BOLD, 18));
        pageTitle.setForeground(UIUtils.WHITE);
        JLabel pageSub = new JLabel("Browse car parts and view service pricing");
        pageSub.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        pageSub.setForeground(UIUtils.DIM_TEXT);
        JPanel headerBox = new JPanel();
        headerBox.setLayout(new BoxLayout(headerBox, BoxLayout.Y_AXIS));
        headerBox.setBackground(UIUtils.DARK_NAVY);
        headerBox.add(pageTitle);
        headerBox.add(Box.createVerticalStrut(2));
        headerBox.add(pageSub);
        headerBox.setBorder(new EmptyBorder(0, 0, 12, 0));

        // ── Tab bar ────────────────────────────────────────────────────────────
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setBackground(UIUtils.DARK_NAVY);
        tabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 75, 130)));

        // Content swap area
        JPanel contentSwap = new JPanel(new BorderLayout());
        contentSwap.setBackground(UIUtils.DARK_NAVY);

        JButton[] tabBtns = new JButton[2];
        String[] tabNames = {"Car Parts", "Services"};
        Color TAB_ACTIVE_LINE = UIUtils.ACCENT_BLUE;

        for (int ti = 0; ti < 2; ti++) {
            JButton tb = new JButton(tabNames[ti]);
            tb.setFont(UIUtils.uiFont(Font.PLAIN, 13));
            tb.setFocusPainted(false);
            tb.setContentAreaFilled(false);
            tb.setOpaque(false);
            tb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tb.setBorder(new EmptyBorder(9, 18, 9, 18));
            tb.setForeground(UIUtils.DIM_TEXT);
            tabBtns[ti] = tb;
            tabBar.add(tb);
        }

        Runnable[] selectTab = {null};
        selectTab[0] = () -> {
            for (int i = 0; i < 2; i++) {
                boolean active = (i == main.productsActiveTab);
                tabBtns[i].setForeground(active ? UIUtils.ACCENT_BLUE : UIUtils.DIM_TEXT);
                tabBtns[i].setFont(UIUtils.uiFont(active ? Font.BOLD : Font.PLAIN, 13));
                tabBtns[i].setBorder(active
                    ? BorderFactory.createCompoundBorder(
                          BorderFactory.createMatteBorder(0, 0, 2, 0, TAB_ACTIVE_LINE),
                          new EmptyBorder(9, 18, 7, 18))
                    : new EmptyBorder(9, 18, 9, 18));
            }
            contentSwap.removeAll();
            contentSwap.add(main.productsActiveTab == 0 ? buildPartsContent(contentSwap, selectTab)
                                                   : main.buildServicesContent(), BorderLayout.CENTER);
            contentSwap.revalidate();
            contentSwap.repaint();
        };

        for (int ti = 0; ti < 2; ti++) {
            final int idx = ti;
            tabBtns[ti].addActionListener(e -> {
                main.productsActiveTab = idx;
                selectTab[0].run();
            });
        }

        JPanel northBox = new JPanel(new BorderLayout());
        northBox.setBackground(UIUtils.DARK_NAVY);
        northBox.add(headerBox, BorderLayout.NORTH);
        northBox.add(tabBar,    BorderLayout.SOUTH);

        root.add(northBox,    BorderLayout.NORTH);
        root.add(contentSwap, BorderLayout.CENTER);

        // Trigger initial tab render
        selectTab[0].run();
        return root;
    }

    // ── Car Parts tab content ──────────────────────────────────────────────────

    JPanel buildPartsContent(JPanel contentSwap, Runnable[] selectTab) {
        List<CarPart> allVisible = FileManager.readAllCarParts().stream()
                .filter(CarPart::isVisible).collect(Collectors.toList());

        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(UIUtils.DARK_NAVY);
        wrapper.setBorder(new EmptyBorder(14, 0, 0, 0));

        if (allVisible.isEmpty()) {
            wrapper.add(main.emptyState("\uD83D\uDD27", "No car parts information available yet."), BorderLayout.CENTER);
            return wrapper;
        }

        // ── Collect unique categories ────────────────────────────────────────
        List<String> categories = new ArrayList<>();
        categories.add("All");
        allVisible.stream().map(CarPart::getCategory).distinct().sorted().forEach(categories::add);

        // ── Filter bar: search + category pills + sort ─────────────────────────
        JPanel filterBar = new JPanel();
        filterBar.setLayout(new BoxLayout(filterBar, BoxLayout.Y_AXIS));
        filterBar.setBackground(UIUtils.DARK_NAVY);

        // Row 1: search field + sort
        JTextField searchField = new JTextField();
        UIUtils.styleTextField(searchField);
        searchField.setPreferredSize(new Dimension(220, 32));
        searchField.setMaximumSize(new Dimension(280, 32));
        searchField.putClientProperty("JTextField.placeholderText", "Search parts...");

        JComboBox<String> sortCb = new JComboBox<>(new String[]{"Price: low to high", "Price: high to low", "Name A-Z"});
        UIUtils.styleCombo(sortCb);
        sortCb.setPreferredSize(new Dimension(170, 32));
        sortCb.setMaximumSize(new Dimension(200, 32));

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setForeground(UIUtils.ACCENT_BLUE);
        searchIcon.setFont(UIUtils.uiFont(Font.PLAIN, 14));

        JLabel sortLbl = new JLabel("Sort:");
        sortLbl.setForeground(UIUtils.DIM_TEXT);
        sortLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setBackground(UIUtils.DARK_NAVY);
        row1.add(searchIcon);
        row1.add(searchField);
        row1.add(Box.createHorizontalStrut(16));
        row1.add(sortLbl);
        row1.add(sortCb);

        // Row 2: category pills
        JPanel pillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pillRow.setBackground(UIUtils.DARK_NAVY);
        JButton[] pillBtns = new JButton[categories.size()];

        JPanel gridWrap   = new JPanel(new BorderLayout());
        gridWrap.setBackground(UIUtils.DARK_NAVY);
        JLabel countLbl = new JLabel();
        countLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
        countLbl.setForeground(UIUtils.DIM_TEXT);
        countLbl.setBorder(new EmptyBorder(0, 0, 6, 0));

        // Create the scroll pane ONCE — reuse it on every filter change
        JScrollPane gridScroller = new JScrollPane();
        gridScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        gridScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gridScroller.setBorder(null);
        gridScroller.getVerticalScrollBar().setUnitIncrement(20);
        gridScroller.getViewport().setBackground(UIUtils.DARK_NAVY);
        gridWrap.add(gridScroller, BorderLayout.CENTER);

        Runnable rebuildGrid = () -> {
            String kw   = main.productsSearchKw.toLowerCase().trim();
            String cat  = main.productsActiveCategory;
            int    sort = main.productsSortMode;

            List<CarPart> filtered = allVisible.stream()
                .filter(p -> "All".equals(cat) || p.getCategory().equals(cat))
                .filter(p -> kw.isEmpty()
                    || p.getName().toLowerCase().contains(kw)
                    || p.getCategory().toLowerCase().contains(kw)
                    || p.getDescription().toLowerCase().contains(kw))
                .sorted((a, b) -> {
                    if (sort == 0) return Double.compare(a.getPrice(), b.getPrice());
                    if (sort == 1) return Double.compare(b.getPrice(), a.getPrice());
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());

            countLbl.setText(filtered.size() + " part" + (filtered.size() != 1 ? "s" : "") + " found");

            // Build grid
            JPanel grid = new JPanel(new GridLayout(0, 4, 12, 12));
            grid.setBackground(UIUtils.DARK_NAVY);
            grid.setBorder(new EmptyBorder(4, 0, 8, 0));

            if (filtered.isEmpty()) {
                JLabel none = new JLabel("No parts match your filter.", JLabel.CENTER);
                none.setForeground(UIUtils.DIM_TEXT);
                none.setFont(UIUtils.uiFont(Font.ITALIC, 13));
                grid.setLayout(new BorderLayout());
                grid.add(none, BorderLayout.CENTER);
            } else {
                for (CarPart cp : filtered) {
                    grid.add(buildPartCard(cp));
                }
            }

            // Wrap grid in a NORTH-aligned container so the viewport
            // keeps a consistent layout whether there are 20 items or 2.
            JPanel viewportContent = new JPanel(new BorderLayout());
            viewportContent.setBackground(UIUtils.DARK_NAVY);
            viewportContent.add(grid, BorderLayout.NORTH);

            // Reuse the SAME scroll pane — just swap its viewport
            gridScroller.setViewportView(viewportContent);
            gridScroller.revalidate();
            gridScroller.repaint();
        };

        // Wire up pills
        for (int pi = 0; pi < categories.size(); pi++) {
            final String cat = categories.get(pi);
            JButton pill = main.makePillBtn(cat, cat.equals(main.productsActiveCategory));
            pillBtns[pi] = pill;
            pill.addActionListener(e -> {
                main.productsActiveCategory = cat;
                for (int pj = 0; pj < pillBtns.length; pj++)
                    main.stylePill(pillBtns[pj], categories.get(pj).equals(cat));
                rebuildGrid.run();
            });
            pillRow.add(pill);
        }

        // Wire search + sort
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { main.productsSearchKw = searchField.getText(); rebuildGrid.run(); }
            public void removeUpdate(DocumentEvent e)  { main.productsSearchKw = searchField.getText(); rebuildGrid.run(); }
            public void changedUpdate(DocumentEvent e) { main.productsSearchKw = searchField.getText(); rebuildGrid.run(); }
        });
        sortCb.addActionListener(e -> { main.productsSortMode = sortCb.getSelectedIndex(); rebuildGrid.run(); });

        filterBar.add(row1);
        filterBar.add(Box.createVerticalStrut(6));
        filterBar.add(pillRow);

        JPanel topBar = new JPanel(new BorderLayout(0, 4));
        topBar.setBackground(UIUtils.DARK_NAVY);
        topBar.add(filterBar,  BorderLayout.NORTH);
        topBar.add(countLbl,   BorderLayout.SOUTH);

        wrapper.add(topBar,   BorderLayout.NORTH);
        wrapper.add(gridWrap, BorderLayout.CENTER);

        // Add a note at bottom linking to Services tab
        JLabel noteLink = new JLabel("\u2139  Parts marked as 'Included' are bundled in your service \u2014 switch to the Services tab for full details.");
        noteLink.setFont(UIUtils.uiFont(Font.ITALIC, 11));
        noteLink.setForeground(UIUtils.DIM_TEXT);
        noteLink.setBorder(new EmptyBorder(6, 0, 0, 0));
        wrapper.add(noteLink, BorderLayout.SOUTH);

        // First render
        rebuildGrid.run();
        return wrapper;
    }

    // Build a single product card
    JPanel buildPartCard(CarPart cp) {
        boolean free    = cp.getPrice() == 0.0;
        boolean inStock = cp.isInStock();
        Color borderCol = inStock ? new Color(50, 85, 150) : new Color(80, 40, 40);

        // ── Resolve image path (3-strategy lookup) ─────────────────────────────
        File imgFile = null;

        // Strategy 1: stored imageUrl
        String storedPath = cp.getImageUrl();
        if (storedPath != null && !storedPath.trim().isEmpty()) {
            File f1 = new File(storedPath.trim());
            if (f1.exists()) imgFile = f1;
        }
        // Strategy 2: exact name match in CustomerDashboard.ICON_BASE
        if (imgFile == null) {
            String[] exts = {".png", ".jpg", ".jpeg"};
            String[] candidates = {
                cp.getName().trim(),
                cp.getName().trim().replace("/", " "),
                cp.getName().trim().replace(" / ", " ")
            };
            outer:
            for (String c2 : candidates)
                for (String ext : exts) {
                    File f2 = new File(CustomerDashboard.ICON_BASE + c2 + ext);
                    if (f2.exists()) { imgFile = f2; break outer; }
                }
        }
        // Strategy 3: fuzzy keyword scan
        if (imgFile == null) {
            try {
                File dir = new File(CustomerDashboard.ICON_BASE);
                if (dir.isDirectory()) {
                    String partLower = cp.getName().trim().toLowerCase()
                            .replaceAll("[^a-z0-9 ]", " ").trim();
                    String[] words = partLower.split("\\s+");
                    File[] files = dir.listFiles();
                    if (files != null) {
                        File best = null; int bestScore = 0;
                        for (File f3 : files) {
                            String fn = f3.getName().toLowerCase();
                            if (!fn.endsWith(".png") && !fn.endsWith(".jpg")) continue;
                            if (fn.contains("blue") || fn.contains("home")
                                    || fn.contains("receipt") || fn.contains("spending")
                                    || fn.contains("star") || fn.contains("vip")
                                    || fn.contains("comment")
                                    || fn.contains("service history") || fn.contains("top-up")
                                    || fn.contains("car parts info") || fn.contains("leave a comment")) continue;
                            int score = 0;
                            for (String w : words)
                                if (w.length() > 3 && fn.contains(w)) score++;
                            if (score > bestScore) { bestScore = score; best = f3; }
                        }
                        if (best != null && bestScore > 0) imgFile = best;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Pre-load the image once (shared by paintComponent)
        final Image[] productImg = {null};
        if (imgFile != null) {
            try {
                File thumbFile = new File(UIUtils.thumbPath(imgFile.getAbsolutePath()));
                if (!thumbFile.exists()) thumbFile = imgFile;
                productImg[0] = new ImageIcon(thumbFile.getAbsolutePath()).getImage(); }
            catch (Exception ignored) {}
        }

        // ── Outer card panel ───────────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(new Color(30, 48, 88));
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(borderCol, 12, 1),
                new EmptyBorder(0, 0, 0, 0)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ── Image area: paints the image stretched to fill the entire panel ────
        JPanel imgArea = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth(), h = getHeight();

                if (productImg[0] != null) {
                    int imgW = productImg[0].getWidth(null);
                    int imgH = productImg[0].getHeight(null);
                    if (imgW > 0 && imgH > 0) {
                        double scaleW = (double) w / imgW;
                        double scaleH = (double) h / imgH;
                        double scale  = Math.min(scaleW, scaleH);
                        int dw = (int) (imgW * scale);
                        int dh = (int) (imgH * scale);
                        int dx = (w - dw) / 2;
                        int dy = (h - dh) / 2;
                        g2.setClip(0, 0, w, h);
                        g2.drawImage(productImg[0], dx, dy, dw, dh, null);
                        g2.setClip(null);
                        g2.setPaint(new GradientPaint(
                                0, h * 0.55f, new Color(0, 0, 0, 0),
                                0, h,          new Color(20, 32, 62, 200)));
                        g2.fillRect(0, 0, w, h);
                    } else {
                        g2.setColor(new Color(22, 36, 70));
                        g2.fillRect(0, 0, w, h);
                    }
                } else {
                    g2.setPaint(new GradientPaint(
                            0, 0, new Color(28, 50, 100),
                            0, h, new Color(20, 36, 74)));
                    g2.fillRect(0, 0, w, h);
                    String letter = cp.getName().trim().isEmpty() ? "?"
                            : String.valueOf(Character.toUpperCase(cp.getName().trim().charAt(0)));
                    Font lf = UIUtils.uiFont(Font.BOLD, 48);
                    g2.setFont(lf);
                    g2.setColor(new Color(60, 100, 170, 120));
                    FontMetrics fm2 = g2.getFontMetrics(lf);
                    g2.drawString(letter,
                            (w - fm2.stringWidth(letter)) / 2,
                            (h + fm2.getAscent() - fm2.getDescent()) / 2);
                }
                g2.dispose();
            }
        };
        imgArea.setOpaque(true);
        imgArea.setBackground(new Color(22, 36, 70));
        imgArea.setPreferredSize(new Dimension(0, 150));

        // ── Stock / Included badge (top-right overlay) ─────────────────────────
        String badgeText = free ? "Included" : (inStock ? "In stock" : "Out of stock");
        Color badgeBg = free ? new Color(15, 70, 35)
                : (inStock ? new Color(15, 60, 30) : new Color(80, 20, 20));
        Color badgeFg = free ? new Color(80, 220, 130)
                : (inStock ? new Color(70, 210, 110) : new Color(230, 90, 90));

        JLabel badge = new JLabel(badgeText);
        badge.setFont(UIUtils.uiFont(Font.BOLD, 10));
        badge.setOpaque(true);
        badge.setBackground(badgeBg);
        badge.setForeground(badgeFg);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);
        imgArea.add(badgeWrap, BorderLayout.NORTH);

        // ── Category chip (bottom-left overlay) ────────────────────────────────
        JLabel catChip = new JLabel(cp.getCategory());
        catChip.setFont(UIUtils.uiFont(Font.BOLD, 9));
        catChip.setOpaque(true);
        catChip.setBackground(new Color(18, 32, 68, 210));
        catChip.setForeground(new Color(160, 200, 245));
        catChip.setBorder(new EmptyBorder(2, 7, 2, 7));
        JPanel catChipWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        catChipWrap.setOpaque(false);
        catChipWrap.add(catChip);
        imgArea.add(catChipWrap, BorderLayout.SOUTH);

        // ── Text body ──────────────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(new Color(30, 48, 88));
        body.setBorder(new EmptyBorder(10, 12, 12, 12));

        JLabel nameLbl = new JLabel(
                "<html><div style='width:140px; word-wrap:break-word;'>"
                + cp.getName() + "</div></html>");
        nameLbl.setFont(UIUtils.uiFont(Font.BOLD, 12));
        nameLbl.setForeground(UIUtils.WHITE);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLbl = new JLabel(free ? "Complimentary"
                : String.format("RM %.2f", cp.getPrice()));
        priceLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));
        priceLbl.setForeground(free ? CustomerDashboard.GREEN_OK : CustomerDashboard.ACCENT_YELLOW);
        priceLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        priceLbl.setBorder(new EmptyBorder(5, 0, 0, 0));

        body.add(nameLbl);
        body.add(priceLbl);

        card.add(imgArea, BorderLayout.NORTH);
        card.add(body,    BorderLayout.CENTER);

        // ── Hover effect ───────────────────────────────────────────────────────
        Color baseCard  = new Color(30, 48, 88);
        Color hoverCard = new Color(38, 62, 112);
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverCard);
                body.setBackground(hoverCard);
                card.setBorder(BorderFactory.createCompoundBorder(
                        new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                        new EmptyBorder(0, 0, 0, 0)));
                card.repaint();
            }
            public void mouseExited(MouseEvent e) {
                card.setBackground(baseCard);
                body.setBackground(baseCard);
                card.setBorder(BorderFactory.createCompoundBorder(
                        new UIUtils.RoundedBorder(borderCol, 12, 1),
                        new EmptyBorder(0, 0, 0, 0)));
                card.repaint();
            }
            public void mouseClicked(MouseEvent e) { showPartDetailDialog(cp); }
        });
        return card;
    }

    // Detail dialog when clicking a product card
    void showPartDetailDialog(CarPart cp) {
        boolean free    = cp.getPrice() == 0.0;
        boolean inStock = cp.isInStock();

        // ── Dialog setup ───────────────────────────────────────────────────────
        JDialog dlg = new JDialog((Dialog) null, cp.getName(), true);
        dlg.setSize(580, 500);
        dlg.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dlg.setResizable(false);

        JPanel outer = new JPanel(new BorderLayout(0, 12));
        outer.setBackground(UIUtils.DARK_NAVY);
        outer.setBorder(new EmptyBorder(16, 18, 16, 18));

        // ── TOP ROW: image (left) + info (right) ──────────────────────────────
        JPanel topCard = new JPanel(new BorderLayout(14, 0));
        topCard.setBackground(UIUtils.TABLE_BG);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 12, 1),
                new EmptyBorder(14, 16, 14, 16)));

        // Image box (cover-paint, 160x160)
        File dlgImgFile = null;
        if (cp.getImageUrl() != null && !cp.getImageUrl().trim().isEmpty()) {
            File f0 = new File(cp.getImageUrl().trim());
            if (f0.exists()) dlgImgFile = f0;
        }
        if (dlgImgFile == null) {
            for (String ext : new String[]{".png",".jpg",".jpeg"}) {
                File fx = new File(CustomerDashboard.ICON_BASE + cp.getName().trim() + ext);
                if (fx.exists()) { dlgImgFile = fx; break; }
            }
        }
        if (dlgImgFile == null) {
            try {
                File dir = new File(CustomerDashboard.ICON_BASE);
                if (dir.isDirectory()) {
                    String partLower = cp.getName().trim().toLowerCase()
                            .replaceAll("[^a-z0-9 ]"," ").trim();
                    String[] words = partLower.split("\\s+");
                    File best = null; int bestScore = 0;
                    for (File fx2 : dir.listFiles()) {
                        String fn = fx2.getName().toLowerCase();
                        if (!fn.endsWith(".png") && !fn.endsWith(".jpg")) continue;
                        if (fn.contains("blue")||fn.contains("home")||fn.contains("receipt")
                                ||fn.contains("spending")||fn.contains("star")||fn.contains("vip")
                                ||fn.contains("comment")
                                ||fn.contains("service history")||fn.contains("top-up")
                                ||fn.contains("car parts")||fn.contains("leave a")
                                ||fn.contains("view feed")) continue;
                        int sc = 0;
                        for (String w : words) if (w.length()>3 && fn.contains(w)) sc++;
                        if (sc > bestScore) { bestScore=sc; best=fx2; }
                    }
                    if (best != null && bestScore > 0) dlgImgFile = best;
                }
            } catch (Exception ignored) {}
        }
        final Image[] dlgImg = {null};
        if (dlgImgFile != null) {
            File tf = new File(UIUtils.thumbPath(dlgImgFile.getAbsolutePath()));
            dlgImg[0] = new ImageIcon((tf.exists() ? tf : dlgImgFile).getAbsolutePath()).getImage();
        }

        JPanel imgBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                int w=getWidth(), h=getHeight();
                g2.setColor(new Color(20,34,68));
                g2.fillRoundRect(0,0,w,h,10,10);
                if (dlgImg[0]!=null) {
                    int iw=dlgImg[0].getWidth(null), ih=dlgImg[0].getHeight(null);
                    if (iw>0&&ih>0) {
                        double sc=Math.min((double)w/iw,(double)h/ih);
                        int dw=(int)(iw*sc),dh=(int)(ih*sc);
                        g2.setClip(0,0,w,h);
                        g2.drawImage(dlgImg[0],(w-dw)/2,(h-dh)/2,dw,dh,null);
                    }
                } else {
                    g2.setFont(UIUtils.uiFont(Font.BOLD,48));
                    g2.setColor(new Color(60,100,160,100));
                    String lt=cp.getName().isEmpty()?"?":
                            String.valueOf(Character.toUpperCase(cp.getName().charAt(0)));
                    FontMetrics fm5=g2.getFontMetrics();
                    g2.drawString(lt,(w-fm5.stringWidth(lt))/2,
                            (h+fm5.getAscent()-fm5.getDescent())/2);
                }
                g2.dispose();
            }
        };
        imgBox.setPreferredSize(new Dimension(160, 160));
        imgBox.setBackground(new Color(20,34,68));
        imgBox.setOpaque(true);
        imgBox.setBorder(new UIUtils.RoundedBorder(new Color(50,80,140),10,1));

        // Info panel (right side)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(UIUtils.TABLE_BG);
        infoPanel.setBorder(new EmptyBorder(0,4,0,0));

        JLabel nameL = new JLabel(
                "<html><div style='width:300px;word-wrap:break-word;'>"+cp.getName()+"</div></html>");
        nameL.setFont(UIUtils.uiFont(Font.BOLD,16));
        nameL.setForeground(UIUtils.WHITE);
        nameL.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel catL = new JLabel(cp.getCategory()+" - "+cp.getPartId());
        catL.setFont(UIUtils.uiFont(Font.PLAIN,11));
        catL.setForeground(UIUtils.DIM_TEXT);
        catL.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceL = new JLabel(free?"Complimentary":String.format("RM %.2f",cp.getPrice()));
        priceL.setFont(UIUtils.uiFont(Font.BOLD,26));
        priceL.setForeground(free?CustomerDashboard.GREEN_OK:CustomerDashboard.ACCENT_YELLOW);
        priceL.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel stockL = new JLabel(inStock||free ? (free?"Included in service":"In stock")
                : "Out of stock");
        stockL.setFont(UIUtils.uiFont(Font.BOLD,12));
        stockL.setForeground(inStock||free ? CustomerDashboard.GREEN_OK : new Color(220,80,80));
        stockL.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(nameL);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(catL);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(priceL);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(stockL);

        topCard.add(imgBox,    BorderLayout.WEST);
        topCard.add(infoPanel, BorderLayout.CENTER);

        // ── MIDDLE: Description ────────────────────────────────────────────────
        JPanel descCard = new JPanel(new BorderLayout(0,8));
        descCard.setBackground(UIUtils.TABLE_BG);
        descCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(new Color(50,80,130),12,1),
                new EmptyBorder(12,14,12,14)));

        JLabel descHdr = new JLabel("Product Description");
        descHdr.setFont(UIUtils.uiFont(Font.BOLD,12));
        descHdr.setForeground(UIUtils.ACCENT_BLUE);

        JTextArea descArea = new JTextArea(cp.getDescription());
        descArea.setEditable(false); descArea.setLineWrap(true); descArea.setWrapStyleWord(true);
        descArea.setBackground(new Color(24,38,72)); descArea.setForeground(UIUtils.LIGHT_GRAY);
        descArea.setFont(UIUtils.uiFont(Font.PLAIN,12)); descArea.setBorder(new EmptyBorder(6,8,6,8));
        descArea.setRows(3);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(null); UIUtils.styleScrollPane(descScroll);

        descCard.add(descHdr,    BorderLayout.NORTH);
        descCard.add(descScroll, BorderLayout.CENTER);

        // ── BOTTOM: Action buttons ─────────────────────────────────────────────
        JPanel actionRow = new JPanel(new GridLayout(1, 1, 10, 0));
        actionRow.setBackground(UIUtils.DARK_NAVY);

        UIUtils.RoundedButton closeBtn = new UIUtils.RoundedButton("Close", new Color(60,80,120));
        closeBtn.setFont(UIUtils.uiFont(Font.BOLD,12));
        closeBtn.setPreferredSize(new Dimension(0,40));
        closeBtn.addActionListener(e -> dlg.dispose());

        if (!inStock) {
            JPanel wrap = new JPanel(new BorderLayout(0,8));
            wrap.setBackground(UIUtils.DARK_NAVY);
            JLabel outLbl = new JLabel("\u274C This item is currently out of stock", JLabel.CENTER);
            outLbl.setFont(UIUtils.uiFont(Font.BOLD,12));
            outLbl.setForeground(new Color(220,80,80));
            wrap.add(outLbl,   BorderLayout.NORTH);
            wrap.add(closeBtn, BorderLayout.SOUTH);
            outer.add(topCard,  BorderLayout.NORTH);
            outer.add(descCard, BorderLayout.CENTER);
            outer.add(wrap,     BorderLayout.SOUTH);
            dlg.setContentPane(outer);
            dlg.setVisible(true);
            return;
        }

        actionRow.add(closeBtn);

        outer.add(topCard,   BorderLayout.NORTH);
        outer.add(descCard,  BorderLayout.CENTER);
        outer.add(actionRow, BorderLayout.SOUTH);
        dlg.setContentPane(outer);
        dlg.setVisible(true);
    }
}
