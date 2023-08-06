package org.kie.trustyai.service.endpoints.explainers.timeseries;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.local.TimeSeriesExplainer;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.external.explainers.local.TSLimeExplainer;
import org.kie.trustyai.external.explainers.local.TSLimeExplanation;
import org.kie.trustyai.external.utils.PythonRuntimeManager;
import org.kie.trustyai.service.data.utils.TimeseriesUtils;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.requests.explainers.ModelConfig;
import org.kie.trustyai.service.payloads.requests.explainers.tslime.TSLimeParameters;
import org.kie.trustyai.service.payloads.requests.explainers.tslime.TSLimeRequest;

import jep.SubInterpreter;

@Tag(name = "TSLime Explainer Endpoint")
@Path("/explainers/timeseries/tslime")
public class TSLimeEndpoint extends ExplainerEndpoint {

    private static final Logger LOG = Logger.getLogger(TSLimeEndpoint.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response explain(TSLimeRequest request) {

        final TSLimeParameters parameters = request.getParameters();
        final ModelConfig modelConfig = request.getModelConfig();

        final int nPerturbations = parameters.getnPerturbations();

        // Convert the request data to Prediction Inputs
        final Map<String, List<Double>> data = request.getData();

        final List<PredictionInput> inputs = TimeseriesUtils.transformData(data, request.getTimestamps(), request.getTimestampName());
        final Dataframe dataframe = Dataframe.createFromInputs(inputs);

        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {

            int inputLength = parameters.getInputLength();
            if (inputLength == 0) {
                inputLength = dataframe.getRowDimension();
            }
            LOG.debug("Using " + inputLength + " observations (from inputLength");

            final TimeSeriesExplainer<TSLimeExplanation> tslime = new TSLimeExplainer.Builder()
                    .withInputLength(inputLength)
                    .withNPerturbations(nPerturbations)
                    .withTimestampColumn(request.getTimestampName())
                    .build(sub, modelConfig.getTarget(), modelConfig.getName(), modelConfig.getVersion());

            // Request the explanation
            final TSLimeExplanation explanation = tslime.explainAsync(dataframe.tail(inputLength).asPredictions(), null).get();
            return Response.ok().entity(explanation).build();
        } catch (ExecutionException e) {
            LOG.error("Error while explaining", e);
            return Response.serverError().entity(e).build();
        } catch (InterruptedException e) {
            LOG.error("Error while explaining", e);
            return Response.serverError().entity(e).build();
        }

    }
}
