package com.campus.placement.ejb;

import com.campus.placement.entity.ApplicationStatus;
import com.campus.placement.entity.Drive;
import com.campus.placement.entity.JobApplication;
import com.campus.placement.entity.StudentProfile;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless session bean for applications. Every write here also drops a line
 * on the JMS queue, so the message driven bean can record the notification
 * without holding up the request that caused it.
 */
@Stateless
@Interceptors(AuditInterceptor.class)
public class ApplicationService {

    @PersistenceContext(unitName = "placementPU")
    private EntityManager em;

    @EJB
    private NotificationSender notifications;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasApplied(Long studentId, Long driveId) {
        if (studentId == null || driveId == null) {
            return false;
        }
        Long count = em.createNamedQuery("JobApplication.exists", Long.class)
                .setParameter("studentId", studentId)
                .setParameter("driveId", driveId)
                .getSingleResult();
        return count > 0;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<JobApplication> forStudent(Long studentId) {
        return em.createNamedQuery("JobApplication.byStudent", JobApplication.class)
                .setParameter("studentId", studentId)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<JobApplication> forDrive(Long driveId) {
        return em.createNamedQuery("JobApplication.byDrive", JobApplication.class)
                .setParameter("driveId", driveId)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<JobApplication> listAll() {
        return em.createNamedQuery("JobApplication.all", JobApplication.class).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public JobApplication find(Long id) {
        return id == null ? null : em.find(JobApplication.class, id);
    }

    /**
     * Records one application. The duplicate check is repeated here even though
     * the page already hides the button, because a replayed POST reaches this
     * method directly.
     */
    public JobApplication apply(StudentProfile student, Drive drive) {
        if (hasApplied(student.getId(), drive.getId())) {
            throw new IllegalStateException("You have already applied to this drive");
        }
        JobApplication application = new JobApplication(student, drive);
        em.persist(application);
        notifications.send(student.getUser().getFullName() + " applied to "
                + drive.getCompany().getName() + " for " + drive.getJobRole());
        return application;
    }

    /** Officer moving one application along the pipeline. */
    public JobApplication updateStatus(Long applicationId, ApplicationStatus status, String remarks) {
        JobApplication application = em.find(JobApplication.class, applicationId);
        if (application == null) {
            throw new IllegalArgumentException("Application not found");
        }
        ApplicationStatus previous = application.getStatus();
        application.setStatus(status);
        application.setRemarks(remarks);
        application.setUpdatedAt(LocalDateTime.now());
        JobApplication saved = em.merge(application);

        notifications.send(saved.getStudent().getUser().getFullName() + " moved from "
                + previous.getLabel() + " to " + status.getLabel() + " for "
                + saved.getDrive().getCompany().getName());
        return saved;
    }

    /** Student pulling out of a drive that is still open. */
    public void withdraw(Long applicationId, Long owningStudentId) {
        JobApplication application = em.find(JobApplication.class, applicationId);
        if (application == null) {
            return;
        }
        if (!application.getStudent().getId().equals(owningStudentId)) {
            throw new SecurityException("That application belongs to another student");
        }
        if (application.getStatus() != ApplicationStatus.APPLIED) {
            throw new IllegalStateException(
                    "The placement cell has already progressed this application, so it cannot be withdrawn");
        }
        notifications.send(application.getStudent().getUser().getFullName()
                + " withdrew from " + application.getDrive().getCompany().getName());
        em.remove(application);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<ApplicationStatus, Long> statusBreakdown() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            Long value = em.createNamedQuery("JobApplication.countByStatus", Long.class)
                    .setParameter("status", status)
                    .getSingleResult();
            counts.put(status, value);
        }
        return counts;
    }

    /** Ids of every student who already holds an offer, fetched in one query. */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public java.util.Set<Long> placedStudentIds() {
        return new java.util.HashSet<>(em.createQuery(
                        "SELECT DISTINCT a.student.id FROM JobApplication a WHERE a.status = :s",
                        Long.class)
                .setParameter("s", ApplicationStatus.SELECTED)
                .getResultList());
    }

    /** Application count per student, so the register can show it without N queries. */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<Long, Long> countsByStudent() {
        Map<Long, Long> counts = new java.util.HashMap<>();
        List<Object[]> rows = em.createQuery(
                        "SELECT a.student.id, COUNT(a) FROM JobApplication a GROUP BY a.student.id",
                        Object[].class)
                .getResultList();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long countAll() {
        return em.createQuery("SELECT COUNT(a) FROM JobApplication a", Long.class).getSingleResult();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long countPlaced() {
        return em.createQuery(
                        "SELECT COUNT(DISTINCT a.student.id) FROM JobApplication a WHERE a.status = :s",
                        Long.class)
                .setParameter("s", ApplicationStatus.SELECTED)
                .getSingleResult();
    }
}
