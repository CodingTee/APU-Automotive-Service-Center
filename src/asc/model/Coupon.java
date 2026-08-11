package asc.model;

/**
 * Coupon issued by Counter Staff to a specific customer.
 * discount is either a fixed RM amount or a percent depending on type.
 *
 * OOP Concept — Composition: A Coupon is PART-OF a Customer.
 *   Coupons are issued exclusively to a customer based on their VIP tier.
 *   They cannot exist meaningfully without the customer. When the customer
 *   is deleted, all their coupons are also deleted (cascade).
 */
public class Coupon {
    private String couponId;
    private String customerId;
    private String code;
    private String discountType;  // FIXED / PERCENT
    private double discountValue;
    private String expiryDate;
    private boolean used;
    private String issuedBy;      // staffId who issued it
    private String issuedDate;

    // ── Object references (populated at runtime, not serialized) ───────────────
    // Composition: Coupon is PART-OF Customer (cascade delete on customer deletion)
    private transient Customer     customer;
    // Association: Coupon references the CounterStaff who issued it
    private transient CounterStaff issuedByStaff;

    public Coupon(String couponId, String customerId, String code,
                  String discountType, double discountValue,
                  String expiryDate, boolean used,
                  String issuedBy, String issuedDate) {
        this.couponId      = couponId;
        this.customerId    = customerId;
        this.code          = code;
        this.discountType  = discountType;
        this.discountValue = discountValue;
        this.expiryDate    = expiryDate;
        this.used          = used;
        this.issuedBy      = issuedBy;
        this.issuedDate    = issuedDate;
    }

    public String  getCouponId()      { return couponId; }
    public String  getCustomerId()    { return customerId; }
    public String  getCode()          { return code; }
    public String  getDiscountType()  { return discountType; }
    public double  getDiscountValue() { return discountValue; }
    public String  getExpiryDate()    { return expiryDate; }
    public boolean isUsed()           { return used; }
    public void    setUsed(boolean u) { this.used = u; }
    public String  getIssuedBy()      { return issuedBy; }
    public String  getIssuedDate()    { return issuedDate; }

    // ── Object-reference getters & setters (Association) ───────────────────────
    public Customer     getCustomer()      { return customer; }
    public void         setCustomer(Customer c)      { this.customer = c; this.customerId = (c != null) ? c.getUserId() : null; }
    public CounterStaff getIssuedByStaff() { return issuedByStaff; }
    public void         setIssuedByStaff(CounterStaff cs) { this.issuedByStaff = cs; this.issuedBy = (cs != null) ? cs.getUserId() : null; }

    /**
     * Returns true if this coupon's expiry date has passed (strictly before today).
     * Null or unparseable expiry dates are treated as expired for safety.
     */
    public boolean isExpired() {
        if (expiryDate == null || expiryDate.isEmpty()) return true;
        try {
            return expiryDate.compareTo(java.time.LocalDate.now().toString()) < 0;
        } catch (Exception e) {
            return true; // safety: treat bad dates as expired
        }
    }

    public String toFileString() {
        return couponId + "|" + customerId + "|" + code + "|"
             + discountType + "|" + String.format("%.2f", discountValue) + "|"
             + expiryDate + "|" + (used ? "1" : "0") + "|"
             + issuedBy + "|" + issuedDate;
    }
}
