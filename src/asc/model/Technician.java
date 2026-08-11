package asc.model;

import asc.gui.technician.TechnicianDashboard;

public class Technician extends User {
    public Technician(String userId, String username, String password,
                      String name, String email, String phone, String gender) {
        super(userId, username, password, name, email, phone, "TECHNICIAN", gender);
    }

    @Override public String getDisplayInfo() {
        return "[Technician] " + name + " | " + email + " | " + phone;
    }

    @Override public void openDashboard() {
        new TechnicianDashboard(this).setVisible(true);
    }
}
