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

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.container.PluginManagerListener;
import org.jivesoftware.openfire.spi.*;
import org.jivesoftware.util.SystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Debugger plugin that prints XML traffic to stdout. By default it will only print
 * raw XML traffic (by using a MINA filter). To turn on printing of interpreted XML
 * (i.e. parsed XML) just enable the system property <tt>plugin.debugger.interpretedAllowed</tt>.
 * There is no need to restart the plugin or the server.
 *
 * @author Gaston Dombiak
 */
public class DebuggerPlugin implements Plugin {

    static final String PLUGIN_NAME = "XML Debugger Plugin"; // Exact match to plugin.xml
    private static final Logger LOGGER = LoggerFactory.getLogger(DebuggerPlugin.class);

    static final String PROPERTY_PREFIX = "plugin.xmldebugger.";
    private static DebuggerPlugin instance;

    private final RawPrintChannelHandlerFactory defaultPortFilter;
    private final RawPrintChannelHandlerFactory oldPortFilter;
    private final RawPrintChannelHandlerFactory s2sPortFilter;
    private final RawPrintChannelHandlerFactory componentPortFilter;
    private final RawPrintChannelHandlerFactory multiplexerPortFilter;
    private final InterpretedXMLPrinter interpretedPrinter;
    public static final SystemProperty<Boolean> logWhitespaceProperty = SystemProperty.Builder.ofType(Boolean.class)
        .setKey(PROPERTY_PREFIX + "logWhitespace")
        .setDefaultValue(Boolean.FALSE)
        .setDynamic(true)
        .setPlugin(PLUGIN_NAME)
        .build();
    public static final SystemProperty<Boolean> loggingToStdOutProperty = SystemProperty.Builder.ofType(Boolean.class)
        .setKey(PROPERTY_PREFIX + "logToStdOut")
        .setDefaultValue(Boolean.TRUE)
        .setDynamic(true)
        .setPlugin(PLUGIN_NAME)
        .build();
    public static final SystemProperty<Boolean> loggingToFileProperty = SystemProperty.Builder.ofType(Boolean.class)
        .setKey(PROPERTY_PREFIX + "logToFile")
        .setDefaultValue(Boolean.FALSE)
        .setDynamic(true)
        .setPlugin(PLUGIN_NAME)
        .build();

    public DebuggerPlugin() {
        defaultPortFilter = new RawPrintChannelHandlerFactory( "C2S-STARTTLS");
        oldPortFilter = new RawPrintChannelHandlerFactory( "C2S-DIRECTTLS");
        s2sPortFilter = new RawPrintChannelHandlerFactory( "S2S-STARTTLS");
        componentPortFilter = new RawPrintChannelHandlerFactory("ExComp-STARTTLS");
        multiplexerPortFilter = new RawPrintChannelHandlerFactory("CM-STARTTLS");
        interpretedPrinter = new InterpretedXMLPrinter();
        setInstance(this);
    }

    private static void setInstance(final DebuggerPlugin instance) {
        DebuggerPlugin.instance = instance;
    }

    public static DebuggerPlugin getInstance() {
        return DebuggerPlugin.instance;
    }

    public void initializePlugin(final PluginManager pluginManager, final File pluginDirectory) {
        pluginManager.addPluginManagerListener(new PluginManagerListener() {
            public void pluginsMonitored() {
                // Stop listening for plugin events
                pluginManager.removePluginManagerListener(this);
                // Start listeners
                addInterceptors();
            }
        });
    }

    private final List<ConnectionListener.SocketAcceptorEventListener> eventListeners = new ArrayList<>();
    private void addInterceptors() {
        ConnectionListener.SocketAcceptorEventListener listener;

        listener = addInterceptorAndListener(ConnectionType.SOCKET_C2S, false, defaultPortFilter);
        if (listener != null) { eventListeners.add(listener); }

        listener = addInterceptorAndListener(ConnectionType.SOCKET_C2S, true, oldPortFilter);
        if (listener != null) { eventListeners.add(listener); }

        listener = addInterceptorAndListener(ConnectionType.SOCKET_S2S, false, s2sPortFilter);
        if (listener != null) { eventListeners.add(listener); }

        listener = addInterceptorAndListener(ConnectionType.COMPONENT, false, componentPortFilter);
        if (listener != null) { eventListeners.add(listener); }

        listener = addInterceptorAndListener(ConnectionType.CONNECTION_MANAGER, false, multiplexerPortFilter);
        if (listener != null) { eventListeners.add(listener); }

        interpretedPrinter.setEnabled(interpretedPrinter.isEnabled());

        LOGGER.info("Plugin initialisation complete");
    }

