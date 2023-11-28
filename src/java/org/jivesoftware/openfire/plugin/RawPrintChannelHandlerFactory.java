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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import org.jivesoftware.openfire.nio.NettyChannelHandlerFactory;
import org.jivesoftware.openfire.nio.NettyConnection;
import org.jivesoftware.util.SystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Netty handler that prints to the stdout received XML stanzas before they are actually parsed and
 * also prints XML stanzas as sent to the XMPP entities. Moreover, it also prints information when
 * a session is closed.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @author Gaston Dombiak
 */
public class RawPrintChannelHandlerFactory implements NettyChannelHandlerFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RawPrintChannelHandlerFactory.class);
    private static final String FILTER_NAME = "rawDebugger";
    private final String prefix;
    private boolean enabled;
    private final SystemProperty<Boolean> enabledProperty;

    RawPrintChannelHandlerFactory(final String prefix) {
        this.prefix = prefix;
        this.enabledProperty = SystemProperty.Builder.ofType(Boolean.class)
            .setKey(DebuggerPlugin.PROPERTY_PREFIX + prefix.toLowerCase())
            .setDefaultValue(Boolean.TRUE)
            .setDynamic(true)
            .setPlugin(DebuggerPlugin.PLUGIN_NAME)
            .addListener(this::enabled)
            .build();
        this.enabled(enabledProperty.getValue());
    }

    ChannelHandler generateHandler() {
        return new RawPrintChannelHandler(prefix);
    }

    @Override
    public void addNewHandlerTo(final ChannelPipeline pipeline)
    {
        final ChannelHandler filter = generateHandler();

        // Find the handler after which to add the new handler. This list is in order of preference.
        final List<String> candidates = List.of(
            "inboundCompressionHandler",
            "outboundCompressionHandler",
            NettyConnection.SSL_HANDLER_NAME,
            "keepAliveHandler"
        );

        for (final String candidate : candidates) {
            if (pipeline.get(candidate) != null) {
                LOGGER.debug("Adding handler '{}' for {} as the first filter after the {} filter in pipeline {}", FILTER_NAME, prefix, candidate, pipeline);
                pipeline.addAfter(candidate, FILTER_NAME, filter);
                break;
            }
        }
        if (pipeline.get(FILTER_NAME) == null) {
            LOGGER.debug("Adding handler '{}' for {} as the first filter in pipeline {}", FILTER_NAME, prefix, pipeline);
            pipeline.addFirst(FILTER_NAME, filter);
        }
    }

    @Override
    public void removeHandlerFrom(final ChannelPipeline pipeline) {
        if (pipeline.get(FILTER_NAME) != null) {
            LOGGER.debug("Removing handler '{}' for {} from pipeline {}", FILTER_NAME, prefix, pipeline);
            pipeline.remove(FILTER_NAME);
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
        LOGGER.info("{} logger {}", prefix, enabled ? "enabled" : "disabled");
    }
}
