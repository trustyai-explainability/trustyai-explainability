package org.kie.trustyai.service.producers;

import java.security.SecureRandom;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class SecureRandomProducer {

    @Produces
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
