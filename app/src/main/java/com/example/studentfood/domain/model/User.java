package com.example.studentfood.domain.model;

import java.io.Serializable;

/**
 * Lớp User - Lớp cơ sở cho mọi người dùng.
 * Đã chuẩn hóa thành Pure POJO theo Clean Architecture.
 */
public abstract class User implements Serializable {

    // ================== PERSISTENCE PROPERTIES (DB) ==================
    protected String userId;
    protected String username; 
    protected String password; 
    protected String email;
    protected String fullName;
    protected String phoneNumber;
    protected Role role;
    protected int status;       // Check (0: Banned, 1: Active, 2: Unverified)
    protected long createdAt;
    protected long birth;
    
    // ================== RELATIONSHIPS (JOINS) ==================
    protected Image avatar;
    protected Location location;

    public enum Role {
        STUDENT, OWNER, ADMIN
    }

    public User() {
        this.createdAt = System.currentTimeMillis();
        this.status = 1; 
    }

    // ================== GETTERS & SETTERS ==================

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Image getAvatar() { return avatar; }
    public void setAvatar(Image avatar) { this.avatar = avatar; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getBirth() { return birth; }
    public void setBirth(long birth) { this.birth = birth; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public abstract void displayRoleSpecificMenu();

    // ================== HELPER WRAPPERS (FOR CONVENIENCE) ==================

    public String getAvatarUrl() {
        return (avatar != null) ? avatar.getImageValue() : "";
    }

    public void setAvatarUrl(String url) {
        if (avatar == null) {
            avatar = new Image();
            avatar.setType(Image.ImageType.AVATAR);
            avatar.setRefType(Image.RefType.USER);
            avatar.setRefId(userId);
        }
        avatar.setImageValue(url);
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            avatar.setSource(Image.ImageSource.URL);
        } else {
            avatar.setSource(Image.ImageSource.LOCAL);
        }
    }

    public String getAddress() {
        return (location != null) ? location.getAddress() : "";
    }

    public void setAddress(String address) {
        if (location == null) {
            location = new Location();
            location.setRefId(userId);
        }
        location.setAddress(address);
    }

    @Override
    public String toString() {
        return "User{" + "userId='" + userId + '\'' + ", username='" + username + '\'' + ", role=" + role + '}';
    }
}
