package org.kie.trustyai.service.mocks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.kie.trustyai.service.prometheus.PrometheusScheduler;

import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
public class MockPrometheusScheduler extends PrometheusScheduler {
}
