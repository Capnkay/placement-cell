package com.campus.placement.ejb;

import com.campus.placement.entity.AppUser;
import com.campus.placement.entity.Role;
import com.campus.placement.entity.StudentProfile;
import com.campus.placement.security.PasswordHasher;
import com.campus.placement.util.Validators;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stateless session bean holding all credential logic. It is the only class in
 * the application allowed to compare a password.
 *
 * <p>The rules it enforces:</p>
 * <ul>
 *   <li>the email must be well formed before the database is touched at all</li>
 *   <li>an unknown email still burns a PBKDF2 round, so a missing account and a
 *       wrong password take the same time and cannot be told apart</li>
 *   <li>five consecutive failures lock the account for fifteen minutes</li>
 *   <li>a deactivated account can never sign in, whatever the password is</li>
 * </ul>
 */
@Stateless
@Interceptors(AuditInterceptor.class)
public class AuthService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    @PersistenceContext(unitName = "placementPU")
    private EntityManager em;

    /**
     * Verifies one sign in attempt and records the consequence.
     */
    public AuthResult authenticate(String rawEmail, String rawPassword) {
        String email = Validators.normaliseEmail(rawEmail);

        if (Validators.isBlank(email) || Validators.isBlank(rawPassword)) {
            return AuthResult.invalidInput("Enter both your email address and your password.");
        }
        if (!Validators.isEmail(email)) {
            return AuthResult.invalidInput(
                    "That is not a valid email address. Use the form name@example.com");
        }

        Optional<AppUser> found = findByEmail(email);
        if (found.isEmpty()) {
            // Same cost as a real check so timing does not reveal the account list.
            PasswordHasher.burn(rawPassword);
            return AuthResult.badCredentials(0);
        }

        AppUser user = found.get();

        if (!user.isActive()) {
            return AuthResult.disabled();
        }
        if (user.isLocked()) {
            long minutes = Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            return AuthResult.locked(minutes);
        }

        if (!PasswordHasher.verify(rawPassword, user.getPasswordHash())) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                user.setFailedAttempts(0);
                em.merge(user);
                return AuthResult.locked(LOCK_MINUTES);
            }
            em.merge(user);
            return AuthResult.badCredentials(MAX_ATTEMPTS - attempts);
        }

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        em.merge(user);
        return AuthResult.success(user);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<AppUser> findByEmail(String email) {
        try {
            return Optional.of(em.createNamedQuery("AppUser.byEmail", AppUser.class)
                    .setParameter("email", Validators.normaliseEmail(email))
                    .getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean emailTaken(String email) {
        return findByEmail(email).isPresent();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AppUser find(Long id) {
        return em.find(AppUser.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<AppUser> listAll() {
        return em.createNamedQuery("AppUser.all", AppUser.class).getResultList();
    }

    /**
     * Creates a student account together with its academic profile. The caller
     * has already validated the shape of every field, this method still refuses
     * a duplicate email or roll number because two browsers can submit at once.
     */
    public StudentProfile registerStudent(String email, String rawPassword, String fullName,
                                          String rollNo, String branch, int batchYear,
                                          double cgpa, int backlogs, String phone) {
        String normalised = Validators.normaliseEmail(email);
        if (emailTaken(normalised)) {
            throw new IllegalArgumentException("An account already exists for " + normalised);
        }
        Long clash = em.createQuery(
                        "SELECT COUNT(s) FROM StudentProfile s WHERE LOWER(s.rollNo) = LOWER(:r)", Long.class)
                .setParameter("r", rollNo.trim())
                .getSingleResult();
        if (clash > 0) {
            throw new IllegalArgumentException("Roll number " + rollNo.trim() + " is already registered");
        }

        AppUser user = new AppUser(normalised, PasswordHasher.hash(rawPassword),
                fullName.trim(), Role.STUDENT);
        em.persist(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setRollNo(rollNo.trim());
        profile.setBranch(branch.trim());
        profile.setBatchYear(batchYear);
        profile.setCgpa(cgpa);
        profile.setBacklogs(backlogs);
        profile.setPhone(phone == null ? null : phone.trim());
        em.persist(profile);
        return profile;
    }

    /** Used by the profile page. Refuses unless the current password matches. */
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        AppUser user = em.find(AppUser.class, userId);
        if (user == null || !PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
            return false;
        }
        user.setPasswordHash(PasswordHasher.hash(newPassword));
        em.merge(user);
        return true;
    }

    /** Placement officer switch to suspend or restore an account. */
    public void setActive(Long userId, boolean active) {
        AppUser user = em.find(AppUser.class, userId);
        if (user != null) {
            user.setActive(active);
            if (active) {
                user.setFailedAttempts(0);
                user.setLockedUntil(null);
            }
            em.merge(user);
        }
    }

    /** Clears a lockout early, for the case where a student calls the office. */
    public void unlock(Long userId) {
        AppUser user = em.find(AppUser.class, userId);
        if (user != null) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            em.merge(user);
        }
    }
}
