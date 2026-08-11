package asc.model;

/**
 * Model class representing a vehicle health record created after a service appointment.
 *
 * OOP Concept — Association: Holds runtime references to the Appointment, Customer, and Technician.
 */
public class VehicleHealthRecord {

    private String recordId;
    private String appointmentId;
    private String customerId;
    private String recordDate;
    private String tirePressure;
    private String brakeThickness;
    private String oilType;
    private String sparkPlugStatus;
    private int    mileage;
    private String notes;
    private String technicianId;

    // ── New fields: real-world "sticker" mileage & advanced inspection ────────
    private int    nextServiceMileage;   // next oil-change due (current + 5000 semi / +10000 full)
    private int    majorServiceMileage;  // next major inspection milestone (20k/40k/60k/80k/100k)
    private String batteryHealth;        // battery CCA health % (e.g. "85%", "Good")
    private String tyreTreadDepth;       // tread depth in mm (e.g. "6.5 mm", new=8mm, legal_min=1.6mm)
    private String transmissionFluid;    // ATF/CVT fluid colour & condition
    private String coolantCondition;     // coolant colour & condition

    // ── Object references (populated at runtime, not serialized) ───────────────
    private transient Appointment appointment;
    private transient Customer     customer;
    private transient Technician   technician;

    /** 11-field constructor — used when reading old-format data (backward compat). */
    public VehicleHealthRecord(String recordId, String appointmentId, String customerId,
                                String recordDate, String tirePressure, String brakeThickness,
                                String oilType, String sparkPlugStatus,
                                int mileage, String notes, String technicianId) {
        this(recordId, appointmentId, customerId, recordDate,
             tirePressure, brakeThickness, oilType, sparkPlugStatus,
             mileage, notes, technicianId,
             0, 0, "", "", "", "");
    }

    /** Full 17-field constructor. */
    public VehicleHealthRecord(String recordId, String appointmentId, String customerId,
                                String recordDate, String tirePressure, String brakeThickness,
                                String oilType, String sparkPlugStatus,
                                int mileage, String notes, String technicianId,
                                int nextServiceMileage, int majorServiceMileage,
                                String batteryHealth, String tyreTreadDepth,
                                String transmissionFluid, String coolantCondition) {
        this.recordId        = recordId;
        this.appointmentId   = appointmentId;
        this.customerId      = customerId;
        this.recordDate      = recordDate;
        this.tirePressure    = tirePressure;
        this.brakeThickness  = brakeThickness;
        this.oilType         = oilType;
        this.sparkPlugStatus = sparkPlugStatus;
        this.mileage         = mileage;
        this.notes           = notes;
        this.technicianId    = technicianId;
        this.nextServiceMileage  = nextServiceMileage;
        this.majorServiceMileage = majorServiceMileage;
        this.batteryHealth       = batteryHealth;
        this.tyreTreadDepth      = tyreTreadDepth;
        this.transmissionFluid   = transmissionFluid;
        this.coolantCondition    = coolantCondition;
    }

    // ── Original getters ──────────────────────────────────────────────────────
    public String getRecordId()        { return recordId; }
    public String getAppointmentId()   { return appointmentId; }
    public String getCustomerId()      { return customerId; }
    public String getRecordDate()      { return recordDate; }
    public String getTirePressure()    { return tirePressure; }
    public String getBrakeThickness()  { return brakeThickness; }
    public String getOilType()         { return oilType; }
    public String getSparkPlugStatus() { return sparkPlugStatus; }
    public int    getMileage()         { return mileage; }
    public String getNotes()           { return notes; }
    public String getTechnicianId()    { return technicianId; }

    // ── New getters ───────────────────────────────────────────────────────────
    public int    getNextServiceMileage()  { return nextServiceMileage; }
    public int    getMajorServiceMileage() { return majorServiceMileage; }
    public String getBatteryHealth()       { return batteryHealth; }
    public String getTyreTreadDepth()      { return tyreTreadDepth; }
    public String getTransmissionFluid()   { return transmissionFluid; }
    public String getCoolantCondition()    { return coolantCondition; }

    // ── Object-reference getters & setters (Association) ───────────────────────
    public Appointment getAppointment() { return appointment; }
    public void        setAppointment(Appointment a) { this.appointment = a; this.appointmentId = (a != null) ? a.getAppointmentId() : null; }
    public Customer    getCustomer()    { return customer; }
    public void        setCustomer(Customer c)        { this.customer = c; this.customerId = (c != null) ? c.getUserId() : null; }
    public Technician  getTechnician()  { return technician; }
    public void        setTechnician(Technician t)    { this.technician = t; this.technicianId = (t != null) ? t.getUserId() : null; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setTirePressure(String v)    { tirePressure = v; }
    public void setBrakeThickness(String v)  { brakeThickness = v; }
    public void setOilType(String v)         { oilType = v; }
    public void setSparkPlugStatus(String v) { sparkPlugStatus = v; }
    public void setMileage(int v)            { mileage = v; }
    public void setNotes(String v)           { notes = v; }

    public void setNextServiceMileage(int v)   { nextServiceMileage = v; }
    public void setMajorServiceMileage(int v)  { majorServiceMileage = v; }
    public void setBatteryHealth(String v)     { batteryHealth = v; }
    public void setTyreTreadDepth(String v)    { tyreTreadDepth = v; }
    public void setTransmissionFluid(String v) { transmissionFluid = v; }
    public void setCoolantCondition(String v)  { coolantCondition = v; }

    public String toFileString() {
        return recordId + "|" + appointmentId + "|" + customerId + "|"
             + recordDate + "|" + tirePressure + "|" + brakeThickness + "|"
             + oilType + "|" + sparkPlugStatus + "|" + mileage + "|"
             + notes + "|" + technicianId + "|"
             + nextServiceMileage + "|" + majorServiceMileage + "|"
             + batteryHealth + "|" + tyreTreadDepth + "|"
             + transmissionFluid + "|" + coolantCondition;
    }
}
