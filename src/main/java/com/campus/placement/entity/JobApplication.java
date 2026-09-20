package com.campus.placement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Join entity between {@link StudentProfile} and {@link Drive}. The unique
 * constraint is what stops a student applying to the same drive twice, even if
 * the browser replays the POST.
 */
@Entity
@Table(name = "job_application",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_drive",
                columnNames = {"student_id", "drive_id"}))
@NamedQuery(name = "JobApplication.byStudent",
        query = "SELECT a FROM JobApplication a WHERE a.student.id = :studentId "
                + "ORDER BY a.appliedAt DESC")
@NamedQuery(name = "JobApplication.byDrive",
        query = "SELECT a FROM JobApplication a WHERE a.drive.id = :driveId "
                + "ORDER BY a.student.rollNo")
@NamedQuery(name = "JobApplication.exists",
        query = "SELECT COUNT(a) FROM JobApplication a "
                + "WHERE a.student.id = :studentId AND a.drive.id = :driveId")
@NamedQuery(name = "JobApplication.all",
        query = "SELECT a FROM JobApplication a ORDER BY a.appliedAt DESC")
@NamedQuery(name = "JobApplication.countByStatus",
        query = "SELECT COUNT(a) FROM JobApplication a WHERE a.status = :status")
public class JobApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "drive_id", nullable = false)
    private Drive drive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String remarks;

    public JobApplication() {
    }

    public JobApplication(StudentProfile student, Drive drive) {
        this.student = student;
        this.drive = drive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public void setStudent(StudentProfile student) {
        this.student = student;
    }

    public Drive getDrive() {
        return drive;
    }

    public void setDrive(Drive drive) {
        this.drive = drive;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
