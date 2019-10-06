package org.jivesoftware.openfire.plugin;

import java.io.IOException;

import org.jivesoftware.util.ParamUtils;
import org.jivesoftware.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ConfigServlet extends HttpServlet {

    private static DebuggerPlugin plugin;

    private static void addSessionFlashes(final HttpServletRequest request, final String... flashes) {
        final HttpSession session = request.getSession();
        for (final String flash : flashes) {
            final Object flashValue = session.getAttribute(flash);
            if (flashValue != null) {
                request.setAttribute(flash, flashValue);
                session.setAttribute(flash, null);
            }
        }
    }

    @Override
    public void init() {
        plugin = DebuggerPlugin.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        request.setAttribute("c2s", plugin.getDefaultPortFilter().isEnabled());
        request.setAttribute("ssl", plugin.getOldPortFilter().isEnabled());
        request.setAttribute("extcomp", plugin.getComponentPortFilter().isEnabled());
        request.setAttribute("cm", plugin.getMultiplexerPortFilter().isEnabled());
        request.setAttribute("interpreted", plugin.getInterpretedPrinter().isEnabled());
        request.setAttribute("logWhitespace", plugin.isLoggingWhitespace());
        request.setAttribute("loggingToStdOut", plugin.isLoggingToStdOut());
        request.setAttribute("loggingToFile", plugin.isLoggingToFile());
        final String csrf = StringUtils.randomString(16);
        request.getSession().setAttribute("csrf", csrf);
        request.setAttribute("csrf", csrf);

        addSessionFlashes(request, "errorMessage", "warningMessage", "successMessage");
        request.getRequestDispatcher("debugger-configuration.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        final HttpSession session = request.getSession();
        final Object csrf = session.getAttribute("csrf");
        if (csrf == null || !csrf.equals(request.getParameter("csrf"))) {
            session.setAttribute("errorMessage", "CSRF Failure!");
            response.sendRedirect(request.getRequestURI());
            return;
        }

        if (request.getParameter("cancel") != null) {
            session.setAttribute("warningMessage", "No changes were made");
            response.sendRedirect(request.getRequestURI());
            return;
        }

        plugin.getDefaultPortFilter().setEnabled(ParamUtils.getBooleanParameter(request, "c2s"));
        plugin.getOldPortFilter().setEnabled(ParamUtils.getBooleanParameter(request, "ssl"));
        plugin.getComponentPortFilter().setEnabled(ParamUtils.getBooleanParameter(request, "extcomp"));
        plugin.getMultiplexerPortFilter().setEnabled(ParamUtils.getBooleanParameter(request, "cm"));
        plugin.getInterpretedPrinter().setEnabled(ParamUtils.getBooleanParameter(request, "interpreted"));
        plugin.setLogWhitespace(ParamUtils.getBooleanParameter(request, "logWhitespace"));
        plugin.setLoggingToStdOut(ParamUtils.getBooleanParameter(request, "loggingToStdOut"));
        plugin.setLoggingToFile(ParamUtils.getBooleanParameter(request, "loggingToFile"));

        session.setAttribute("successMessage", "Logging settings updated");
        response.sendRedirect(request.getRequestURI());
    }
}
