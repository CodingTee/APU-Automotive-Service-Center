package asc.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a service appointment.
 *
 * OOP Concept — Association: Holds references to related entities
 *   (Customer, Technician, CounterStaff) via object references instead of ID strings.
 *   The ID fields (customerId, technicianId, counterStaffId) are kept for file
 *   persistence; the object references are populated at runtime by FileManager.
 *
 * STATUS LIFECYCLE:
 *   SCHEDULED        → Staff created & technician assigned
 *   AWAITING_PAYMENT → Technician marked job done; waiting for customer payment at counter
 *   COMPLETED        → Staff confirmed payment received; receipt generated
 *   MISSED           → Appointment date passed without completion (auto)
 */
public class Appointment {
  // ── Status constants (single source of truth) ──────────────────────────────
  public static final String STATUS_SCHEDULED        = "SCHEDULED";
  public static final String STATUS_AWAITING_PAYMENT = "AWAITING_PAYMENT";
  public static final String STATUS_COMPLETED        = "COMPLETED";
  public static final String STATUS_MISSED           = "MISSED";

  // ── ID fields (used for file persistence) ──────────────────────────────────
  private String appointmentId;
  private String customerId;
  private String technicianId;
  private String counterStaffId;
  private String serviceType;
  private String status;
  private String date;
  private String time;
  private String vehicleInfo;
  private String notes;

  // ── Object references (populated at runtime, not serialized) ───────────────
  private transient Customer      customer;
  private transient Technician   technician;
  private transient CounterStaff counterStaff;

  // ── Association: Appointment references AppointmentPart ─────────────────
  //  Parts used in this appointment are recorded as consumption history.
  //  They are NOT deleted when the appointment is deleted because the
  //  physical parts were already consumed (installed on the vehicle).
  //  This is ASSOCIATION (not Composition) — AppointmentPart records
  //  have independent meaning as historical consumption records.
  private transient List<AppointmentPart> parts = new ArrayList<>();

  // ── Composition: Appointment COMPOSES Feedback ────────────────────────
  //  Feedback is PART-OF the appointment — technician writes feedback
  //  specifically for this service appointment. Without the appointment,
  //  the feedback has no meaningful context.
  private transient List<Feedback> feedbacks = new ArrayList<>();

  // ── Composition: Appointment COMPOSES Comment ──────────────────────────
  //  Comment is PART-OF the appointment — customer leaves a review
  //  specifically for this service appointment. Without the appointment,
  //  the comment has no meaningful context.
  private transient List<Comment> comments = new ArrayList<>();

  public Appointment(String appointmentId, String customerId, String technicianId,
                     String counterStaffId, String serviceType, String status,
                     String date, String time, String vehicleInfo, String notes) {
    this.appointmentId  = appointmentId;
    this.customerId     = customerId;
    this.technicianId   = technicianId;
    this.counterStaffId = counterStaffId;
    this.serviceType    = serviceType;
    this.status         = status;
    this.date           = date;
    this.time           = time;
    this.vehicleInfo    = vehicleInfo;
    this.notes          = notes;
  }

  public String getAppointmentId()  { return appointmentId; }
  public String getCustomerId()     { return customerId; }
  public String getTechnicianId()   { return technicianId; }
  public void   setTechnicianId(String technicianId) { this.technicianId = technicianId; }
  public String getCounterStaffId() { return counterStaffId; }
  public void   setCounterStaffId(String counterStaffId) { this.counterStaffId = counterStaffId; }
  public String getServiceType()    { return serviceType; }
  public void   setServiceType(String serviceType) { this.serviceType = serviceType; }
  public String getStatus()         { return status; }
  public void   setStatus(String status) { this.status = status; }
  public String getDate()           { return date; }
  public void   setDate(String date){ this.date = date; }
  public String getTime()           { return time; }
  public void   setTime(String time){ this.time = time; }
  public String getVehicleInfo()    { return vehicleInfo; }
  public String getNotes()          { return notes; }
  public void   setNotes(String notes) { this.notes = notes; }

  // ── Object-reference getters & setters (Association) ───────────────────────
  public Customer      getCustomer()      { return customer; }
  public void          setCustomer(Customer c)      { this.customer = c; this.customerId = (c != null) ? c.getUserId() : null; }
  public Technician    getTechnician()    { return technician; }
  public void          setTechnician(Technician t)  { this.technician = t; this.technicianId = (t != null) ? t.getUserId() : null; }
  public CounterStaff  getCounterStaff()  { return counterStaff; }
  public void          setCounterStaff(CounterStaff cs) { this.counterStaff = cs; this.counterStaffId = (cs != null) ? cs.getUserId() : null; }

  // ── Association: Appointment references AppointmentPart ─────────────────
  //  Parts are consumption records; they can exist independently.
  public List<AppointmentPart> getParts() { return parts; }
  public void addPart(AppointmentPart part) {
    if (part != null) {
      part.setAppointment(this);  // Set the back-reference
      this.parts.add(part);
    }
  }
  public void removePart(AppointmentPart part) { this.parts.remove(part); }
  public void clearParts() { this.parts.clear(); }

  // ── Composition: Appointment COMPOSES Feedback ────────────────────────
  public List<Feedback> getFeedbacks() { return feedbacks; }
  public void addFeedback(Feedback f) {
    if (f != null && !feedbacks.contains(f)) {
      f.setAppointment(this);  // Set the back-reference
      this.feedbacks.add(f);
    }
  }
  public void removeFeedback(Feedback f) { this.feedbacks.remove(f); }
  public void clearFeedbacks() { this.feedbacks.clear(); }

  // ── Composition: Appointment COMPOSES Comment ──────────────────────────
  public List<Comment> getComments() { return comments; }
  public void addComment(Comment c) {
    if (c != null && !comments.contains(c)) {
      c.setAppointment(this);  // Set the back-reference
      this.comments.add(c);
    }
  }
  public void removeComment(Comment c) { this.comments.remove(c); }
  public void clearComments() { this.comments.clear(); }

  /** True if the technician can still mark the job as done. */
  public boolean isEditable() {
    return STATUS_SCHEDULED.equals(status);
  }

  /** True if payment can be collected by counter staff. */
  public boolean isAwaitingPayment() {
    return STATUS_AWAITING_PAYMENT.equals(status);
  }

  /** True if the whole lifecycle is done. */
  public boolean isCompleted() {
    return STATUS_COMPLETED.equals(status);
  }

  /** True if the appointment was missed (past date, never completed). */
  public boolean isMissed() {
    return STATUS_MISSED.equals(status);
  }

  /** Human-readable label for display in tables. */
  public String getStatusLabel() {
    switch (status) {
      case STATUS_SCHEDULED:        return "Scheduled";
      case STATUS_AWAITING_PAYMENT: return "Awaiting Payment";
      case STATUS_COMPLETED:        return "Completed";
      case STATUS_MISSED:           return "Missed";
      default:                      return status;   // backward-compat with old data
    }
  }

  /** Returns the duration of the service in hours based on type. */
  public int getDurationHours() {
    return serviceType.equalsIgnoreCase("NORMAL") ? 1 : 3;
  }

  /** Returns pipe-separated string for file storage. */
  public String toFileString() {
    return appointmentId + "|" + customerId + "|" + technicianId + "|" + counterStaffId + "|" +
           serviceType + "|" + status + "|" + date + "|" + time + "|" + vehicleInfo + "|" + notes;
  }

  @Override
  public String toString() {
    return appointmentId + " | " + serviceType + " | " + getStatusLabel() + " | " + date + " " + time;
  }
}
