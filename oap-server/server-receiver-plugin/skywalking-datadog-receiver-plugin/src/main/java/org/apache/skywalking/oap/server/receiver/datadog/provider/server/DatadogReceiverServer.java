/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.receiver.datadog.provider.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.server.pool.CustomThreadFactory;
import org.apache.skywalking.oap.server.receiver.datadog.provider.handler.DatadogTraceHandler;
import org.apache.skywalking.oap.server.receiver.datadog.provider.config.DatadogReceiverConfig;

@Slf4j
public class DatadogReceiverServer {

    private final DatadogReceiverConfig config;

    private final ModuleManager manager;

    public DatadogReceiverServer(DatadogReceiverConfig config, ModuleManager manager) {
        this.config = config;
        this.manager = manager;
    }

    public void start() throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(config.getMaxThread(), new CustomThreadFactory("Datadog-receiver-boss"));
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(config.getMaxThread(), new CustomThreadFactory("Datadog-receiver-work"));

        ChannelFuture future = new ServerBootstrap()
                .group(workerGroup, bossGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new HttpServerCodec());
                        nioSocketChannel.pipeline().addLast(new HttpObjectAggregator(512 * 1024));
                        nioSocketChannel.pipeline().addLast(new DatadogTraceHandler(manager));
                    }
                }).bind(config.getPort());
        log.info("Datadog receiver server start success at port:{}", config.getPort());
    }
}
