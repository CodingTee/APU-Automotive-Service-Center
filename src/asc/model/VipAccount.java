package asc.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Redesigned VIP system – 4 tiers:
 *   NONE  (no tier)  →  0 pts
 *   BRONZE           →  1 000 pts
 *   GOLD             →  10 000 pts
 *   BLACKGOLD        →  100 000 pts
 *
 * Points are earned 1 pt per RM spent (service payments + self top-ups).
 *
 * Benefits:
 *   BRONZE    – RM 20 coupon issued every calendar month
 *   GOLD      – BRONZE coupons + 2 % discount on all services
 *   BLACKGOLD – RM 50 coupon issued every calendar month + 5 % discount
 *
 * OOP Concept — Composition: A VipAccount is PART-OF a Customer.
 *   It cannot exist without its owner. When the customer is deleted,
 *   the VIP account is also deleted (cascade).
 */
public class VipAccount {

    // ── Tier identifiers ─────────────────────────────────────────────────────
    public static final String TIER_NONE      = "NONE";
    public static final String TIER_BRONZE    = "BRONZE";
    public static final String TIER_GOLD      = "GOLD";
    public static final String TIER_BLACKGOLD = "BLACKGOLD";

    // ── Thresholds (cumulative points) ───────────────────────────────────────
    public static final int THRESHOLD_BRONZE    =   1_000;
    public static final int THRESHOLD_GOLD      =  10_000;
    public static final int THRESHOLD_BLACKGOLD = 100_000;

    // ── Service discounts (%) ────────────────────────────────────────────────
    public static final double DISCOUNT_NONE      = 0.0;
    public static final double DISCOUNT_BRONZE    = 0.0;   // no % discount
    public static final double DISCOUNT_GOLD      = 2.0;
    public static final double DISCOUNT_BLACKGOLD = 5.0;

    // ── Monthly coupon values ─────────────────────────────────────────────────
    public static final double COUPON_BRONZE    = 20.0;
    public static final double COUPON_BLACKGOLD = 50.0;

    // ── Legacy constant kept for file-format compatibility ─────────
    public static final String SUB_NONE = "NONE";

    // ── Fields ───────────────────────────────────────────────────────────────
    private String vipId;
    private String customerId;
    private String phone;
    private int    points;          // cumulative lifetime points
    private String tier;
    private String createdDate;
    private double totalSpent;
    private double discountPct;     // current tier discount %
    private String lastCouponMonth; // "yyyy-MM" of last monthly coupon issued
    private String lastCouponTier;  // tier at which last coupon was issued (for upgrade detection)
    private String subscriptionType;   // kept for legacy file compat

    // ── Composition: VipAccount is PART-OF Customer ─────────────────────────
    private transient Customer customer;

    // ── Canonical constructor (12 fields matching toFileString) ──────────────
    public VipAccount(String vipId, String customerId, String phone,
                      int points, String tier, String createdDate,
                      double totalSpent, double discountPct,
                      int unused1, int unused2,          // legacy valetUsed / valetQuota
                      String subscriptionType, String subscriptionExpiry) {
        this.vipId              = vipId;
        this.customerId         = customerId;
        this.phone              = phone;
        this.points             = points;
        this.createdDate        = createdDate;
        this.totalSpent         = totalSpent;
        this.subscriptionType   = subscriptionType;
        this.lastCouponTier     = TIER_NONE;  // old records: no tier tracking
        // lastCouponMonth is passed via subscriptionExpiry parameter for new records;
        // detect by format "yyyy-MM"
        if (subscriptionExpiry != null && subscriptionExpiry.matches("\\d{4}-\\d{2}")) {
            this.lastCouponMonth = subscriptionExpiry;
        } else {
            this.lastCouponMonth = "";
        }
        recalcTier();
    }

    /** 13-field constructor — includes lastCouponTier for upgrade-aware issuance. */
    public VipAccount(String vipId, String customerId, String phone,
                      int points, String tier, String createdDate,
                      double totalSpent, double discountPct,
                      int unused1, int unused2,
                      String subscriptionType, String subscriptionExpiry,
                      String lastCouponTier) {
        this(vipId, customerId, phone, points, tier, createdDate,
             totalSpent, discountPct, unused1, unused2,
             subscriptionType, subscriptionExpiry);
        if (lastCouponTier != null && !lastCouponTier.isEmpty() && !"N/A".equals(lastCouponTier)) {
            this.lastCouponTier = lastCouponTier;
        }
    }

