/*
 * Copyright (C) 2023-2026 Ignite Realtime Foundation. All rights reserved.
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RawPrintChannelHandler extends ChannelDuplexHandler
{
    private static final Logger Log = LoggerFactory.getLogger(RawPrintChannelHandler.class);

    private final String prefix;

    public RawPrintChannelHandler(final String prefix)
    {
        this.prefix = prefix;
    }

    private String messagePrefix(final SocketAddress remoteAddress, final String messageType, final String channelContextName) {
        final String nowAsString = ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT );
        return String.format("%s - %s %-16s - %s - (%11s)", nowAsString, prefix, remoteAddress == null ? "???" : remoteAddress, messageType, channelContextName);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception
    {
        super.read(ctx);
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception
    {
        final SocketAddress remoteAddress = ctx.channel() != null ? ctx.channel().remoteAddress() : null;
        DebuggerPlugin.log(messagePrefix(remoteAddress, "OPEN", ctx.name()));

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception
    {
        final SocketAddress remoteAddress = ctx.channel() != null ? ctx.channel().remoteAddress() : null;
        DebuggerPlugin.log(messagePrefix(remoteAddress, "CLSD", ctx.name()));

        super.channelInactive(ctx);
    }

    private static String payloadAsText(final Object msg)
    {
        if (msg instanceof String) {
            return (String) msg;
        }
        if (msg instanceof ByteBuf) {
            // Use the readable region without changing reader indexes.
            return ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
        }
        if (msg instanceof ByteBufHolder) {
            return ((ByteBufHolder) msg).content().toString(CharsetUtil.UTF_8);
        }

        Log.debug("Unrecognized payload type '{}' - returning 'toString' as a fallback option.", msg.getClass().getName());
        return msg.toString();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final String payload = payloadAsText(msg);
        if (payload != null && (DebuggerPlugin.logWhitespaceProperty.getValue() || !payload.isEmpty())) {
            final SocketAddress remoteAddress = ctx.channel() != null ? ctx.channel().remoteAddress() : null;
            DebuggerPlugin.log(messagePrefix(remoteAddress, "RECV", ctx.name()) + ": " + payload);
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
    {
        final String payload = payloadAsText(msg);
        if (payload != null && (DebuggerPlugin.logWhitespaceProperty.getValue() || !payload.isEmpty())) {
            final SocketAddress remoteAddress = ctx.channel() != null ? ctx.channel().remoteAddress() : null;
            DebuggerPlugin.log(messagePrefix(remoteAddress, "SENT", ctx.name()) + ": " + payload);
        }

        super.write(ctx, msg, promise);
    }
}
