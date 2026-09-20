package com.campus.placement.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Academic record for a student. One profile maps to exactly one {@link AppUser}
 * (OneToOne) and owns many {@link JobApplication} rows (OneToMany).
 */
@Entity
@Table(name = "student_profile")
@NamedQuery(name = "StudentProfile.byUser",
        query = "SELECT s FROM StudentProfile s WHERE s.user.id = :userId")
@NamedQuery(name = "StudentProfile.all",
        query = "SELECT s FROM StudentProfile s ORDER BY s.rollNo")
@NamedQuery(name = "StudentProfile.byRoll",
        query = "SELECT s FROM StudentProfile s WHERE LOWER(s.rollNo) = LOWER(:rollNo)")
public class StudentProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "roll_no", nullable = false, unique = true, length = 30)
    private String rollNo;

    @Column(nullable = false, length = 60)
    private String branch;

    @Column(name = "batch_year", nullable = false)
    private int batchYear;

    @Column(nullable = false)
    private double cgpa;

    @Column(nullable = false)
    private int backlogs;

    @Column(length = 20)
    private String phone;

    /** File name of the uploaded resume, stored under the upload directory. */
    @Column(name = "resume_file", length = 255)
    private String resumeFile;

    @OneToMany(mappedBy = "student", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<JobApplication> applications = new ArrayList<>();

    public StudentProfile() {
    }

    public boolean isPlaced() {
        return applications.stream()
                .anyMatch(a -> a.getStatus() == ApplicationStatus.SELECTED);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getBatchYear() {
        return batchYear;
    }

    public void setBatchYear(int batchYear) {
        this.batchYear = batchYear;
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public int getBacklogs() {
        return backlogs;
    }

    public void setBacklogs(int backlogs) {
        this.backlogs = backlogs;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getResumeFile() {
        return resumeFile;
    }

    public void setResumeFile(String resumeFile) {
        this.resumeFile = resumeFile;
    }

    public List<JobApplication> getApplications() {
        return applications;
    }

    public void setApplications(List<JobApplication> applications) {
        this.applications = applications;
    }
}
