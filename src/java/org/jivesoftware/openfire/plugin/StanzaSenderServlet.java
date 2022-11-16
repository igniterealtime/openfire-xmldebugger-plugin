package org.jivesoftware.openfire.plugin;

import org.dom4j.Element;
import org.jivesoftware.admin.FlashMessageTag;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StanzaSenderServlet extends HttpServlet
{
    private static final Logger Log = LoggerFactory.getLogger(StanzaSenderServlet.class); // beware: this plugin uses its own log file (logged messages will end up in that file, not in openfire.log)

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("stanza-send.jsp").forward(request, response);
        final HttpSession session = request.getSession();
        session.removeAttribute("stanza");
        session.removeAttribute("result");
        session.removeAttribute(FlashMessageTag.SUCCESS_MESSAGE_KEY);
        session.removeAttribute(FlashMessageTag.WARNING_MESSAGE_KEY);
        session.removeAttribute(FlashMessageTag.ERROR_MESSAGE_KEY);
    }

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
        session.setAttribute("stanza", stanza);

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
            XMPPServer.getInstance().getPacketRouter().route(packet);
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
}
