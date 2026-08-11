package asc.model;

import asc.gui.customer.CustomerDashboard;
import asc.util.FileManager;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Customer entity in the APU-ASC system.
 *
 * OOP Relationships:
 *   - Aggregation:  Customer ◇── Appointment (appointments can exist independently)
 *   - Composition:  Customer ◆── VipAccount  (VIP account cannot exist without the customer)
 *   - Composition:  Customer ◆── Coupon      (coupons are issued to this customer; meaningless without them)
 */
public class Customer extends User {
    private String vehicleInfo;

    // ── Aggregation: Customer AGGREGATES Appointment ─────────────────────
    //  Appointments are associated with this customer, but can theoretically
    //  exist independently (e.g., for historical records after customer deletion).
    //  This is AGGREGATION (not Composition) because the child (Appointment)
    //  can exist without the parent (Customer).
    private transient List<Appointment> appointments = new ArrayList<>();

    // ── Composition: Customer COMPOSES VipAccount ───────────────────────
    //  A VIP account is PART-OF the customer and cannot exist without them.
    //  When the customer is deleted, their VIP account is also deleted (cascade).
    private transient VipAccount vipAccount;

    // ── Composition: Customer COMPOSES Coupon ────────────────────────────
    //  Coupons are issued specifically to this customer based on their VIP tier.
    //  They are PART-OF the customer's ownership and are deleted when the
    //  customer is deleted (cascade).
    private transient List<Coupon> coupons = new ArrayList<>();

    public Customer(String userId, String username, String password,
                    String name, String email, String phone,
                    String vehicleInfo, String gender) {
        super(userId, username, password, name, email, phone, "CUSTOMER", gender);
        this.vehicleInfo = vehicleInfo;
    }

    public String getVehicleInfo()         { return vehicleInfo; }
    public void   setVehicleInfo(String v) { this.vehicleInfo = v; }

    // =========================================================================
    //  MULTI-VEHICLE SUPPORT
    //  Vehicles are stored as "||" separated list in the vehicleInfo field.
    //  e.g. "toyota vios WXY1234||honda city ABC5678"
    // =========================================================================

    /** Returns all vehicles owned by this customer as a list. */
    public List<String> getVehicleList() {
        if (vehicleInfo == null || vehicleInfo.isBlank()) return new java.util.ArrayList<>();
        String[] parts = vehicleInfo.split("\\|\\|", -1);
        List<String> list = new java.util.ArrayList<>();
        for (String v : parts) { String t = v.trim(); if (!t.isEmpty()) list.add(t); }
        return list;
    }

    /** Returns the primary (first) vehicle, or the raw string if no delimiter. */
    public String getPrimaryVehicle() {
        List<String> list = getVehicleList();
        return list.isEmpty() ? vehicleInfo : list.get(0);
    }

    /** Adds a new vehicle (if not already present). Saves multi-vehicle as "||" separated. */
    public void addVehicle(String vehicle) {
        List<String> list = getVehicleList();
        if (!list.contains(vehicle.trim())) list.add(vehicle.trim());
        this.vehicleInfo = String.join("||", list);
    }

    /** Removes a vehicle by exact name. */
    public void removeVehicle(String vehicle) {
        List<String> list = getVehicleList();
        list.remove(vehicle.trim());
        this.vehicleInfo = list.isEmpty() ? "" : String.join("||", list);
    }

    /** True if this customer has more than one vehicle. */
    public boolean hasMultipleVehicles() { return getVehicleList().size() > 1; }

    // ── Aggregation: Customer AGGREGATES Appointment ─────────────────────
    //  Appointments can exist independently of Customer (historical records).
    public List<Appointment> getAppointments() { return appointments; }
    public void addAppointment(Appointment a) {
        if (a != null && !appointments.contains(a)) {
            appointments.add(a);
        }
    }
    public void removeAppointment(Appointment a) { appointments.remove(a); }

    // ── Composition: Customer COMPOSES VipAccount ───────────────────────
    public VipAccount getVipAccount() { return vipAccount; }
    public void       setVipAccount(VipAccount v) { this.vipAccount = v; }

