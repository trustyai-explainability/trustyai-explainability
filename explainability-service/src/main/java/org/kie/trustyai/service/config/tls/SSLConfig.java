package org.kie.trustyai.service.config.tls;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SSLConfig {

    @ConfigProperty(name = "quarkus.http.ssl.certificate.files")
    Optional<List<Path>> certificateFile;

    @ConfigProperty(name = "quarkus.http.ssl.certificate.key-files")
    Optional<List<Path>> keyFile;

    public Optional<List<Path>> getCertificateFile() {
        return certificateFile;
    }

    public Optional<List<Path>> getKeyFile() {
        return keyFile;
    }
}
