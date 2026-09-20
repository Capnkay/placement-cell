package com.campus.placement.ejb;

import com.campus.placement.entity.AuditLog;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Writes one audit row in a transaction of its own.
 *
 * <p>The interceptor cannot carry a transaction attribute, and many of the
 * methods it wraps are read only, so they run with no transaction at all. Asking
 * the entity manager to persist in that situation throws. Delegating the write
 * to a bean marked {@code REQUIRES_NEW} gives the audit row a transaction
 * whatever the caller was doing, and keeps it committed even if the business
 * method later rolls back, which is the behaviour an audit trail should have.</p>
 */
@Stateless
public class AuditWriter {

    @PersistenceContext(unitName = "placementPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void write(String actor, String action, String detail, Long durationMs) {
        em.persist(new AuditLog(actor, action, detail, durationMs));
    }
}
