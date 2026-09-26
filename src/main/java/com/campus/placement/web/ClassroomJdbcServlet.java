package com.campus.placement.web;

import com.campus.placement.ejb.MockScoreBean;
import com.campus.placement.util.Validators;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Shows the practical-session style of JDBC next to the project's own.
 *
 * <p>The form calls {@link MockScoreBean}, a stateful bean written like the marks
 * entry practical. The officer chooses which of its two methods runs, the
 * classroom one that joins strings into SQL or the one that binds parameters, and
 * the table underneath is read back from MySQL so the insert can be seen landing.</p>
 *
 * <p>The bean is looked up by its JNDI name on each request rather than injected.
 * A stateful bean is one instance per client, and a servlet is shared by every
 * request, so injecting it would have every visitor using the same instance.
 * Every value is checked here before the bean sees it, because the classroom
 * method is unsafe by design.</p>
 */
@WebServlet(name = "ClassroomJdbcServlet", urlPatterns = {"/admin/classroom-jdbc"})
public class ClassroomJdbcServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));
        try {
            MockScoreBean bean = bean();
            request.setAttribute("scores", bean.latest());
            bean.finished();
        } catch (SQLException | NamingException ex) {
            request.setAttribute(Web.ATTR_ERROR, "The scores could not be read: " + ex.getMessage());
        }
        Web.render(request, response, "admin-classroom.jsp", "classroom");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String name = Validators.trim(request.getParameter("sname"));
        int aptitude = Validators.toInt(request.getParameter("aptitude"), -1);
        int technical = Validators.toInt(request.getParameter("technical"), -1);
        int interview = Validators.toInt(request.getParameter("interview"), -1);
        boolean classroom = "classroom".equals(request.getParameter("mode"));

        if (!Validators.isName(name)) {
            Web.flashError(request, "Enter the student's name using letters, spaces and . ' - only.");
        } else if (!inRange(aptitude) || !inRange(technical) || !inRange(interview)) {
            Web.flashError(request, "Each score must be a whole number from 0 to 100.");
        } else {
            try {
                MockScoreBean bean = bean();
                boolean saved = true;
                if (classroom) {
                    saved = bean.addScore(name, aptitude, technical, interview) == 1;
                } else {
                    bean.addScoreSafely(name, aptitude, technical, interview);
                }
                bean.finished();
                if (saved) {
                    Web.flashSuccess(request, "Saved " + name + " using the "
                            + (classroom ? "classroom (Statement)" : "pooled (PreparedStatement)") + " method.");
                } else {
                    Web.flashError(request, "The classroom method could not save " + name + ". Its SQL was built "
                            + "by joining strings, and the name changed the statement. The pooled method saves it.");
                }
            } catch (SQLException | NamingException ex) {
                Web.flashError(request, "The score could not be saved: " + ex.getMessage());
            }
        }
        Web.redirect(request, response, "/admin/classroom-jdbc");
    }

    private boolean inRange(int score) {
        return score >= 0 && score <= 100;
    }

    private MockScoreBean bean() throws NamingException {
        return (MockScoreBean) new InitialContext().lookup("java:module/MockScoreBean");
    }
}
