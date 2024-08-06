package org.kie.trustyai.connectors.utils;

import java.io.File;
import java.util.Optional;

/**
 * Class to hold TLS credentials
 */
public class TLSCredentials {

    private final File certificate;
    private final File key;
    private final Optional<File> root;

    public TLSCredentials(File certificate, File key) {
        this(certificate, key, Optional.empty());
    }

    public TLSCredentials(File certificate, File key, Optional<File> root) {
        this.certificate = certificate;
        this.key = key;
        this.root = root;
    }

    public File getCertificate() {
        return certificate;
    }

    public File getKey() {
        return key;
    }

    public Optional<File> getRoot() {
        return root;
    }
}
