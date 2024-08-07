package org.kie.trustyai.service.endpoints.explainers;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.utils.TLSCredentials;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.service.mocks.MockInferenceServiceImpl;
import org.kie.trustyai.service.utils.ResourceReader;

import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ServerBuilder;
import io.grpc.TlsServerCredentials;
import io.grpc.inprocess.InProcessServerBuilder;

public class GrpcMockServer {

    private static final Logger LOG = Logger.getLogger(GrpcMockServer.class);

    private io.grpc.Server server;
    private final String serverName = InProcessServerBuilder.generateName();
    private ManagedChannel channel;
    private final int port = 50051;

    private final PredictionProvider predictionProvider;
    private final Optional<TLSCredentials> credentials;

    public GrpcMockServer(PredictionProvider predictionProvider) {
        this.predictionProvider = predictionProvider;
        this.credentials = Optional.empty();
    }

    /**
     * Credentials to use in unit testing
     * 
     * @return TLS credentials
     * @throws IOException
     */
    public static TLSCredentials getCredentials() throws IOException {
        final File clientCertFile = ResourceReader.resourceAsFile("credentials/server.crt");
        final File clientKeyFile = ResourceReader.resourceAsFile("credentials/server.key");
        final File clientRootFile = ResourceReader.resourceAsFile("credentials/server.crt");
        return new TLSCredentials(clientCertFile, clientKeyFile, Optional.of(clientRootFile));

    }

    public GrpcMockServer(PredictionProvider predictionProvider,
            Optional<TLSCredentials> credentials) {
        this.predictionProvider = predictionProvider;
        this.credentials = credentials;
    }

    public void start() throws IOException {

        if (this.credentials.isPresent()) {
            final TlsServerCredentials.Builder tlsBuilder = TlsServerCredentials.newBuilder()
                    .keyManager(this.credentials.get().getCertificate(), this.credentials.get().getKey());
            server = Grpc.newServerBuilderForPort(port, tlsBuilder.build())
                    .addService(new MockInferenceServiceImpl(this.predictionProvider))
                    .build()
                    .start();
        } else {
            server = ServerBuilder.forPort(port)
                    .addService(new MockInferenceServiceImpl(this.predictionProvider))
                    .build()
                    .start();
        }

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
