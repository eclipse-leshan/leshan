/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.transport.javacoap.server.coaptcp.transport;

import static org.eclipse.leshan.transport.javacoap.server.coaptcp.transport.NettyUtils.toCompletableFuture;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.net.ssl.SSLHandshakeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.transport.CoapTcpListener;
import com.mbed.coap.transport.CoapTcpTransport;
import com.mbed.coap.transport.TransportContext;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

public class NettyCoapTcpTransport implements CoapTcpTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyCoapTcpTransport.class);

    private final InetSocketAddress localAddress;
    private volatile Channel mainChannel;
    private final ConcurrentMap<SocketAddress, Channel> activeChannels = new ConcurrentHashMap<>();
    private volatile CoapTcpListener listener;
    private CompletableFuture<CoapPacket> receivePromise = new CompletableFuture<>();
    private final SslContext sslContext;
    private final Function<Channel, TransportContext> contextResolver;
    private final BiFunction<TransportContext, TransportContext, Boolean> contextMatcher;

    public NettyCoapTcpTransport(InetSocketAddress localadddress, //
            Function<Channel, TransportContext> contextResolver, //
            BiFunction<TransportContext, TransportContext, Boolean> contextMatcher, //
            SslContext sslContext) {
        this.localAddress = localadddress;
        this.sslContext = sslContext;
        this.contextResolver = contextResolver;
        this.contextMatcher = contextMatcher;
    }

    @Override
    public synchronized void start() throws IOException {
        // Init transport
        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
        bootstrap.group(bossGroup, workerGroup) //
                .channel(NioServerSocketChannel.class) //
                .childHandler(new ChannelRegistry()) //
                .option(ChannelOption.SO_BACKLOG, 100) //
                .option(ChannelOption.AUTO_READ, true) //
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // start it
        mainChannel = bootstrap.bind(localAddress).syncUninterruptibly().channel();
    }

    private class ChannelRegistry extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {

            // Handler order:
            // 0. Register/unregister new channel: all messages can only be sent
            // over open connections.
            // 1. Generate Idle events
            // 2. Close idle channels.
            // 3. Stream-to-message decoder
            // 4. Hand-off decoded messages to CoAP stack
            // 5. Close connections on errors.
            if (sslContext != null) {
                ch.pipeline().addFirst(sslContext.newHandler(ch.alloc()));
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        // Trigger channel active on TLS handshake Complete
                        if (evt instanceof SslHandshakeCompletionEvent) {
                            if (((SslHandshakeCompletionEvent) evt).isSuccess()) {
                                super.channelActive(ctx);
                            }
                        }
                    }

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        // block default channel active event.
                    }
                });
            }
            ch.pipeline().addLast(new TransportContextHandler(contextResolver));
            ch.pipeline().addLast(new ChannelTracker());
            // Remove IdleStateHandler for now because, we could define expected behavoir
            // See : https://github.com/eclipse-leshan/leshan/wiki/CoAP-over-TCP#half-open-connection-at-server-side
            // ch.pipeline().addLast(new IdleStateHandler(0, 0, 10 /* seconds */));
            ch.pipeline().addLast(new IdleStateHandler(0, 0, 0 /* disabled */));
            ch.pipeline().addLast(new CloseOnIdleHandler());
            ch.pipeline().addLast(new CoapTcpDecoder());
            ch.pipeline().addLast(new CoapTcpEncoder(contextMatcher));
            ch.pipeline().addLast(new DispatchHandler());
            ch.pipeline().addLast(new CloseOnErrorHandler());
        }
    }

    class ChannelTracker extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            activeChannels.put(ctx.channel().remoteAddress(), ctx.channel());
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (ctx.channel().remoteAddress() != null) {
                activeChannels.remove(ctx.channel().remoteAddress());
            } else {
                // it seems that sometime remoteAddress is null, I don't know why ...
                LOGGER.warn("Channel Remote Address is null");
                activeChannels.values().remove(ctx.channel());
            }
            super.channelInactive(ctx);
        }
    }

    public class DispatchHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!receivePromise.complete((CoapPacket) msg)) {
                ctx.fireChannelRead(msg);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
//            TransportContext tansportContext = ctx.channel().attr(TransportContextHandler.TRANSPORT_CONTEXT_ATTR).get();
//            if (tansportContext == null)
//                throw new IllegalStateException("transport context should not be null");

            if (listener != null
                    // Not clear what is the consequence but it seems that remote addresse can be null :
                    // https://github.com/netty/netty/issues/8501
                    && ctx.channel().remoteAddress() != null)
                listener.onConnected((InetSocketAddress) ctx.channel().remoteAddress());

            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//            TransportContext tansportContext = ctx.channel().attr(TransportContextHandler.TRANSPORT_CONTEXT_ATTR).get();
//            if (tansportContext == null)
//                throw new IllegalStateException("transport context should not be null");

            if (listener != null
                    // Not clear what is the consequence but it seems that remote addresse can be null :
                    // https://github.com/netty/netty/issues/8501
                    && ctx.channel().remoteAddress() != null)
                listener.onDisconnected((InetSocketAddress) ctx.channel().remoteAddress());

            super.channelInactive(ctx);
        }
    }

    private static class CloseOnIdleHandler extends ChannelDuplexHandler {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.channel().close();
            }
        }
    }

    private static class CloseOnErrorHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (!checkAndHandleIfExpectedError(ctx, cause)) {
                LOGGER.warn("Unexpected Error :", cause);
                ctx.close();
            }
        }

        private boolean checkAndHandleIfExpectedError(ChannelHandlerContext ctx, Throwable error) {
            if (error instanceof SSLHandshakeException || error.getCause() instanceof SSLHandshakeException) {
                LOGGER.debug("Handshake Failed with {} peer:", ctx.channel().remoteAddress(), error);
                return true;
            }
            return false;
        }
    }

    @Override
    public void stop() {
        mainChannel.close();
        mainChannel.closeFuture().syncUninterruptibly();
    }

    @Override
    public CompletableFuture<Boolean> sendPacket(CoapPacket packet) {
        InetSocketAddress peerAddress = packet.getRemoteAddress();
        Channel channel = activeChannels.get(peerAddress);
        if (channel == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(
                    new UnconnectedPeerException(String.format("Peer %s is not connected", peerAddress)));
            return future;
        }
        ChannelPromise channelPromise = channel.newPromise();
        channel.writeAndFlush(packet, channelPromise);

        return toCompletableFuture(channelPromise).thenApply(__ -> true);
    }

    public void closeConnections(Predicate<Channel> filter) {
        for (Channel channel : activeChannels.values()) {
            if (filter.test(channel)) {
                SslHandler sslHandler = channel.pipeline().get(SslHandler.class);

                // Invalidate TLS session to not allow to resume
                sslHandler.engine().getSession().invalidate();

                // Close TLS connection
                sslHandler.closeOutbound();
            }
        }
    }

    @Override
    public CompletableFuture<CoapPacket> receive() {
        receivePromise = new CompletableFuture<>();
        return receivePromise;
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) mainChannel.localAddress();
    }

    public Channel getChannel() {
        return mainChannel;
    }

    @Override
    public void setListener(CoapTcpListener listener) {
        this.listener = listener;
    }
}
