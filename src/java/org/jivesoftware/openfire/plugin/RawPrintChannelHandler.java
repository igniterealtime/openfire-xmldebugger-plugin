/*
 * Copyright (C) 2023 Ignite Realtime Foundation. All rights reserved.
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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.net.SocketAddress;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RawPrintChannelHandler extends ChannelDuplexHandler
{
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
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception
    {
        DebuggerPlugin.log(messagePrefix(remoteAddress, "OPEN", ctx.name()));

        super.connect(ctx, remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
    {
        final SocketAddress remoteAddress = ctx.channel() != null ? ctx.channel().remoteAddress() : null;
        DebuggerPlugin.log(messagePrefix(remoteAddress, "CLSD", ctx.name()));

        super.disconnect(ctx, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Decode the bytebuffer and print it to the stdout
        if (msg instanceof String && (DebuggerPlugin.logWhitespaceProperty.getValue() || !(msg.toString()).isEmpty())) {
            final SocketAddress remoteAddress = ctx.channel() != null ? ctx.channel().remoteAddress() : null;
            DebuggerPlugin.log(messagePrefix(remoteAddress, "RECV", ctx.name()) + ": " + msg);
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
    {
        if (msg instanceof String && (DebuggerPlugin.logWhitespaceProperty.getValue() || !(msg.toString()).isEmpty())) {
            final SocketAddress remoteAddress = ctx.channel() != null ? ctx.channel().remoteAddress() : null;
            DebuggerPlugin.log(messagePrefix(remoteAddress, "SENT", ctx.name()) + ": " + msg);
        }

        super.write(ctx, msg, promise);
    }
}
