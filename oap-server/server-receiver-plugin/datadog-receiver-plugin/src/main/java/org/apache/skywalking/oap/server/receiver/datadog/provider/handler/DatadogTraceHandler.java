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
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
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
import org.apache.skywalking.oap.server.receiver.datadog.provider.entity.DDSpan;
import org.apache.skywalking.oap.server.receiver.zipkin.SpanForwardService;
import org.apache.skywalking.oap.server.receiver.zipkin.ZipkinReceiverModule;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableValue;
import zipkin2.Span;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            if (!uri.contains("/trace")) {
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
            List<List<DDSpan>> ddSpanList = deserializeMsgPack(bytes);
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

    private List<List<DDSpan>> deserializeMsgPack(byte[] bytes) {
        try (MessageUnpacker messageUnpacker = MessagePack.newDefaultUnpacker(bytes)) {
            ImmutableValue immutableValue = messageUnpacker.unpackValue();
            String json = immutableValue.toJson();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DDSpan.class));
            return objectMapper.readValue(json, collectionType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Span> covertToZipKinSpan(List<List<DDSpan>> ddSpanList) {
        List<Span> list = new ArrayList<>();
        for (List<DDSpan> ddSpans : ddSpanList) {
            for (DDSpan ddSpan : ddSpans) {
                final Span.Builder spanBuilder = Span.newBuilder();
                spanBuilder.traceId(String.valueOf(ddSpan.getTraceID()));
                spanBuilder.parentId(ddSpan.getParentID());
                spanBuilder.id(ddSpan.getSpanID());
                spanBuilder.name(ddSpan.getName());

                spanBuilder.timestamp(ddSpan.getStart());
                spanBuilder.duration(ddSpan.getDuration());

                spanBuilder.kind(getSpanKind(ddSpan));
            }
        }
        return list;
    }

    private Span.Kind getSpanKind(DDSpan ddSpan) {

        Map<String, String> meta = ddSpan.getMeta();
        String spanKind = meta.get(SPAN_KIND);
        meta.remove(spanKind);
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