    // ── Composition: Customer COMPOSES Coupon ────────────────────────────
    public List<Coupon> getCoupons() { return coupons; }
    public void addCoupon(Coupon c) {
        if (c != null && !coupons.contains(c)) {
            coupons.add(c);
        }
    }
    public void removeCoupon(Coupon c) { coupons.remove(c); }

    @Override public String getDisplayInfo() {
        return "[Customer] " + name + " | " + email + " | " + phone + " | Vehicle(s): " + vehicleInfo;
    }

    @Override public void openDashboard() {
        new CustomerDashboard(this).setVisible(true);
    }

    @Override public String toFileString() {
        return super.toFileString() + "|" + vehicleInfo;
    }

    // =========================================================================
    //  DATA RETRIEVAL
    // =========================================================================

    /** Returns all appointments belonging to this customer. */
    public List<Appointment> getMyAppointments() {
        return FileManager.readAllAppointments().stream()
                .filter(a -> a.getCustomerId().equals(userId))
                .collect(Collectors.toList());
    }

    /** Returns all SERVICE payments belonging to this customer, keyed by appointmentId.
     *  TOPUP- payments are excluded — they represent wallet credit purchases,
     *  not actual service spending. */
    public Map<String, Payment> getMyPaymentMap() {
        Map<String, Payment> map = new HashMap<>();
        for (Payment pay : FileManager.readAllPayments()) {
            if (!pay.getCustomerId().equals(userId)) continue;
            if (pay.getAppointmentId().startsWith("TOPUP-")) continue;
            map.put(pay.getAppointmentId(), pay);
        }
        return map;
    }

    /** Returns all feedbacks for this customer's appointments. */
    public List<Feedback> getMyFeedbacks() {
        Map<String, Appointment> aptMap = new HashMap<>();
        for (Appointment a : getMyAppointments())
            aptMap.put(a.getAppointmentId(), a);

        return FileManager.readAllFeedbacks().stream()
                .filter(f -> aptMap.containsKey(f.getAppointmentId()))
                .collect(Collectors.toList());
    }

