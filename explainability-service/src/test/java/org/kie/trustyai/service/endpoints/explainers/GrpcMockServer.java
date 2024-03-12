package org.kie.trustyai.service.endpoints.explainers;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.service.mocks.MockInferenceServiceImpl;

import io.grpc.ManagedChannel;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

public class GrpcMockServer {

    private static final Logger LOG = Logger.getLogger(GrpcMockServer.class);

    private io.grpc.Server server;
    private final String serverName = InProcessServerBuilder.generateName();
    private ManagedChannel channel;
    private final int port = 50051;

    private final PredictionProvider predictionProvider;

    public GrpcMockServer(PredictionProvider predictionProvider) {
        this.predictionProvider = predictionProvider;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new MockInferenceServiceImpl(this.predictionProvider))
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.debug("Shutting down gRPC server since work is done");
            GrpcMockServer.this.stop();
            LOG.debug("gRPC server shut down");
        }));
    }

    public void stop() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public String getServerName() {
        return serverName;
    }

    public int getPort() {
        return port;
    }
}
