package org.kie.trustyai.connectors.kserve.v1;

import java.util.concurrent.atomic.AtomicBoolean;

import org.kie.trustyai.explainability.model.PredictionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class KServeV1HTTPMockServer {
    private static final Logger logger = LoggerFactory.getLogger(KServeV1HTTPMockServer.class);

    private final int port;
    private final String endpoint;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private PredictionProvider predictionProvider;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public KServeV1HTTPMockServer(int port, String endpoint, PredictionProvider predictionProvider) {
        this.port = port;
        this.endpoint = endpoint;
        this.predictionProvider = predictionProvider;
    }

    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new JsonRequestHandler(endpoint, predictionProvider));
                        }
                    });

            channelFuture = bootstrap.bind(port).sync();
            logger.info("Starting mock server started on port " + port);
            running.set(true);
        } catch (Exception e) {
            throw new Exception("Failed to start server", e);
        }
    }

    public void stop() throws InterruptedException {
        if (running.compareAndSet(true, false)) {
            try {
                channelFuture.channel().close().sync();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
    }

    public void setPredictionProvider(PredictionProvider predictionProvider) {
        this.predictionProvider = predictionProvider;
    }

}