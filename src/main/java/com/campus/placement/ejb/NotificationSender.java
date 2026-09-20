package com.campus.placement.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Producer side of the asynchronous notification path.
 *
 * <p>The queue and its connection factory are declared here with the portable
 * Jakarta EE annotations, so the application creates its own JMS resources when
 * it deploys instead of relying on someone configuring the server first.</p>
 *
 * <p>The send is deliberately fire and forget. A status change must be saved
 * even if the broker is unhappy, so a JMS failure is logged and swallowed
 * rather than rolling back the officer's edit.</p>
 */
@JMSDestinationDefinition(
        name = "java:app/jms/NotificationQueue",
        interfaceName = "jakarta.jms.Queue",
        destinationName = "PlacementNotificationQueue")
@JMSConnectionFactoryDefinition(
        name = "java:app/jms/NotificationFactory",
        interfaceName = "jakarta.jms.ConnectionFactory")
@Stateless
public class NotificationSender {

    private static final Logger LOG = Logger.getLogger(NotificationSender.class.getName());

    @Resource(lookup = "java:app/jms/NotificationFactory")
    private ConnectionFactory factory;

    @Resource(lookup = "java:app/jms/NotificationQueue")
    private Queue queue;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void send(String message) {
        if (factory == null || queue == null) {
            LOG.warning("JMS resources are not available, skipping notification: " + message);
            return;
        }
        try (JMSContext context = factory.createContext()) {
            context.createProducer().send(queue, message);
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Notification could not be queued: " + message, ex);
        }
    }
}
