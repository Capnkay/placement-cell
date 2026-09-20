package com.campus.placement.jdbc;

import java.io.Serializable;

/**
 * One candidate returned by the filtered search. Used to show a
 * {@code PreparedStatement} being built with a variable number of parameters
 * without ever concatenating a user value into the SQL text.
 */
public class CandidateRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private long profileId;
    private String rollNo;
    private String fullName;
    private String email;
    private String branch;
    private double cgpa;
    private int backlogs;
    private int applications;
    private boolean placed;

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
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

    public int getApplications() {
        return applications;
    }

    public void setApplications(int applications) {
        this.applications = applications;
    }

    public boolean isPlaced() {
        return placed;
    }

    public void setPlaced(boolean placed) {
        this.placed = placed;
    }
}
