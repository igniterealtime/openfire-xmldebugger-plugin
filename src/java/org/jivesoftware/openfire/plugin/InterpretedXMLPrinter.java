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

import java.net.UnknownHostException;

import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.SystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Packet;

/**
 * Packet interceptor that prints to the stdout XML packets (i.e. XML after
 * it was parsed).<p>
 * <p>
 * If you find in the logs an entry for raw XML, an entry that a session was closed and
 * never find the corresponding interpreted XML for the raw XML then there was an error
 * while parsing the XML that closed the session.
 *
 * @author Gaston Dombiak.
 */
public class InterpretedXMLPrinter implements PacketInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterpretedXMLPrinter.class);
    private final SystemProperty<Boolean> enabledProperty = SystemProperty.Builder.ofType(Boolean.class)
        .setKey(DebuggerPlugin.PROPERTY_PREFIX + "interpretedAllowed")
        .setDefaultValue(Boolean.FALSE)
        .setDynamic(true)
        .setPlugin(DebuggerPlugin.PLUGIN_NAME)
        .addListener(this::enabled)
        .build();
    private boolean enabled;

    @Override
    public void interceptPacket(final Packet packet, final Session session, final boolean incoming, final boolean processed) {
        if (session != null && !processed) {
            String hostAddress;
            try {
                hostAddress = "/" + session.getHostAddress() + ":?????";
            } catch (final UnknownHostException ignored) {
                hostAddress = "";
            }
            // Pad this out so it aligns with the RawPrintChannelHandlerFactory output
            DebuggerPlugin.log(String.format("INT %-16s - %s - (%11s): %s", hostAddress, incoming ? "RECV" : "SENT", session.getStreamID(), packet.toXML()));
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        enabledProperty.setValue(enabled);
    }

    private void enabled(final boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            LOGGER.info("Interpreted XML logger enabled");
            InterceptorManager.getInstance().addInterceptor(this);
        } else {
            LOGGER.info("Interpreted XML logger disabled");
            shutdown();
        }
    }

    void shutdown() {
        InterceptorManager.getInstance().removeInterceptor(this);
    }

}