    /** Returns all completed appointments belonging to this customer. */
    public List<Appointment> getMyCompletedAppointments() {
        return getMyAppointments().stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toList());
    }

    /** Returns all pending appointments belonging to this customer, sorted by date. */
    public List<Appointment> getMyPendingAppointments() {
        return getMyAppointments().stream()
                .filter(a -> Appointment.STATUS_SCHEDULED.equalsIgnoreCase(a.getStatus()))
                .sorted(Comparator.comparing(Appointment::getDate))
                .collect(Collectors.toList());
    }

    /** Returns comments by this customer, keyed by appointmentId. */
    public Map<String, Comment> getMyCommentMap() {
        Map<String, Comment> map = new HashMap<>();
        for (Comment c : FileManager.readAllComments())
            if (c.getCustomerId().equals(userId))
                map.put(c.getAppointmentId(), c);
        return map;
    }

    // =========================================================================
    //  STATISTICS
    // =========================================================================

    /** Total amount paid by this customer across all payments. */
    public double getTotalSpent() {
        return getMyPaymentMap().values().stream()
                .mapToDouble(Payment::getAmount).sum();
    }

    /** Number of completed appointments. */
    public long getCompletedCount() {
        return getMyAppointments().stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .count();
    }

    /** Number of pending (non-completed) appointments. */
    public long getPendingCount() {
        return getMyAppointments().size() - getCompletedCount();
    }

    /** Monthly SERVICE spending map (key = "YYYY-MM"), sorted by month.
     *  TOPUP- payments are excluded — only counter service payments count. */
    public Map<String, Double> getMonthlySpending() {
        Map<String, Double> monthly = new TreeMap<>();
        for (Payment pay : FileManager.readAllPayments()) {
            if (!pay.getCustomerId().equals(userId)) continue;
            if (pay.getAppointmentId().startsWith("TOPUP-")) continue;
            String month = pay.getPaymentDate().substring(0, 7);
            monthly.merge(month, pay.getAmount(), Double::sum);
        }
        return monthly;
    }

    // =========================================================================
    //  COMMENT ACTIONS
    // =========================================================================

    /**
     * Submits or updates a comment for a completed appointment.
     * Appends star rating string to the comment text if rating > 0.
     *
     * @param apt         the appointment being commented on
     * @param csText      counter staff comment text
     * @param techText    technician comment text
     * @param csStars     counter staff star rating (0 = no rating)
     * @param techStars   technician star rating (0 = no rating)
     * @param existingMap current comment map (will be updated in place)
     */
    public void submitComment(Appointment apt,
                              String csText, String techText,
                              int csStars, int techStars,
                              Map<String, Comment> existingMap) {
        // Clean text: strip any legacy [Rating:...] tags
        csText   = csText.replaceAll("\\s*\\[Rating:[^\\]]*\\]", "").trim();
        techText = techText.replaceAll("\\s*\\[Rating:[^\\]]*\\]", "").trim();
        if (csText.isEmpty())   csText   = "(No comment provided)";
        if (techText.isEmpty()) techText = "(No comment provided)";

        Comment existing = existingMap.get(apt.getAppointmentId());
        if (existing != null) {
            existing.setCounterStaffComment(csText);
            existing.setTechnicianComment(techText);
            existing.setCsStars(csStars);
            existing.setTechStars(techStars);
            existing.setCommentDate(LocalDate.now().toString());
            FileManager.updateComment(existing);
        } else {
            Comment nc = new Comment(
                    FileManager.generateCommentId(),
                    apt.getAppointmentId(),
                    userId,
                    csText, techText,
                    LocalDate.now().toString(),
                    csStars, techStars);
            FileManager.saveComment(nc);
            existingMap.put(apt.getAppointmentId(), nc);
        }
        // ── Notification: technician gets rating ──────────────────────────
        if (techStars > 0 && apt.getTechnicianId() != null && !apt.getTechnicianId().isEmpty()) {
            String starStr = Comment.starStr(techStars);
            FileManager.saveNotification(new asc.model.Notification(
                    apt.getTechnicianId(), asc.model.Notification.RATING_RECEIVED,
                    "New Rating Received",
                    "A customer rated you " + techStars + "/5 stars " + starStr
                    + " for appointment " + apt.getAppointmentId() + "."));
        }
    }

    // =========================================================================
    //  RECEIPT GENERATION
    // =========================================================================

    /**
     * Generates a formatted plain-text receipt for the given appointment and payment.
     *
     * @param apt the appointment
     * @param pay the corresponding payment
     * @return formatted receipt string ready for display or file export
     */
    public String buildReceiptText(Appointment apt, Payment pay) {
        String type = "NORMAL".equalsIgnoreCase(apt.getServiceType())
                ? "Normal Service (1 hr)" : "Major Service (3 hr)";
        String line = "=".repeat(46);
        String dash = "-".repeat(46);
        return  line + "\n"
              + "       APU AUTOMOTIVE SERVICE CENTRE\n"
              + "            Official Receipt\n"
              + line + "\n\n"
              + "Receipt No   : " + pay.getReceiptNumber()  + "\n"
              + "Payment ID   : " + pay.getPaymentId()      + "\n"
              + "Payment Date : " + pay.getPaymentDate()    + "\n\n"
              + dash + "\nCUSTOMER DETAILS\n" + dash + "\n"
              + "Name         : " + name     + "\n"
              + "Customer ID  : " + userId   + "\n"
              + "Phone        : " + phone    + "\n"
              + "Email        : " + email    + "\n\n"
              + dash + "\nSERVICE DETAILS\n" + dash + "\n"
              + "Apt ID       : " + apt.getAppointmentId() + "\n"
              + "Service Type : " + type                   + "\n"
              + "Vehicle      : " + apt.getVehicleInfo()   + "\n"
              + "Description  : " + apt.getNotes()         + "\n"
              + "Date & Time  : " + apt.getDate() + " " + apt.getTime() + "\n\n"
              + dash + "\nPAYMENT DETAILS\n" + dash + "\n"
              + String.format("Amount       : RM %.2f%n", pay.getAmount())
              + "Method       : " + pay.getPaymentMethod() + "\n\n"
              + line + "\n"
              + "     Thank you for choosing APU-ASC!\n"
              + "       See you on the next visit.\n"
              + line;
    }
}
