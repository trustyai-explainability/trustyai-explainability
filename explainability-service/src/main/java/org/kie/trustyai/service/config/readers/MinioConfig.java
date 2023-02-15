package org.kie.trustyai.service.config.readers;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "minio", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface MinioConfig {

    Optional<String> bucketName();

    Optional<String> endpoint();

    Optional<String> inputFilename();

    Optional<String> outputFilename();

    Optional<String> secretKey();

    Optional<String> accessKey();
}
