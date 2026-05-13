package org.jivesoftware.openfire.plugin;

import org.dom4j.Element;
import org.jivesoftware.admin.FlashMessageTag;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.DomainPair;
import org.jivesoftware.openfire.session.IncomingServerSession;
import org.jivesoftware.openfire.session.OutgoingServerSession;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.ParamUtils;
import org.jivesoftware.util.SAXReaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StanzaSenderServlet extends HttpServlet
{
    private static final Logger Log = LoggerFactory.getLogger(StanzaSenderServlet.class); // beware: this plugin uses its own log file (logged messages will end up in that file, not in openfire.log)
    private static final String DEFAULT_CONNECTION_VALUE = "router";

    /**
     * Renders the stanza sender page and exposes currently available connections for selection.
     *
     * @param request the incoming HTTP request.
     * @param response the HTTP response used to render the JSP.
     * @throws ServletException if forwarding to the JSP fails.
     * @throws IOException if an I/O error occurs while handling the request.
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("connections", getAvailableConnections());
        request.getRequestDispatcher("stanza-send.jsp").forward(request, response);
        final HttpSession session = request.getSession();
        session.removeAttribute("stanza");
        session.removeAttribute("connection");
        session.removeAttribute("result");
        session.removeAttribute(FlashMessageTag.SUCCESS_MESSAGE_KEY);
        session.removeAttribute(FlashMessageTag.WARNING_MESSAGE_KEY);
        session.removeAttribute(FlashMessageTag.ERROR_MESSAGE_KEY);
    }

    /**
     * Sends a stanza either through the packet router (default) or through a selected active session.
     *
     * When an IQ request stanza is submitted, this method temporarily installs an interceptor to
     * capture the corresponding IQ response for display in the UI.
     *
     * @param request the incoming HTTP request that contains form data.
     * @param response the HTTP response used for redirects.
     * @throws IOException if an I/O error occurs while redirecting the response.
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException
    {
        final HttpSession session = request.getSession();
        if (request.getParameter("cancel") != null) {
            session.setAttribute(FlashMessageTag.WARNING_MESSAGE_KEY, "No changes were made");
            response.sendRedirect(request.getRequestURI());
            return;
        }

        final String stanza = ParamUtils.getStringParameter(request, "stanza", "").trim();
        final String connection = ParamUtils.getStringParameter(request, "connection", DEFAULT_CONNECTION_VALUE).trim();
        session.setAttribute("stanza", stanza);
        session.setAttribute("connection", connection);

        if (stanza.isEmpty()) {
            session.setAttribute(FlashMessageTag.ERROR_MESSAGE_KEY, "Please provide input.");
            response.sendRedirect(request.getRequestURI());
            return;
        }

        final Element element;
        try {
            element = SAXReaderUtil.readRootElement(stanza);
        } catch (ExecutionException | InterruptedException e) {
            Log.warn("Unable to parse provided input as XML. Input: '{}'", stanza, e);
            session.setAttribute(FlashMessageTag.ERROR_MESSAGE_KEY, "Unable to parse provided input as XML.");
            response.sendRedirect(request.getRequestURI());
            return;
        }

        final PacketInterceptor resultListener;
        final CompletableFuture<Packet> result = new CompletableFuture<>();
        final Packet packet;
        try {
            switch (element.getName()) {
                case "iq":
                    packet = new IQ(element);
                    if (((IQ)packet).isRequest() && packet.getID() != null) {
                        resultListener = new PacketInterceptor()
                        {
                            @Override
                            public void interceptPacket(Packet response, Session session, boolean read, boolean processed) throws PacketRejectedException
                            {
                                if (!processed && response instanceof IQ && ((IQ) response).isResponse() && response.getID().equals(packet.getID()))
                                {
                                    result.complete(response);
                                }
                            }
                        };
                    } else {
                        resultListener = null;
                    }
                    break;

                case "message":
                    packet = new Message(element);
                    resultListener = null;
                    break;

                case "presence":
                    packet = new Presence(element);
                    resultListener = null;
                    break;

                default:
                    Log.warn("Unable to parse provided input as XMPP. Unrecognized element name: {}", element.getName());
                    session.setAttribute(FlashMessageTag.ERROR_MESSAGE_KEY, "Unable to parse provided input as XMPP. Unrecognized element name: " + element.getName());
                    response.sendRedirect(request.getRequestURI());
                    return;
            }
        } catch (RuntimeException e) {
            Log.warn("Unable to parse provided input as XMPP. Input: '{}'", element.asXML(), e);
            session.setAttribute(FlashMessageTag.ERROR_MESSAGE_KEY, "Unable to parse provided input as XMPP.");
            response.sendRedirect(request.getRequestURI());
            return;
        }

        try {
            if (resultListener != null) {
                InterceptorManager.getInstance().addInterceptor(resultListener);
            }
            if (DEFAULT_CONNECTION_VALUE.equals(connection)) {
                XMPPServer.getInstance().getPacketRouter().route(packet);
            } else {
                final Session selectedSession = resolveSession(connection);
                if (selectedSession == null) {
                    session.setAttribute(FlashMessageTag.ERROR_MESSAGE_KEY, "The selected connection is no longer available.");
                    response.sendRedirect(request.getRequestURI());
                    return;
                }
                selectedSession.process(packet);
            }
            session.setAttribute(FlashMessageTag.SUCCESS_MESSAGE_KEY, "Stanza sent");
            if (resultListener != null) {
                final Packet resultingStanza = result.get(15, TimeUnit.SECONDS);
                session.setAttribute(FlashMessageTag.SUCCESS_MESSAGE_KEY, "Stanza sent, result received.");
                session.setAttribute("result", resultingStanza.toString());
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.warn("No response to stanza captured.", e);
            session.setAttribute(FlashMessageTag.WARNING_MESSAGE_KEY, "No response to stanza captured.");
            response.sendRedirect(request.getRequestURI());
            return;
        } finally {
            if (resultListener != null) {
                InterceptorManager.getInstance().removeInterceptor(resultListener);
            }
        }

        response.sendRedirect(request.getRequestURI());
    }

    /**
     * Collects all currently active C2S and S2S connections that can be selected in the UI.
     *
     * @return a list of selectable connection options.
     */
    private static List<ConnectionOption> getAvailableConnections() {
        final SessionManager sessionManager = SessionManager.getInstance();
        final List<ConnectionOption> result = new ArrayList<>();

        for (final ClientSession clientSession : sessionManager.getSessions()) {
            final String streamId = clientSession.getStreamID() != null ? clientSession.getStreamID().getID() : null;
            if (streamId != null) {
                result.add(new ConnectionOption("c2s:" + streamId, "C2S - " + clientSession.getAddress() + " (stream: " + streamId + ")"));
            }
        }

        for (final String remoteDomain : sessionManager.getIncomingServers()) {
            for (final IncomingServerSession incomingSession : sessionManager.getIncomingServerSessions(remoteDomain)) {
                final String streamId = incomingSession.getStreamID() != null ? incomingSession.getStreamID().getID() : null;
                if (streamId != null) {
                    result.add(new ConnectionOption("s2s-in:" + streamId, "S2S incoming - " + incomingSession.getAddress() + " (stream: " + streamId + ")"));
                }
            }
        }

        for (final DomainPair domainPair : sessionManager.getOutgoingDomainPairs()) {
            result.add(new ConnectionOption("s2s-out:" + domainPair.getLocal() + "|" + domainPair.getRemote(), "S2S outgoing - " + domainPair.getLocal() + " -> " + domainPair.getRemote()));
        }

        return result;
    }

    /**
     * Resolves the submitted selection value to a currently active session.
     *
     * @param selection the form value of the selected connection option.
     * @return the matched session, or {@code null} when the selection is invalid or no longer active.
     */
    private static Session resolveSession(final String selection) {
        final SessionManager sessionManager = SessionManager.getInstance();

        if (selection.startsWith("c2s:")) {
            final String streamId = selection.substring("c2s:".length());
            for (final ClientSession clientSession : sessionManager.getSessions()) {
                if (clientSession.getStreamID() != null && streamId.equals(clientSession.getStreamID().getID())) {
                    return clientSession;
                }
            }
            return null;
        }

        if (selection.startsWith("s2s-in:")) {
            final String streamId = selection.substring("s2s-in:".length());
            for (final String remoteDomain : sessionManager.getIncomingServers()) {
                for (final IncomingServerSession incomingSession : sessionManager.getIncomingServerSessions(remoteDomain)) {
                    if (incomingSession.getStreamID() != null && streamId.equals(incomingSession.getStreamID().getID())) {
                        return incomingSession;
                    }
                }
            }
            return null;
        }

        if (selection.startsWith("s2s-out:")) {
            final String value = selection.substring("s2s-out:".length());
            final int splitPosition = value.indexOf('|');
            if (splitPosition <= 0 || splitPosition == value.length() - 1) {
                return null;
            }
            final String localDomain = value.substring(0, splitPosition);
            final String remoteDomain = value.substring(splitPosition + 1);
            final OutgoingServerSession outgoingServerSession = sessionManager.getOutgoingServerSession(new DomainPair(localDomain, remoteDomain));
            return outgoingServerSession;
        }

        return null;
    }

    /**
     * Represents a single connection option rendered in the stanza sender dropdown.
     */
    public static class ConnectionOption {
        private final String value;
        private final String label;

        /**
         * Creates a new option with machine-readable value and human-readable label.
         *
         * @param value the form value that identifies a connection.
         * @param label the display label shown to administrators.
         */
        public ConnectionOption(final String value, final String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * Returns the form value that identifies the selected connection.
         *
         * @return the option value.
         */
        public String getValue() {
            return value;
        }

        /**
         * Returns the label shown in the connection dropdown.
         *
         * @return the option label.
         */
        public String getLabel() {
            return label;
        }
    }
}
