package org.apache.skywalking.oap.server.receiver.datadog.provider.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.library.server.pool.CustomThreadFactory;
import org.apache.skywalking.oap.server.receiver.datadog.provider.config.DatadogReceiverConfig;

@Slf4j
public class DatadogReceiverServer {

    private DatadogReceiverConfig config;

    private Channel channel;

    public DatadogReceiverServer(DatadogReceiverConfig config) {
        this.config = config;
    }

    public void start() throws InterruptedException {

        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1, new CustomThreadFactory("Datadog-receiver"));
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(config.getMaxThread(), new CustomThreadFactory("Datadog-receiver"));

        ChannelFuture future = new ServerBootstrap()
                .group(workerGroup, bossGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new HttpServerCodec());
                        nioSocketChannel.pipeline().addLast(new HttpObjectAggregator(512 * 1024));
                        nioSocketChannel.pipeline().addLast(new DatadogTraceHandler());
                    }
                }).bind(config.getPort());

        channel = future.sync().channel();
        log.info("Datadog receiver server start success at port:{}", config.getPort());
    }
}
