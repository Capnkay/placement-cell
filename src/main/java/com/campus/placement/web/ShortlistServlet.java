package com.campus.placement.web;

import com.campus.placement.ejb.ApplicationService;
import com.campus.placement.ejb.DriveService;
import com.campus.placement.ejb.ShortlistBean;
import com.campus.placement.entity.Drive;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Batch shortlisting, and the one place a stateful session bean is used.
 *
 * <p>A stateful bean is deliberately not injected into a servlet field. A
 * servlet is a single shared instance serving every user at once, so a field
 * would hand one officer's basket to the next. Instead the bean is created
 * through a JNDI lookup and the reference is kept in that officer's
 * HttpSession, which is what ties one conversation to one browser.</p>
 *
 * <p>The JNDI name follows the portable pattern
 * {@code java:module/<bean-name>}, so the same code runs on any Jakarta EE
 * server without a vendor specific name.</p>
 */
@WebServlet(name = "ShortlistServlet", urlPatterns = {"/admin/shortlist"})
public class ShortlistServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String JNDI_NAME = "java:module/ShortlistBean";

    @EJB
    private DriveService driveService;

    @EJB
    private ApplicationService applicationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Long driveId = Validators.toLong(request.getParameter("drive"));
        List<Drive> drives = driveService.listAll();

        if (driveId == null) {
            request.setAttribute("drives", drives);
            request.setAttribute("csrf", Web.csrfToken(request.getSession()));
            Web.render(request, response, "admin-shortlist.jsp", "shortlist");
            return;
        }

        Drive drive = driveService.find(driveId);
        if (drive == null) {
            Web.flashError(request, "That drive could not be found.");
            Web.redirect(request, response, "/admin/shortlist");
            return;
        }

        ShortlistBean basket = basket(request);
        basket.startFor(drive.getId(), drive.getCompany().getName() + " : " + drive.getJobRole());

        request.setAttribute("drives", drives);
        request.setAttribute("drive", drive);
        request.setAttribute("applications", applicationService.forDrive(drive.getId()));
        request.setAttribute("picked", basket.getPicked());
        request.setAttribute("pickedCount", basket.getCount());
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));

        Web.render(request, response, "admin-shortlist.jsp", "shortlist");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = Validators.trim(request.getParameter("action"));
        Long driveId = Validators.toLong(request.getParameter("driveId"));
        ShortlistBean basket = basket(request);

        switch (action) {
            case "toggle" -> {
                Long applicationId = Validators.toLong(request.getParameter("applicationId"));
                if (applicationId != null) {
                    basket.toggle(applicationId);
                }
            }
            case "clear" -> {
                basket.clear();
                Web.flashSuccess(request, "The selection has been cleared.");
            }
            case "commit" -> {
                List<String> moved = basket.commit();
                if (moved.isEmpty()) {
                    Web.flashError(request, "Nothing was selected, so no one was shortlisted.");
                } else {
                    Web.flashSuccess(request, moved.size() + " candidate(s) shortlisted: "
                            + String.join(", ", moved) + ".");
                }
            }
            case "finish" -> {
                endConversation(request);
                Web.flashSuccess(request, "The shortlisting session has been closed.");
                Web.redirect(request, response, "/admin/shortlist");
                return;
            }
            default -> {
                // Unknown action, fall through to the redirect below.
            }
        }

        Web.redirect(request, response, driveId == null
                ? "/admin/shortlist" : "/admin/shortlist?drive=" + driveId);
    }

    /**
     * Returns this officer's bean, creating it on first use. The lookup is the
     * explicit form of what {@code @EJB} does, and it is the right tool here
     * because the instance has to be tied to the session rather than to the
     * servlet.
     */
    private ShortlistBean basket(HttpServletRequest request) throws ServletException {
        HttpSession session = request.getSession();
        Object existing = session.getAttribute(Web.SESSION_SHORTLIST);
        if (existing instanceof ShortlistBean bean) {
            return bean;
        }
        try {
            ShortlistBean bean = (ShortlistBean) new InitialContext().lookup(JNDI_NAME);
            session.setAttribute(Web.SESSION_SHORTLIST, bean);
            return bean;
        } catch (NamingException ex) {
            throw new ServletException("The shortlisting bean could not be looked up at "
                    + JNDI_NAME, ex);
        }
    }

    private void endConversation(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Object existing = session.getAttribute(Web.SESSION_SHORTLIST);
        if (existing instanceof ShortlistBean bean) {
            try {
                bean.finish();
            } catch (RuntimeException ignored) {
                // Already gone, nothing further to do.
            }
        }
        session.removeAttribute(Web.SESSION_SHORTLIST);
    }
}
