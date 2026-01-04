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
 */
package org.apache.seata.server.cluster.manager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.listener.ClusterChangeListener;
import org.apache.seata.server.cluster.watch.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ClusterWatcherManager implements ClusterChangeListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Map<String, Queue<Watcher<HttpContext>>> WATCHERS = new ConcurrentHashMap<>();

    private static final Map<String, Long> GROUP_UPDATE_TERM = new ConcurrentHashMap<>();

    private static final Map<Watcher<HttpContext>, Boolean> HTTP2_HEADERS_SENT = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("long-polling", 1));

    @PostConstruct
    public void init() {
        // Periodically check and respond to watchers that have timed out
        scheduledThreadPoolExecutor.scheduleAtFixedRate(
                () -> {
                    for (String group : WATCHERS.keySet()) {
                        Optional.ofNullable(WATCHERS.remove(group))
                                .ifPresent(watchers -> watchers.parallelStream().forEach(watcher -> {
                                    HttpContext context = watcher.getAsyncContext();
                                    boolean isHttp2 = context.isHttp2();
                                    if (isHttp2) {
                                        if (!context.getContext().channel().isActive()) {
                                            watcher.setDone(true);
                                            HTTP2_HEADERS_SENT.remove(watcher);
                                        } else {
                                            registryWatcher(watcher);
                                        }
                                    } else {
                                        if (System.currentTimeMillis() >= watcher.getTimeout()) {
                                            watcher.setDone(true);
                                            sendWatcherResponse(watcher, HttpResponseStatus.NOT_MODIFIED, true, false);
                                        } else if (!watcher.isDone()) {
                                            registryWatcher(watcher);
                                        }
                                    }
                                }));
                    }
                },
                1,
                1,
                TimeUnit.SECONDS);
    }

    @Override
    @EventListener
    @Async
    public void onChangeEvent(ClusterChangeEvent event) {
        if (event.getTerm() > 0) {
            GROUP_UPDATE_TERM.put(event.getGroup(), event.getTerm());
            // Notify all watchers of cluster information changes
            Optional.ofNullable(WATCHERS.remove(event.getGroup()))
                    .ifPresent(watchers -> watchers.parallelStream().forEach(this::notifyWatcher));
        }
    }

    private void notifyWatcher(Watcher<HttpContext> watcher) {
        HttpContext context = watcher.getAsyncContext();
        boolean isHttp2 = context instanceof HttpContext && context.isHttp2();

        if (!isHttp2) {
            watcher.setDone(true);
        }

        boolean isFirstResponse = !HTTP2_HEADERS_SENT.getOrDefault(watcher, false);
        sendWatcherResponse(watcher, HttpResponseStatus.OK, false, isFirstResponse);
        if (isFirstResponse && isHttp2) {
            HTTP2_HEADERS_SENT.put(watcher, true);
        }

        // Update watcher's term to the latest term to prevent infinite loop
        // This ensures that when registryWatcher is called, it won't trigger notifyWatcher again
        String group = watcher.getGroup();
        Long latestTerm = GROUP_UPDATE_TERM.get(group);
        if (latestTerm != null && latestTerm > watcher.getTerm()) {
            watcher.setTerm(latestTerm);
        }

        // For HTTP/2, re-register the watcher to continue listening for future updates
        if (isHttp2 && !watcher.isDone()) {
            registryWatcher(watcher);
        }
    }
    /**
     * Send watcher response to the client.
     *
     * @param watcher     the watcher instance
     * @param nettyStatus the HTTP status code
     * @param closeStream whether to close the HTTP/2 stream (endStream=true)
     * @param sendHeaders whether to send HTTP/2 headers frame (only needed for first response)
     */
    private void sendWatcherResponse(
            Watcher<HttpContext> watcher, HttpResponseStatus nettyStatus, boolean closeStream, boolean sendHeaders) {

        HttpContext context = watcher.getAsyncContext();
        if (!(context instanceof HttpContext)) {
            logger.warn(
                    "Unsupported context type for watcher on group {}: {}",
                    watcher.getGroup(),
                    context != null ? context.getClass().getName() : "null");
            return;
        }
        ChannelHandlerContext ctx = context.getContext();

        if (!ctx.channel().isActive()) {
            HTTP2_HEADERS_SENT.remove(watcher);
            logger.warn(
                    "Netty channel is not active for watcher on group {}, cannot send response.", watcher.getGroup());
            return;
        }

        if (!context.isHttp2()) {
            HttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, nettyStatus, Unpooled.EMPTY_BUFFER);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

            if (!context.isKeepAlive()) {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                ctx.writeAndFlush(response);
            }
            return;
        }

        // For HTTP/2, headers must be sent first on the initial response
        if (sendHeaders) {
            Http2Headers headers = new DefaultHttp2Headers().status(nettyStatus.codeAsText());
            headers.set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream; charset=utf-8");
            headers.set(HttpHeaderNames.CACHE_CONTROL, "no-cache");

            ctx.write(new DefaultHttp2HeadersFrame(headers));
        }

        String group = watcher.getGroup();
        String sse = buildSSEFormat(nettyStatus, closeStream, sendHeaders, group);

        ByteBuf content = Unpooled.copiedBuffer(sse, StandardCharsets.UTF_8);

        // Send DATA frame (if closeStream is true, it will end the current stream)
        ctx.write(new DefaultHttp2DataFrame(content, closeStream));
        ctx.flush();
    }

    private String buildSSEFormat(
            HttpResponseStatus nettyStatus, boolean closeStream, boolean sendHeaders, String group) {
        // Determine event type (embedded in JSON, not in SSE event field)
        String eventType;
        if (sendHeaders) {
            // Send keepalive event when stream is first established to confirm connection
            eventType = "keepalive";
        } else if (closeStream && nettyStatus == HttpResponseStatus.NOT_MODIFIED) {
            // Timeout event, stream needs to be closed
            eventType = "timeout";
        } else {
            // Normal cluster update event
            eventType = "cluster-update";
        }

        String json = String.format(
                "{\"type\":\"%s\",\"group\":\"%s\",\"term\":%d,\"timestamp\":%d}",
                eventType, group, GROUP_UPDATE_TERM.getOrDefault(group, 0L), System.currentTimeMillis());
        logger.debug("Sending watch event: {}", json);

        // SSE format: only send data: field, event type is embedded in JSON
        return "data: " + json + "\n\n";
    }

    public void registryWatcher(Watcher<HttpContext> watcher) {
        String group = watcher.getGroup();
        Long term = GROUP_UPDATE_TERM.get(group);
        HttpContext context = watcher.getAsyncContext();
        boolean isHttp2 = context.isHttp2();
        if (term == null || watcher.getTerm() >= term) {
            // For HTTP/2, must send response headers immediately, cannot delay
            if (isHttp2 && !HTTP2_HEADERS_SENT.getOrDefault(watcher, false)) {
                sendWatcherResponse(watcher, HttpResponseStatus.OK, false, true);
                HTTP2_HEADERS_SENT.put(watcher, true);
            }
            WATCHERS.computeIfAbsent(group, value -> new ConcurrentLinkedQueue<>())
                    .add(watcher);
        } else {
            notifyWatcher(watcher);
        }
    }
}
