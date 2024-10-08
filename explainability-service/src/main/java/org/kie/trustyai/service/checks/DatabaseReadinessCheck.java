package org.kie.trustyai.service.checks;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.lookup.LookupUnlessProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

@Readiness
@ApplicationScoped
@LookupUnlessProperty(name = "service.storage-format", stringValue = "PVC")
public class DatabaseReadinessCheck implements HealthCheck {

    @Inject
    EntityManager entityManager;

    @Override
    public HealthCheckResponse call() {
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return HealthCheckResponse.up("Database connection");
        } catch (PersistenceException e) {
            return HealthCheckResponse.down("Database connection");
        }
    }
}
