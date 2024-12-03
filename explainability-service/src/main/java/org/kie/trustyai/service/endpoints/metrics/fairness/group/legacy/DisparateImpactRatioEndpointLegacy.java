package org.kie.trustyai.service.endpoints.metrics.fairness.group.legacy;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.service.endpoints.metrics.fairness.group.DisparateImpactRatioEndpoint;

import io.quarkus.resteasy.reactive.server.EndpointDisabled;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Tag(name = "{Legacy}: Disparate Impact Ratio ", description = "This endpoint has moved to /metrics/group/fairness/dir and will be removed in a later release.")
@Path("/metrics/dir")
@EndpointDisabled(name = "endpoints.fairness", stringValue = "disable")
@Deprecated(forRemoval = true)
public class DisparateImpactRatioEndpointLegacy extends DisparateImpactRatioEndpoint {
    public DisparateImpactRatioEndpointLegacy() {
        super();
    }
}
