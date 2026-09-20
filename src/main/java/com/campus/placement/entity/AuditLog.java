package com.campus.placement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Written by the {@code AuditInterceptor} around every audited bean method and
 * by the message driven bean that drains the notification queue.
 */
@Entity
@Table(name = "audit_log")
@NamedQuery(name = "AuditLog.recent",
        query = "SELECT a FROM AuditLog a ORDER BY a.at DESC")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(length = 600)
    private String detail;

    @Column(name = "duration_ms")
    private Long durationMs;

    /** Named logged_at because AT is a reserved word in Derby. */
    @Column(name = "logged_at", nullable = false)
    private LocalDateTime at = LocalDateTime.now();

    public AuditLog() {
    }

    public AuditLog(String actor, String action, String detail, Long durationMs) {
        this.actor = actor;
        this.action = action;
        this.detail = detail;
        this.durationMs = durationMs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public LocalDateTime getAt() {
        return at;
    }

    public void setAt(LocalDateTime at) {
        this.at = at;
    }
}
