package com.campus.placement.web;

import com.campus.placement.entity.AppUser;
import com.campus.placement.entity.Role;
import java.io.Serializable;

/**
 * The small immutable copy of an account that lives in the HttpSession.
 *
 * <p>A managed entity is deliberately not stored in the session: it would be
 * detached the moment the transaction ended, and a stale copy of a role is a
 * security bug waiting to happen. Only the identifiers the pages actually need
 * are carried, and the password digest is never among them.</p>
 */
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String fullName;
    private final Role role;
    private final Long studentProfileId;

    public SessionUser(AppUser user, Long studentProfileId) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.studentProfileId = studentProfileId;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public Long getStudentProfileId() {
        return studentProfileId;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    /** First letters of the name, used for the avatar chip in the header. */
    public String getInitials() {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
