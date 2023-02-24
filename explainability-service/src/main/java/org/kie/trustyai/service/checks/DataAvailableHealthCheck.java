package org.kie.trustyai.service.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.kie.trustyai.service.data.storage.Storage;

@Readiness
@ApplicationScoped
public class DataAvailableHealthCheck implements HealthCheck {

    @Inject
    Instance<Storage> storage;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Data").up();
        if (storage.get().inputExists() && storage.get().outputExists()) {
            responseBuilder.withData("available", "data available");
        } else {
            responseBuilder.withData("available", "data not yet available");
        }
        return responseBuilder.build();

    }
}
