package org.kie.trustyai.service.mocks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.kie.trustyai.service.prometheus.PrometheusScheduler;

import io.quarkus.arc.Priority;
import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
@Priority(1)
public class MockPrometheusScheduler extends PrometheusScheduler {

    public void empty() {
        this.getAllRequests().clear();
    }

}
