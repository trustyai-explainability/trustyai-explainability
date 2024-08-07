package org.kie.trustyai.service.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "kubernetes", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface KubernetesConfig {

    String namespace();

}
