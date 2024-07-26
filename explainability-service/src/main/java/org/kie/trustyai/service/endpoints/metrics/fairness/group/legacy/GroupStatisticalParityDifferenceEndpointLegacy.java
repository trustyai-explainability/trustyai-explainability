package org.kie.trustyai.service.endpoints.metrics.fairness.group.legacy;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.service.endpoints.metrics.fairness.group.GroupStatisticalParityDifferenceEndpoint;

import io.quarkus.resteasy.reactive.server.EndpointDisabled;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Tag(name = "{Legacy}: Statistical Parity Difference", description = "This endpoint has moved to /metrics/group/fairness/spd and will be removed in a later release.")
@Path("/metrics/spd")
@EndpointDisabled(name = "endpoints.fairness", stringValue = "disable")
@Deprecated(forRemoval = true)
public class GroupStatisticalParityDifferenceEndpointLegacy extends GroupStatisticalParityDifferenceEndpoint {
    public GroupStatisticalParityDifferenceEndpointLegacy() {
        super();
    }
}
