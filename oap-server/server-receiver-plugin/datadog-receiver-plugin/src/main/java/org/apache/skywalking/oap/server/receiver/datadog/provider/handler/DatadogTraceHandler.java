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

package org.apache.skywalking.oap.server.receiver.datadog.provider.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.apache.skywalking.oap.server.library.util.StringUtil;
import org.apache.skywalking.oap.server.receiver.datadog.provider.decoder.DDSpanDecoder;
import org.apache.skywalking.oap.server.receiver.datadog.provider.decoder.DDSpanDecoderFactory;
import org.apache.skywalking.oap.server.receiver.datadog.provider.entity.DDSpan;
import org.apache.skywalking.oap.server.receiver.zipkin.SpanForwardService;
import org.apache.skywalking.oap.server.receiver.zipkin.ZipkinReceiverModule;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableValue;
import zipkin2.Endpoint;
import zipkin2.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.skywalking.oap.server.receiver.datadog.provider.constants.MetaKeyConstants.PEER_HOSTNAME;
import static org.apache.skywalking.oap.server.receiver.datadog.provider.constants.MetaKeyConstants.PEER_HOST_IPV4;
import static org.apache.skywalking.oap.server.receiver.datadog.provider.constants.MetaKeyConstants.PEER_HOST_IPV6;
import static org.apache.skywalking.oap.server.receiver.datadog.provider.constants.MetaKeyConstants.SPAN_KIND;

@Slf4j
public class DatadogTraceHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private SpanForwardService forwardService;

    private final ModuleManager manager;

    public DatadogTraceHandler(ModuleManager manager) {
        this.manager = manager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    request.protocolVersion(),
                    HttpResponseStatus.OK);
            String uri = request.uri();
            if (!uri.contains("/traces")) {
                ctx.writeAndFlush(response);
                return;
            }

            ByteBuf content = request.content();
            int length = content.readableBytes();
            if (length == 0) {
                ctx.writeAndFlush(response);
                return;
            }

            byte[] bytes = new byte[length];
            content.readBytes(bytes);

            DDSpanDecoder ddSpanDecoder = DDSpanDecoderFactory.getDecoder(uri);
            if (ddSpanDecoder == null) {
                ctx.writeAndFlush(response);
                return;
            }

            List<DDSpan> ddSpanList = ddSpanDecoder.deserializeMsgPack(bytes);

            if (CollectionUtils.isEmpty(ddSpanList)) {
                ctx.writeAndFlush(response);
                return;
            }

            List<Span> spans = covertToZipKinSpan(ddSpanList);
            getSpanForward().send(spans);
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            log.error("dd trace handler error", e);
            FullHttpResponse response = new DefaultFullHttpResponse(
                    request.protocolVersion(),
                    HttpResponseStatus.INTERNAL_SERVER_ERROR);
            ctx.writeAndFlush(response);
        }
    }

    private List<DDSpan> deserializeMsgPack(byte[] bytes) {
        try (MessageUnpacker messageUnpacker = MessagePack.newDefaultUnpacker(bytes)) {
            ImmutableValue immutableValue = messageUnpacker.unpackValue();
            String json = immutableValue.toJson();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());
            CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DDSpan.class));
            List<List<DDSpan>> list = objectMapper.readValue(json, collectionType);
            return list.stream().flatMap(List::stream).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Span> covertToZipKinSpan(List<DDSpan> ddSpanList) {

        List<Span> list = new ArrayList<>();
        for (DDSpan ddSpan : ddSpanList) {
            final Span.Builder spanBuilder = Span.newBuilder();
            spanBuilder.traceId(String.valueOf(ddSpan.getTraceID()));
            spanBuilder.parentId(ddSpan.getParentID());
            spanBuilder.id(ddSpan.getSpanID());
            spanBuilder.name(ddSpan.getResource());

            spanBuilder.timestamp(TimeUnit.NANOSECONDS.toMicros(ddSpan.getStart()));
            spanBuilder.duration(TimeUnit.NANOSECONDS.toMicros(ddSpan.getDuration()));

            spanBuilder.localEndpoint(getLocalEndpoint(ddSpan));
            spanBuilder.remoteEndpoint(getRemoteEndpoint(ddSpan));

            spanBuilder.kind(getSpanKind(ddSpan));
            for (Map.Entry<String, String> metaEntry : ddSpan.getMeta().entrySet()) {
                spanBuilder.putTag(metaEntry.getKey(), metaEntry.getValue());
            }

            list.add(spanBuilder.build());
        }
        return list;
    }

    private Endpoint getLocalEndpoint(DDSpan ddSpan) {
        final Endpoint.Builder builder = Endpoint.newBuilder();
        builder.serviceName(ddSpan.getService());

        Map<String, String> meta = ddSpan.getMeta();
        String ipv4 = meta.get(PEER_HOST_IPV4);
        String hostName = meta.get(PEER_HOSTNAME);
        if (!builder.parseIp(ipv4)) {
            builder.parseIp(meta.get(PEER_HOST_IPV6));
        }

        if (StringUtil.isNotBlank(hostName)) {
            builder.parseIp(hostName);
        }

        return builder.build();
    }

    private Endpoint getRemoteEndpoint(DDSpan ddSpan) {
        final Endpoint.Builder builder = Endpoint.newBuilder();
        builder.serviceName(ddSpan.getService());

        Map<String, String> meta = ddSpan.getMeta();
        String ipv4 = meta.get(PEER_HOST_IPV4);
        String hostName = meta.get(PEER_HOSTNAME);
        if (!builder.parseIp(ipv4)) {
            builder.parseIp(meta.get(PEER_HOST_IPV6));
        }

        if (StringUtil.isNotBlank(hostName)) {
            builder.parseIp(hostName);
        }

        return builder.build();
    }

    private Span.Kind getSpanKind(DDSpan ddSpan) {

        Map<String, String> meta = ddSpan.getMeta();
        String spanKind = meta.get(SPAN_KIND);
        meta.remove(SPAN_KIND);
        if (StringUtil.isBlank(spanKind)) {
            return null;
        }

        switch (spanKind) {
            case "client":
                return Span.Kind.CLIENT;
            case "server":
                return Span.Kind.SERVER;
            case "producer":
                return Span.Kind.PRODUCER;
            case "consumer":
                return Span.Kind.CONSUMER;
            default:
                switch (ddSpan.getType()) {
                    case "web":
                        return Span.Kind.SERVER;
                    case "http":
                        return Span.Kind.CLIENT;
                    default:
                        return null;
                }
        }
    }

    private SpanForwardService getSpanForward() {
        if (forwardService == null) {
            forwardService = manager.find(ZipkinReceiverModule.NAME).provider().getService(SpanForwardService.class);
        }
        return forwardService;
    }
}
