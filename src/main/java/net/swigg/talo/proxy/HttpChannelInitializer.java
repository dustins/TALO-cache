/*
 * Copyright
 */

package net.swigg.talo.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final CacheHandler cacheHandler;

    public HttpChannelInitializer(CacheHandler cacheHandler) {
        this.cacheHandler = cacheHandler;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                new HttpServerCodec(),
                new HttpObjectAggregator(1048576),
                cacheHandler
        );
    }
}
