package com.youthnightschool.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "courses",
    indexes = {
        @Index(columnList = "college, month"),
        @Index(columnList = "title"),
        @Index(columnList = "audience")
    }
)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "legacy_id", unique = true, length = 64)
    private String legacyId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "college", nullable = false, length = 255)
    private String college;

    @Column(name = "teacher", nullable = false, length = 100)
    private String teacher = "";

    @Column(name = "location", nullable = false, length = 255)
    private String location = "";

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description = "";

    @Column(name = "cover_image", nullable = false, length = 500)
    private String coverImage = "";

    @Column(name = "difficulty", nullable = false, length = 64)
    private String difficulty = "";

    @Column(name = "audience", nullable = false, length = 255)
    private String audience = "";

    @Column(name = "duration", nullable = false, length = 64)
    private String duration = "";

    @Column(name = "fee", nullable = false, length = 64)
    private String fee = "";

    @Column(name = "notice", nullable = false, columnDefinition = "TEXT")
    private String notice = "";

    @Column(name = "materials", nullable = false, columnDefinition = "TEXT")
    private String materials = "";

    @Column(name = "tags", nullable = false, columnDefinition = "TEXT")
    private String tags = "";

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "week", nullable = false, length = 64)
    private String week;

    @Column(name = "time_start", nullable = false, length = 32)
    private String timeStart;

    @Column(name = "time_end", nullable = false, length = 32)
    private String timeEnd;

    @Column(name = "signup_start_at")
    private Instant signupStartAt;

    @Column(name = "signup_end_at")
    private Instant signupEndAt;

    @Column(name = "max_seats", nullable = false)
    private Integer maxSeats = 30;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "available";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSignup> signups = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLegacyId() {
        return legacyId;
    }

    public void setLegacyId(String legacyId) {
        this.legacyId = legacyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getMaterials() {
        return materials;
    }

    public void setMaterials(String materials) {
        this.materials = materials;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public Instant getSignupStartAt() {
        return signupStartAt;
    }

    public void setSignupStartAt(Instant signupStartAt) {
        this.signupStartAt = signupStartAt;
    }

    public Instant getSignupEndAt() {
        return signupEndAt;
    }

    public void setSignupEndAt(Instant signupEndAt) {
        this.signupEndAt = signupEndAt;
    }

    public Integer getMaxSeats() {
        return maxSeats;
    }

    public void setMaxSeats(Integer maxSeats) {
        this.maxSeats = maxSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CourseSignup> getSignups() {
        return signups;
    }

    public void setSignups(List<CourseSignup> signups) {
        this.signups = signups;
    }
}
