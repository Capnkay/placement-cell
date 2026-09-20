package com.campus.placement.ejb;

import com.campus.placement.entity.AuditLog;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message driven bean, the consumer half of the notification path.
 *
 * <p>It has no client visible interface and nobody calls it. The container
 * creates instances from a pool and hands each queued message to
 * {@code onMessage}, which is what makes the work asynchronous: the officer's
 * request has already returned a page by the time this runs.</p>
 *
 * <p>Lifecycle in this application: created by the container, kept in the pool
 * between messages, destroyed on undeploy. There is no conversational state, so
 * any instance can serve any message.</p>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = "java:app/jms/NotificationQueue"),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode",
                propertyValue = "Auto-acknowledge")
})
public class NotificationMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(NotificationMDB.class.getName());

    @PersistenceContext(unitName = "placementPU")
    private EntityManager em;

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage text)) {
                LOG.warning("Ignoring a non text message on the notification queue");
                return;
            }
            String body = text.getText();
            LOG.info("Notification delivered: " + body);
            em.persist(new AuditLog("notifier", "NOTIFICATION", body, null));
        } catch (JMSException ex) {
            LOG.log(Level.SEVERE, "Notification message could not be read", ex);
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Notification could not be recorded", ex);
        }
    }
}
