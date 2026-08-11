package asc.gui.customer;

import asc.model.*;
import asc.util.FileManager;
import asc.util.UIUtils;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Customer Support chat-bot dialog.
 *
 * A non-modal JDialog anchored to the bottom-right of the parent frame.
 * Presents a chat-style interface where the bot greets the user, shows
 * quick-reply topic buttons, and answers questions in a decision-tree
 * pattern.  The "Report a Bug" flow lets the user type a free-text
 * description which is saved to data/bug_reports.txt.
 */
public class CustomerSupportDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    // ── Colours ────────────────────────────────────────────────────────────
    private static final Color CHAT_BG       = new Color(22, 36, 68);
    private static final Color BOT_BUBBLE     = new Color(36, 58, 102);
    private static final Color BOT_BUBBLE_BD  = new Color(110, 155, 220);
    private static final Color USER_BUBBLE    = new Color(50, 120, 200);
    private static final Color USER_BUBBLE_BD = new Color(140, 195, 250);
    private static final Color TITLE_BG       = new Color(28, 46, 88);
    private static final Color QUICK_BG        = new Color(30, 50, 95);
    private static final Color QUICK_HOVER     = new Color(50, 80, 140);
    private static final Color INPUT_BG        = new Color(36, 58, 102);
    private static final Color DIALOG_BORDER   = new Color(120, 165, 230);

    // ── UI components ──────────────────────────────────────────────────────
    private final JPanel chatPanel;
    private final JScrollPane chatScroll;
    private final JPanel quickReplyPanel;
    private final CardLayout quickReplyCards;
    private final JPanel inputPanel;
    private final CardLayout inputCards;

    // ── State ──────────────────────────────────────────────────────────────
    private final Customer currentCustomer;

    /** One question inside a topic. */
    private static class TopicQ {
        final String label;
        final Color accent;
        final Runnable answer;
        boolean asked = false;
        TopicQ(String label, Color accent, Runnable answer) {
            this.label = label; this.accent = accent; this.answer = answer;
        }
    }

    private final Map<String, List<TopicQ>> topicQuestions = new LinkedHashMap<>();
    private final Map<String, JPanel> topicPanels = new HashMap<>();
    private JPanel bugCancelPanel = null;

    // ── Quick-reply panel identifiers (CardLayout keys) ────────────────────
    private static final String CARD_MAIN      = "main";
    private static final String CARD_APPOINT   = "appoint";
    private static final String CARD_VIP       = "vip";
    private static final String CARD_PAYMENT   = "payment";
    private static final String CARD_HEALTH    = "health";
    private static final String CARD_SERVICES  = "services";
    private static final String CARD_ACCOUNT   = "account";
    private static final String CARD_FAQ       = "faq";
    private static final String CARD_BUG       = "bug";

    private static final String CARD_INPUT_NONE  = "none";
    private static final String CARD_INPUT_BUG    = "bugInput";
    private static final String CARD_INPUT_TEXT   = "textInput";

    public CustomerSupportDialog(JFrame parent, Customer customer) {
        super(parent, "Customer Support", false); // non-modal
        this.currentCustomer = customer;

        setSize(400, 520);
        setMinimumSize(new Dimension(340, 400));
        setResizable(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationBottomRight(parent);
        setUndecorated(true); // custom title bar with close button

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(CHAT_BG);
        root.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(DIALOG_BORDER, 14, 3),
                new EmptyBorder(0, 0, 12, 0)));

        // ── Title bar ───────────────────────────────────────────────────────
        root.add(buildTitleBar(), BorderLayout.NORTH);

        // ── Chat area ──────────────────────────────────────────────────────
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(CHAT_BG);
        chatPanel.setBorder(new EmptyBorder(6, 12, 6, 12));
        chatPanel.add(Box.createVerticalGlue(), 0); // push all messages to the bottom

        chatScroll = new JScrollPane(chatPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.setBorder(null);
        chatScroll.getViewport().setBackground(CHAT_BG);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));

        root.add(chatScroll, BorderLayout.CENTER);

        // ── Quick-reply + input area (bottom) ──────────────────────────────
        // Use BoxLayout.Y_AXIS (not BorderLayout) so each sub-panel only
        // takes its preferred height. With BorderLayout, the inputPanel
        // (in CENTER) stretched to fill all remaining vertical space,
        // leaving a big empty area when no input was active.
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(CHAT_BG);

        quickReplyCards = new CardLayout();
        quickReplyPanel = new JPanel(quickReplyCards);
        quickReplyPanel.setBackground(CHAT_BG);
        quickReplyPanel.setBorder(new EmptyBorder(6, 8, 4, 8));
        quickReplyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Custom CardLayout that only reports the visible card's size,
        // not the maximum of all cards (which would leave dead space).
        inputCards = new CardLayout() {
            @Override
            public Dimension preferredLayoutSize(Container parent) {
                synchronized (parent.getTreeLock()) {
                    for (Component c : parent.getComponents()) {
                        if (c.isVisible()) return c.getPreferredSize();
                    }
                    return new Dimension(0, 0);
                }
            }
            @Override
            public Dimension minimumLayoutSize(Container parent) {
                synchronized (parent.getTreeLock()) {
                    for (Component c : parent.getComponents()) {
                        if (c.isVisible()) return c.getMinimumSize();
                    }
                    return new Dimension(0, 0);
                }
            }
        };
        inputPanel = new JPanel(inputCards);
        inputPanel.setBackground(CHAT_BG);
        inputPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        inputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // initialise topics and build main menu
        initTopicQuestions();
        buildMainMenu();
        buildInputPanels();

        bottom.add(quickReplyPanel);
        bottom.add(inputPanel);

        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        // ── Greeting ────────────────────────────────────────────────────────
        addBotMessage("Hi " + currentCustomer.getName().split(" ")[0]
                    + "! I'm your APU-ASC support assistant.\n"
                    + "How can I help you today?");
        addBotMessage("Choose a topic from the buttons below or type your question directly."
                    + " I'm here to help!");
        quickReplyCards.show(quickReplyPanel, CARD_MAIN);

        // Re-position when parent moves or resizes
        parent.addComponentListener(new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e) { setLocationBottomRight(parent); }
            @Override public void componentResized(ComponentEvent e) { setLocationBottomRight(parent); }
        });
    }

    // =========================================================================
    //  TITLE BAR
    // =========================================================================
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(TITLE_BG);
        bar.setBorder(new EmptyBorder(10, 14, 10, 10));

        JLabel botIcon = new JLabel("\uD83E\uDD16", JLabel.LEFT);
        botIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        JLabel title = new JLabel("Customer Support");
        title.setFont(UIUtils.uiFont(Font.BOLD, 14));
        title.setForeground(UIUtils.WHITE);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(botIcon);
        left.add(title);

        // Close button (image icon)
        JButton closeBtn = new JButton();
        ImageIcon closeIcon = UIUtils.loadMenuIcon("data/Picture/NavBar/Red_Cross.png", 20, 20);
        closeBtn.setIcon(closeIcon);
        closeBtn.setBackground(TITLE_BG);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(28, 28));
        closeBtn.addActionListener(e -> dispose());

        bar.add(left, BorderLayout.WEST);
        bar.add(closeBtn, BorderLayout.EAST);
        return bar;
    }

    // =========================================================================
    //  CHAT BUBBLES
    // =========================================================================
    private void addBotMessage(String text) {
        addMessage(text, false);
    }

    private void addUserMessage(String text) {
        addMessage(text, true);
    }

    private void addMessage(String text, boolean isUser) {
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setOpaque(false);

        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        area.setBackground(isUser ? USER_BUBBLE : BOT_BUBBLE);
        area.setForeground(UIUtils.WHITE);
        area.setBorder(new EmptyBorder(8, 12, 8, 12));
        area.setOpaque(false);
        area.setColumns(22);

        // Rounded bubble wrapper with custom paint
        JPanel wrapper = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isUser ? USER_BUBBLE : BOT_BUBBLE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(isUser ? USER_BUBBLE_BD : BOT_BUBBLE_BD);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(2, 2, 2, 2)); // 2px padding so the 2px stroke is never clipped
        wrapper.add(area, BorderLayout.CENTER);
        wrapper.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));

        // Alignment: bot left, user right
        JPanel row = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(wrapper);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

        chatPanel.add(row);
        chatPanel.add(Box.createVerticalStrut(12));
        chatPanel.revalidate();
        SwingUtilities.invokeLater(() -> {
            JScrollBar sb = chatScroll.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });
    }

    // =========================================================================
    //  QUICK-REPLY BUTTONS
    // =========================================================================
    private JButton makeQuickBtn(String label, Color accent, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFont(UIUtils.uiFont(Font.PLAIN, 11));
        btn.setForeground(UIUtils.WHITE);
        btn.setBackground(QUICK_BG);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1, true),
                new EmptyBorder(6, 12, 6, 12)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(QUICK_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(QUICK_BG); }
        });
        btn.addActionListener(e -> {
            addUserMessage(label);
            action.run();
        });
        return btn;
    }

    // =========================================================================
    //  MAIN MENU
    // =========================================================================
    private void buildMainMenu() {
        JPanel old = topicPanels.get(CARD_MAIN);
        if (old != null) {
            quickReplyPanel.remove(old);
            topicPanels.remove(CARD_MAIN);
        }
        JPanel mainPanel = wrapButtons(new JButton[]{
            makeQuickBtn("\uD83D\uDC1B Report a Bug", new Color(210, 100, 80), () -> showBugFlow()),
            makeQuickBtn("\uD83D\uDCC5 Appointment Help", UIUtils.ACCENT_BLUE, () -> enterTopic(CARD_APPOINT, "I can help you with appointments! What would you like to know?")),
            makeQuickBtn("\uD83C\uDFC6 VIP & Rewards", UIUtils.WARN_YELLOW, () -> enterTopic(CARD_VIP, "I can explain how our VIP program works! What would you like to know?")),
            makeQuickBtn("\uD83D\uDCB3 Payment & Wallet", UIUtils.PURPLE, () -> enterTopic(CARD_PAYMENT, "I can help with payments! What would you like to know?")),
            makeQuickBtn("\uD83D\uDE97 Vehicle Health", UIUtils.GREEN_OK, () -> enterTopic(CARD_HEALTH, "I can explain vehicle health records! What would you like to know?")),
            makeQuickBtn("\uD83D\uDD27 Services & Pricing", UIUtils.ERROR_RED, () -> enterTopic(CARD_SERVICES, "I can help with service information! What would you like to know?")),
            makeQuickBtn("\uD83D\uDC64 Account & Profile", new Color(200, 120, 180), () -> enterTopic(CARD_ACCOUNT, "I can help with account settings! What would you like to know?")),
            makeQuickBtn("\u2753 General FAQ", UIUtils.DIM_TEXT, () -> enterTopic(CARD_FAQ, "Here are some common questions! What would you like to know?"))
        });
        quickReplyPanel.add(mainPanel, CARD_MAIN);
        topicPanels.put(CARD_MAIN, mainPanel);
    }

    // =========================================================================
    //  TOPIC QUESTIONS
    // =========================================================================
    private void initTopicQuestions() {
        // ── APPOINTMENT HELP ────────────────────────────────────────────────
        List<TopicQ> appoint = new ArrayList<>();
        appoint.add(new TopicQ("How to book?", UIUtils.ACCENT_BLUE, () -> {
            addBotMessage("Appointments are booked by our Counter Staff. Simply visit or call the service centre, "
                    + "provide your vehicle details, and they will schedule an appointment for you at your "
                    + "preferred date and time.\n\nYou can view your upcoming appointments in the 'Service History' page.");
        }));
        appoint.add(new TopicQ("My appointment status", UIUtils.ACCENT_BLUE, () -> showAppointmentStatus()));
        appoint.add(new TopicQ("Cancel appointment?", UIUtils.ACCENT_BLUE, () -> {
            addBotMessage("Currently, self-cancellation is not available. Please contact our Counter Staff "
                    + "to cancel or reschedule your appointment. Make sure to have your Appointment ID ready.");
        }));
        appoint.add(new TopicQ("Service types?", UIUtils.ACCENT_BLUE, () -> {
            addBotMessage("We offer two types of service:\n\n"
                + "\u2022 Normal Service (1 hour) \u2014 basic maintenance including oil change, filter check, "
                + "and general inspection.\n\n"
                + "\u2022 Major Service (3 hours) \u2014 comprehensive service including all normal service items "
                + "plus brake inspection, transmission fluid check, coolant flush, and detailed diagnostics.\n\n"
                + "Visit the 'Products' page to see what's included in each service.");
        }));
        topicQuestions.put(CARD_APPOINT, appoint);

        // ── VIP & REWARDS ───────────────────────────────────────────────────
        List<TopicQ> vip = new ArrayList<>();
        vip.add(new TopicQ("How tiers work", UIUtils.WARN_YELLOW, () -> {
            addBotMessage("There are 4 VIP tiers based on your total points:\n\n"
                + "\u2022 NONE (0 pts) \u2014 starting tier\n"
                + "\u2022 BRONZE (1,000 pts) \u2014 first milestone\n"
                + "\u2022 GOLD (10,000 pts) \u2014 2% service discount\n"
                + "\u2022 BLACKGOLD (100,000 pts) \u2014 5% service discount + higher coupon\n\n"
                + "Your tier upgrades instantly when you reach the threshold!");
        }));
        vip.add(new TopicQ("How to earn points", UIUtils.WARN_YELLOW, () -> {
            addBotMessage("You earn 1 point for every RM 1 spent on services (excluding Wallet Balance "
                + "payments). Points are added automatically when your payment is completed.\n\n"
                + "Example: A RM 500 service earns you 500 points!");
        }));
        vip.add(new TopicQ("Tier benefits", UIUtils.WARN_YELLOW, () -> {
            addBotMessage("VIP Tier Benefits:\n\n"
                + "\u2022 NONE: No discount, no coupon\n"
                + "\u2022 BRONZE: No % discount, RM 20 monthly coupon\n"
                + "\u2022 GOLD: 2% off services, RM 20 monthly coupon\n"
                + "\u2022 BLACKGOLD: 5% off services, RM 50 monthly coupon\n\n"
                + "Discounts apply automatically at checkout. Coupons are issued on the 1st of each month.");
        }));
        vip.add(new TopicQ("Coupon info", UIUtils.WARN_YELLOW, () -> {
            addBotMessage("VIP coupons are automatically issued on the 1st of each month if you've reached "
                + "a qualifying tier (BRONZE or above).\n\n"
                + "Coupon values:\n"
                + "\u2022 BRONZE / GOLD: RM 20 per month\n"
                + "\u2022 BLACKGOLD: RM 50 per month\n\n"
                + "Coupons can be used when paying for services. Check the 'VIP & Coupons' page to see "
                + "your available coupons.");
        }));
        topicQuestions.put(CARD_VIP, vip);

        // ── PAYMENT & WALLET ────────────────────────────────────────────────
        List<TopicQ> payment = new ArrayList<>();
        payment.add(new TopicQ("Payment methods", UIUtils.PURPLE, () -> {
            addBotMessage("We accept the following payment methods:\n\n"
                + "\u2022 Cash\n"
                + "\u2022 Credit Card\n"
                + "\u2022 QR Scan (E-Wallet)\n"
                + "\u2022 Wallet Balance (top-up in advance)\n"
                + "\u2022 Online Transfer\n"
                + "\u2022 Others\n\n"
                + "You can also apply your VIP coupons to reduce the total amount.");
        }));
        payment.add(new TopicQ("How to top up", UIUtils.PURPLE, () -> {
            addBotMessage("To top up your wallet:\n\n"
                + "1. Go to 'Top-Up Wallet' in the sidebar menu\n"
                + "2. Enter the amount you want to add\n"
                + "3. Select your payment method\n"
                + "4. Confirm the top-up\n\n"
                + "Your wallet balance will be updated instantly and can be used for future service payments.");
        }));
        payment.add(new TopicQ("View receipts", UIUtils.PURPLE, () -> {
            addBotMessage("To view your payment receipts:\n\n"
                + "1. Go to 'Receipt & Print' in the sidebar\n"
                + "2. You'll see a list of all your past payments\n"
                + "3. Click on any payment to view details\n"
                + "4. You can download each receipt as a PDF for your records\n\n"
                + "Receipts include the service type, amount, payment method, and receipt number.");
        }));
        payment.add(new TopicQ("Refund policy", UIUtils.PURPLE, () -> {
            addBotMessage("Refunds are handled on a case-by-case basis.\n\n"
                + "To request a refund:\n"
                + "1. Visit our Counter Staff with your receipt number\n"
                + "2. Explain the reason for the refund\n"
                + "3. The manager will review and approve/deny the request\n\n"
                + "Refunds are typically processed within 3-5 business days.");
        }));
        topicQuestions.put(CARD_PAYMENT, payment);

        // ── VEHICLE HEALTH ─────────────────────────────────────────────────
        List<TopicQ> health = new ArrayList<>();
        health.add(new TopicQ("What is it?", UIUtils.GREEN_OK, () -> {
            addBotMessage("After each service, our technicians record a detailed inspection of your vehicle.\n\n"
                + "This includes:\n"
                + "\u2022 Tyre pressure\n"
                + "\u2022 Brake thickness\n"
                + "\u2022 Oil type\n"
                + "\u2022 Spark plug status\n"
                + "\u2022 Mileage\n"
                + "\u2022 Battery health\n"
                + "\u2022 Tyre tread depth\n"
                + "\u2022 Transmission fluid\n"
                + "\u2022 Coolant condition\n\n"
                + "You can view all records in the 'Vehicle Health' page.");
        }));
        health.add(new TopicQ("Service intervals", UIUtils.GREEN_OK, () -> {
            addBotMessage("Recommended service intervals:\n\n"
                + "\u2022 Normal Service: every 5,000 km\n"
                + "\u2022 Major Service: every 20,000 km\n\n"
                + "The system also tracks your next service due mileage based on the oil type used:\n"
                + "\u2022 Semi-Synthetic oil: next service in 5,000 km\n"
                + "\u2022 Full Synthetic oil: next service in 10,000 km\n\n"
                + "Major service milestones are tracked at 20k/40k/60k/80k/100k km.");
        }));
        health.add(new TopicQ("Understanding readings", UIUtils.GREEN_OK, () -> {
            addBotMessage("Here's what each reading means:\n\n"
                + "\u2022 Tyre Pressure: measured in PSI (ideal: 30-35 PSI)\n"
                + "\u2022 Brake Thickness: in mm (safe: >3mm, replace: <3mm)\n"
                + "\u2022 Oil Type: Semi-Synthetic (5,000 km) or Full Synthetic (10,000 km)\n"
                + "\u2022 Battery Health: percentage of battery capacity (e.g. 85%)\n"
                + "\u2022 Tyre Tread Depth: in mm (new=8mm, legal min=1.6mm)\n"
                + "\u2022 Transmission Fluid: colour indicates condition (red=good, brown=change needed)\n"
                + "\u2022 Coolant Condition: colour and clarity indicate health");
        }));
        topicQuestions.put(CARD_HEALTH, health);

        // ── SERVICES & PRICING ──────────────────────────────────────────────
        List<TopicQ> services = new ArrayList<>();
        services.add(new TopicQ("Normal vs Major", UIUtils.ERROR_RED, () -> {
            addBotMessage("Service Comparison:\n\n"
                + "\u2022 Normal Service (1 hour):\n"
                + "   - Oil change\n"
                + "   - Filter check/replacement\n"
                + "   - General inspection\n"
                + "   - Basic diagnostics\n\n"
                + "\u2022 Major Service (3 hours):\n"
                + "   - Everything in Normal Service\n"
                + "   - Brake system inspection\n"
                + "   - Transmission fluid check\n"
                + "   - Coolant flush\n"
                + "   - Detailed diagnostics\n"
                + "   - Spark plug inspection");
        }));
        services.add(new TopicQ("Pricing", UIUtils.ERROR_RED, () -> {
            addBotMessage("Service prices are set by the Manager and may change over time.\n\n"
                + "Please check the 'Products' page for current pricing.\n\n"
                + "VIP members get discounts:\n"
                + "\u2022 GOLD: 2% off services\n"
                + "\u2022 BLACKGOLD: 5% off services");
        }));
        services.add(new TopicQ("Included parts", UIUtils.ERROR_RED, () -> {
            addBotMessage("Each service type includes specific parts at no extra charge.\n\n"
                + "Visit the 'Products' page to see the full list of included parts for "
                + "Normal and Major services.\n\n"
                + "Additional parts beyond the included set may incur extra charges, "
                + "which will be shown in your receipt.");
        }));
        topicQuestions.put(CARD_SERVICES, services);

        // ── ACCOUNT & PROFILE ───────────────────────────────────────────────
        List<TopicQ> account = new ArrayList<>();
        account.add(new TopicQ("Change password", new Color(200, 120, 180), () -> {
            addBotMessage("To change your password:\n\n"
                + "1. Click your avatar (top-right corner)\n"
                + "2. Click 'My Profile'\n"
                + "3. Enter your new password in 'New Password'\n"
                + "4. Re-enter it in 'Confirm Password'\n"
                + "5. Click 'Save Profile' to confirm");
        }));
        account.add(new TopicQ("Update vehicle", new Color(200, 120, 180), () -> {
            addBotMessage("To manage your vehicles:\n\n"
                + "1. Click your avatar (top-right corner)\n"
                + "2. Click 'My Vehicle'\n"
                + "3. To add: enter car model and plate number, click 'Add Vehicle'\n"
                + "4. To remove: click 'Remove' next to the vehicle\n"
                + "5. To change primary: click 'Set Primary'\n\n"
                + "Note: You must have at least one vehicle registered.");
        }));
        account.add(new TopicQ("Change photo", new Color(200, 120, 180), () -> {
            addBotMessage("To change your profile photo:\n\n"
                + "1. Click your avatar (top-right corner)\n"
                + "2. Click on your photo in the popup\n"
                + "3. Choose a new image from your computer\n"
                + "4. The photo updates instantly!\n\n"
                + "Supported formats: PNG, JPG, JPEG");
        }));
        account.add(new TopicQ("Update profile", new Color(200, 120, 180), () -> {
            addBotMessage("To update your profile:\n\n"
                + "1. Click your avatar (top-right corner)\n"
                + "2. Click 'My Profile'\n"
                + "3. Update your name, email, phone, or username\n"
                + "4. Click 'Save Profile' to confirm changes\n\n"
                + "Email must be valid format. Phone must be 10-11 digits.");
        }));
        topicQuestions.put(CARD_ACCOUNT, account);

        // ── GENERAL FAQ ─────────────────────────────────────────────────────
        List<TopicQ> faq = new ArrayList<>();
        faq.add(new TopicQ("Operating hours", UIUtils.DIM_TEXT, () -> {
            addBotMessage("Our service centre operates:\n\n"
                + "\u2022 Monday \u2013 Friday: 9:00 AM \u2013 6:00 PM\n"
                + "\u2022 Saturday: 9:00 AM \u2013 4:00 PM\n"
                + "\u2022 Sunday & Public Holidays: Closed\n\n"
                + "Please book your appointment in advance to avoid waiting.");
        }));
        faq.add(new TopicQ("Location", UIUtils.DIM_TEXT, () -> {
            addBotMessage("APU Automotive Service Centre\n"
                + "Asia Pacific University of Technology & Innovation\n"
                + "TPM, Bukit Jalil\n"
                + "57000 Kuala Lumpur, Malaysia");
        }));
        faq.add(new TopicQ("Contact info", UIUtils.DIM_TEXT, () -> {
            addBotMessage("You can reach us through:\n\n"
                + "\u2022 Email: support@apu-asc.my\n"
                + "\u2022 Phone: +603-8992-5111\n"
                + "\u2022 Visit us in person during operating hours\n\n"
                + "Or use this support chat for quick questions!");
        }));
        faq.add(new TopicQ("System version", UIUtils.DIM_TEXT, () -> {
            addBotMessage("APU-ASC Management System v1.0\n\n"
                + "Developed using Java Swing\n"
                + "Powered by PDFBox for receipt generation\n"
                + "Data stored locally (no internet required)");
        }));
        topicQuestions.put(CARD_FAQ, faq);
    }

    // =========================================================================
    //  TOPIC NAVIGATION
    // =========================================================================
    private void enterTopic(String cardName, String greeting) {
        List<TopicQ> questions = topicQuestions.get(cardName);
        boolean allAsked = questions != null && questions.stream().allMatch(q -> q.asked);
        if (allAsked) {
            addBotMessage("You've already asked all the questions in this topic! Is there anything else I can help you with?");
        } else {
            addBotMessage(greeting);
        }
        rebuildTopicPanel(cardName);
        quickReplyCards.show(quickReplyPanel, cardName);
        quickReplyPanel.revalidate();
        quickReplyPanel.repaint();
    }

    private void rebuildTopicPanel(String cardName) {
        JPanel old = topicPanels.get(cardName);
        if (old != null) quickReplyPanel.remove(old);

        List<TopicQ> questions = topicQuestions.get(cardName);
        if (questions == null) return;

        List<JButton> buttons = new ArrayList<>();
        for (TopicQ q : questions) {
            if (!q.asked) {
                // Note: do NOT call addUserMessage(q.label) here.
                // makeQuickBtn's own actionListener already adds the user bubble
                // before running this action — otherwise we'd get a duplicate bubble.
                buttons.add(makeQuickBtn(q.label, q.accent, () -> {
                    q.asked = true;
                    q.answer.run();
                    boolean remaining = questions.stream().anyMatch(tq -> !tq.asked);
                    if (remaining) {
                        addBotMessage("Is there anything else related you'd like to know?");
                    } else {
                        addBotMessage("You've asked all the questions in this topic! Is there anything else I can help you with?");
                    }
                    rebuildTopicPanel(cardName);
                    quickReplyCards.show(quickReplyPanel, cardName);
                    quickReplyPanel.revalidate();
                    quickReplyPanel.repaint();
                }));
            }
        }
        buttons.add(makeBackBtn());

        JPanel panel = wrapButtons(buttons.toArray(new JButton[0]));
        quickReplyPanel.add(panel, cardName);
        topicPanels.put(cardName, panel);
    }

    private JButton makeBackBtn() {
        return makeQuickBtn("\u2190 Back to Main Menu", UIUtils.DIM_TEXT, () -> {
            // Reset all topic questions so they're available when re-entering
            // any topic from the main menu. Within a topic session (without
            // going back), asked questions stay hidden to prevent repeats.
            for (List<TopicQ> qs : topicQuestions.values()) {
                for (TopicQ q : qs) q.asked = false;
            }
            quickReplyCards.show(quickReplyPanel, CARD_MAIN);
            inputCards.show(inputPanel, CARD_INPUT_NONE);
            addBotMessage("Is there anything else I can help you with?");
        });
    }

    /**
     * Arrange buttons in rows of up to 3, stacked vertically.
     * Uses fixed column count so height is deterministic and does not
     * depend on the container's runtime width (which avoids the
     * chicken-and-egg problem WrapLayout had).
     */
    private JPanel wrapButtons(JButton[] buttons) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(CHAT_BG);

        int perRow = 3;
        JPanel row = null;
        int count = 0;
        for (JButton b : buttons) {
            if (count % perRow == 0) {
                if (row != null) {
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
                    container.add(row);
                }
                row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
                row.setBackground(CHAT_BG);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            row.add(b);
            count++;
        }
        if (row != null) {
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            container.add(row);
        }
        return container;
    }

    // =========================================================================
    //  INPUT PANELS
    // =========================================================================
    private void buildInputPanels() {
        // Empty (no input)
        JPanel emptyPanel = new JPanel();
        emptyPanel.setBackground(CHAT_BG);
        emptyPanel.setPreferredSize(new Dimension(0, 0));
        emptyPanel.setMinimumSize(new Dimension(0, 0));
        emptyPanel.setMaximumSize(new Dimension(0, 0));
        inputPanel.add(emptyPanel, CARD_INPUT_NONE);

        // Bug report input
        JPanel bugPanel = new JPanel(new BorderLayout(6, 0));
        bugPanel.setBackground(CHAT_BG);
        bugPanel.setBorder(new EmptyBorder(4, 8, 8, 8));
        JTextArea bugArea = new JTextArea(3, 30);
        bugArea.setLineWrap(true);
        bugArea.setWrapStyleWord(true);
        bugArea.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        bugArea.setBackground(INPUT_BG);
        bugArea.setForeground(UIUtils.WHITE);
        bugArea.setCaretColor(UIUtils.WHITE);
        bugArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane bugScroll = new JScrollPane(bugArea);
        bugScroll.setBorder(BorderFactory.createLineBorder(new Color(60, 95, 165), 1, true));

        UIUtils.RoundedButton submitBtn = new UIUtils.RoundedButton("Submit Bug Report", UIUtils.ERROR_RED);
        submitBtn.setFont(UIUtils.uiFont(Font.BOLD, 11));
        submitBtn.setPreferredSize(new Dimension(130, 34));
        submitBtn.addActionListener(e -> {
            String desc = bugArea.getText().trim();
            if (desc.isEmpty()) {
                addBotMessage("Please describe the bug before submitting.");
                return;
            }
            String bugId = saveBugReport(desc);
            addBotMessage("Thank you for reporting this issue! Your bug report has been logged.\n\n"
                + "Bug ID: " + bugId + "\n"
                + "Our team will investigate this issue.\n\n"
                + "We apologize for any inconvenience caused.");
            bugArea.setText("");
            inputCards.show(inputPanel, CARD_INPUT_NONE);
            quickReplyCards.show(quickReplyPanel, CARD_MAIN);
        });

        bugPanel.add(bugScroll, BorderLayout.CENTER);
        bugPanel.add(submitBtn, BorderLayout.EAST);
        inputPanel.add(bugPanel, CARD_INPUT_BUG);

        // Free text input (keyword matching)
        JPanel textPanel = new JPanel(new BorderLayout(6, 0));
        textPanel.setBackground(CHAT_BG);
        textPanel.setBorder(new EmptyBorder(4, 8, 8, 8));
        JTextField textField = new JTextField();
        textField.setFont(UIUtils.uiFont(Font.PLAIN, 12));
        textField.setBackground(INPUT_BG);
        textField.setForeground(UIUtils.WHITE);
        textField.setCaretColor(UIUtils.WHITE);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 95, 165), 1, true),
                new EmptyBorder(6, 8, 6, 8)));

        UIUtils.RoundedButton sendBtn = new UIUtils.RoundedButton("Send", UIUtils.ACCENT_BLUE);
        sendBtn.setFont(UIUtils.uiFont(Font.BOLD, 11));
        sendBtn.setPreferredSize(new Dimension(70, 34));
        Runnable sendAction = () -> {
            String text = textField.getText().trim();
            if (text.isEmpty()) return;
            addUserMessage(text);
            textField.setText("");
            handleFreeTextInput(text);
        };
        sendBtn.addActionListener(e -> sendAction.run());
        textField.addActionListener(e -> sendAction.run());

        textPanel.add(textField, BorderLayout.CENTER);
        textPanel.add(sendBtn, BorderLayout.EAST);
        inputPanel.add(textPanel, CARD_INPUT_TEXT);
    }

    // =========================================================================
    //  FLOW HANDLERS
    // =========================================================================
    private void showBugFlow() {
        addBotMessage("I'm sorry to hear you're experiencing an issue.\n\n"
            + "Please describe the bug in detail below. What were you doing when it happened?");
        inputCards.show(inputPanel, CARD_INPUT_BUG);

        if (bugCancelPanel != null) quickReplyPanel.remove(bugCancelPanel);
        bugCancelPanel = wrapButtons(new JButton[]{
            makeQuickBtn("\u2190 Cancel", UIUtils.DIM_TEXT, () -> {
                inputCards.show(inputPanel, CARD_INPUT_NONE);
                quickReplyCards.show(quickReplyPanel, CARD_MAIN);
                addBotMessage("Bug report cancelled. Is there anything else I can help you with?");
            })
        });
        quickReplyPanel.add(bugCancelPanel, CARD_BUG);
        quickReplyCards.show(quickReplyPanel, CARD_BUG);
        quickReplyPanel.revalidate();
        quickReplyPanel.repaint();
    }

    /** Show the user's actual appointment status from live data. */
    private void showAppointmentStatus() {
        List<Appointment> allApts = FileManager.readAllAppointments();
        List<Appointment> myApts = new ArrayList<>();
        for (Appointment a : allApts) {
            if (a.getCustomerId().equals(currentCustomer.getUserId())) {
                myApts.add(a);
            }
        }

        if (myApts.isEmpty()) {
            addBotMessage("You don't have any appointments yet.\n\n"
                + "Please visit our Counter Staff to book one. They will help you schedule "
                + "a service at your preferred date and time.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Here are your appointments:\n\n");
        for (Appointment a : myApts) {
            sb.append("\u2022 ").append(a.getAppointmentId()).append("\n");
            sb.append("   Date: ").append(a.getDate()).append(" at ").append(a.getTime()).append("\n");
            sb.append("   Service: ").append(a.getServiceType()).append("\n");
            sb.append("   Status: ").append(a.getStatusLabel()).append("\n");
            if (a.getTechnicianId() != null && !a.getTechnicianId().isEmpty()) {
                sb.append("   Technician: ").append(a.getTechnicianId()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("You can view more details in the 'Service History' page.");
        addBotMessage(sb.toString());
    }

    // =========================================================================
    //  FREE TEXT KEYWORD MATCHING
    // =========================================================================
    private void handleFreeTextInput(String text) {
        String lower = text.toLowerCase();

        // Keyword routing
        if (lower.contains("bug") || lower.contains("error") || lower.contains("crash") || lower.contains("broken")) {
            addBotMessage("It sounds like you're experiencing a bug. Please use the 'Report a Bug' button "
                + "to submit a detailed report, and our team will investigate.");
            quickReplyCards.show(quickReplyPanel, CARD_MAIN);
            return;
        }
        if (lower.contains("appointment") || lower.contains("book") || lower.contains("schedule")) {
            enterTopic(CARD_APPOINT, "I can help you with appointments! What would you like to know?");
            return;
        }
        if (lower.contains("vip") || lower.contains("point") || lower.contains("tier") || lower.contains("reward")) {
            enterTopic(CARD_VIP, "I can explain how our VIP program works! What would you like to know?");
            return;
        }
        if (lower.contains("pay") || lower.contains("wallet") || lower.contains("topup") || lower.contains("top up") || lower.contains("receipt") || lower.contains("refund")) {
            enterTopic(CARD_PAYMENT, "I can help with payments! What would you like to know?");
            return;
        }
        if (lower.contains("vehicle") || lower.contains("health") || lower.contains("car") || lower.contains("tyre") || lower.contains("brake") || lower.contains("oil")) {
            enterTopic(CARD_HEALTH, "I can explain vehicle health records! What would you like to know?");
            return;
        }
        if (lower.contains("service") || lower.contains("price") || lower.contains("cost") || lower.contains("normal") || lower.contains("major")) {
            enterTopic(CARD_SERVICES, "I can help with service information! What would you like to know?");
            return;
        }
        if (lower.contains("password") || lower.contains("profile") || lower.contains("account") || lower.contains("photo") || lower.contains("vehicle")) {
            enterTopic(CARD_ACCOUNT, "I can help with account settings! What would you like to know?");
            return;
        }
        if (lower.contains("hour") || lower.contains("location") || lower.contains("contact") || lower.contains("phone") || lower.contains("email") || lower.contains("address")) {
            enterTopic(CARD_FAQ, "Here are some common questions! What would you like to know?");
            return;
        }
        if (lower.contains("hi") || lower.contains("hello") || lower.contains("hey")) {
            addBotMessage("Hello! How can I help you today? Choose a topic below or type your question.");
            return;
        }
        if (lower.contains("thank")) {
            addBotMessage("You're welcome! Is there anything else I can help you with?");
            return;
        }

        // Default fallback
        addBotMessage("I'm not sure I understand that question. Please try one of the topic buttons below, "
            + "or rephrase your question with keywords like 'appointment', 'VIP', 'payment', 'vehicle', etc.");
        quickReplyCards.show(quickReplyPanel, CARD_MAIN);
    }

    // =========================================================================
    //  BUG REPORT PERSISTENCE
    // =========================================================================
    private String saveBugReport(String description) {
        String bugId = "BUG-" + System.currentTimeMillis();
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String line = bugId + "|" + currentCustomer.getUserId() + "|" + timestamp + "|" + description.replace("\n", " ").replace("|", "/");

        File f = new File("data/bug_reports.txt");
        f.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(f, true)) {
            fw.write(line + "\n");
        } catch (IOException ex) {
            return "BUG-ERROR";
        }
        return bugId;
    }

    // =========================================================================
    //  POSITIONING
    // =========================================================================
    private void setLocationBottomRight(JFrame parent) {
        int x = parent.getX() + parent.getWidth() - getWidth() - 20;
        int y = parent.getY() + parent.getHeight() - getHeight() - 20;
        if (y < parent.getY() + 100) y = parent.getY() + 100;
        setLocation(x, y);
    }
}
