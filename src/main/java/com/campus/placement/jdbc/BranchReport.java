package com.campus.placement.jdbc;

import java.io.Serializable;

/**
 * One row of the branch wise placement report.
 *
 * <p>This is a plain result carrier, not an entity. Nothing here is managed by
 * Hibernate: the fields are filled by reading a {@code ResultSet} column by
 * column in {@link PlacementReportDao}. A report row has no identity and is
 * never saved back, so mapping it as an entity would be the wrong tool.</p>
 */
public class BranchReport implements Serializable {

    private static final long serialVersionUID = 1L;

    private String branch;
    private int students;
    private int applications;
    private int shortlisted;
    private int selected;
    private double averageCgpa;
    private double bestPackage;

    public double getPlacementRate() {
        return students == 0 ? 0 : Math.round(selected * 1000.0 / students) / 10.0;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getStudents() {
        return students;
    }

    public void setStudents(int students) {
        this.students = students;
    }

    public int getApplications() {
        return applications;
    }

    public void setApplications(int applications) {
        this.applications = applications;
    }

    public int getShortlisted() {
        return shortlisted;
    }

    public void setShortlisted(int shortlisted) {
        this.shortlisted = shortlisted;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    public double getAverageCgpa() {
        return averageCgpa;
    }

    public void setAverageCgpa(double averageCgpa) {
        this.averageCgpa = averageCgpa;
    }

    public double getBestPackage() {
        return bestPackage;
    }

    public void setBestPackage(double bestPackage) {
        this.bestPackage = bestPackage;
    }
}
