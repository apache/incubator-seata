package org.apache.seata.namingserver.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.NettyRuntime;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.protocol.detector.HttpDetector;
import org.apache.seata.core.protocol.detector.ProtocolDetector;
import org.apache.seata.core.rpc.netty.ProtocolDetectHandler;
import org.apache.seata.namingserver.manager.ClusterWatcherManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NettyNamingServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyNamingServer.class);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private final ServerBootstrap serverBootstrap = new ServerBootstrap();

    private EventLoopGroup eventLoopGroupWorker;

    private EventLoopGroup eventLoopGroupBoss;

    @Value("${server.port}")
    private Integer port;

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    @PostConstruct
    public void init() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        this.eventLoopGroupBoss = new NioEventLoopGroup(1,
                new NamedThreadFactory("NettyBoss", 1));
        this.eventLoopGroupWorker = new NioEventLoopGroup(NettyRuntime.availableProcessors() * 2,
                new NamedThreadFactory("NettyServerNIOWorker",
                        NettyRuntime.availableProcessors() * 2));
        this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupWorker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, 153600)
                .childOption(ChannelOption.SO_RCVBUF, 153600)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(1048576,
                                67108864))
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ProtocolDetectHandler(new ProtocolDetector[]{new HttpDetector(), new NamingServerHttp2Detector(clusterWatcherManager)}));
                    }
                });

        try {
            this.serverBootstrap.bind(port).sync();
            initialized.set(true);
        } catch (Exception exx) {
            throw new RuntimeException("Server start failed", exx);
        }
    }

    @PreDestroy
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        this.eventLoopGroupBoss.shutdownGracefully();
        this.eventLoopGroupWorker.shutdownGracefully();;
    }
}
