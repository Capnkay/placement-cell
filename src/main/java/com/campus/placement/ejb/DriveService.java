package com.campus.placement.ejb;

import com.campus.placement.entity.Company;
import com.campus.placement.entity.Drive;
import com.campus.placement.entity.DriveStatus;
import com.campus.placement.entity.StudentProfile;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Stateless session bean owning drives and the eligibility rule set.
 * The rules live here rather than in a servlet or a JSP so that the same
 * decision is reached whether the caller is the student portal, the officer
 * console or the bulk shortlist bean.
 */
@Stateless
@Interceptors(AuditInterceptor.class)
public class DriveService {

    @PersistenceContext(unitName = "placementPU")
    private EntityManager em;

    @EJB
    private ApplicationService applicationService;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Drive> listAll() {
        return em.createNamedQuery("Drive.all", Drive.class).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Drive> listOpen() {
        return em.createNamedQuery("Drive.open", Drive.class)
                .setParameter("status", DriveStatus.OPEN)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Drive find(Long id) {
        return id == null ? null : em.find(Drive.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long countOpen() {
        return em.createQuery("SELECT COUNT(d) FROM Drive d WHERE d.status = :s", Long.class)
                .setParameter("s", DriveStatus.OPEN)
                .getSingleResult();
    }

    public Drive save(Drive drive) {
        if (drive.getId() == null) {
            em.persist(drive);
            return drive;
        }
        return em.merge(drive);
    }

    public void setStatus(Long driveId, DriveStatus status) {
        Drive drive = em.find(Drive.class, driveId);
        if (drive != null) {
            drive.setStatus(status);
            em.merge(drive);
        }
    }

    public void delete(Long driveId) {
        Drive drive = em.find(Drive.class, driveId);
        if (drive != null) {
            em.remove(drive);
        }
    }

    // Company register kept beside drives because the officer edits them together.

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Company> listCompanies() {
        return em.createNamedQuery("Company.all", Company.class).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Company findCompany(Long id) {
        return id == null ? null : em.find(Company.class, id);
    }

    public Company saveCompany(Company company) {
        if (company.getId() == null) {
            em.persist(company);
            return company;
        }
        return em.merge(company);
    }

    public void deleteCompany(Long id) {
        Company company = em.find(Company.class, id);
        if (company != null) {
            em.remove(company);
        }
    }

    /**
     * Counted with a query rather than by walking the lazy collection, because
     * an entity handed back to a servlet is detached and would fail to load it.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long countDrivesFor(Long companyId) {
        return em.createQuery("SELECT COUNT(d) FROM Drive d WHERE d.company.id = :id", Long.class)
                .setParameter("id", companyId)
                .getSingleResult();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long countApplicationsFor(Long driveId) {
        return em.createQuery(
                        "SELECT COUNT(a) FROM JobApplication a WHERE a.drive.id = :id", Long.class)
                .setParameter("id", driveId)
                .getSingleResult();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean companyNameTaken(String name, Long ignoreId) {
        Long count = em.createQuery(
                        "SELECT COUNT(c) FROM Company c WHERE LOWER(c.name) = LOWER(:n) "
                                + "AND (:id IS NULL OR c.id <> :id)", Long.class)
                .setParameter("n", name == null ? "" : name.trim())
                .setParameter("id", ignoreId)
                .getSingleResult();
        return count > 0;
    }

    /**
     * The single source of truth for "can this student apply to this drive".
     *
     * <p>The bean's job here is only to answer the one question that needs the
     * database, which is whether an application already exists. The rules
     * themselves live in {@link EligibilityRules} so they can be tested on their
     * own, and so the same verdict is reached whether the caller is the student
     * portal, the officer console or the batch shortlist.</p>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Eligibility check(StudentProfile student, Drive drive) {
        boolean applied = student != null && drive != null
                && applicationService.hasApplied(student.getId(), drive.getId());
        return EligibilityRules.check(student, drive, applied, LocalDate.now());
    }
}
