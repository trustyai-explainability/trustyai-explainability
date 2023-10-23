package org.kie.trustyai.service.endpoints.metrics.fairness.group.legacy;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.service.endpoints.metrics.fairness.group.DisparateImpactRatioEndpoint;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Tag(name = "Disparate Impact Ratio Endpoint (Legacy)", description = "Disparate Impact Ratio (DIR) measures imbalances in " +
        "classifications by calculating the ratio between the proportion of the majority and protected classes getting" +
        " a particular outcome. This endpoint will be moving to /metrics/group/fairness/dir in a later release.")
@Path("/metrics/dir")
@Deprecated(forRemoval = true)
public class DisparateImpactRatioEndpointLegacy extends DisparateImpactRatioEndpoint {
    public DisparateImpactRatioEndpointLegacy() {
        super();
    }
}
