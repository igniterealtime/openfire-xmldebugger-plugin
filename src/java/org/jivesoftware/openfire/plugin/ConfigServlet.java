/*
 * Copyright (C) 2005-2008 Jive Software. 2023 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        request.setAttribute("interpreted", plugin.getInterpretedPrinter().isEnabled());
        request.setAttribute("logWhitespace", DebuggerPlugin.logWhitespaceProperty.getValue());
        request.setAttribute("loggingToStdOut", DebuggerPlugin.loggingToStdOutProperty.getValue());
        request.setAttribute("loggingToFile", DebuggerPlugin.loggingToFileProperty.getValue());

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
        plugin.getInterpretedPrinter().setEnabled(ParamUtils.getBooleanParameter(request, "interpreted"));
        DebuggerPlugin.logWhitespaceProperty.setValue(ParamUtils.getBooleanParameter(request, "logWhitespace"));
        DebuggerPlugin.loggingToStdOutProperty.setValue(ParamUtils.getBooleanParameter(request, "loggingToStdOut"));
        DebuggerPlugin.loggingToFileProperty.setValue(ParamUtils.getBooleanParameter(request, "loggingToFile"));

        session.setAttribute(FlashMessageTag.SUCCESS_MESSAGE_KEY, "Logging settings updated");
        response.sendRedirect(request.getRequestURI());
    }
}
