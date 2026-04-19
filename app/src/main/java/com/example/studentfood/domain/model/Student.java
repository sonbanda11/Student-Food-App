package com.example.studentfood.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model Student: Người dùng sinh viên.
 */
public class Student extends User {

    // ================== PERSISTENCE PROPERTIES (DB) ==================
    private String universityName;
    private float rewardPoints;
    private int totalReviews; // Cache
    
    // Relationship IDs
    private List<String> favoriteShopIds = new ArrayList<>();
    private List<String> searchHistory = new ArrayList<>();

    public Student() {
        super();
        setRole(Role.STUDENT);
    }

    // ================== GETTERS & SETTERS ==================

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    private String studentId;

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }

    public List<String> getFavoriteShopIds() { return favoriteShopIds; }
    public void setFavoriteShopIds(List<String> favoriteShopIds) { this.favoriteShopIds = favoriteShopIds; }

    public float getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(float rewardPoints) { this.rewardPoints = rewardPoints; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    public List<String> getSearchHistory() { return searchHistory; }
    public void setSearchHistory(List<String> searchHistory) { this.searchHistory = searchHistory; }

    @Override
    public void displayRoleSpecificMenu() {
        // TODO: Implement UI logic for Student menu
    }

    @Override
    public String toString() {
        return "Student{" + "university='" + universityName + '\'' + ", points=" + rewardPoints + '}';
    }
}
