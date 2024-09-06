package org.kie.trustyai.service.mocks;

import org.kie.trustyai.service.prometheus.PrometheusScheduler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Alternative
@ApplicationScoped
public class MockPrometheusScheduler extends PrometheusScheduler {

    public void empty() {
        this.getAllRequests().clear();
    }

}
