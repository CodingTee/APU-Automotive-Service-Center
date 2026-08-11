package asc.model;

/**
 * Feedback written by a technician for a specific appointment.
 *
 * OOP Concept — Composition: Feedback is PART-OF an Appointment.
 *   A technician writes feedback specifically for this service appointment.
 *   Without the appointment, the feedback has no meaningful context.
 *   When the appointment is deleted, the feedback is also deleted (cascade).
 */
public class Feedback {
    private String feedbackId;
    private String appointmentId;
    private String technicianId;
    private String content;
    private String feedbackDate;

    // ── Composition: Feedback is PART-OF Appointment ──────────────────────────
    private transient Appointment appointment;
    // ── Association: Feedback references the Technician who wrote it ────────
    private transient Technician  technician;

    public Feedback(String feedbackId, String appointmentId, String technicianId, String content, String feedbackDate) {
        this.feedbackId = feedbackId;
        this.appointmentId = appointmentId;
        this.technicianId = technicianId;
        this.content = content;
        this.feedbackDate = feedbackDate;
    }

    public String getFeedbackId() { return feedbackId; }
    public String getAppointmentId() { return appointmentId; }
    public String getTechnicianId() { return technicianId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFeedbackDate() { return feedbackDate; }
    public void setFeedbackDate(String date) { this.feedbackDate = date; }

    // ── Composition: Feedback is PART-OF Appointment ──────────────────────────
    public Appointment getAppointment() { return appointment; }
    public void        setAppointment(Appointment a) { this.appointment = a; this.appointmentId = (a != null) ? a.getAppointmentId() : null; }
    public Technician  getTechnician()  { return technician; }
    public void        setTechnician(Technician t)   { this.technician = t; this.technicianId = (t != null) ? t.getUserId() : null; }

    public String toFileString() {
        return feedbackId + "|" + appointmentId + "|" + technicianId + "|" + content + "|" + feedbackDate;
    }
}
