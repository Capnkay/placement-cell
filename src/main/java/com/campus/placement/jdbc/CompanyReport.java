package com.campus.placement.jdbc;

import java.io.Serializable;

/**
 * One row of the recruiter wise hiring report, read straight from a ResultSet.
 */
public class CompanyReport implements Serializable {

    private static final long serialVersionUID = 1L;

    private String company;
    private String sector;
    private int drives;
    private int applications;
    private int selected;
    private double averagePackage;
    private double bestPackage;

    public double getConversionRate() {
        return applications == 0 ? 0 : Math.round(selected * 1000.0 / applications) / 10.0;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public int getDrives() {
        return drives;
    }

    public void setDrives(int drives) {
        this.drives = drives;
    }

    public int getApplications() {
        return applications;
    }

    public void setApplications(int applications) {
        this.applications = applications;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    public double getAveragePackage() {
        return averagePackage;
    }

    public void setAveragePackage(double averagePackage) {
        this.averagePackage = averagePackage;
    }

    public double getBestPackage() {
        return bestPackage;
    }

    public void setBestPackage(double bestPackage) {
        this.bestPackage = bestPackage;
    }
}
