package com.campus.placement.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placement drive announced by a {@link Company}. Carries the eligibility
 * rule set that {@code DriveService} evaluates against a {@link StudentProfile}.
 */
@Entity
@Table(name = "drive")
@NamedQuery(name = "Drive.all",
        query = "SELECT d FROM Drive d ORDER BY d.driveDate DESC")
@NamedQuery(name = "Drive.open",
        query = "SELECT d FROM Drive d WHERE d.status = :status ORDER BY d.lastDate")
@NamedQuery(name = "Drive.byCompany",
        query = "SELECT d FROM Drive d WHERE d.company.id = :companyId ORDER BY d.driveDate DESC")
public class Drive implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "job_role", nullable = false, length = 120)
    private String jobRole;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(name = "package_lpa", nullable = false)
    private double packageLpa;

    @Column(name = "min_cgpa", nullable = false)
    private double minCgpa;

    @Column(name = "max_backlogs", nullable = false)
    private int maxBacklogs;

    /** Comma separated branch list, for example "Computer Science,Information Technology". */
    @Column(name = "allowed_branches", nullable = false, length = 400)
    private String allowedBranches;

    @Column(name = "drive_date", nullable = false)
    private LocalDate driveDate;

    @Column(name = "last_date", nullable = false)
    private LocalDate lastDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriveStatus status = DriveStatus.OPEN;

    @Column(length = 2000)
    private String description;

    @OneToMany(mappedBy = "drive", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<JobApplication> applications = new ArrayList<>();

    public Drive() {
    }

    /** Branch list exposed to JSTL as a real collection. */
    public List<String> getBranchList() {
        if (allowedBranches == null || allowedBranches.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedBranches.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public boolean isExpired() {
        return lastDate != null && lastDate.isBefore(LocalDate.now());
    }

    public boolean isAcceptingApplications() {
        return status == DriveStatus.OPEN && !isExpired();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getPackageLpa() {
        return packageLpa;
    }

    public void setPackageLpa(double packageLpa) {
        this.packageLpa = packageLpa;
    }

    public double getMinCgpa() {
        return minCgpa;
    }

    public void setMinCgpa(double minCgpa) {
        this.minCgpa = minCgpa;
    }

    public int getMaxBacklogs() {
        return maxBacklogs;
    }

    public void setMaxBacklogs(int maxBacklogs) {
        this.maxBacklogs = maxBacklogs;
    }

    public String getAllowedBranches() {
        return allowedBranches;
    }

    public void setAllowedBranches(String allowedBranches) {
        this.allowedBranches = allowedBranches;
    }

    public LocalDate getDriveDate() {
        return driveDate;
    }

    public void setDriveDate(LocalDate driveDate) {
        this.driveDate = driveDate;
    }

    public LocalDate getLastDate() {
        return lastDate;
    }

    public void setLastDate(LocalDate lastDate) {
        this.lastDate = lastDate;
    }

    public DriveStatus getStatus() {
        return status;
    }

    public void setStatus(DriveStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<JobApplication> getApplications() {
        return applications;
    }

    public void setApplications(List<JobApplication> applications) {
        this.applications = applications;
    }
}
