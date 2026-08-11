package asc.model;

import asc.gui.manager.ManagerDashboard;

public class Manager extends User {
    public Manager(String userId, String username, String password,
                   String name, String email, String phone, String gender) {
        super(userId, username, password, name, email, phone, "MANAGER", gender);
    }

    @Override public String getDisplayInfo() {
        return "[Manager] " + name + " | " + email + " | " + phone;
    }

    @Override public void openDashboard() {
        new ManagerDashboard(this).setVisible(true);
    }
}