    private static ConnectionListener.SocketAcceptorEventListener addInterceptorAndListener(ConnectionType type, boolean directTLS, RawPrintChannelHandlerFactory handler) {
        final ConnectionManagerImpl connManager = (ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager();
        final ConnectionListener listener = connManager.getListener(type, directTLS);
        final ConnectionListener.SocketAcceptorEventListener eventListener = new ConnectionListener.SocketAcceptorEventListener() {
            @Override
            public void acceptorStarting(ConnectionAcceptor connectionAcceptor) {
                if (connectionAcceptor instanceof NettyConnectionAcceptor) {
                    LOGGER.info("Re-registering channel handler on {}", connectionAcceptor);
                    ((NettyConnectionAcceptor) connectionAcceptor).addChannelHandler(handler);
                }
            }

            @Override
            public void acceptorStopping(ConnectionAcceptor connectionAcceptor) {}
        };
        if (!(listener.getConnectionAcceptor() instanceof NettyConnectionAcceptor)) {
            listener.add(eventListener);
            LOGGER.info("Registering channel handler on {}", listener.getConnectionAcceptor());
            ((NettyConnectionAcceptor) listener.getConnectionAcceptor()).addChannelHandler(handler);
            return eventListener;
        } else {
            LOGGER.warn("Unable to register channel handler for connection type {} (direct TLS: {}): The connection acceptor is not an instance of NettyConnectionAcceptor: {}", type, directTLS, listener.getConnectionAcceptor());
            return null;
        }
    }

    private static void removeInterceptorAndListener(ConnectionType type, boolean directTLS, RawPrintChannelHandlerFactory handler, List<ConnectionListener.SocketAcceptorEventListener> eventListeners) {
        final ConnectionManagerImpl connManager = (ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager();
        final ConnectionListener listener = connManager.getListener(type, directTLS);

        eventListeners.forEach(listener::remove); // One of these should match, the others are no-ops.
        LOGGER.info("Removing channel handler from {}", listener.getConnectionAcceptor());

        ((NettyConnectionAcceptor) listener.getConnectionAcceptor()).removeChannelHandler(handler);
    }

    public void destroyPlugin() {
        removeInterceptorAndListener(ConnectionType.SOCKET_C2S, false, defaultPortFilter, eventListeners);
        removeInterceptorAndListener(ConnectionType.SOCKET_C2S, true, oldPortFilter, eventListeners);
        removeInterceptorAndListener(ConnectionType.SOCKET_S2S, false, s2sPortFilter, eventListeners);
        removeInterceptorAndListener(ConnectionType.COMPONENT, false, componentPortFilter, eventListeners);
        removeInterceptorAndListener(ConnectionType.CONNECTION_MANAGER, false, multiplexerPortFilter, eventListeners);
        eventListeners.clear();
        // Remove the packet interceptor that prints interpreted XML
        interpretedPrinter.shutdown();

        LOGGER.info("Plugin destruction complete");
    }

    public RawPrintChannelHandlerFactory getDefaultPortFilter() {
        return defaultPortFilter;
    }

    public RawPrintChannelHandlerFactory getOldPortFilter() {
        return oldPortFilter;
    }

    public RawPrintChannelHandlerFactory getComponentPortFilter() {
        return componentPortFilter;
    }

    public RawPrintChannelHandlerFactory getMultiplexerPortFilter() {
        return multiplexerPortFilter;
    }

    public InterpretedXMLPrinter getInterpretedPrinter() {
        return interpretedPrinter;
    }

    public static void log(final String messageToLog) {
        if (loggingToStdOutProperty.getValue()) {
            System.out.println(messageToLog);
        }
        if (loggingToFileProperty.getValue()) {
            LOGGER.info(messageToLog);
        }
    }
}
