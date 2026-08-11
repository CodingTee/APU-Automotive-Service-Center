package asc.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * System notification sent to a user.
 * Flat-file format:
 *   notificationId|userId|type|title|message|timestamp|isRead
 *
 * OOP Concept — Composition: A Notification is PART-OF a User.
 *   It cannot exist without a recipient. When the user is deleted,
 *   all their notifications are also deleted (cascade).
 */
public class Notification {

    // ── Type constants ─────────────────────────────────────────────────────────
    public static final String APPOINTMENT_ASSIGNED     = "New Appointment";
    public static final String APPOINTMENT_RESCHEDULED  = "Appointment Rescheduled";
    public static final String APPOINTMENT_REMINDER     = "Appointment Today";
    public static final String APPOINTMENT_COMPLETED    = "Job Completed";
    public static final String SCHEDULE_PUBLISHED       = "Schedule Published";
    public static final String PART_LOW_STOCK           = "Low Stock Alert";
    public static final String PART_OUT_OF_STOCK        = "Out of Stock";
    public static final String PART_RESTOCKED           = "Part Restocked";
    public static final String PAYMENT_RECEIVED         = "Payment Confirmed";
    public static final String VIP_TIER_UPGRADE         = "VIP Tier Upgrade";
    public static final String COUPON_ISSUED            = "Monthly Coupon";
    public static final String HEALTH_RECORD_ADDED      = "Health Record Added";
    public static final String RATING_RECEIVED          = "Rating Received";
    public static final String GENERAL                  = "System Notice";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ID_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private String notificationId;
    private String userId;
    private String type;
    private String title;
    private String message;
    private String timestamp;   // "yyyy-MM-dd HH:mm:ss"
    private boolean isRead;

    // ── Composition: Notification is PART-OF User ────────────────────────────
    private transient User user;  // can be Manager, CounterStaff, Technician, or Customer

    public Notification(String notificationId, String userId, String type,
                        String title, String message, String timestamp, boolean isRead) {
        this.notificationId = notificationId;
        this.userId         = userId;
        this.type           = type;
        this.title          = title;
        this.message        = message;
        this.timestamp      = timestamp;
        this.isRead         = isRead;
    }

    /** Convenience: auto-generate ID and timestamp, isRead=false. */
    public Notification(String userId, String type, String title, String message) {
        this(generateId(), userId, type, title, message,
             LocalDateTime.now().format(FMT), false);
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public String  getNotificationId() { return notificationId; }
    public String  getUserId()         { return userId; }
    public String  getType()           { return type; }
    public String  getTitle()          { return title; }
    public String  getMessage()        { return message; }
    public String  getTimestamp()      { return timestamp; }
    public boolean isRead()            { return isRead; }

    public void    setRead(boolean r)  { this.isRead = r; }

    // ── Composition: Notification is PART-OF User ────────────────────────────
    public User getUser()               { return user; }
    public void setUser(User u)         { this.user = u; this.userId = (u != null) ? u.getUserId() : null; }

    // ── Serialization ──────────────────────────────────────────────────────────
    public String toFileString() {
        return notificationId + "|" + userId + "|" + type + "|"
             + title + "|" + message + "|" + timestamp + "|" + (isRead ? "1" : "0");
    }

    public static String generateId() {
        return "NOTIF-" + LocalDateTime.now().format(ID_FMT);
    }

    /** Type → icon character for notification card display. */
    public static String iconForType(String type) {
        if (type == null) return "\uD83D\uDD14"; // 🔔
        if (type.contains("Appointment")) return "\uD83D\uDCC5"; // 📅
        if (type.contains("Schedule"))    return "\uD83D\uDCCB"; // 📋
        if (type.contains("Stock") || type.contains("Restocked")) return "\uD83D\uDD27"; // 🔧
        if (type.contains("VIP") || type.contains("Tier")) return "\uD83D\uDC51"; // 👑
        if (type.contains("Coupon"))      return "\uD83C\uDF9F\uFE0F"; // 🎟️
        if (type.contains("Payment"))     return "\uD83D\uDCB0"; // 💰
        if (type.contains("Health"))      return "\uD83D\uDE97"; // 🚗
        if (type.contains("Rating") || type.contains("Feedback")) return "\u2B50"; // ⭐
        return "\uD83D\uDD14"; // 🔔
    }
}
