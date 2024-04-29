package org.kie.trustyai.connectors.kserve.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

public class JsonRequestHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger logger = LoggerFactory.getLogger(JsonRequestHandler.class);
    private final String endpoint;
    private final PredictionProvider predictionProvider;

    public JsonRequestHandler(String endpoint, PredictionProvider predictionProvider) {
        this.endpoint = endpoint;
        this.predictionProvider = predictionProvider;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws IOException, ExecutionException, InterruptedException {
        if (msg instanceof FullHttpRequest) {
            final FullHttpRequest request = (FullHttpRequest) msg;
            logger.info("Received request: {}", request);

            if (request.uri().equals(endpoint) && request.method().name().equals("POST")) {
                final String payload = request.content().toString(CharsetUtil.UTF_8);
                logger.info("Received payload: {}", payload);

                final List<PredictionInput> predictionInputList = KServeV1HTTPPayloadParser.getInstance().parseRequest(payload);
                final List<PredictionOutput> predictionOutputs = predictionProvider.predictAsync(predictionInputList).get();

                List<Object> values = new ArrayList<>();
                for (PredictionOutput predictionOutput : predictionOutputs) {
                    final List<Output> outputs = predictionOutput.getOutputs();
                    for (Output output : outputs) {
                        values.add(output.getValue().asNumber());
                    }
                }

                final String response = new ObjectMapper().writeValueAsString(Map.of("predictions", values));

                final FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(response, CharsetUtil.UTF_8));

                HttpUtil.setContentLength(httpResponse, httpResponse.content().readableBytes());
                httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            } else {
                FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
                ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception caught:", cause);
        ctx.close();
    }
}