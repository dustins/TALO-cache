/*
 * Copyright
 */

package net.swigg.talo.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.Executors;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
public class TALOServer {
    private final ChannelInitializer<SocketChannel> childHandler;

    private final ProxyConfiguration configuration;

    private Channel channel;

    public TALOServer(ProxyConfiguration configuration, ChannelInitializer<SocketChannel> childHandler) {
        this.configuration = configuration;
        this.childHandler = childHandler;
    }

    public void run() {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(0, Executors.newCachedThreadPool());
        final EventLoopGroup workerGroup = new NioEventLoopGroup(200, Executors.newFixedThreadPool(200));

        try {
            final ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(childHandler)
                    .childOption(ChannelOption.AUTO_READ, false);

            final ChannelFuture f = bootstrap.bind(configuration.getBindPort()).sync();

            channel = f.channel();
        } catch (Exception e) {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void shutdownGracefully() {
        channel.eventLoop().parent().shutdownGracefully();
        channel.eventLoop().shutdownGracefully();
    }
}
