package com.campus.placement.web;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Watches the HttpSession lifecycle so the system page can report how many
 * people are signed in right now. This is the listener half of session
 * tracking: the container calls it on creation and on invalidation or timeout,
 * and nothing in the servlets has to remember to count.
 */
@WebListener
public class SessionTracker implements HttpSessionListener {

    private static final Logger LOG = Logger.getLogger(SessionTracker.class.getName());
    private static final AtomicInteger ACTIVE = new AtomicInteger();
    private static final AtomicInteger TOTAL = new AtomicInteger();

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        event.getSession().setMaxInactiveInterval(30 * 60);
        ACTIVE.incrementAndGet();
        TOTAL.incrementAndGet();
        LOG.fine("Session created: " + event.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        ACTIVE.decrementAndGet();
        LOG.fine("Session destroyed: " + event.getSession().getId());
    }

    public static int getActive() {
        return Math.max(0, ACTIVE.get());
    }

    public static int getTotal() {
        return TOTAL.get();
    }
}
