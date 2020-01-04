/*
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
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

import java.io.File;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.container.PluginManagerListener;
import org.jivesoftware.openfire.spi.ConnectionManagerImpl;
import org.jivesoftware.openfire.spi.ConnectionType;
import org.jivesoftware.util.SystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final RawPrintFilter defaultPortFilter;
    private final RawPrintFilter oldPortFilter;
    private final RawPrintFilter componentPortFilter;
    private final RawPrintFilter multiplexerPortFilter;
    private final InterpretedXMLPrinter interpretedPrinter;
    private boolean logWhitespace;
    private final SystemProperty<Boolean> logWhitespaceProperty = SystemProperty.Builder.ofType(Boolean.class)
        .setKey(PROPERTY_PREFIX + "logWhitespace")
        .setDefaultValue(Boolean.FALSE)
        .setDynamic(true)
        .setPlugin(PLUGIN_NAME)
        .addListener(enabled -> DebuggerPlugin.this.logWhitespace = enabled)
        .build();
    private boolean loggingToStdOut;
    private final SystemProperty<Boolean> loggingToStdOutProperty = SystemProperty.Builder.ofType(Boolean.class)
        .setKey(PROPERTY_PREFIX + "logToStdOut")
        .setDefaultValue(Boolean.TRUE)
        .setDynamic(true)
        .setPlugin(PLUGIN_NAME)
        .addListener(enabled -> DebuggerPlugin.this.loggingToStdOut = enabled)
        .build();
    private boolean loggingToFile;
    private final SystemProperty<Boolean> loggingToFileProperty = SystemProperty.Builder.ofType(Boolean.class)
        .setKey(PROPERTY_PREFIX + "logToFile")
        .setDefaultValue(Boolean.FALSE)
        .setDynamic(true)
        .setPlugin(PLUGIN_NAME)
        .addListener(enabled -> DebuggerPlugin.this.loggingToFile = enabled)
        .build();

    public DebuggerPlugin() {
        loggingToStdOut = loggingToStdOutProperty.getValue();
        loggingToFile = loggingToFileProperty.getValue();
        logWhitespace = logWhitespaceProperty.getValue();
        defaultPortFilter = new RawPrintFilter(this, "C2S");
        oldPortFilter = new RawPrintFilter(this, "SSL");
        componentPortFilter = new RawPrintFilter(this, "ExComp");
        multiplexerPortFilter = new RawPrintFilter(this, "CM");
        interpretedPrinter = new InterpretedXMLPrinter(this);
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

    private void addInterceptors() {
        // Add filter to filter chain builder
        final ConnectionManagerImpl connManager = (ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager();

        defaultPortFilter.addFilterToChain(connManager.getSocketAcceptor(ConnectionType.SOCKET_C2S, false));
        oldPortFilter.addFilterToChain(connManager.getSocketAcceptor(ConnectionType.SOCKET_C2S, true));
        componentPortFilter.addFilterToChain(connManager.getSocketAcceptor(ConnectionType.COMPONENT, false));
        multiplexerPortFilter.addFilterToChain(connManager.getSocketAcceptor(ConnectionType.CONNECTION_MANAGER, false));

        interpretedPrinter.setEnabled(interpretedPrinter.isEnabled());

        LOGGER.info("Plugin initialisation complete");
    }

    public void destroyPlugin() {
        // Remove filter from filter chain builder
        final ConnectionManagerImpl connManager = (ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager();

        defaultPortFilter.removeFilterFromChain(connManager.getSocketAcceptor(ConnectionType.SOCKET_C2S, false));
        oldPortFilter.removeFilterFromChain(connManager.getSocketAcceptor(ConnectionType.SOCKET_C2S, true));
        componentPortFilter.removeFilterFromChain(connManager.getSocketAcceptor(ConnectionType.COMPONENT, false));
        multiplexerPortFilter.removeFilterFromChain(connManager.getSocketAcceptor(ConnectionType.CONNECTION_MANAGER, false));

        // Remove the filters from existing sessions
        defaultPortFilter.shutdown();
        oldPortFilter.shutdown();
        componentPortFilter.shutdown();
        multiplexerPortFilter.shutdown();

        // Remove the packet interceptor that prints interpreted XML
        interpretedPrinter.shutdown();

        LOGGER.info("Plugin destruction complete");
    }

    public RawPrintFilter getDefaultPortFilter() {
        return defaultPortFilter;
    }

    public RawPrintFilter getOldPortFilter() {
        return oldPortFilter;
    }

    public RawPrintFilter getComponentPortFilter() {
        return componentPortFilter;
    }

    public RawPrintFilter getMultiplexerPortFilter() {
        return multiplexerPortFilter;
    }

    public InterpretedXMLPrinter getInterpretedPrinter() {
        return interpretedPrinter;
    }

    public boolean isLoggingToStdOut() {
        return loggingToStdOut;
    }

    public void setLoggingToStdOut(final boolean enabled) {
        loggingToStdOutProperty.setValue(enabled);
    }

    public boolean isLoggingToFile() {
        return loggingToFile;
    }

    public void setLoggingToFile(final boolean enabled) {
        loggingToFileProperty.setValue(enabled);
    }

    void log(final String messageToLog) {
        if (loggingToStdOut) {
            System.out.println(messageToLog);
        }
        if (loggingToFile) {
            LOGGER.info(messageToLog);
        }
    }

    public void setLogWhitespace(final boolean enabled) {
        logWhitespaceProperty.setValue(enabled);
    }

    public boolean isLoggingWhitespace() {
        return logWhitespace;
    }
}
