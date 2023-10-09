package org.kie.trustyai.service.endpoints.metrics.fairness.group.legacy;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.service.endpoints.metrics.fairness.group.GroupStatisticalParityDifferenceEndpoint;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Tag(name = "Statistical Parity Difference Endpoint (Legacy)", description = "Statistical Parity Difference (SPD) measures imbalances in classifications by calculating the " +
        "difference between the proportion of the majority and protected classes getting a particular outcome. This endpoint will be moving to /metrics/group/fairness/spd in a later release.")
@Path("/metrics/spd")
@Deprecated(forRemoval = true)
public class GroupStatisticalParityDifferenceEndpointLegacy extends GroupStatisticalParityDifferenceEndpoint {
    public GroupStatisticalParityDifferenceEndpointLegacy() {
        super();
    }
}
