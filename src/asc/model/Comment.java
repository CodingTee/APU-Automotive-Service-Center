package asc.model;

/**
 * Comment left by a customer for a completed appointment.
 * Stores separate star ratings for counter staff and technician.
 * File format: commentId|appointmentId|customerId|csComment|techComment|date|csStars|techStars
 *
 * OOP Concept — Composition: Comment is PART-OF an Appointment.
 *   A customer leaves a review specifically for this service appointment.
 *   Without the appointment, the comment has no meaningful context.
 *   When the appointment is deleted, the comment is also deleted (cascade).
 */
public class Comment {
    private String commentId;
    private String appointmentId;
    private String customerId;
    private String counterStaffComment;
    private String technicianComment;
    private String commentDate;
    private int    csStars;    // 0–5
    private int    techStars;  // 0–5

    // ── Composition: Comment is PART-OF Appointment ───────────────────────────
    private transient Appointment appointment;
    // ── Association: Comment references the Customer who wrote it ─────────────
    private transient Customer     customer;

    // Full constructor (new format with stars)
    public Comment(String commentId, String appointmentId, String customerId,
                   String counterStaffComment, String technicianComment,
                   String commentDate, int csStars, int techStars) {
        this.commentId           = commentId;
        this.appointmentId       = appointmentId;
        this.customerId          = customerId;
        this.counterStaffComment = counterStaffComment;
        this.technicianComment   = technicianComment;
        this.commentDate         = commentDate;
        this.csStars             = csStars;
        this.techStars           = techStars;
    }

    // Legacy constructor (6-field, no stars) – for backward compatibility
    public Comment(String commentId, String appointmentId, String customerId,
                   String counterStaffComment, String technicianComment,
                   String commentDate) {
        this(commentId, appointmentId, customerId,
             counterStaffComment, technicianComment, commentDate, 0, 0);
    }

    public String getCommentId()             { return commentId; }
    public String getAppointmentId()         { return appointmentId; }
    public String getCustomerId()            { return customerId; }
    public String getCounterStaffComment()   { return counterStaffComment; }
    public void   setCounterStaffComment(String c) { this.counterStaffComment = c; }
    public String getTechnicianComment()     { return technicianComment; }
    public void   setTechnicianComment(String c)   { this.technicianComment = c; }
    public String getCommentDate()           { return commentDate; }
    public void   setCommentDate(String d)   { this.commentDate = d; }
    public int    getCsStars()               { return csStars; }
    public void   setCsStars(int s)          { this.csStars   = Math.max(0, Math.min(5, s)); }
    public int    getTechStars()             { return techStars; }
    public void   setTechStars(int s)        { this.techStars = Math.max(0, Math.min(5, s)); }

    // ── Composition: Comment is PART-OF Appointment ───────────────────────────
    public Appointment getAppointment() { return appointment; }
    public void        setAppointment(Appointment a) { this.appointment = a; this.appointmentId = (a != null) ? a.getAppointmentId() : null; }
    public Customer    getCustomer()    { return customer; }
    public void        setCustomer(Customer c)        { this.customer = c; this.customerId = (c != null) ? c.getUserId() : null; }

    /** Returns a unicode star string, e.g. 3 → "★★★☆☆" */
    public String getCsStarDisplay()   { return starStr(csStars); }
    public String getTechStarDisplay() { return starStr(techStars); }

    static String starStr(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < n ? "\u2605" : "\u2606");
        return sb.toString();
    }

    public String toFileString() {
        return commentId + "|" + appointmentId + "|" + customerId + "|"
             + counterStaffComment + "|" + technicianComment + "|"
             + commentDate + "|" + csStars + "|" + techStars;
    }
}
