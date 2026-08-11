package asc.model;

/**
 * Model class representing a payment transaction.
 *
 * OOP Concept — Aggregation: A Payment is associated with an Appointment and a Customer.
 * Object references are held at runtime; ID strings are used for file persistence.
 */
public class Payment {
    private String paymentId;
    private String appointmentId;
    private String customerId;
    private double amount;
    private String paymentDate;
    private String paymentMethod;
    private String receiptNumber;

    // ── Object references (populated at runtime, not serialized) ───────────────
    private transient Appointment appointment;
    private transient Customer     customer;

    public Payment(String paymentId, String appointmentId, String customerId, double amount,
                   String paymentDate, String paymentMethod, String receiptNumber) {
        this.paymentId = paymentId;
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.receiptNumber = receiptNumber;
    }

    public String getPaymentId() { return paymentId; }
    public String getAppointmentId() { return appointmentId; }
    public String getCustomerId() { return customerId; }
    public double getAmount() { return amount; }
    public String getPaymentDate() { return paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getReceiptNumber() { return receiptNumber; }

    // ── Object-reference getters & setters (Association) ───────────────────────
    public Appointment getAppointment() { return appointment; }
    public void        setAppointment(Appointment a) { this.appointment = a; this.appointmentId = (a != null) ? a.getAppointmentId() : null; }
    public Customer    getCustomer()    { return customer; }
    public void        setCustomer(Customer c)        { this.customer = c; this.customerId = (c != null) ? c.getUserId() : null; }

    public String toFileString() {
        return paymentId + "|" + appointmentId + "|" + customerId + "|" + amount + "|" + paymentDate + "|" + paymentMethod + "|" + receiptNumber;
    }

    @Override
    public String toString() {
        return receiptNumber + " | " + appointmentId + " | RM" + String.format("%.2f", amount) + " | " + paymentDate;
    }
}