    /** Legacy 6-field constructor (from old files). */
    public VipAccount(String vipId, String customerId, String phone,
                      double balance, String oldTier, String createdDate) {
        this.vipId              = vipId;
        this.customerId         = customerId;
        this.phone              = phone;
        this.points             = (int) balance;
        this.createdDate        = createdDate;
        this.totalSpent         = balance;
        this.subscriptionType   = SUB_NONE;
        this.lastCouponMonth    = "";
        this.lastCouponTier     = TIER_NONE;
        recalcTier();
    }

    // ── Enrolment factory ─────────────────────────────────────────────────────
    public static VipAccount enrol(String vipId, String customerId, String phone) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        VipAccount v = new VipAccount(vipId, customerId, phone,
                0, TIER_NONE, today, 0.0, 0.0, 0, 0, SUB_NONE, "N/A");
        v.lastCouponMonth = "";
        v.lastCouponTier  = TIER_NONE;
        return v;
    }

    // ── Points & tier ─────────────────────────────────────────────────────────
    /** Add points based on amount * multiplier. Recalculates tier. */
    public void addPointsFromPayment(double amountRM, double multiplier) {
        this.points     += (int)(amountRM * multiplier);
        this.totalSpent += amountRM;
        recalcTier();
    }

    public void recalcTier() {
        if      (points >= THRESHOLD_BLACKGOLD) { tier = TIER_BLACKGOLD; discountPct = DISCOUNT_BLACKGOLD; }
        else if (points >= THRESHOLD_GOLD)      { tier = TIER_GOLD;      discountPct = DISCOUNT_GOLD;      }
        else if (points >= THRESHOLD_BRONZE)    { tier = TIER_BRONZE;    discountPct = DISCOUNT_BRONZE;    }
        else                                    { tier = TIER_NONE;      discountPct = DISCOUNT_NONE;      }
    }

    // ── Tier progression ──────────────────────────────────────────────────────
    public int pointsToNextTier() {
        if (points >= THRESHOLD_BLACKGOLD) return 0;
        if (points >= THRESHOLD_GOLD)      return THRESHOLD_BLACKGOLD - points;
        if (points >= THRESHOLD_BRONZE)    return THRESHOLD_GOLD    - points;
        return THRESHOLD_BRONZE - points;
    }

    public int currentTierFloor() {
        if (points >= THRESHOLD_BLACKGOLD) return THRESHOLD_BLACKGOLD;
        if (points >= THRESHOLD_GOLD)      return THRESHOLD_GOLD;
        if (points >= THRESHOLD_BRONZE)    return THRESHOLD_BRONZE;
        return 0;
    }

    public int nextTierThreshold() {
        if (points >= THRESHOLD_BLACKGOLD) return THRESHOLD_BLACKGOLD;
        if (points >= THRESHOLD_GOLD)      return THRESHOLD_BLACKGOLD;
        if (points >= THRESHOLD_BRONZE)    return THRESHOLD_GOLD;
        return THRESHOLD_BRONZE;
    }

    public String nextTierName() {
        if (points >= THRESHOLD_BLACKGOLD) return "MAX TIER";
        if (points >= THRESHOLD_GOLD)      return "BLACKGOLD";
        if (points >= THRESHOLD_BRONZE)    return "GOLD";
        return "BRONZE";
    }

    /** 0.0 – 1.0 progress within the current tier band. */
    public double tierProgressRatio() {
        if (points >= THRESHOLD_BLACKGOLD) return 1.0;
        int floor = currentTierFloor();
        int ceil  = nextTierThreshold();
        if (ceil == floor) return 1.0;
        return (double)(points - floor) / (ceil - floor);
    }

    // ── Monthly coupon logic ──────────────────────────────────────────────────
    /**
     * Numeric rank for tier comparison: NONE=0, BRONZE=1, GOLD=2, BLACKGOLD=3.
     */
    public static int tierLevel(String t) {
        if (TIER_BLACKGOLD.equals(t)) return 3;
        if (TIER_GOLD.equals(t))      return 2;
        if (TIER_BRONZE.equals(t))    return 1;
        return 0;
    }

    /** Inverse of tierLevel: 1→BRONZE, 2→GOLD, 3→BLACKGOLD. */
    public static String tierName(int level) {
        switch (level) {
            case 3: return TIER_BLACKGOLD;
            case 2: return TIER_GOLD;
            case 1: return TIER_BRONZE;
            default: return TIER_NONE;
        }
    }

    /** Monthly coupon face value for a specific tier. */
    public static double couponValueForTier(String t) {
        if (TIER_BLACKGOLD.equals(t)) return COUPON_BLACKGOLD;
        if (TIER_GOLD.equals(t) || TIER_BRONZE.equals(t)) return COUPON_BRONZE;
        return 0;
    }

    /**
     * Returns true if this account needs a monthly coupon issuance.
     * True when: (a) it's a new calendar month, OR
     *           (b) same month but tier was upgraded beyond lastCouponTier.
     */
    public boolean needsMonthlyCoupon() {
        if (TIER_NONE.equals(tier)) return false;
        String thisMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        // New month → always needs coupon
        if (!thisMonth.equals(lastCouponMonth)) return true;
        // Same month but tier upgraded → needs coupon for new tier(s)
        return tierLevel(tier) > tierLevel(lastCouponTier);
    }

    /** Mark that monthly coupon(s) have been issued — records both month and current tier. */
    public void markCouponIssued() {
        this.lastCouponMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        this.lastCouponTier  = this.tier;
    }

    // ── Discount ──────────────────────────────────────────────────────────────
    /** Service discount % (0 for NONE/BRONZE, 2 for GOLD, 5 for BLACKGOLD). */
    public double getDiscountPct() { return discountPct; }

    /** Apply discount to an amount: returns amount - (amount * discountPct / 100). */
    public double applyDiscount(double amount) {
        return amount * (1.0 - discountPct / 100.0);
    }

    // ── Presentation helpers ──────────────────────────────────────────────────
    public String tierBadge() {
        switch (tier) {
            case TIER_BLACKGOLD: return "\u2B50 BLACKGOLD";
            case TIER_GOLD:      return "\uD83E\uDD47 GOLD";
            case TIER_BRONZE:    return "\uD83E\uDD49 BRONZE";
            default:             return "  NONE";
        }
    }

    public java.awt.Color tierColor() {
        switch (tier) {
            case TIER_BLACKGOLD: return new java.awt.Color(255, 215,   0);  // gold-black
            case TIER_GOLD:      return new java.awt.Color(255, 200,  60);
            case TIER_BRONZE:    return new java.awt.Color(205, 127,  50);
            default:             return new java.awt.Color(160, 180, 210);
        }
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String getVipId()            { return vipId; }
    public String getCustomerId()       { return customerId; }
    public String getPhone()            { return phone; }
    public int    getPoints()           { return points; }
    public String getTier()             { return tier; }
    public String getCreatedDate()      { return createdDate; }
    public double getTotalSpent()       { return totalSpent; }
    public String getLastCouponMonth()  { return lastCouponMonth; }
    public String getLastCouponTier()   { return lastCouponTier; }

    public void setPhone(String v)          { phone = v; }
    public void setPoints(int v)            { points = v; recalcTier(); }
    public void setTotalSpent(double v)     { totalSpent = v; }
    public void setLastCouponMonth(String v){ lastCouponMonth = v; }

    // ── Composition: VipAccount is PART-OF Customer ─────────────────────────
    public Customer getCustomer() { return customer; }
    public void     setCustomer(Customer c) { this.customer = c; this.customerId = (c != null) ? c.getUserId() : null; }

    // ── Persistence ───────────────────────────────────────────────────────────
    /**
     * 13-field format:
     * vipId | customerId | phone | points | tier | createdDate |
     * totalSpent | discountPct | 0 | 0 | subType | lastCouponMonth | lastCouponTier
     */
    public String toFileString() {
        return vipId + "|" + customerId + "|" + phone + "|"
             + points + "|" + tier + "|" + createdDate + "|"
             + String.format("%.2f", totalSpent) + "|"
             + String.format("%.2f", discountPct) + "|"
             + "0|0|"
             + subscriptionType + "|"
             + (lastCouponMonth != null && !lastCouponMonth.isEmpty() ? lastCouponMonth : "N/A") + "|"
             + (lastCouponTier  != null && !lastCouponTier.isEmpty()  ? lastCouponTier  : TIER_NONE);
    }
}
