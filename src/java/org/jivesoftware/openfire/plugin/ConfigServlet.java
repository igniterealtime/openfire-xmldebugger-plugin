package org.jivesoftware.openfire.plugin;

import java.io.IOException;

import org.jivesoftware.admin.FlashMessageTag;
import org.jivesoftware.util.ParamUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@SuppressWarnings("serial")
public class ConfigServlet extends HttpServlet {

    private static DebuggerPlugin plugin;

    @Override
    public void init() {
        plugin = DebuggerPlugin.getInstance();
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        request.setAttribute("c2s", plugin.getDefaultPortFilter().isEnabled());
        request.setAttribute("ssl", plugin.getOldPortFilter().isEnabled());
        request.setAttribute("extcomp", plugin.getComponentPortFilter().isEnabled());
        request.setAttribute("cm", plugin.getMultiplexerPortFilter().isEnabled());
        request.setAttribute("interpreted", InterpretedXMLPrinter.ENABLED.getValue());
        request.setAttribute("logWhitespace", DebuggerPlugin.LOG_WHITESPACE.getValue());
        request.setAttribute("loggingToStdOut", DebuggerPlugin.LOG_TO_STDOUT.getValue());
        request.setAttribute("loggingToFile", DebuggerPlugin.LOG_TO_FILE.getValue());

        request.getRequestDispatcher("debugger-configuration.jsp").forward(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        final HttpSession session = request.getSession();

        if (request.getParameter("cancel") != null) {
            session.setAttribute(FlashMessageTag.WARNING_MESSAGE_KEY, "No changes were made");
            response.sendRedirect(request.getRequestURI());
            return;
        }

        plugin.getDefaultPortFilter().setEnabled(ParamUtils.getBooleanParameter(request, "c2s"));
        plugin.getOldPortFilter().setEnabled(ParamUtils.getBooleanParameter(request, "ssl"));
        plugin.getComponentPortFilter().setEnabled(ParamUtils.getBooleanParameter(request, "extcomp"));
        plugin.getMultiplexerPortFilter().setEnabled(ParamUtils.getBooleanParameter(request, "cm"));
        InterpretedXMLPrinter.ENABLED.setValue(ParamUtils.getBooleanParameter(request, "interpreted"));
        DebuggerPlugin.LOG_WHITESPACE.setValue(ParamUtils.getBooleanParameter(request, "logWhitespace"));
        DebuggerPlugin.LOG_TO_STDOUT.setValue(ParamUtils.getBooleanParameter(request, "loggingToStdOut"));
        DebuggerPlugin.LOG_TO_FILE.setValue(ParamUtils.getBooleanParameter(request, "loggingToFile"));

        session.setAttribute(FlashMessageTag.SUCCESS_MESSAGE_KEY, "Logging settings updated");
        response.sendRedirect(request.getRequestURI());
    }
}
