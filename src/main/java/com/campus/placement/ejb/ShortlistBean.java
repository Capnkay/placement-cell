package com.campus.placement.ejb;

import com.campus.placement.entity.ApplicationStatus;
import com.campus.placement.entity.JobApplication;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Stateful session bean: the shortlisting basket.
 *
 * <p>The placement officer opens a drive, ticks candidates across several page
 * loads, and only then commits. That running selection is conversational state
 * belonging to one officer, which is exactly what a stateful bean is for. The
 * servlet keeps the reference in the HttpSession, so the same bean instance is
 * reached on every request from that browser.</p>
 *
 * <p>Lifecycle you can watch in the server log: {@code @PostConstruct} when the
 * officer first opens a drive, {@code @PreDestroy} when the basket is committed
 * or cleared through the {@code @Remove} method, or when the thirty minute
 * {@code @StatefulTimeout} expires because the officer wandered off.</p>
 */
@Stateful
@StatefulTimeout(value = 30, unit = TimeUnit.MINUTES)
public class ShortlistBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ShortlistBean.class.getName());

    @PersistenceContext(unitName = "placementPU")
    private transient EntityManager em;

    @EJB
    private transient NotificationSender notifications;

    private Long driveId;
    private String driveLabel = "";
    private final Set<Long> picked = new LinkedHashSet<>();

    @PostConstruct
    public void created() {
        LOG.info("ShortlistBean created, a new shortlisting conversation has started");
    }

    @PreDestroy
    public void destroyed() {
        LOG.info("ShortlistBean destroyed, the shortlisting conversation has ended");
    }

    public void startFor(Long driveId, String driveLabel) {
        if (!driveId.equals(this.driveId)) {
            picked.clear();
        }
        this.driveId = driveId;
        this.driveLabel = driveLabel;
    }

    public void toggle(Long applicationId) {
        if (!picked.remove(applicationId)) {
            picked.add(applicationId);
        }
    }

    public void add(Long applicationId) {
        picked.add(applicationId);
    }

    public void clear() {
        picked.clear();
    }

    public boolean contains(Long applicationId) {
        return picked.contains(applicationId);
    }

    public Set<Long> getPicked() {
        return new LinkedHashSet<>(picked);
    }

    public int getCount() {
        return picked.size();
    }

    public Long getDriveId() {
        return driveId;
    }

    public String getDriveLabel() {
        return driveLabel;
    }

    /**
     * Writes the whole basket in one transaction and returns the names moved,
     * so the officer sees precisely who was shortlisted.
     */
    public List<String> commit() {
        List<String> moved = new ArrayList<>();
        for (Long id : picked) {
            JobApplication application = em.find(JobApplication.class, id);
            if (application == null || application.getStatus() == ApplicationStatus.SHORTLISTED) {
                continue;
            }
            application.setStatus(ApplicationStatus.SHORTLISTED);
            application.setRemarks("Shortlisted in a batch by the placement cell");
            em.merge(application);
            moved.add(application.getStudent().getUser().getFullName());
        }
        if (!moved.isEmpty()) {
            notifications.send(moved.size() + " candidate(s) shortlisted for " + driveLabel);
        }
        picked.clear();
        return moved;
    }

    /**
     * Ends the conversation. Calling a {@code @Remove} method is how a client
     * tells the container it is finished with a stateful bean.
     */
    @Remove
    public void finish() {
        picked.clear();
        LOG.info("ShortlistBean finish() called, the container will now discard this instance");
    }
}
