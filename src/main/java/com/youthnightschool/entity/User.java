package com.youthnightschool.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "openid", unique = true, nullable = false, length = 64)
    private String openid;

    @Column(name = "unionid", length = 64)
    private String unionid;

    @Column(name = "nick_name", length = 100)
    private String nickName;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "phone", unique = true, length = 32)
    private String phone;

    @Column(name = "roles", columnDefinition = "text")
    private String roles = "user";

    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @Column(name = "create_time", nullable = false)
    private Instant createTime;

    @Column(name = "last_login_time", nullable = false)
    private Instant lastLoginTime;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Session> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SignLog> signLogs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSignup> courseSignups = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        this.createTime = now;
        this.lastLoginTime = now;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public List<String> getRolesList() {
        if (roles == null || roles.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(java.util.Arrays.asList(roles.split(",")));
    }

    public void setRolesList(List<String> rolesList) {
        this.roles = rolesList == null ? "user" : String.join(",", rolesList);
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Instant lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
    }

    public List<SignLog> getSignLogs() {
        return signLogs;
    }

    public void setSignLogs(List<SignLog> signLogs) {
        this.signLogs = signLogs;
    }

    public List<CourseSignup> getCourseSignups() {
        return courseSignups;
    }

    public void setCourseSignups(List<CourseSignup> courseSignups) {
        this.courseSignups = courseSignups;
    }
}
