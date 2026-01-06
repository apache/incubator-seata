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
import org.apache.seata.common.Constants;
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
import java.util.ArrayList;
import java.util.List;
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

    // Separate watchers for HTTP/1.1 (one-time requests) and HTTP/2 (long-lived connections)
    private static final Map<String, Queue<Watcher<HttpContext>>> HTTP1_WATCHERS = new ConcurrentHashMap<>();
    private static final Map<String, Queue<Watcher<HttpContext>>> HTTP2_WATCHERS = new ConcurrentHashMap<>();

    private static final Map<String, Long> GROUP_UPDATE_TERM = new ConcurrentHashMap<>();

    private static final Map<Watcher<HttpContext>, Boolean> HTTP2_HEADERS_SENT = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("long-polling", 1));

    @PostConstruct
    public void init() {
        // Periodically check and respond to watchers
        scheduledThreadPoolExecutor.scheduleAtFixedRate(
                () -> {
                    // Check HTTP/1.1 watchers for timeout
                    for (String group : HTTP1_WATCHERS.keySet()) {
                        Optional.ofNullable(HTTP1_WATCHERS.remove(group))
                                .ifPresent(watchers -> watchers.parallelStream().forEach(watcher -> {
                                    if (System.currentTimeMillis() >= watcher.getTimeout()) {
                                        watcher.setDone(true);
                                        sendWatcherResponse(watcher, HttpResponseStatus.NOT_MODIFIED, true, false);
                                    } else if (!watcher.isDone()) {
                                        // Re-register if not timeout
                                        registryWatcher(watcher);
                                    }
                                }));
                    }
                    
                    // Check HTTP/2 watchers for connection validity (don't remove, just check)
                    for (Map.Entry<String, Queue<Watcher<HttpContext>>> entry : HTTP2_WATCHERS.entrySet()) {
                        String group = entry.getKey();
                        Queue<Watcher<HttpContext>> watchers = entry.getValue();
                        if (watchers == null || watchers.isEmpty()) {
                            continue;
                        }
                        
                        // Create snapshot to avoid concurrent modification
                        List<Watcher<HttpContext>> watchersToCheck = new ArrayList<>(watchers);
                        
                        watchersToCheck.forEach(watcher -> {
                            HttpContext context = watcher.getAsyncContext();
                            if (!context.getContext().channel().isActive()) {
                                // Remove invalid watcher
                                watchers.remove(watcher);
                                watcher.setDone(true);
                                HTTP2_HEADERS_SENT.remove(watcher);
                                logger.debug("Removed inactive HTTP/2 watcher for group: {}", group);
                            }
                        });
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
            logger.info("收到集群变更事件的通知");
            GROUP_UPDATE_TERM.put(event.getGroup(), event.getTerm());
            String group = event.getGroup();
            
            // Handle HTTP/1.1 watchers: remove and notify (one-time request)
            Optional.ofNullable(HTTP1_WATCHERS.remove(group))
                    .ifPresent(watchers -> watchers.parallelStream().forEach(watcher -> {
                        notifyWatcher(watcher);
                        // HTTP/1.1 watcher is done after notification
                        watcher.setDone(true);
                    }));
            
            // Handle HTTP/2 watchers: notify without removing (long-lived connection)
            Queue<Watcher<HttpContext>> http2Watchers = HTTP2_WATCHERS.get(group);
            if (http2Watchers != null && !http2Watchers.isEmpty()) {
                // Create snapshot to avoid concurrent modification during iteration
                List<Watcher<HttpContext>> watchersToNotify = new ArrayList<>(http2Watchers);
                watchersToNotify.forEach(watcher -> {
                    // Only notify active watchers
                    if (watcher.getAsyncContext().getContext().channel().isActive() && !watcher.isDone()) {
                        notifyWatcher(watcher);
                    } else {
                        // Remove inactive watcher
                        http2Watchers.remove(watcher);
                        HTTP2_HEADERS_SENT.remove(watcher);
                    }
                });
            }
        }
    }

    private void notifyWatcher(Watcher<HttpContext> watcher) {
        HttpContext context = watcher.getAsyncContext();
        boolean isHttp2 = context.isHttp2();

        if (!isHttp2) {
            watcher.setDone(true);
        }

        boolean isFirstResponse = !HTTP2_HEADERS_SENT.getOrDefault(watcher, false);
        sendWatcherResponse(watcher, HttpResponseStatus.OK, false, isFirstResponse);
        if (isFirstResponse && isHttp2) {
            HTTP2_HEADERS_SENT.put(watcher, true);
        }

        // Update watcher's term to the latest term to prevent infinite loop
        String group = watcher.getGroup();
        Long latestTerm = GROUP_UPDATE_TERM.get(group);
        if (latestTerm != null && latestTerm > watcher.getTerm()) {
            watcher.setTerm(latestTerm);
        }
        
        // Note: HTTP/2 watchers are not re-registered here because they remain in HTTP2_WATCHERS
        // HTTP/1.1 watchers are done after notification, so no re-registration needed
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
        String eventData = buildEventData(nettyStatus, closeStream, sendHeaders, group);
        ByteBuf content = Unpooled.copiedBuffer(eventData, StandardCharsets.UTF_8);

        // Send DATA frame (if closeStream is true, it will end the current stream)
        ctx.write(new DefaultHttp2DataFrame(content, closeStream));
        ctx.flush();
    }

    private String buildEventData(
            HttpResponseStatus nettyStatus, boolean closeStream, boolean sendHeaders, String group) {
        // Determine event type (embedded in JSON, not in a separate event field)
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
        return Constants.WATCH_EVENT_PREFIX + json + "\n";
    }

    public void registryWatcher(Watcher<HttpContext> watcher) {
        String group = watcher.getGroup();
        Long term = GROUP_UPDATE_TERM.get(group);
        HttpContext context = watcher.getAsyncContext();
        boolean isHttp2 = context.isHttp2();

        // For HTTP/2, must send response headers immediately, cannot delay
        if (isHttp2 && !HTTP2_HEADERS_SENT.getOrDefault(watcher, false)) {
            sendWatcherResponse(watcher, HttpResponseStatus.OK, false, true);
            HTTP2_HEADERS_SENT.put(watcher, true);
        }

        // Add to appropriate watcher collection based on protocol
        // For HTTP/2, always add to queue (long-lived connection)
        // For HTTP/1.1, only add if term hasn't been updated
        if (isHttp2) {
            HTTP2_WATCHERS.computeIfAbsent(group, value -> new ConcurrentLinkedQueue<>())
                    .add(watcher);

            // If term has been updated, notify immediately
            if (term != null && term > watcher.getTerm()) {
                notifyWatcher(watcher);
            }
        } else {
            if (term == null || watcher.getTerm() >= term) {
                HTTP1_WATCHERS.computeIfAbsent(group, value -> new ConcurrentLinkedQueue<>())
                        .add(watcher);
            } else {
                // Term has been updated, notify immediately (one-time request)
                notifyWatcher(watcher);
            }
        }
    }
}
