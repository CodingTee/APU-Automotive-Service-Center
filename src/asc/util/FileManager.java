package asc.util;

import asc.model.*;
import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FileManager {

    private static final String DATA_DIR           = "data/";
    private static final String MANAGERS_FILE      = DATA_DIR + "managers.txt";
    private static final String COUNTER_STAFF_FILE = DATA_DIR + "counter_staff.txt";
    private static final String TECHNICIANS_FILE   = DATA_DIR + "technicians.txt";
    private static final String CUSTOMERS_FILE     = DATA_DIR + "customers.txt";
    private static final String APPOINTMENTS_FILE  = DATA_DIR + "appointments.txt";
    private static final String PAYMENTS_FILE      = DATA_DIR + "payments.txt";
    private static final String FEEDBACKS_FILE     = DATA_DIR + "feedbacks.txt";
    private static final String COMMENTS_FILE      = DATA_DIR + "comments.txt";
    private static final String PRICES_FILE        = DATA_DIR + "prices.txt";
    private static final String VIP_FILE           = DATA_DIR + "vip_accounts.txt";
    private static final String COUPONS_FILE       = DATA_DIR + "coupons.txt";
    private static final String CAR_PARTS_FILE     = DATA_DIR + "car_parts.txt";
    private static final String HEALTH_FILE         = DATA_DIR + "vehicle_health.txt";
    private static final String APT_PARTS_FILE      = DATA_DIR + "appointment_parts.txt";
    private static final String WORK_DAYS_FILE       = DATA_DIR + "staff_employee_scheduling.txt";
    private static final String PUBLISHED_WD_FILE    = DATA_DIR + "published_employee_scheduling.txt";
    private static final String NOTIFICATIONS_FILE  = DATA_DIR + "notifications.txt";

    public static void initializeFiles() {
        new File(DATA_DIR).mkdirs();
        for (String f : new String[]{MANAGERS_FILE, COUNTER_STAFF_FILE, TECHNICIANS_FILE,
                CUSTOMERS_FILE, APPOINTMENTS_FILE, PAYMENTS_FILE,
                FEEDBACKS_FILE, COMMENTS_FILE, PRICES_FILE,
                VIP_FILE, COUPONS_FILE, CAR_PARTS_FILE, HEALTH_FILE, APT_PARTS_FILE,
                WORK_DAYS_FILE, PUBLISHED_WD_FILE, NOTIFICATIONS_FILE})
            createFileIfNotExists(f);
        if (readAllManagers().isEmpty())
            saveManager(new Manager("MGR001","admin","admin123","Administrator","admin@apuasc.com","0123456789","Male"));
        File pf = new File(PRICES_FILE);
        if (pf.length() == 0) savePrices(new ServicePrice(100.00, 300.00));
    }

    private static void createFileIfNotExists(String path) {
        File f = new File(path);
        if (!f.exists()) { try { f.createNewFile(); } catch (IOException e) { handleError("Cannot create "+path, e); } }
    }
    private static void handleError(String msg, Exception e) { System.err.println(msg+": "+e.getMessage()); }

    // ── Read ─────────────────────────────────────────────────────────────────

    public static List<Manager> readAllManagers() {
        List<Manager> list = new ArrayList<>();
        for (String line : readLines(MANAGERS_FILE)) {
            String[] p = line.split("\\|", -1);
            String gender = p.length >= 7 ? p[6] : "Male";
            if (p.length >= 6) list.add(new Manager(p[0],p[1],p[2],p[3],p[4],p[5],gender));
        }
        return list;
    }

    public static List<CounterStaff> readAllCounterStaff() {
        List<CounterStaff> list = new ArrayList<>();
        for (String line : readLines(COUNTER_STAFF_FILE)) {
            String[] p = line.split("\\|", -1);
            String gender = p.length >= 7 ? p[6] : "Male";
            if (p.length >= 6) list.add(new CounterStaff(p[0],p[1],p[2],p[3],p[4],p[5],gender));
        }
        return list;
    }

    public static List<Technician> readAllTechnicians() {
        List<Technician> list = new ArrayList<>();
        for (String line : readLines(TECHNICIANS_FILE)) {
            String[] p = line.split("\\|", -1);
            String gender = p.length >= 7 ? p[6] : "Male";
            if (p.length >= 6) list.add(new Technician(p[0],p[1],p[2],p[3],p[4],p[5],gender));
        }
        return list;
    }

    public static List<Customer> readAllCustomers() {
        List<Customer> list = new ArrayList<>();
        for (String line : readLines(CUSTOMERS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 8) {
                // Rejoin vehicleInfo from position 7 onward — vehicleInfo itself
                // contains "||" as a multi-vehicle delimiter, and split("\\|")
                // would tear it apart. Rejoining with "|" restores the original.
                String vi = p[7];
                for (int i = 8; i < p.length; i++) vi += "|" + p[i];
                list.add(new Customer(p[0], p[1], p[2], p[3], p[4], p[5], vi, p[6]));
            }
            else if (p.length >= 7) list.add(new Customer(p[0],p[1],p[2],p[3],p[4],p[5],p[6],"Male"));
        }
        return list;
    }

    public static List<Appointment> readAllAppointments() {
        List<Appointment> list = new ArrayList<>();
        for (String line : readLines(APPOINTMENTS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 10) list.add(new Appointment(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8],p[9]));
        }
        return list;
    }

    public static List<Payment> readAllPayments() {
        List<Payment> list = new ArrayList<>();
        for (String line : readLines(PAYMENTS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 7) {
                try { list.add(new Payment(p[0],p[1],p[2],Double.parseDouble(p[3]),p[4],p[5],p[6])); }
                catch (NumberFormatException ignored) {}
            }
        }
        return list;
    }

    public static List<Feedback> readAllFeedbacks() {
        List<Feedback> list = new ArrayList<>();
        for (String line : readLines(FEEDBACKS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 5) list.add(new Feedback(p[0],p[1],p[2],p[3],p[4]));
        }
        return list;
    }

    public static List<Comment> readAllComments() {
        List<Comment> list = new ArrayList<>();
        for (String line : readLines(COMMENTS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 6) {
                int cs = 0, tech = 0;
                if (p.length >= 8) {
                    try { cs   = Integer.parseInt(p[6].trim()); } catch (Exception ignored) {}
                    try { tech = Integer.parseInt(p[7].trim()); } catch (Exception ignored) {}
                } else {
                    cs   = extractStarCount(p[3]);
                    tech = extractStarCount(p[4]);
                }
                list.add(new Comment(p[0],p[1],p[2],p[3],p[4],p[5],cs,tech));
            }
        }
        return list;
    }

    private static int extractStarCount(String text) {
        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile("\\[Rating:([\u2605]*)\\]").matcher(text);
        int count = 0;
        while (m.find()) count = m.group(1).length();
        return count;
    }

    public static List<VipAccount> readAllVipAccounts() {
        List<VipAccount> list = new ArrayList<>();
        for (String line : readLines(VIP_FILE)) {
            String[] p = line.split("\\|", -1);
            try {
                if (p.length >= 13) {
                    // 13-field format (includes lastCouponTier)
                    list.add(new VipAccount(p[0], p[1], p[2],
                            Integer.parseInt(p[3].trim()), p[4], p[5],
                            Double.parseDouble(p[6].trim()),
                            Double.parseDouble(p[7].trim()),
                            Integer.parseInt(p[8].trim()),
                            Integer.parseInt(p[9].trim()),
                            p[10], p[11], p[12]));
                } else if (p.length >= 12) {
                    // 12-field format (no lastCouponTier — defaults to NONE)
                    list.add(new VipAccount(p[0], p[1], p[2],
                            Integer.parseInt(p[3].trim()), p[4], p[5],
                            Double.parseDouble(p[6].trim()),
                            Double.parseDouble(p[7].trim()),
                            Integer.parseInt(p[8].trim()),
                            Integer.parseInt(p[9].trim()),
                            p[10], p[11]));
                } else if (p.length >= 6) {
                    list.add(new VipAccount(p[0], p[1], p[2],
                            Double.parseDouble(p[3].trim()), p[4], p[5]));
                }
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    public static List<Coupon> readAllCoupons() {
        List<Coupon> list = new ArrayList<>();
        for (String line : readLines(COUPONS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 9) {
                try { list.add(new Coupon(p[0],p[1],p[2],p[3],Double.parseDouble(p[4]),p[5],"1".equals(p[6]),p[7],p[8])); }
                catch (NumberFormatException ignored) {}
            }
        }
        return list;
    }

    public static List<CarPart> readAllCarParts() {
        List<CarPart> list = new ArrayList<>();
        for (String line : readLines(CAR_PARTS_FILE)) {
            String[] p = line.split("\\|", -1);
            try {
                if (p.length >= 12) {
                    // 12-field format: ...|inStock|addedBy|addedDate|stockQuantity|lowStockThreshold
                    list.add(new CarPart(p[0],p[1],p[2],p[3],Double.parseDouble(p[4]),
                        p[5],"1".equals(p[6]),"1".equals(p[7]),p[8],p[9],
                        Integer.parseInt(p[10]),Integer.parseInt(p[11])));
                } else if (p.length >= 10) {
                    // Old 10-field format: stockQuantity defaults to 0
                    list.add(new CarPart(p[0],p[1],p[2],p[3],Double.parseDouble(p[4]),
                        p[5],"1".equals(p[6]),"1".equals(p[7]),p[8],p[9],0,3));
                } else if (p.length >= 9) {
                    list.add(new CarPart(p[0],p[1],p[2],p[3],Double.parseDouble(p[4]),
                        p[5],"1".equals(p[6]),p[7],p[8]));
                }
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    public static ServicePrice readPrices() {
        List<String> lines = readLines(PRICES_FILE);
        if (!lines.isEmpty()) return ServicePrice.fromFileString(lines.get(0));
        return new ServicePrice(100.00, 300.00);
    }

    public static List<VehicleHealthRecord> readAllHealthRecords() {
        List<VehicleHealthRecord> list = new ArrayList<>();
        int lineNum = 0;
        for (String line : readLines(HEALTH_FILE)) {
            lineNum++;
            String[] p = line.split("\\|", -1);
            if (p.length >= 11) {
                try {
                    int mileage = Integer.parseInt(p[8].trim());
                    // Backward compat: old 11-field records get defaults for the 6 new fields
                    int    nextSvc  = p.length >= 12 ? parseIntSafe(p[11]) : 0;
                    int    majorSvc = p.length >= 13 ? parseIntSafe(p[12]) : 0;
                    String battery  = p.length >= 14 ? p[13].trim() : "";
                    String tread    = p.length >= 15 ? p[14].trim() : "";
                    String transFl  = p.length >= 16 ? p[15].trim() : "";
                    String coolant  = p.length >= 17 ? p[16].trim() : "";
                    VehicleHealthRecord rec = new VehicleHealthRecord(
                            p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                            mileage, p[9], p[10],
                            nextSvc, majorSvc, battery, tread, transFl, coolant);
                    // Warn if technicianId is null/empty — this will cause display issues
                    if (rec.getTechnicianId() == null || rec.getTechnicianId().isBlank()) {
                        System.err.println("[WARN] VehicleHealthRecord " + rec.getRecordId()
                                + " has null/empty technicianId (line " + lineNum + ")");
                    }
                    list.add(rec);
                } catch (NumberFormatException ex) {
                    System.err.println("[WARN] Failed to parse health record at line " + lineNum
                            + ": " + ex.getMessage() + " | line=" + line);
                }
            } else {
                System.err.println("[WARN] Skipping malformed health record line " + lineNum
                        + " (expected >= 11 fields, got " + p.length + "): " + line);
            }
        }
        return list;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    public static void saveManager(Manager m)               { appendLine(MANAGERS_FILE,      m.toFileString()); }
    public static void saveCounterStaff(CounterStaff cs)    { appendLine(COUNTER_STAFF_FILE, cs.toFileString()); }
    public static void saveTechnician(Technician t)         { appendLine(TECHNICIANS_FILE,   t.toFileString()); }
    public static void saveCustomer(Customer c)             { appendLine(CUSTOMERS_FILE,     c.toFileString()); }
    public static void saveAppointment(Appointment a)       { appendLine(APPOINTMENTS_FILE,  a.toFileString()); }
    public static void savePayment(Payment p)               { appendLine(PAYMENTS_FILE,      p.toFileString()); }
    public static void saveFeedback(Feedback f)             { appendLine(FEEDBACKS_FILE,     f.toFileString()); }
    public static void saveComment(Comment c)               { appendLine(COMMENTS_FILE,      c.toFileString()); }
    public static void saveVipAccount(VipAccount v)         { appendLine(VIP_FILE,           v.toFileString()); }
    public static void saveCoupon(Coupon c)                 { appendLine(COUPONS_FILE,       c.toFileString()); }
    public static void saveCarPart(CarPart cp)              { appendLine(CAR_PARTS_FILE,     cp.toFileString()); }
    public static void saveHealthRecord(VehicleHealthRecord r) { appendLine(HEALTH_FILE,     r.toFileString()); }

    // ── Appointment Parts (used parts per appointment) ────────────────────────
    public static List<AppointmentPart> readAllAppointmentParts() {
        List<AppointmentPart> list = new ArrayList<>();
        for (String line : readLines(APT_PARTS_FILE)) {
            AppointmentPart ap = AppointmentPart.fromLine(line);
            if (ap != null) list.add(ap);
        }
        return list;
    }

    public static List<AppointmentPart> getPartsForAppointment(String aptId) {
        List<AppointmentPart> list = new ArrayList<>();
        for (AppointmentPart ap : readAllAppointmentParts())
            if (ap.getAppointmentId().equals(aptId)) list.add(ap);
        return list;
    }

    public static void saveAppointmentPart(AppointmentPart ap) {
        appendLine(APT_PARTS_FILE, ap.toFileString());
    }

    /** Replace all parts for a given appointment (used when tech submits checklist). */
    public static void replaceAppointmentParts(String aptId, List<AppointmentPart> parts) {
        List<String> lines = new ArrayList<>();
        for (AppointmentPart ap : readAllAppointmentParts())
            if (!ap.getAppointmentId().equals(aptId)) lines.add(ap.toFileString());
        for (AppointmentPart ap : parts) lines.add(ap.toFileString());
        writeAllLines(APT_PARTS_FILE, lines);
    }
    public static void savePrices(ServicePrice p) {
        List<String> l = new ArrayList<>(); l.add(p.toFileString()); writeAllLines(PRICES_FILE, l);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public static void updateManager(Manager u) {
        List<String> lines = new ArrayList<>();
        for (Manager m : readAllManagers()) lines.add(m.getUserId().equals(u.getUserId()) ? u.toFileString() : m.toFileString());
        writeAllLines(MANAGERS_FILE, lines);
    }
    public static void updateCounterStaff(CounterStaff u) {
        List<String> lines = new ArrayList<>();
        for (CounterStaff cs : readAllCounterStaff()) lines.add(cs.getUserId().equals(u.getUserId()) ? u.toFileString() : cs.toFileString());
        writeAllLines(COUNTER_STAFF_FILE, lines);
    }
    public static void updateTechnician(Technician u) {
        List<String> lines = new ArrayList<>();
        for (Technician t : readAllTechnicians()) lines.add(t.getUserId().equals(u.getUserId()) ? u.toFileString() : t.toFileString());
        writeAllLines(TECHNICIANS_FILE, lines);
    }
    public static void updateCustomer(Customer u) {
        List<String> lines = new ArrayList<>();
        for (Customer c : readAllCustomers()) lines.add(c.getUserId().equals(u.getUserId()) ? u.toFileString() : c.toFileString());
        writeAllLines(CUSTOMERS_FILE, lines);
    }
    public static void updateAppointment(Appointment u) {
        List<String> lines = new ArrayList<>();
        for (Appointment a : readAllAppointments()) lines.add(a.getAppointmentId().equals(u.getAppointmentId()) ? u.toFileString() : a.toFileString());
        writeAllLines(APPOINTMENTS_FILE, lines);
    }
    public static void updateFeedback(Feedback u) {
        List<String> lines = new ArrayList<>();
        for (Feedback f : readAllFeedbacks()) lines.add(f.getFeedbackId().equals(u.getFeedbackId()) ? u.toFileString() : f.toFileString());
        writeAllLines(FEEDBACKS_FILE, lines);
    }
    public static void updateComment(Comment u) {
        List<String> lines = new ArrayList<>();
        for (Comment c : readAllComments()) lines.add(c.getCommentId().equals(u.getCommentId()) ? u.toFileString() : c.toFileString());
        writeAllLines(COMMENTS_FILE, lines);
    }
    public static void updateVipAccount(VipAccount u) {
        List<String> lines = new ArrayList<>();
        for (VipAccount v : readAllVipAccounts()) lines.add(v.getVipId().equals(u.getVipId()) ? u.toFileString() : v.toFileString());
        writeAllLines(VIP_FILE, lines);
    }
    public static void updateCoupon(Coupon u) {
        List<String> lines = new ArrayList<>();
        for (Coupon c : readAllCoupons()) lines.add(c.getCouponId().equals(u.getCouponId()) ? u.toFileString() : c.toFileString());
        writeAllLines(COUPONS_FILE, lines);
    }
    public static void updateCarPart(CarPart u) {
        List<String> lines = new ArrayList<>();
        for (CarPart cp : readAllCarParts()) lines.add(cp.getPartId().equals(u.getPartId()) ? u.toFileString() : cp.toFileString());
        writeAllLines(CAR_PARTS_FILE, lines);
    }
    public static void updateHealthRecord(VehicleHealthRecord u) {
        List<String> lines = new ArrayList<>();
        for (VehicleHealthRecord r : readAllHealthRecords())
            lines.add(r.getRecordId().equals(u.getRecordId()) ? u.toFileString() : r.toFileString());
        writeAllLines(HEALTH_FILE, lines);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public static void deleteManager(String id) {
        List<String> l = new ArrayList<>();
        for (Manager m : readAllManagers()) if (!m.getUserId().equals(id)) l.add(m.toFileString());
        writeAllLines(MANAGERS_FILE, l);
    }
    public static void deleteCounterStaff(String id) {
        List<String> l = new ArrayList<>();
        for (CounterStaff cs : readAllCounterStaff()) if (!cs.getUserId().equals(id)) l.add(cs.toFileString());
        writeAllLines(COUNTER_STAFF_FILE, l);
    }
    public static void deleteTechnician(String id) {
        List<String> l = new ArrayList<>();
        for (Technician t : readAllTechnicians()) if (!t.getUserId().equals(id)) l.add(t.toFileString());
        writeAllLines(TECHNICIANS_FILE, l);
    }
    /**
     * Deletes a customer by ID.
     *
     * OOP Relationships demonstrated:
     *   - AGGREGATION: Does NOT delete appointments (Appointment can exist independently)
     *   - COMPOSITION: Cascade-deletes VipAccount, Coupon, Notification
     *     (these are PART-OF the customer and cannot exist without them)
     */
    public static void deleteCustomer(String id) {
        // Composition: Cascade delete VipAccount
        List<String> vipLines = new ArrayList<>();
        for (VipAccount v : readAllVipAccounts()) if (!v.getCustomerId().equals(id)) vipLines.add(v.toFileString());
        writeAllLines(VIP_FILE, vipLines);

        // Composition: Cascade delete Coupons
        List<String> couponLines = new ArrayList<>();
        for (Coupon c : readAllCoupons()) if (!c.getCustomerId().equals(id)) couponLines.add(c.toFileString());
        writeAllLines(COUPONS_FILE, couponLines);

        // Composition: Cascade delete Notifications (User-level cascade)
        clearAllNotifications(id);

        // Finally delete the customer
        List<String> l = new ArrayList<>();
        for (Customer c : readAllCustomers()) if (!c.getUserId().equals(id)) l.add(c.toFileString());
        writeAllLines(CUSTOMERS_FILE, l);
    }
    /**
     * Deletes an appointment by ID.
     *
     * OOP Relationships demonstrated:
     *   - COMPOSITION: Cascade-deletes Feedback and Comment
     *     (these are PART-OF the appointment and cannot exist without it)
     *   - ASSOCIATION: Does NOT delete AppointmentParts because parts are
     *     historical consumption records — the physical parts were already
     *     consumed (installed on the vehicle). Deleting the appointment record
     *     should NOT erase the consumption history.
     */
    public static void deleteAppointment(String id) {
        // Composition: Cascade delete Feedbacks
        List<String> fbLines = new ArrayList<>();
        for (Feedback f : readAllFeedbacks()) if (!f.getAppointmentId().equals(id)) fbLines.add(f.toFileString());
        writeAllLines(FEEDBACKS_FILE, fbLines);

        // Composition: Cascade delete Comments
        List<String> cmLines = new ArrayList<>();
        for (Comment c : readAllComments()) if (!c.getAppointmentId().equals(id)) cmLines.add(c.toFileString());
        writeAllLines(COMMENTS_FILE, cmLines);

        // Finally delete the appointment itself
        List<String> l = new ArrayList<>();
        for (Appointment a : readAllAppointments()) if (!a.getAppointmentId().equals(id)) l.add(a.toFileString());
        writeAllLines(APPOINTMENTS_FILE, l);
    }

    // ── Staff Working Days (Per-Week Format) ────────────────────────────────────
    // New format: staffId|role|weekStart(yyyy-MM-dd)|daysStr(7 chars of 0/1)
    // Old format (migrated on read): staffId|role|daysStr(7 chars)

    /** Returns working days for a specific week as boolean[7]: index 0=Mon...6=Sun. */
    public static boolean[] getStaffEmployeeScheduling(String staffId, String weekStart) {
        boolean[] days = {false, false, false, false, false, false, false}; // default all OFF

        // First pass: check if this staff has ANY per-week records (indicates migration done)
        boolean hasPerWeek = false;
        for (String line : readLines(WORK_DAYS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && p[0].trim().equals(staffId)) {
                hasPerWeek = true;
                if (p[2].trim().equals(weekStart)) {
                    for (int i = 0; i < 7; i++) {
                        days[i] = i < p[3].length() && p[3].charAt(i) == '1';
                    }
                    return days;
                }
            }
        }

        // If this staff has been migrated to per-week format but no record exists for this
        // specific week, return all OFF. The old-format fallback should NOT apply.
        if (hasPerWeek) {
            return days;
        }

        // Fallback: old 3-part format (no weekStart) — only used before migration
        for (String line : readLines(WORK_DAYS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length == 3 && p[0].trim().equals(staffId)) {
                for (int i = 0; i < 7; i++) {
                    days[i] = i < p[2].length() && p[2].charAt(i) == '1';
                }
                return days;
            }
        }
        return days;
    }

    /** Save working days for a specific week. Replaces old 3-part records with per-week records. */
    public static void saveStaffEmployeeScheduling(String staffId, String role, String weekStart, String daysStr) {
        List<String> lines = new ArrayList<>();
        boolean updated = false;
        for (String line : readLines(WORK_DAYS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && p[0].trim().equals(staffId) && p[2].trim().equals(weekStart)) {
                lines.add(staffId + "|" + role + "|" + weekStart + "|" + daysStr);
                updated = true;
            } else if (p.length == 3 && p[0].trim().equals(staffId)) {
                // Migrate old record: REPLACE with new per-week record (don't keep old)
                lines.add(staffId + "|" + role + "|" + weekStart + "|" + daysStr);
                updated = true;
            } else {
                lines.add(line);
            }
        }
        if (!updated) {
            lines.add(staffId + "|" + role + "|" + weekStart + "|" + daysStr);
        }
        writeAllLines(WORK_DAYS_FILE, lines);
    }

    /** Read all working day records for a specific week. Returns list of {staffId, role, daysStr}. */
    public static List<String[]> readAllEmployeeScheduling(String weekStart) {
        List<String[]> result = new ArrayList<>();
        boolean hasPerWeek = false;
        for (String line : readLines(WORK_DAYS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4) {
                hasPerWeek = true;
                if (p[2].trim().equals(weekStart)) {
                    result.add(new String[]{p[0].trim(), p[1].trim(), p[3].trim()});
                }
            }
        }
        // Only fall back to old format if NO per-week records exist at all
        if (!hasPerWeek) {
            for (String line : readLines(WORK_DAYS_FILE)) {
                String[] p = line.split("\\|", -1);
                if (p.length == 3) {
                    result.add(new String[]{p[0].trim(), p[1].trim(), p[2].trim()});
                }
            }
        }
        return result;
    }

    /** Batch-save draft or publish employee scheduling for the current week. */
    public static void saveStaffEmployeeSchedulingBatch(List<String[]> rows, boolean publish) {
        // Rewrite entire draft file with updated rows
        List<String> lines = new ArrayList<>(readLines(WORK_DAYS_FILE));
        // Remove old rows for the weeks present in rows
        Set<String> weekKeys = new java.util.HashSet<>();
        for (String[] r : rows) weekKeys.add(r[0] + "|" + r[2]);
        List<String> newLines = new ArrayList<>();
        for (String line : lines) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && weekKeys.contains(p[0].trim() + "|" + p[2].trim())) continue;
            newLines.add(line);
        }
        for (String[] r : rows) {
            newLines.add(r[0] + "|" + r[1] + "|" + r[2] + "|" + r[3]);
        }
        writeAllLines(WORK_DAYS_FILE, newLines);
        if (publish) publishWeek(rows.get(0)[2]);
    }

    /** Check if a staff member works on a given date (yyyy-MM-dd).
     *  Checks published schedule first, then falls back to draft. */
    public static boolean isStaffWorkingOnDay(String staffId, String date) {
        try {
            LocalDate d = LocalDate.parse(date);
            String ws = d.with(java.time.temporal.ChronoField.DAY_OF_WEEK, 1).toString();
            int idx = d.getDayOfWeek().getValue() - 1;

            // Check published first — staff My Schedule pages must use published data
            boolean hasPublished = false;
            for (String line : readLines(PUBLISHED_WD_FILE)) {
                String[] p = line.split("\\|", -1);
                if (p.length >= 4) {
                    hasPublished = true;
                    if (p[0].trim().equals(staffId) && p[2].trim().equals(ws)) {
                        return idx < p[3].length() && p[3].charAt(idx) == '1';
                    }
                }
            }
            if (hasPublished) {
                // Published data exists for this staff/role, but no entry for this specific
                // week — fall back to draft so staff still sees something
                boolean[] days = getStaffEmployeeScheduling(staffId, ws);
                return days[idx];
            }

            // No published data at all — fall back to draft
            boolean[] days = getStaffEmployeeScheduling(staffId, ws);
            return days[idx];
        } catch (Exception e) {
            return true;
        }
    }

    // ── Published Working Days (Per-Week) ──────────────────────────────────────
    // Same per-week format: staffId|role|weekStart|daysStr

    /** Publish only the specified week — copy that week's draft to published. */
    public static void publishWeek(String weekStart) {
        List<String> publishedLines = new ArrayList<>();
        // Keep published lines for other weeks
        for (String line : readLines(PUBLISHED_WD_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && !p[2].trim().equals(weekStart)) {
                publishedLines.add(line);
            }
            // Drop old-format (3-part) records — they will be migrated below if needed
        }
        // Add this week's draft data
        boolean foundPerWeek = false;
        for (String line : readLines(WORK_DAYS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && p[2].trim().equals(weekStart)) {
                publishedLines.add(line);
                foundPerWeek = true;
            }
        }
        // Only migrate old-format records if NO per-week records exist for this week
        if (!foundPerWeek) {
            for (String line : readLines(WORK_DAYS_FILE)) {
                String[] p = line.split("\\|", -1);
                if (p.length == 3) {
                    publishedLines.add(p[0] + "|" + p[1] + "|" + weekStart + "|" + p[2]);
                }
            }
        }
        writeAllLines(PUBLISHED_WD_FILE, publishedLines);
    }

    /** Unpublish a specific week — remove its entries from published. */
    public static void unpublishWeek(String weekStart) {
        List<String> publishedLines = new ArrayList<>();
        for (String line : readLines(PUBLISHED_WD_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && !p[2].trim().equals(weekStart)) {
                publishedLines.add(line);
            }
        }
        writeAllLines(PUBLISHED_WD_FILE, publishedLines);
    }

    /** Check if a specific week has been published. */
    public static boolean isWeekPublished(String weekStart) {
        for (String line : readLines(PUBLISHED_WD_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && p[2].trim().equals(weekStart)) return true;
        }
        // Only fall back to old format if published file has ONLY old-format data
        // (no per-week records at all — meaning migration hasn't happened yet)
        boolean hasPerWeek = false;
        for (String line : readLines(PUBLISHED_WD_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4) { hasPerWeek = true; break; }
        }
        if (!hasPerWeek) {
            for (String line : readLines(PUBLISHED_WD_FILE)) {
                String[] p = line.split("\\|", -1);
                if (p.length == 3) return true;
            }
        }
        return false;
    }

    /** Check if any draft working-day records exist for a specific week.
     *  Used by the Copy Last Week button to pre-validate before attempting copy. */
    public static boolean hasEmployeeSchedulingForWeek(String weekStart) {
        for (String line : readLines(WORK_DAYS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && p[2].trim().equals(weekStart)) return true;
        }
        return false;
    }

    /** Check if a week has at least one staff member with a working day set.
     *  Used to distinguish "all red" weeks from weeks with actual data. */
    public static boolean hasAnyWorkingDaysInWeek(String weekStart) {
        for (String line : readLines(WORK_DAYS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && p[2].trim().equals(weekStart)
                    && p[3].trim().contains("1")) return true;
        }
        return false;
    }

    /** Copy working days from the previous week to the current week.
     *  @return true if previous-week data was found and copied; false if no data exists */
    public static boolean copyFromPreviousEmployeeSchedulingWeek(String weekStart) {
        LocalDate current = LocalDate.parse(weekStart);
        String prevWeekStr = current.minusWeeks(1).toString();
        boolean hasPrevWeek = false;
        for (String line : readLines(WORK_DAYS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && p[2].trim().equals(prevWeekStr)) {
                saveStaffEmployeeScheduling(p[0].trim(), p[1].trim(), weekStart, p[3].trim());
                hasPrevWeek = true;
            }
        }
        // Only fall back to old format if no per-week records exist for the previous week
        if (!hasPrevWeek) {
            for (String line : readLines(WORK_DAYS_FILE)) {
                String[] p = line.split("\\|", -1);
                if (p.length == 3) {
                    saveStaffEmployeeScheduling(p[0].trim(), p[1].trim(), weekStart, p[2].trim());
                    hasPrevWeek = true;
                }
            }
        }
        return hasPrevWeek;
    }

    /** Get published working days for a staff member on a specific week. */
    public static boolean[] getPublishedEmployeeScheduling(String staffId, String weekStart) {
        // Check if this staff has any per-week published records
        boolean hasPerWeek = false;
        for (String line : readLines(PUBLISHED_WD_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 4 && p[0].trim().equals(staffId)) {
                hasPerWeek = true;
                if (p[2].trim().equals(weekStart)) {
                    boolean[] days = new boolean[7];
                    for (int i = 0; i < 7; i++) {
                        days[i] = i < p[3].length() && p[3].charAt(i) == '1';
                    }
                    return days;
                }
            }
        }
        // If staff has per-week records but none for this week, return draft as fallback
        if (hasPerWeek) {
            return getStaffEmployeeScheduling(staffId, weekStart);
        }
        // Fallback: old 3-part format
        for (String line : readLines(PUBLISHED_WD_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length == 3 && p[0].trim().equals(staffId)) {
                boolean[] days = new boolean[7];
                for (int i = 0; i < 7; i++) {
                    days[i] = i < p[2].length() && p[2].charAt(i) == '1';
                }
                return days;
            }
        }
        return getStaffEmployeeScheduling(staffId, weekStart);
    }

    /** Check if any published data exists at all. */
    public static boolean hasPublishedEmployeeScheduling() {
        return !readLines(PUBLISHED_WD_FILE).isEmpty();
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public static User authenticate(String username, String password) {
        for (Manager m : readAllManagers())           if (m.getUsername().equals(username) && m.getPassword().equals(password)) return m;
        for (CounterStaff cs : readAllCounterStaff()) if (cs.getUsername().equals(username) && cs.getPassword().equals(password)) return cs;
        for (Technician t : readAllTechnicians())     if (t.getUsername().equals(username) && t.getPassword().equals(password)) return t;
        for (Customer c : readAllCustomers())         if (c.getUsername().equals(username) && c.getPassword().equals(password)) return c;
        return null;
    }

    // ── ID Generation ─────────────────────────────────────────────────────────

    public static String generateManagerId()      { return generateId("MGR",  readAllManagers().stream().mapToInt(m->extractNum(m.getUserId(),"MGR")).max().orElse(0)); }
    public static String generateCounterStaffId() { return generateId("CS",   readAllCounterStaff().stream().mapToInt(cs->extractNum(cs.getUserId(),"CS")).max().orElse(0)); }
    public static String generateTechnicianId()   { return generateId("TECH", readAllTechnicians().stream().mapToInt(t->extractNum(t.getUserId(),"TECH")).max().orElse(0)); }
    public static String generateCustomerId()     { return generateId("CUST", readAllCustomers().stream().mapToInt(c->extractNum(c.getUserId(),"CUST")).max().orElse(0)); }
    public static String generateAppointmentId()  { return generateId("APT",  readAllAppointments().stream().mapToInt(a->extractNum(a.getAppointmentId(),"APT")).max().orElse(0)); }
    public static String generatePaymentId()      { return generateId("PAY",  readAllPayments().stream().mapToInt(p->extractNum(p.getPaymentId(),"PAY")).max().orElse(0)); }
    public static String generateReceiptNumber()  { return generateId("RCP",  readAllPayments().stream().mapToInt(p->extractNum(p.getReceiptNumber(),"RCP")).max().orElse(0)); }
    public static String generateFeedbackId()     { return generateId("FB",   readAllFeedbacks().stream().mapToInt(f->extractNum(f.getFeedbackId(),"FB")).max().orElse(0)); }
    public static String generateCommentId()      { return generateId("CMT",  readAllComments().stream().mapToInt(c->extractNum(c.getCommentId(),"CMT")).max().orElse(0)); }
    public static String generateVipId()          { return generateId("VIP",  readAllVipAccounts().stream().mapToInt(v->extractNum(v.getVipId(),"VIP")).max().orElse(0)); }
    public static String generateCouponId()       { return generateId("CPN",  readAllCoupons().stream().mapToInt(c->extractNum(c.getCouponId(),"CPN")).max().orElse(0)); }
    public static String generateCarPartId()      { return generateId("PART", readAllCarParts().stream().mapToInt(cp->extractNum(cp.getPartId(),"PART")).max().orElse(0)); }
    public static String generateHealthRecordId() { return generateId("VHR",  readAllHealthRecords().stream().mapToInt(r->extractNum(r.getRecordId(),"VHR")).max().orElse(0)); }

    private static int    extractNum(String id, String prefix) { try { return Integer.parseInt(id.replace(prefix,"")); } catch(Exception e){ return 0; } }
    private static String generateId(String prefix, int max)   { return prefix + String.format("%03d", max + 1); }

    // ── Utilities ─────────────────────────────────────────────────────────────

    public static boolean isUsernameExists(String username) {
        if (username == null || username.isEmpty()) return false;
        for (Manager m : readAllManagers())           if (m.getUsername().equals(username)) return true;
        for (CounterStaff cs : readAllCounterStaff()) if (cs.getUsername().equals(username)) return true;
        for (Technician t : readAllTechnicians())     if (t.getUsername().equals(username)) return true;
        for (Customer c : readAllCustomers())         if (c.getUsername().equals(username)) return true;
        return false;
    }

    /**
     * Returns true if the technician has been scheduled (published) to work on the given date.
     * If the manager has not published a schedule for this technician, or the day is marked OFF,
     * returns false — indicating the technician is "Off / Not Available" for that date.
     */
    public static boolean isTechnicianScheduledOnDate(String techId, String date) {
        try {
            LocalDate d = LocalDate.parse(date);
            String ws = d.with(java.time.temporal.ChronoField.DAY_OF_WEEK, 1).toString();
            int dayIdx = d.getDayOfWeek().getValue() - 1; // Mon=0 ... Sun=6

            // Check if this tech has any per-week published records
            boolean hasPerWeek = false;
            for (String line : readLines(PUBLISHED_WD_FILE)) {
                String[] p = line.split("\\|", -1);
                if (p.length >= 4 && p[0].trim().equals(techId)) {
                    hasPerWeek = true;
                    if (p[2].trim().equals(ws)) {
                        String daysStr = p[3].trim();
                        return dayIdx < daysStr.length() && daysStr.charAt(dayIdx) == '1';
                    }
                }
            }
            // If tech has per-week records but none for this week, fall back to draft
            if (hasPerWeek) {
                boolean[] draftDays = getStaffEmployeeScheduling(techId, ws);
                return dayIdx < draftDays.length && draftDays[dayIdx];
            }

            // Fallback: old 3-part format (no weekStart) — only used before migration
            for (String line : readLines(PUBLISHED_WD_FILE)) {
                String[] p = line.split("\\|", -1);
                if (p.length == 3 && p[0].trim().equals(techId)) {
                    String daysStr = p[2].trim();
                    return dayIdx < daysStr.length() && daysStr.charAt(dayIdx) == '1';
                }
            }
        } catch (Exception e) {
            return false;
        }
        // No published data at all for this technician — fall back to draft
        return isStaffWorkingOnDay(techId, date);
    }

    /**
     * Returns true if the technician is "Off" on the given date — i.e. not scheduled by manager
     * (not in published working days, or day is marked 0).
     * Counter staff cannot assign appointments to technicians who are Off.
     */
    public static boolean isTechnicianOffOnDay(String techId, String date) {
        return !isTechnicianScheduledOnDate(techId, date);
    }

    public static boolean isTechnicianAvailable(String techId, String date, String time, String serviceType) {
        // Check published working days first — if manager has not scheduled this technician, they are OFF
        if (!isTechnicianScheduledOnDate(techId, date)) return false;
        int newStart = parseTimeToMinutes(time);
        int newEnd   = newStart + (serviceType.equalsIgnoreCase("NORMAL") ? 60 : 180);
        for (Appointment a : readAllAppointments()) {
            if (!a.getTechnicianId().equals(techId) || !a.getDate().equals(date)) continue;
            if ("COMPLETED".equalsIgnoreCase(a.getStatus())) continue;
            int es = parseTimeToMinutes(a.getTime());
            int ee = es + (a.getServiceType().equalsIgnoreCase("NORMAL") ? 60 : 180);
            if (newStart < ee && newEnd > es) return false;
        }
        return true;
    }

    private static int parseTimeToMinutes(String time) {
        try { String[] p = time.split(":"); return Integer.parseInt(p[0])*60+Integer.parseInt(p[1]); }
        catch(Exception e){ return 0; }
    }

    // ── Find ──────────────────────────────────────────────────────────────────

    public static Manager      findManagerById(String id)             { return readAllManagers().stream().filter(m->m.getUserId().equals(id)).findFirst().orElse(null); }
    public static CounterStaff findCounterStaffById(String id)        { return readAllCounterStaff().stream().filter(cs->cs.getUserId().equals(id)).findFirst().orElse(null); }
    public static Technician   findTechnicianById(String id)          { return readAllTechnicians().stream().filter(t->t.getUserId().equals(id)).findFirst().orElse(null); }
    public static Customer     findCustomerById(String id)            { return readAllCustomers().stream().filter(c->c.getUserId().equals(id)).findFirst().orElse(null); }
    public static Appointment  findAppointmentById(String id)         { return readAllAppointments().stream().filter(a->a.getAppointmentId().equals(id)).findFirst().orElse(null); }
    public static Feedback     findFeedbackByAppointmentId(String id) { return readAllFeedbacks().stream().filter(f->f.getAppointmentId().equals(id)).findFirst().orElse(null); }
    public static Comment      findCommentByAppointmentId(String id)  { return readAllComments().stream().filter(c->c.getAppointmentId().equals(id)).findFirst().orElse(null); }
    public static Payment      findPaymentByAppointmentId(String id)  { return readAllPayments().stream().filter(p->p.getAppointmentId().equals(id)).findFirst().orElse(null); }
    public static VipAccount   findVipByCustomerId(String id)         { return readAllVipAccounts().stream().filter(v->v.getCustomerId().equals(id)).findFirst().orElse(null); }
    public static VipAccount   findVipByPhone(String phone)           { return readAllVipAccounts().stream().filter(v->v.getPhone().equals(phone)).findFirst().orElse(null); }
    public static CarPart      findCarPartById(String id)             { return readAllCarParts().stream().filter(cp->cp.getPartId().equals(id)).findFirst().orElse(null); }
    public static VehicleHealthRecord findHealthRecordByAppointmentId(String aptId) { return readAllHealthRecords().stream().filter(r->r.getAppointmentId().equals(aptId)).findFirst().orElse(null); }

    /** Generic lookup: searches Manager, CounterStaff, Technician, then Customer. */
    public static asc.model.User findUserById(String id) {
        Manager      m  = findManagerById(id);      if (m  != null) return m;
        CounterStaff cs = findCounterStaffById(id); if (cs != null) return cs;
        Technician  t  = findTechnicianById(id);  if (t  != null) return t;
        Customer    c  = findCustomerById(id);     if (c  != null) return c;
        return null;
    }

    public static List<Coupon> findCouponsByCustomerId(String id) {
        List<Coupon> l = new ArrayList<>();
        for (Coupon c : readAllCoupons()) if (c.getCustomerId().equals(id)) l.add(c);
        return l;
    }

    public static List<VehicleHealthRecord> findHealthRecordsByCustomerId(String customerId) {
        List<VehicleHealthRecord> list = new ArrayList<>();
        for (VehicleHealthRecord r : readAllHealthRecords()) if (r.getCustomerId().equals(customerId)) list.add(r);
        return list;
    }

    /**
     * Returns all unused, non-expired coupons for a customer.
     * Called during counter checkout to populate the coupon selector.
     */
    public static List<Coupon> findAvailableCoupons(String customerId) {
        List<Coupon> result = new ArrayList<>();
        for (Coupon c : readAllCoupons()) {
            if (!c.getCustomerId().equals(customerId)) continue;
            if (c.isUsed()) continue;
            if (c.isExpired()) continue;
            result.add(c);
        }
        return result;
    }

    /**
     * Physically removes all expired coupons from the data file.
     * Also removes used coupons older than 90 days to prevent file bloat.
     * Called at application startup and during monthly refresh.
     * @return number of coupons removed
     */
    public static int cleanupExpiredCoupons() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String cutoff = LocalDate.now().minusDays(90).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<Coupon> all = readAllCoupons();
        List<Coupon> keep = new ArrayList<>();
        int removed = 0;

        for (Coupon c : all) {
            // Remove if expired
            if (c.getExpiryDate() != null && c.getExpiryDate().compareTo(today) < 0) {
                removed++;
                continue;
            }
            // Remove used coupons older than 90 days (housekeeping)
            if (c.isUsed() && c.getIssuedDate() != null && c.getIssuedDate().compareTo(cutoff) < 0) {
                removed++;
                continue;
            }
            keep.add(c);
        }

        if (removed > 0) {
            List<String> lines = new ArrayList<>();
            for (Coupon c : keep) lines.add(c.toFileString());
            writeAllLines(COUPONS_FILE, lines);
            System.out.println("[CPN] Cleaned up " + removed + " expired/stale coupons.");
        }
        return removed;
    }

    /**
     * Full monthly coupon refresh:
     * 1. Cleans up all expired coupons from the data file
     * 2. Iterates all VIP accounts and issues new monthly coupons for
     *    those eligible (new calendar month, tier requires one)
     * Call this at app startup so coupons are always current.
     * @return number of newly issued coupons
     */
    public static int autoRefreshAllMonthlyCoupons() {
        // Step 1: Remove expired coupons from file
        int cleaned = cleanupExpiredCoupons();

        // Step 2: Issue new monthly coupons for all eligible VIPs
        int issued = 0;
        for (VipAccount vip : readAllVipAccounts()) {
            java.util.List<Coupon> list = issueMonthlyCouponIfNeeded(vip);
            issued += list.size();
        }

        if (cleaned > 0 || issued > 0) {
            System.out.println("[CPN] Monthly refresh: cleaned " + cleaned
                    + " expired, issued " + issued + " new coupons.");
        }
        return issued;
    }

    /**
     * Checks if a VIP account needs monthly coupons and issues them for ALL
     * eligible tiers.  When a customer upgrades within the same month (e.g.
     * BRONZE → GOLD), both BRONZE and GOLD coupons are issued.
     * From the following month onward only the current (highest) tier is issued.
     *
     * @return list of newly issued coupons (empty if none needed)
     */
    public static java.util.List<Coupon> issueMonthlyCouponIfNeeded(VipAccount vip) {
        if (vip == null || !vip.needsMonthlyCoupon())
            return java.util.Collections.emptyList();

        String thisMonth = YearMonth.now().toString();
        boolean isNewMonth  = !thisMonth.equals(vip.getLastCouponMonth());
        boolean isFirstTime = vip.getLastCouponMonth() == null
                           || vip.getLastCouponMonth().isEmpty()
                           || "N/A".equals(vip.getLastCouponMonth());

        // Determine which tier levels to issue:
        //   First time ever → all tiers from BRONZE (1) up to current tier
        //   New month       → current tier only (only the highest)
        //   Same month upgrade → from lastCouponTier+1 up to current tier
        int startLevel;
        if (isFirstTime) {
            startLevel = 1;
        } else if (isNewMonth) {
            startLevel = VipAccount.tierLevel(vip.getTier());
        } else {
            startLevel = VipAccount.tierLevel(vip.getLastCouponTier()) + 1;
        }
        int endLevel = VipAccount.tierLevel(vip.getTier());

        String today  = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String expiry = LocalDate.now()
                .withDayOfMonth(LocalDate.now().lengthOfMonth())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String monthTag = YearMonth.now().toString().replace("-", "");

        java.util.List<Coupon> issued = new java.util.ArrayList<>();

        for (int level = startLevel; level <= endLevel; level++) {
            String tierName  = VipAccount.tierName(level);
            double value     = VipAccount.couponValueForTier(tierName);
            if (value <= 0) continue;

            String couponId = generateCouponId();
            String code     = "VIP-" + tierName + "-" + monthTag;

            // Idempotency guard – skip if this customer already has this code
            boolean already = readAllCoupons().stream()
                    .anyMatch(c -> code.equals(c.getCode())
                                && vip.getCustomerId().equals(c.getCustomerId()));
            if (already) continue;

            Coupon coupon   = new Coupon(couponId, vip.getCustomerId(), code,
                    "FIXED", value, expiry, false, "SYSTEM", today);
            saveCoupon(coupon);
            // ── Notification: customer gets coupon ─────────────────────────
            saveNotification(new Notification(vip.getCustomerId(), Notification.COUPON_ISSUED,
                    "Monthly Coupon Issued",
                    "Your " + tierName + " VIP coupon (RM " + String.format("%.2f", value)
                    + " off) has been issued! Code: " + code + ". Expires: " + expiry + "."));
            issued.add(coupon);
        }

        if (!issued.isEmpty()) {
            vip.markCouponIssued();
            updateVipAccount(vip);
        }

        return issued;
    }

    /**
     * Records a customer self top-up payment (TOPUP- prefix) and awards VIP points.
     * Returns the Payment object.
     */
    public static Payment processCustomerTopup(String customerId, double amount, String method) {
        String today   = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Payment pay = new Payment(generatePaymentId(), "TOPUP-" + customerId,
                customerId, amount, today, method, generateReceiptNumber());
        savePayment(pay);
        // Award VIP points for top-up
        VipAccount vip = findVipByCustomerId(customerId);
        if (vip != null) {
            vip.addPointsFromPayment(amount, 1.0);
            updateVipAccount(vip);
        }
        return pay;
    }

    /** Auto-transition SCHEDULED appointments past their date to MISSED. */
    public static void autoMarkMissedAppointments() {
        LocalDate today = LocalDate.now();
        boolean changed = false;
        for (Appointment a : readAllAppointments()) {
            if (!Appointment.STATUS_SCHEDULED.equals(a.getStatus())) continue;
            try {
                LocalDate apptDate = LocalDate.parse(a.getDate());
                if (apptDate.isBefore(today)) {
                    a.setStatus(Appointment.STATUS_MISSED);
                    updateAppointment(a);
                    changed = true;
                }
            } catch (Exception ignored) { /* skip malformed dates */ }
        }
        if (changed) System.out.println("[APPT] Auto-marked missed appointments.");
    }

    // ── Notifications ──────────────────────────────────────────────────────────

    public static void saveNotification(Notification n) {
        appendLine(NOTIFICATIONS_FILE, n.toFileString());
    }

    public static List<Notification> readAllNotifications() {
        List<Notification> list = new ArrayList<>();
        for (String line : readLines(NOTIFICATIONS_FILE)) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 7) {
                list.add(new Notification(p[0], p[1], p[2], p[3], p[4], p[5],
                        "1".equals(p[6])));
            }
        }
        return list;
    }

    /** Notifications for a specific user, newest first. */
    public static List<Notification> getNotificationsForUser(String userId) {
        List<Notification> list = new ArrayList<>();
        for (Notification n : readAllNotifications()) {
            if (n.getUserId().equals(userId)) list.add(n);
        }
        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())); // newest first
        return list;
    }

    /** Count of unread notifications for a user. */
    public static int getUnreadCount(String userId) {
        int count = 0;
        for (Notification n : readAllNotifications()) {
            if (n.getUserId().equals(userId) && !n.isRead()) count++;
        }
        return count;
    }

    /** Delete a single notification by ID (rewrite file without it). */
    public static void deleteNotification(String notifId) {
        List<String> lines = readLines(NOTIFICATIONS_FILE);
        lines.removeIf(line -> line.startsWith(notifId + "|"));
        writeAllLines(NOTIFICATIONS_FILE, lines);
    }

    /** Delete all notifications for a user (rewrite file without them). */
    public static void clearAllNotifications(String userId) {
        List<String> lines = readLines(NOTIFICATIONS_FILE);
        lines.removeIf(line -> {
            String[] p = line.split("\\|", -1);
            return p.length >= 2 && p[1].equals(userId);
        });
        writeAllLines(NOTIFICATIONS_FILE, lines);
    }

    /** Duplicate check: returns true if a notification with same (userId, type, message) already exists today. */
    public static boolean notificationExistsToday(String userId, String type, String message) {
        String today = java.time.LocalDate.now().toString(); // "2026-05-28"
        for (Notification n : readAllNotifications()) {
            if (n.getUserId().equals(userId) && n.getType().equals(type)
                    && n.getMessage().equals(message)
                    && n.getTimestamp().startsWith(today)) {
                return true;
            }
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ── Object Reference Resolution (Association) ─────────────────────────────
    // ══════════════════════════════════════════════════════════════════════════
    //
    // OOP Concept — Association Resolution:
    // After reading entities from flat files, this method populates the transient
    // object-reference fields on each entity so that code can navigate
    // relationships directly (e.g. appointment.getCustomer().getName())
    // instead of looking up by ID every time.
    //
    // The ID fields are kept for file persistence; the object references exist
    // only in memory at runtime and are not serialized.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Resolves all object references across the entire data model.
     * Should be called once at application startup or after a bulk data load.
     */
    public static void resolveReferences() {
        // Build lookup maps for O(1) resolution instead of repeated linear scans
        Map<String, Customer>     customerMap  = new HashMap<>();
        Map<String, Technician>   techMap      = new HashMap<>();
        Map<String, CounterStaff> csMap        = new HashMap<>();
        Map<String, Appointment>  aptMap       = new HashMap<>();
        Map<String, CarPart>      carPartMap   = new HashMap<>();

        for (Customer c : readAllCustomers())     customerMap.put(c.getUserId(), c);
        for (Technician t : readAllTechnicians()) techMap.put(t.getUserId(), t);
        for (CounterStaff cs : readAllCounterStaff()) csMap.put(cs.getUserId(), cs);
        for (Appointment a : readAllAppointments())  aptMap.put(a.getAppointmentId(), a);
        for (CarPart cp : readAllCarParts())          carPartMap.put(cp.getPartId(), cp);

        // Resolve Appointment → Customer, Technician, CounterStaff
        for (Appointment a : aptMap.values()) {
            a.setCustomer(customerMap.get(a.getCustomerId()));
            a.setTechnician(techMap.get(a.getTechnicianId()));
            a.setCounterStaff(csMap.get(a.getCounterStaffId()));
        }

        // Resolve Payment → Appointment, Customer
        for (Payment p : readAllPayments()) {
            p.setAppointment(aptMap.get(p.getAppointmentId()));
            p.setCustomer(customerMap.get(p.getCustomerId()));
        }

        // Resolve VipAccount → Customer
        for (VipAccount v : readAllVipAccounts()) {
            v.setCustomer(customerMap.get(v.getCustomerId()));
        }

        // Resolve Feedback → Appointment, Technician
        for (Feedback f : readAllFeedbacks()) {
            f.setAppointment(aptMap.get(f.getAppointmentId()));
            f.setTechnician(techMap.get(f.getTechnicianId()));
        }

        // Resolve Comment → Appointment, Customer
        for (Comment c : readAllComments()) {
            c.setAppointment(aptMap.get(c.getAppointmentId()));
            c.setCustomer(customerMap.get(c.getCustomerId()));
        }

        // ── COMPOSITION: Populate Appointment.feedbacks ───────────────────
        //  Appointment COMPOSES Feedback (1:N, PART-OF the appointment)
        for (Feedback f : readAllFeedbacks()) {
            Appointment a = aptMap.get(f.getAppointmentId());
            if (a != null) a.addFeedback(f);
        }

        // ── COMPOSITION: Populate Appointment.comments ─────────────────────
        //  Appointment COMPOSES Comment (1:N, PART-OF the appointment)
        for (Comment c : readAllComments()) {
            Appointment a = aptMap.get(c.getAppointmentId());
            if (a != null) a.addComment(c);
        }

        // Resolve Coupon → Customer, issuedByStaff (CounterStaff)
        for (Coupon cp : readAllCoupons()) {
            cp.setCustomer(customerMap.get(cp.getCustomerId()));
            cp.setIssuedByStaff(csMap.get(cp.getIssuedBy()));
        }

        // Resolve VehicleHealthRecord → Appointment, Customer, Technician
        for (VehicleHealthRecord r : readAllHealthRecords()) {
            r.setAppointment(aptMap.get(r.getAppointmentId()));
            r.setCustomer(customerMap.get(r.getCustomerId()));
            r.setTechnician(techMap.get(r.getTechnicianId()));
        }

        // Resolve AppointmentPart → Appointment, CarPart
        for (AppointmentPart ap : readAllAppointmentParts()) {
            ap.setAppointment(aptMap.get(ap.getAppointmentId()));
            ap.setCarPart(carPartMap.get(ap.getPartId()));
        }

        // Resolve Notification → User (any type)
        for (Notification n : readAllNotifications()) {
            User u = findUserById(n.getUserId());
            n.setUser(u);
        }

        // ── AGGREGATION: Populate Customer.appointments ─────────────────
        //  Customer AGGREGATES Appointment (child can exist without parent)
        for (Customer c : customerMap.values()) {
            c.getAppointments().clear();
            for (Appointment a : aptMap.values()) {
                if (c.getUserId().equals(a.getCustomerId())) {
                    c.addAppointment(a);
                }
            }
        }

        // ── COMPOSITION: Populate Customer.vipAccount ────────────────────
        //  Customer COMPOSES VipAccount (1:1, PART-OF the customer)
        Map<String, VipAccount> vipMap = new HashMap<>();
        for (VipAccount v : readAllVipAccounts()) vipMap.put(v.getCustomerId(), v);
        for (Customer c : customerMap.values()) {
            c.setVipAccount(vipMap.get(c.getUserId()));
        }

        // ── COMPOSITION: Populate Customer.coupons ──────────────────────
        //  Customer COMPOSES Coupon (1:N, PART-OF the customer)
        List<Coupon> allCoupons = readAllCoupons();
        for (Customer c : customerMap.values()) {
            c.getCoupons().clear();
            for (Coupon cp : allCoupons) {
                if (c.getUserId().equals(cp.getCustomerId())) {
                    c.addCoupon(cp);
                }
            }
        }

        // ── COMPOSITION: Populate User.notifications ────────────────────
        //  User COMPOSES Notification (PART-OF the user)
        for (Notification n : readAllNotifications()) {
            User u = findUserById(n.getUserId());
            if (u != null) u.addNotification(n);
        }

        // ── ASSOCIATION: Populate Appointment.parts ──────────────────────
        //  Appointment references AppointmentPart as consumption history.
        //  Parts are NOT deleted when appointment is deleted (Association).
        List<AppointmentPart> allParts = readAllAppointmentParts();
        for (Appointment a : aptMap.values()) {
            a.getParts().clear();
            for (AppointmentPart ap : allParts) {
                if (a.getAppointmentId().equals(ap.getAppointmentId())) {
                    a.addPart(ap);
                    // ── AGGREGATION: Resolve CarPart in AppointmentPart ─────
                    if (ap.getCarPart() == null) {
                        ap.setCarPart(carPartMap.get(ap.getPartId()));
                    }
                }
            }
        }
    }

    /**
     * Resolves references for a single appointment and its related entities.
     * Useful after creating/updating an appointment to keep references fresh.
     */
    public static void resolveAppointmentReferences(Appointment apt) {
        apt.setCustomer(findCustomerById(apt.getCustomerId()));
        apt.setTechnician(findTechnicianById(apt.getTechnicianId()));
        apt.setCounterStaff(findCounterStaffById(apt.getCounterStaffId()));
        // Also resolve carPart for this appointment's parts
        for (AppointmentPart ap : apt.getParts()) {
            if (ap.getCarPart() == null) {
                ap.setCarPart(findCarPartById(ap.getPartId()));
            }
        }
    }

    // ── Low-level I/O ─────────────────────────────────────────────────────────

    private static List<String> readLines(String path) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) if (!line.trim().isEmpty()) lines.add(line.trim());
        } catch (IOException e) { handleError("Read error: "+path, e); }
        return lines;
    }

    private static void appendLine(String path, String line) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path, true))) { pw.println(line); }
        catch (IOException e) { handleError("Write error: "+path, e); }
    }

    private static void writeAllLines(String path, List<String> lines) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path, false))) { for (String l : lines) pw.println(l); }
        catch (IOException e) { handleError("Write error: "+path, e); }
    }
}
