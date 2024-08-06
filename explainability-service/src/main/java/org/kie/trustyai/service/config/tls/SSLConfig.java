package org.kie.trustyai.service.config.tls;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;


@ApplicationScoped
public class SSLConfig {

    @ConfigProperty(name = "quarkus.http.ssl.certificate.files")
    List<String> certificateFile;

    @ConfigProperty(name = "quarkus.http.ssl.certificate.key-files")
    List<String> keyFile;

    public List<String> getCertificateFile() {
        return certificateFile;
    }

    public List<String> getKeyFile() {
        return keyFile;
    }
}

