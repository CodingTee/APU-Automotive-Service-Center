package asc;

import asc.gui.shared.LoginFrame;
import asc.util.FileManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the APU-ASC Management System.
 *
 * Look & Feel priority:
 *   1. FlatDarkLaf (if FlatLaf jar is on the classpath)  → modern IntelliJ-style dark UI
 *   2. System L&F (Nimbus on Linux, Windows Classic, Aqua on Mac)
 *   3. Swing default (Metal) — last resort, always works
 *
 * To enable FlatLaf, add the jar to your project classpath:
 *   https://www.formdev.com/flatlaf/  or via Maven:
 *   <dependency>
 *     <groupId>com.formdev</groupId>
 *     <artifactId>flatlaf</artifactId>
 *     <version>3.4</version>
 *   </dependency>
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Try FlatLaf (graceful degradation if jar not present) ─────────
        boolean flatLafLoaded = false;
        try {
            Class<?> flatDark = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            flatDark.getMethod("setup").invoke(null);

            // Extra FlatLaf tweaks for our navy theme
            UIManager.put("Component.arc",              14);
            UIManager.put("Button.arc",                 12);
            UIManager.put("TextComponent.arc",           8);
            UIManager.put("ScrollBar.thumbArc",         999);
            UIManager.put("ScrollBar.thumbInsets",      new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("TabbedPane.tabArc",          10);

            flatLafLoaded = true;
            System.out.println("[LAF] FlatDarkLaf loaded successfully.");
        } catch (ClassNotFoundException ignored) {
            System.out.println("[LAF] FlatLaf not found – falling back to system/Nimbus L&F.");
        } catch (Exception e) {
            System.err.println("[LAF] FlatLaf init failed: " + e.getMessage());
        }

        // ── 2. Nimbus fallback (looks better than Metal) ──────────────────────
        if (!flatLafLoaded) {
            try {
                for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(laf.getName())) {
                        UIManager.setLookAndFeel(laf.getClassName());
                        System.out.println("[LAF] Nimbus loaded.");
                        break;
                    }
                }
            } catch (Exception ignored) {
                // Nimbus not available on all JDKs – Metal is fine
                System.out.println("[LAF] Nimbus unavailable, using default Metal.");
            }
        }

        // ── 3. Initialize data & resolve object references ──────────────────
        FileManager.initializeFiles();
        FileManager.resolveReferences();     // populate object associations (OOP: Association)
        FileManager.autoMarkMissedAppointments();
        FileManager.autoRefreshAllMonthlyCoupons();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
