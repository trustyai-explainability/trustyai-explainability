package org.kie.trustyai.service.endpoints.metrics.identity;

import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.endpoints.metrics.BaseEndpoint;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.RequestReconciler;
import org.kie.trustyai.service.payloads.metrics.identity.IdentityMetricRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.identity.ValidIdentityMetricRequest;

import io.quarkus.cache.CacheResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Tag(name = "Identity Endpoint", description = "The identity endpoint simply returns a mean of a model's last N feature or output values. This is useful" +
        "for tracking the model's feature and output trends, or if the model contains diagnostic metrics as part of its inference output.")
@Path("/metrics/identity")
public class IdentityEndpoint extends BaseEndpoint<IdentityMetricRequest> {
    protected static final Logger LOG = Logger.getLogger(IdentityEndpoint.class);

    public IdentityEndpoint() {
        super("IDENTITY");
    }

    public MetricThreshold thresholdFunction(Number lowerBound, Number upperBound, Number metricValue) {
        return new MetricThreshold(lowerBound.doubleValue(), upperBound.doubleValue(), metricValue.doubleValue());
    }

    @Override
    @CacheResult(cacheName = "metrics-calculator-identity", keyGenerator = MetricCalculationCacheKeyGen.class)
    public MetricValueCarrier calculate(Dataframe dataframe, BaseMetricRequest request) {
        List<Value> vs = dataframe.getColumn(dataframe.getColumnNames().indexOf(((IdentityMetricRequest) request).getColumnName()));
        double value = vs.stream().mapToDouble(Value::asNumber).sum() / ((double) vs.size());
        return new MetricValueCarrier(value);
    }

    public String getGeneralDefinition() {
        return "This metric simply returns a mean of a model's last N feature or output values.";
    }

    public String getSpecificDefinitionFunction(IdentityMetricRequest request) {
        return String.format("This metric simply returns a mean of the last N values of %s from inference data of model=%s.",
                request.getColumnName(),
                request.getModelId());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response response(@ValidIdentityMetricRequest IdentityMetricRequest request) throws DataframeCreateException {

        final Dataframe dataframe;
        final StorageMetadata storageMetadata;
        try {
            if (Objects.isNull(request.getBatchSize())) {
                final int defaultBatchSize = serviceConfig.batchSize().getAsInt();
                LOG.warn("Request batch size is empty. Using the default value of " + defaultBatchSize);
                request.setBatchSize(defaultBatchSize);
            }
            dataframe = super.dataSource.get().getDataframe(request.getModelId(), request.getBatchSize()).filterRowsBySynthetic(false);
            storageMetadata = dataSource.get().getMetadata(request.getModelId());
        } catch (DataframeCreateException e) {
            LOG.error("No data available for model " + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("No data available").build();
        }

        RequestReconciler.reconcile(request, storageMetadata);

        final MetricValueCarrier metricValue;
        try {
            metricValue = this.calculate(dataframe, request);
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric for model " + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error calculating metric: " + e.getMessage()).build();
        }
        final String metricDefinition = this.getSpecificDefinitionFunction(request);

        MetricThreshold thresholds = thresholdFunction(request.getLowerThreshold(), request.getUpperThreshold(), metricValue.getValue());
        final BaseMetricResponse dirObj = new BaseMetricResponse(metricValue, metricDefinition, thresholds, super.getMetricName());
        return Response.ok(dirObj).build();
    }

    @GET
    @Path("/definition")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDefinition() {
        return Response.ok(getGeneralDefinition()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/definition")
    public Response getSpecificDefinition(IdentityMetricRequest request) {
        try {
            RequestReconciler.reconcile(request, dataSource);
        } catch (DataframeCreateException e) {
            LOG.error("No data available: " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        return Response.ok(this.getSpecificDefinitionFunction(request)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public Response createRequest(@ValidIdentityMetricRequest IdentityMetricRequest request) {
        return super.createRequestGeneric(request);
    }
}
