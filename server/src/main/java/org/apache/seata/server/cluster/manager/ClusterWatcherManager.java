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

    private static final Map<String, Long> GROUP_UPDATE_TIME = new ConcurrentHashMap<>();

    private static final Map<Watcher<HttpContext>, Boolean> HTTP2_HEADERS_SENT = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("long-polling", 1));

    @PostConstruct
    public void init() {
        // Responds to monitors that time out
        scheduledThreadPoolExecutor.scheduleAtFixedRate(
                () -> {
                    for (String group : WATCHERS.keySet()) {
                        Optional.ofNullable(WATCHERS.remove(group))
                                .ifPresent(watchers -> watchers.parallelStream().forEach(watcher -> {
                                    if (System.currentTimeMillis() >= watcher.getTimeout()) {
                                        watcher.setDone(true);
                                        // 如果超时则一定关闭流，无论是http1还是http2。对于后者 即使之前已经发送过 200 的 必须重新发送携带304的头
                                        sendWatcherResponse(watcher, HttpResponseStatus.NOT_MODIFIED, true, true);
                                        HTTP2_HEADERS_SENT.remove(watcher);
                                    } else if (!watcher.isDone()) {
                                        // Re-register if not done and not timeout
                                        registryWatcher(watcher);
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
            GROUP_UPDATE_TIME.put(event.getGroup(), event.getTerm());
            // Notifications are made of changes in cluster information
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

        // For HTTP/2, re-register the watcher to continue listening for future updates
        if (isHttp2 && !watcher.isDone()) {
            registryWatcher(watcher);
        }
    }

    /**
     * Send watcher response to the client.
     *
     * @param watcher the watcher instance
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
            // channel不可用的时候确保能清理掉watcher 防止内存泄漏
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
        } else {
            try {
                // 第一次发送携带头，后续只需要携带data
                if (sendHeaders) {
                    Http2Headers headers = new DefaultHttp2Headers().status(nettyStatus.codeAsText());
                    headers.set(HttpHeaderNames.CONTENT_LENGTH, "0");
                    ctx.write(new DefaultHttp2HeadersFrame(headers));
                }

                ctx.write(new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER, closeStream));
                ctx.flush();

                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Sent HTTP/2 response with status: {} to watcher on group {}, stream closed: {}, headers sent: {}",
                            nettyStatus.code(),
                            watcher.getGroup(),
                            closeStream,
                            sendHeaders);
                }
            } catch (Exception e) {
                logger.error(
                        "Failed to send HTTP/2 response for watcher on group {}: {}",
                        watcher.getGroup(),
                        e.getMessage(),
                        e);
            }
        }
    }

    public void registryWatcher(Watcher<HttpContext> watcher) {
        String group = watcher.getGroup();
        Long term = GROUP_UPDATE_TIME.get(group);
        if (term == null || watcher.getTerm() >= term) {
            WATCHERS.computeIfAbsent(group, value -> new ConcurrentLinkedQueue<>())
                    .add(watcher);
        } else {
            notifyWatcher(watcher);
        }
    }
}
