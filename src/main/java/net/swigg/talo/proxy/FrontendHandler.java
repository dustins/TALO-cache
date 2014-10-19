/*
 * Copyright
 */

package net.swigg.talo.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
class FrontendHandler extends ChannelHandlerAdapter {
    static private final Logger LOGGER = LoggerFactory.getLogger(FrontendHandler.class);

    private final ProxyConfiguration configuration;

    private volatile Channel outboundChannel;

    public FrontendHandler(ProxyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (outboundChannel == null) {
            final Channel inboundChannel = ctx.channel();

            // Start the connection attempt.
            final Bootstrap b = new Bootstrap();
            b.group(inboundChannel.eventLoop())
                    .channel(ctx.channel().getClass())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(1048576),
                                    new BackendHandler(inboundChannel)
                            );
                        }
                    });
//                    .option(ChannelOption.AUTO_READ, false);

            final ChannelFuture f = b.connect(configuration.getProxyHost(), configuration.getProxyPort());

            outboundChannel = f.channel();
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        LOGGER.debug("Success connecting. ({} / {})", inboundChannel.id().asShortText(), outboundChannel.id().asShortText());

                        // connection complete start to read first data
                        outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) {
                                if (future.isSuccess()) {
                                    LOGGER.debug("Request forwarded. ({} / {})", inboundChannel.id().asShortText(), outboundChannel.id().asShortText());
                                    // was able to flush out data, start to read the next chunk
                                    ctx.channel().read();
                                } else {
                                    LOGGER.debug("Failed forwarding. ({} / {})", inboundChannel.id().asShortText(), outboundChannel.id().asShortText());
                                    future.channel().close();
                                }
                            }
                        });
                    } else {
                        LOGGER.debug("Failure connecting. ({} / {})", inboundChannel.id().asShortText(), outboundChannel.id().asShortText());
                        // Close the connection if the connection attempt has failed.
                        inboundChannel.close();
                    }
                }
            });
        } else {
            LOGGER.warn("Outbound already exists. ({})", ctx.channel().id().asShortText());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Unable to complete request.", cause);
        closeOnFlush(ctx.channel());
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
