package org.kie.trustyai.service.config.readers;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "minio", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface MinioConfig {

    String bucketName();

    String endpoint();

    String inputFilename();

    String outputFilename();

    String secretKey();

    String accessKey();
}
