package org.kie.trustyai.service.mocks;

import org.kie.trustyai.service.prometheus.PrometheusScheduler;

import io.quarkus.test.Mock;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Alternative
@ApplicationScoped
public class MockPrometheusScheduler extends PrometheusScheduler {

    public void empty() {
        this.getAllRequests().clear();
    }

}
