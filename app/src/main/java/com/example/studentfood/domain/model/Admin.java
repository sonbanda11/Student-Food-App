package com.example.studentfood.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Admin quản trị hệ thống - Kế thừa từ User
 * Đã chuẩn hóa thành Pure POJO theo Clean Architecture.
 */
public class Admin extends User {

    // ================== PERSISTENCE PROPERTIES (DB) ==================
    private String staffId;            // Mã nhân viên quản trị
    private int adminLevel;            // 1: Moderator, 2: Super Admin
    private List<String> permissions;  // Danh sách các quyền (VD: APPROVE_SHOP, LOCK_USER)

    public Admin() {
        super();
        this.permissions = new ArrayList<>();
        setRole(Role.ADMIN);
    }

    // ================== GETTERS & SETTERS ==================
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public int getAdminLevel() { return adminLevel; }
    public void setAdminLevel(int adminLevel) { this.adminLevel = adminLevel; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    @Override
    public void displayRoleSpecificMenu() {
        // TODO: Implement UI logic for Admin menu
    }

    @Override
    public String toString() {
        return "Admin{" +
                "staffId='" + staffId + '\'' +
                ", adminLevel=" + adminLevel +
                ", " + super.toString() +
                '}';
    }
}
