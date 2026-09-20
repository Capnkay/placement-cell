package com.campus.placement.ejb;

import com.campus.placement.entity.AuditLog;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * Read side of the audit trail. The rows themselves are written by the
 * interceptor and by the message driven bean, never by a servlet.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AuditService {

    @PersistenceContext(unitName = "placementPU")
    private EntityManager em;

    public List<AuditLog> recent(int limit) {
        return em.createNamedQuery("AuditLog.recent", AuditLog.class)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<AuditLog> search(String term, int limit) {
        String like = "%" + (term == null ? "" : term.trim().toLowerCase()) + "%";
        return em.createQuery(
                        "SELECT a FROM AuditLog a WHERE LOWER(a.action) LIKE :q "
                                + "OR LOWER(a.detail) LIKE :q OR LOWER(a.actor) LIKE :q "
                                + "ORDER BY a.at DESC", AuditLog.class)
                .setParameter("q", like)
                .setMaxResults(limit)
                .getResultList();
    }

    public long count() {
        return em.createQuery("SELECT COUNT(a) FROM AuditLog a", Long.class).getSingleResult();
    }
}
