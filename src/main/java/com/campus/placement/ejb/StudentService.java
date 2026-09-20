package com.campus.placement.ejb;

import com.campus.placement.entity.AppUser;
import com.campus.placement.entity.StudentProfile;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

/**
 * Stateless session bean for the student register: profiles, resumes and the
 * bulk import that the non blocking upload servlet feeds.
 */
@Stateless
@Interceptors(AuditInterceptor.class)
public class StudentService {

    @PersistenceContext(unitName = "placementPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<StudentProfile> listAll() {
        return em.createNamedQuery("StudentProfile.all", StudentProfile.class).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<StudentProfile> findByUser(Long userId) {
        try {
            return Optional.of(em.createNamedQuery("StudentProfile.byUser", StudentProfile.class)
                    .setParameter("userId", userId)
                    .getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public StudentProfile find(Long id) {
        return em.find(StudentProfile.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean rollNoTaken(String rollNo, Long ignoreProfileId) {
        Long count = em.createQuery(
                        "SELECT COUNT(s) FROM StudentProfile s WHERE LOWER(s.rollNo) = LOWER(:r) "
                                + "AND (:id IS NULL OR s.id <> :id)", Long.class)
                .setParameter("r", rollNo == null ? "" : rollNo.trim())
                .setParameter("id", ignoreProfileId)
                .getSingleResult();
        return count > 0;
    }

    /** Student editing their own academic details. */
    public StudentProfile updateProfile(Long profileId, String branch, int batchYear,
                                        double cgpa, int backlogs, String phone) {
        StudentProfile profile = em.find(StudentProfile.class, profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Profile not found");
        }
        profile.setBranch(branch);
        profile.setBatchYear(batchYear);
        profile.setCgpa(cgpa);
        profile.setBacklogs(backlogs);
        profile.setPhone(phone);
        return em.merge(profile);
    }

    /** Records the stored file name after the resume upload servlet writes it to disk. */
    public void attachResume(Long profileId, String storedFileName) {
        StudentProfile profile = em.find(StudentProfile.class, profileId);
        if (profile != null) {
            profile.setResumeFile(storedFileName);
            em.merge(profile);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<StudentProfile> search(String term) {
        String like = "%" + (term == null ? "" : term.trim().toLowerCase()) + "%";
        return em.createQuery(
                        "SELECT s FROM StudentProfile s WHERE LOWER(s.rollNo) LIKE :q "
                                + "OR LOWER(s.user.fullName) LIKE :q OR LOWER(s.branch) LIKE :q "
                                + "ORDER BY s.rollNo", StudentProfile.class)
                .setParameter("q", like)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long countStudents() {
        return em.createQuery("SELECT COUNT(s) FROM StudentProfile s", Long.class).getSingleResult();
    }

    /** Removes a student along with the account that signs in to it. */
    public void delete(Long profileId) {
        StudentProfile profile = em.find(StudentProfile.class, profileId);
        if (profile != null) {
            AppUser user = profile.getUser();
            em.remove(profile);
            if (user != null) {
                em.remove(em.contains(user) ? user : em.merge(user));
            }
        }
    }
}
