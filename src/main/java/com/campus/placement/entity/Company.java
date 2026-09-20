package com.campus.placement.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A recruiting organisation. One company runs many {@link Drive} rows.
 */
@Entity
@Table(name = "company")
@NamedQuery(name = "Company.all", query = "SELECT c FROM Company c ORDER BY c.name")
@NamedQuery(name = "Company.byName",
        query = "SELECT c FROM Company c WHERE LOWER(c.name) = LOWER(:name)")
public class Company implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String sector;

    @Column(name = "hr_email", nullable = false, length = 190)
    private String hrEmail;

    @Column(length = 200)
    private String website;

    @OneToMany(mappedBy = "company", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<Drive> drives = new ArrayList<>();

    public Company() {
    }

    public Company(String name, String sector, String hrEmail, String website) {
        this.name = name;
        this.sector = sector;
        this.hrEmail = hrEmail;
        this.website = website;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getHrEmail() {
        return hrEmail;
    }

    public void setHrEmail(String hrEmail) {
        this.hrEmail = hrEmail;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public List<Drive> getDrives() {
        return drives;
    }

    public void setDrives(List<Drive> drives) {
        this.drives = drives;
    }
}
