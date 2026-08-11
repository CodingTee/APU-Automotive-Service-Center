package asc.model;

import asc.gui.counter.CounterStaffDashboard;

public class CounterStaff extends User {
    public CounterStaff(String userId, String username, String password,
                        String name, String email, String phone, String gender) {
        super(userId, username, password, name, email, phone, "COUNTER_STAFF", gender);
    }

    @Override public String getDisplayInfo() {
        return "[Counter Staff] " + name + " | " + email + " | " + phone;
    }

    @Override public void openDashboard() {
        new CounterStaffDashboard(this).setVisible(true);
    }
}
