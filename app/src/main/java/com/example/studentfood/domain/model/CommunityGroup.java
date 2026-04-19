package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.util.Locale;

/**
 * CommunityGroup: Nhóm cộng đồng (Hội nhóm sinh viên chia sẻ về món ăn/địa điểm)
 */
public class CommunityGroup implements Serializable {
    private String groupId;
    private String groupName;
    private String description;
    private String coverImageUrl;
    private int memberCount;
    private int postCount;
    private String category;
    private boolean isJoined;

    public CommunityGroup() {}

    public CommunityGroup(String groupId, String groupName, String description,
                          String coverImageUrl, int memberCount, int postCount, String category) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.memberCount = memberCount;
        this.postCount = postCount;
        this.category = category;
        this.isJoined = false;
    }

    public String getGroupId()       { return groupId; }
    public String getGroupName()     { return groupName; }
    public String getDescription()   { return description; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public int getMemberCount()      { return memberCount; }
    public int getPostCount()        { return postCount; }
    public String getCategory()      { return category; }
    public boolean isJoined()        { return isJoined; }
    public void setJoined(boolean j) { isJoined = j; }

    public String getFormattedMembers() {
        if (memberCount >= 1000) return String.format(Locale.getDefault(), "%.1fK thành viên", memberCount / 1000.0);
        return memberCount + " thành viên";
    }
}
