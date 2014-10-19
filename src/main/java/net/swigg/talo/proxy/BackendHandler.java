/*
 * Copyright
 */

package net.swigg.talo.proxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
class BackendHandler extends ChannelHandlerAdapter {
    static private final Logger LOGGER = LoggerFactory.getLogger(BackendHandler.class);

    private final Channel inboundChannel;

    public BackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        LOGGER.debug(String.format("Channel active. (%s / %s)", inboundChannel.id().asShortText(), ctx.channel().id().asShortText()));
        ctx.read();

        // todo check if this line is unnecessary
        ctx.write(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        LOGGER.debug(String.format("Writing remote response. (%s / %s)", inboundChannel.id().asShortText(), ctx.channel().id().asShortText()));
        inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    LOGGER.debug(String.format("Remote response written. (%s / %s)", inboundChannel.id().asShortText(), ctx.channel().id().asShortText()));
                    ctx.channel().read();
                } else {
                    LOGGER.debug(String.format("Failed writing remote response. (%s / %s)", inboundChannel.id().asShortText(), ctx.channel().id().asShortText()));
                    future.channel().close();
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        FrontendHandler.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Failure communicating with backend.", cause);
        FrontendHandler.closeOnFlush(ctx.channel());
    }
}
