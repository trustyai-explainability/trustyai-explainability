package org.kie.trustyai.service.endpoints.data;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v2.TensorConverter;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.DatapointSource;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.data.parsers.CSVParser;
import org.kie.trustyai.service.data.utils.DownloadUtils;
import org.kie.trustyai.service.data.utils.UploadUtils;
import org.kie.trustyai.service.payloads.data.download.DataRequestPayload;
import org.kie.trustyai.service.payloads.data.download.DataResponsePayload;
import org.kie.trustyai.service.payloads.data.download.MatchOperation;
import org.kie.trustyai.service.payloads.data.download.RowMatcher;
import org.kie.trustyai.service.payloads.data.upload.ModelInferJointPayload;
import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.validators.data.ValidDataDownloadRequest;

@ApplicationScoped
@Path("/data")
public class DataEndpoint {

    private static final Logger LOG = Logger.getLogger(DataEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    CSVParser csvParser = new CSVParser();

    public final static String TRUSTY_PREFIX = "trustyai.";

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/download")
    public Response download(@ValidDataDownloadRequest DataRequestPayload dataRequestPayload) {
        String modelId = dataRequestPayload.getModelId();
        Dataframe df = dataSource.get().getDataframe(modelId).copy();
        Metadata metadata = dataSource.get().getMetadata(modelId);

        for (RowMatcher rowMatcher : dataRequestPayload.getMatchAll()) {
            if (rowMatcher.getColumnName().startsWith(DataEndpoint.TRUSTY_PREFIX)) {
                Dataframe.InternalColumn internalColumn = Dataframe.InternalColumn.valueOf(rowMatcher.getColumnName().replace(TRUSTY_PREFIX, ""));
                if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.BETWEEN) {
                    df = DownloadUtils.betweenMatcherInternal(df, rowMatcher, internalColumn, false);
                } else if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.EQUALS) {
                    df = DownloadUtils.equalsMatcherInternal(df, rowMatcher, internalColumn, false);
                }
            } else {
                int columnIndex = df.getColumnNames().indexOf(rowMatcher.getColumnName());
                DataType columnType = DownloadUtils.getDataType(metadata, rowMatcher);

                // row match
                if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.BETWEEN) {
                    df = DownloadUtils.betweenMatcher(df, rowMatcher, columnIndex, columnType, false);
                } else if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.EQUALS) {
                    df = DownloadUtils.equalsMatcher(df, rowMatcher, columnIndex, columnType, false);
                }
            }
        }

        for (RowMatcher rowMatcher : dataRequestPayload.getMatchNone()) {
            if (rowMatcher.getColumnName().startsWith(DataEndpoint.TRUSTY_PREFIX)) {
                Dataframe.InternalColumn internalColumn = Dataframe.InternalColumn.valueOf(rowMatcher.getColumnName().replace(TRUSTY_PREFIX, ""));
                if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.BETWEEN) {
                    df = DownloadUtils.betweenMatcherInternal(df, rowMatcher, internalColumn, true);
                } else if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.EQUALS) {
                    df = DownloadUtils.equalsMatcherInternal(df, rowMatcher, internalColumn, true);
                }
            } else {
                int columnIndex = df.getColumnNames().indexOf(rowMatcher.getColumnName());
                DataType columnType = DownloadUtils.getDataType(metadata, rowMatcher);

                // row match
                if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.BETWEEN) {
                    df = DownloadUtils.betweenMatcher(df, rowMatcher, columnIndex, columnType, true);
                } else if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.EQUALS) {
                    df = DownloadUtils.equalsMatcher(df, rowMatcher, columnIndex, columnType, true);
                }
            }
        }

        Dataframe returnDF;
        if (!dataRequestPayload.getMatchAny().isEmpty()) {
            returnDF = df.filterByColumnValue(0, v -> false); //get null df
            for (RowMatcher rowMatcher : dataRequestPayload.getMatchAny()) {
                if (rowMatcher.getColumnName().startsWith(DataEndpoint.TRUSTY_PREFIX)) {
                    Dataframe.InternalColumn internalColumn = Dataframe.InternalColumn.valueOf(rowMatcher.getColumnName().replace(TRUSTY_PREFIX, ""));
                    if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.BETWEEN) {
                        returnDF.addPredictions(DownloadUtils.betweenMatcherInternal(df, rowMatcher, internalColumn, false).asPredictions());
                    } else if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.EQUALS) {
                        returnDF.addPredictions(DownloadUtils.equalsMatcherInternal(df, rowMatcher, internalColumn, false).asPredictions());
                    }
                } else {
                    int columnIndex = df.getColumnNames().indexOf(rowMatcher.getColumnName());
                    DataType columnType = DownloadUtils.getDataType(metadata, rowMatcher);

                    // row match
                    if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.BETWEEN) {
                        returnDF.addPredictions(DownloadUtils.betweenMatcher(df, rowMatcher, columnIndex, columnType, false).asPredictions());
                    } else if (MatchOperation.valueOf(rowMatcher.getOperation()) == MatchOperation.EQUALS) {
                        returnDF.addPredictions(DownloadUtils.equalsMatcher(df, rowMatcher, columnIndex, columnType, false).asPredictions());
                    }
                }
            }
        } else {
            returnDF = df;
        }

        DataResponsePayload dataResponsePayload = new DataResponsePayload();
        dataResponsePayload.setDataCSV(csvParser.convertToString(returnDF, true));

        return Response.ok().entity(dataResponsePayload).build();

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/upload")
    public Response upload(ModelInferJointPayload jointPayload) throws DataframeCreateException {
        if (jointPayload.getRequest() == null || jointPayload.getRequest().getInputs().length < 1) {
            return Response.serverError().entity("Directly uploaded datapoints must specify at least one `inputs` field.").status(Response.Status.BAD_REQUEST).build();
        }

        // parse inputs
        ModelInferRequest.Builder inferRequestBuilder = ModelInferRequest.newBuilder();
        inferRequestBuilder.setModelName(jointPayload.getModelName());
        try {
            UploadUtils.populateRequestBuilder(inferRequestBuilder, jointPayload.getRequest());
        } catch (IllegalArgumentException e) {
            return Response.serverError().entity(e.getMessage()).status(Response.Status.BAD_REQUEST).build();
        }

        List<PredictionInput> predictionInputs;
        try {
            predictionInputs = TensorConverter.parseKserveModelInferRequest(inferRequestBuilder.build());
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing input payload: " + e.getMessage());
        }

        List<PredictionOutput> predictionOutputs;
        if (jointPayload.getResponse() != null && jointPayload.getResponse().getOutputs().length > 0) {
            // parse outputs
            ModelInferResponse.Builder inferResponseBuilder = ModelInferResponse.newBuilder();
            inferResponseBuilder.setModelName(jointPayload.getModelName());
            try {
                UploadUtils.populateResponseBuilder(inferResponseBuilder, jointPayload.getResponse());
            } catch (IllegalArgumentException e) {
                return Response.serverError().entity(e.getMessage()).status(Response.Status.BAD_REQUEST).build();
            }

            try {
                predictionOutputs = TensorConverter.parseKserveModelInferResponse(inferResponseBuilder.build(), predictionInputs.size());
            } catch (IllegalArgumentException e) {
                throw new DataframeCreateException("Error parsing output payload: " + e.getMessage());
            }
        } else {
            //todo, grab automatically from model
            throw new IllegalArgumentException("No output payload specified in request");
        }

        final List<Prediction> predictions =
                IntStream.range(0, predictionInputs.size()).mapToObj(i -> new SimplePrediction(predictionInputs.get(i), predictionOutputs.get(i))).collect(Collectors.toList());
        final Dataframe dataframe = Dataframe.createFrom(predictions);

        // tag the points accordingly
        DatapointSource dpSource;
        if (jointPayload.getDataTag() == null) {
            dpSource = DatapointSource.UNLABELED;
        } else {
            try {
                dpSource = DatapointSource.valueOf(jointPayload.getDataTag());
            } catch (IllegalArgumentException e) {
                return Response.serverError()
                        .entity("Provided datapoint tag=" + jointPayload.getDataTag() + " is not valid. Must be one of " + Arrays.toString(DatapointSource.values()))
                        .status(Response.Status.BAD_REQUEST)
                        .build();
            }
        }

        EnumMap<DatapointSource, List<List<Integer>>> taggingMap = new EnumMap<>(DatapointSource.class);
        taggingMap.put(dpSource, List.of(List.of(0, dataframe.getRowDimension())));
        dataframe.tagDataPoints(taggingMap);

        dataSource.get().saveDataframe(dataframe, jointPayload.getModelName());
        return Response.ok().entity(predictions.size() + " datapoints successfully added to " + jointPayload.getModelName() + " data.").build();
    }
}
