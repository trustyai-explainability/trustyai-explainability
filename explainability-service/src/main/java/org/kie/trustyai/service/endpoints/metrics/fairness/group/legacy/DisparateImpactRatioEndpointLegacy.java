package org.kie.trustyai.service.endpoints.metrics.fairness.group.legacy;

import io.quarkus.cache.CacheResult;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.metrics.fairness.FairnessDefinitions;
import org.kie.trustyai.metrics.fairness.group.DisparateImpactRatio;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.endpoints.metrics.fairness.group.DisparateImpactRatioEndpoint;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;
import java.util.List;

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
