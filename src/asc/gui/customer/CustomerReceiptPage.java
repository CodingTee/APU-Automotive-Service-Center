package asc.gui.customer;

import asc.model.*;
import asc.util.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class CustomerReceiptPage extends JPanel {

    private static final long serialVersionUID = 1L;
    final CustomerDashboard main;

    public CustomerReceiptPage(CustomerDashboard m) {
        main = m;
        setLayout(new BorderLayout(0, 8));
        setBackground(UIUtils.DARK_NAVY);
        setBorder(new EmptyBorder(18, 18, 14, 18));
        add(buildPanel(), BorderLayout.CENTER);
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIUtils.DARK_NAVY);

        panel.add(main.sectionTitle("Receipt Preview, Print & Download",
                CustomerDashboard.ICON_BASE + "Receipt & Print(Blue).png"), BorderLayout.NORTH);

        Map<String, Payment> payMap = main.currentCustomer.getMyPaymentMap();
        List<Appointment> paid = main.currentCustomer.getMyAppointments().stream()
                .filter(a -> payMap.containsKey(a.getAppointmentId()))
                .collect(Collectors.toList());

        if (paid.isEmpty()) {
            panel.add(main.emptyState("\uD83D\uDCB3", "No payment records found."), BorderLayout.CENTER);
            return panel;
        }

        // --- Receipt selection dropdown ---
        String[] opts = paid.stream().map(a -> {
            Payment p = payMap.get(a.getAppointmentId());
            return a.getAppointmentId() + "  |  " + a.getDate() + "  |  RM " + String.format("%.2f", p.getAmount());
        }).toArray(String[]::new);
        JComboBox<String> combo = new JComboBox<>(opts);
        UIUtils.styleCombo(combo);

        JLabel selLbl = new JLabel("Select Receipt:");
        selLbl.setForeground(UIUtils.WHITE);
        selLbl.setFont(UIUtils.uiFont(Font.BOLD, 13));

        // --- Top bar: exact copy of ManagerReportsPage filterBar ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        topBar.setBackground(new Color(24, 40, 82));
        topBar.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(UIUtils.ACCENT_BLUE, 10, 1),
                new EmptyBorder(6, 10, 6, 10)));
        topBar.add(selLbl);
        topBar.add(combo);

        // --- Receipt text pane: left-aligned monospaced for proper field alignment ---
        JTextPane receiptPane = new JTextPane();
        receiptPane.setEditable(false);
        receiptPane.setBackground(UIUtils.TABLE_BG);
        receiptPane.setForeground(UIUtils.WHITE);
        receiptPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
        receiptPane.setBorder(new EmptyBorder(12, 18, 12, 18));

        // --- Scroll pane: exact copy ---
        JScrollPane textScroll = new JScrollPane(receiptPane);
        textScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        textScroll.setBorder(null);
        textScroll.getViewport().setBackground(UIUtils.DARK_NAVY);
        UIUtils.styleScrollPane(textScroll);
        textScroll.getVerticalScrollBar().setUnitIncrement(16);
        textScroll.getHorizontalScrollBar().setUnitIncrement(16);

        // --- Card wrapper: exact copy ---
        JPanel cardWrapper = new JPanel(new BorderLayout());
        cardWrapper.setBackground(UIUtils.TABLE_BG);
        cardWrapper.setBorder(new EmptyBorder(8, 8, 8, 8));
        cardWrapper.setPreferredSize(new Dimension(470, 200));
        cardWrapper.add(textScroll, BorderLayout.CENTER);

        // --- Center the card: exact copy of textSection ---
        JPanel textSection = new JPanel(new GridBagLayout());
        textSection.setBackground(UIUtils.DARK_NAVY);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill   = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 40, 10, 40);
        textSection.add(cardWrapper, gbc);

        // Wire the combo to update the receipt text
        Runnable show = () -> {
            int idx = combo.getSelectedIndex();
            if (idx < 0) return;
            Appointment a = paid.get(idx);
            receiptPane.setText(main.currentCustomer.buildReceiptText(a,
                    payMap.get(a.getAppointmentId())));
            receiptPane.setCaretPosition(0);
        };
        combo.addActionListener(e -> show.run());
        show.run();

        // --- Download PDF button ---
        UIUtils.RoundedButton pdfBtn = new UIUtils.RoundedButton("Download PDF",
                new Color(160, 60, 200));
        pdfBtn.setPreferredSize(new Dimension(170, 36));
        pdfBtn.addActionListener(e -> exportReceiptToPdf(paid, payMap, combo));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnRow.setBackground(UIUtils.DARK_NAVY);
        btnRow.add(pdfBtn);

        // --- Centre layout: exact copy ---
        JPanel centre = new JPanel(new BorderLayout(0, 8));
        centre.setBackground(UIUtils.DARK_NAVY);
        centre.add(topBar, BorderLayout.NORTH);
        centre.add(textSection, BorderLayout.CENTER);
        centre.add(btnRow, BorderLayout.SOUTH);

        panel.add(centre, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    //  PDF EXPORT (PDFBox — matching ManagerReportsPage style)
    // =========================================================================

    void exportReceiptToPdf(List<Appointment> paid, Map<String, Payment> payMap,
                            JComboBox<String> combo) {
        Window win = SwingUtilities.getWindowAncestor(this);
        int idx = combo.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(win, "Please select a receipt first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Appointment apt = paid.get(idx);
        Payment pay = payMap.get(apt.getAppointmentId());
        String text = main.currentCustomer.buildReceiptText(apt, pay);

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Receipt as PDF");
        fc.setSelectedFile(new File("Receipt_" + pay.getReceiptNumber() + ".pdf"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
        if (fc.showSaveDialog(win) != JFileChooser.APPROVE_OPTION) return;

        File dest = fc.getSelectedFile();
        if (!dest.getName().toLowerCase().endsWith(".pdf"))
            dest = new File(dest.getAbsolutePath() + ".pdf");

        try (PDDocument doc = new PDDocument()) {
            String[] lines = text.replace("\r", "").split("\n", -1);
            float margin = 50;
            PDRectangle pageSize = PDRectangle.A4;
            float pageW = pageSize.getWidth();
            float usableH = pageSize.getHeight() - margin * 2;
            float lineH = 14.5f;
            int linesPerPage = (int)(usableH / lineH);

            // ── Color palette ──
            Color clrBorder    = new Color(0, 100, 210);    // blue borders
            Color clrTitle     = new Color(0, 80, 180);     // blue titles
            Color clrSection   = new Color(200, 150, 0);    // gold section headers
            Color clrText      = new Color(40, 40, 40);     // dark gray body
            Color clrAmount    = new Color(180, 40, 40);    // red for amount
            Color clrFooter    = new Color(100, 100, 180);  // muted blue footer

            for (int start = 0; start < lines.length; start += linesPerPage) {
                PDPage page = new PDPage(pageSize);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float y = pageSize.getHeight() - margin;
                    for (int i = start; i < Math.min(start + linesPerPage, lines.length); i++) {
                        String ln = lines[i];
                        String stripped = ln.strip();
                        String upper    = stripped.toUpperCase();

                        // ── Border lines: draw as solid vector lines ──
                        if (!stripped.isEmpty() && stripped.chars().allMatch(c -> c == '=')) {
                            cs.setStrokingColor(clrBorder);
                            cs.setLineWidth(1.5f);
                            cs.moveTo(margin, y + 3.5f);
                            cs.lineTo(pageW - margin, y + 3.5f);
                            cs.stroke();
                        } else if (!stripped.isEmpty() && stripped.chars().allMatch(c -> c == '-')) {
                            cs.setStrokingColor(clrBorder);
                            cs.setLineWidth(0.8f);
                            cs.moveTo(margin, y + 3.5f);
                            cs.lineTo(pageW - margin, y + 3.5f);
                            cs.stroke();
                        }
                        // ── Normal text lines ──
                        else {
                            PDType1Font font;
                            float fontSize;
                            Color color;

                            if (upper.contains("APU AUTOMOTIVE") || upper.contains("OFFICIAL RECEIPT")) {
                                font = PDType1Font.HELVETICA_BOLD; fontSize = 13;  color = clrTitle;
                            } else if (upper.contains("AMOUNT") && upper.contains("RM")) {
                                font = PDType1Font.COURIER_BOLD;     fontSize = 10;  color = clrAmount;
                            } else if (isReceiptSectionLine(stripped)) {
                                font = PDType1Font.HELVETICA_BOLD; fontSize = 11;  color = clrSection;
                            } else if (upper.contains("THANK YOU") || upper.contains("SEE YOU")) {
                                font = PDType1Font.HELVETICA_OBLIQUE; fontSize = 10;  color = clrFooter;
                            } else if (ln.isBlank()) {
                                y -= lineH;
                                continue;
                            } else {
                                font = PDType1Font.COURIER;           fontSize = 10;  color = clrText;
                            }

                            String out = ln.length() > 110 ? ln.substring(0, 110) : ln;
                            cs.setNonStrokingColor(color);
                            cs.setFont(font, fontSize);

                            // ── Centre title & footer lines ──
                            boolean centre = upper.contains("APU AUTOMOTIVE")
                                          || upper.contains("OFFICIAL RECEIPT")
                                          || upper.contains("THANK YOU")
                                          || upper.contains("SEE YOU");
                            if (centre) {
                                out = stripped;  // remove leading spaces from original text
                                float textW = font.getStringWidth(out) / 1000f * fontSize;
                                float cx = (pageW - textW) / 2f;
                                cs.beginText();
                                cs.newLineAtOffset(cx, y);
                                cs.showText(out);
                                cs.endText();
                            } else {
                                cs.beginText();
                                cs.newLineAtOffset(margin, y);
                                cs.showText(out);
                                cs.endText();
                            }
                        }
                        y -= lineH;
                    }

                    // footer line
                    cs.setNonStrokingColor(new Color(130, 130, 130));
                    cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                    cs.beginText();
                    cs.newLineAtOffset(margin, 30);
                    cs.showText("APU-ASC Official Receipt  |  Generated: " + LocalDate.now());
                    cs.endText();
                }
            }

            doc.save(dest);
            JOptionPane.showMessageDialog(win,
                    "Exported to:\n" + dest.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(win,
                    "Export failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** True if {@code stripped} is a receipt section header
     *  (all uppercase, contains letters, 5–40 chars, not a border). */
    private static boolean isReceiptSectionLine(String stripped) {
        if (stripped.isEmpty() || stripped.length() < 5 || stripped.length() > 40) return false;
        if (stripped.chars().noneMatch(Character::isLetter)) return false;
        return stripped.equals(stripped.toUpperCase());
    }
}
