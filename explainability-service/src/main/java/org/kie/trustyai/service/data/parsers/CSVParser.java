package org.kie.trustyai.service.data.parsers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionMetadata;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.model.tensor.Tensor;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.utils.CSVUtils;
import org.kie.trustyai.service.payloads.service.InferenceId;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.context.ApplicationScoped;

@LookupIfProperty(name = "service.data.format", stringValue = "CSV")
@ApplicationScoped
public class CSVParser implements DataParser {

    private static final Logger LOG = Logger.getLogger(CSVParser.class);
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    public static final ZoneOffset ZONE_OFFSET = ZoneOffset.UTC;
    public static final String TENSOR_PREFIX = "tensor[";

    @Override
    public Dataframe toDataframe(ByteBuffer byteBuffer, StorageMetadata storageMetadata) throws DataframeCreateException {
        final String data = UTF8.decode(byteBuffer).toString();

        final List<Prediction> predictions;
        try {
            predictions = CSVUtils.parse(data, storageMetadata);
        } catch (IOException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        LOG.info("Creating dataframe from CSV data");
        return Dataframe.createFrom(predictions);
    }

    @Override
    public List<InferenceId> toInferenceIds(ByteBuffer byteBuffer) {
        final String data = UTF8.decode(byteBuffer).toString();
        try {
            final List<List<Value>> values = CSVUtils.parseRaw(data);
            final List<InferenceId> inferenceIds = new ArrayList<>();
            if (values != null && !values.isEmpty()) {
                for (List<Value> value : values) {
                    if (value.size() == 2) {
                        final String id = value.get(0).asString();
                        final LocalDateTime predictionTime = LocalDateTime.parse(value.get(1).asString());
                        inferenceIds.add(new InferenceId(id, predictionTime));
                    } else if (value.size() == 3) {
                        final String id = value.get(1).asString();
                        final LocalDateTime predictionTime = LocalDateTime.parse(value.get(2).asString());
                        inferenceIds.add(new InferenceId(id, predictionTime));
                    }
                }
            }
            return inferenceIds;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Dataframe toDataframe(ByteBuffer dataByteBuffer, ByteBuffer internalDataByteBuffer, StorageMetadata storageMetadata)
            throws DataframeCreateException {

        // read predictions
        final String data = UTF8.decode(dataByteBuffer).toString();
        final List<Prediction> predictions;
        try {
            predictions = CSVUtils.parse(data, storageMetadata);
        } catch (IOException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        // read metadata
        List<PredictionMetadata> predictionsMetadata = new ArrayList<>();
        final String internalData = UTF8.decode(internalDataByteBuffer).toString();
        final List<List<Value>> values;
        try {
            values = CSVUtils.parseRaw(internalData);
        } catch (IOException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        if (values != null) {
            for (int i = 0; i < predictions.size(); i++) {
                String datapointTag = values.get(i).get(0).asString();
                String id = values.get(i).get(1).asString();
                LocalDateTime predictionTime = LocalDateTime.parse(values.get(i).get(2).asString());
                PredictionMetadata predictionMetadata = new PredictionMetadata(id, predictionTime, datapointTag);
                predictionsMetadata.add(predictionMetadata);
            }
        }

        // create predictions with metadata
        List<Prediction> predictionsFinal = new ArrayList<>();
        int i = 0;
        for (Prediction prediction : predictions) {
            predictionsFinal.add(new SimplePrediction(prediction.getInput(), prediction.getOutput(),
                    prediction.getExecutionId(), predictionsMetadata.get(i)));
            i++;
        }

        LOG.info("Creating dataframe from CSV data");
        return Dataframe.createFrom(predictionsFinal);
    }

    public String convertToString(Dataframe dataframe, boolean includeHeader, boolean includeInternalData) {
        final StringBuilder output = new StringBuilder();
        if (includeHeader) {
            output.append(String.join(",", dataframe.getColumnNames().stream().map(name -> "\"" + name + "\"")
                    .collect(Collectors.toList())));
            if (includeInternalData) {
                output.append(",\"")
                        .append("_trustyai_tag")
                        .append("\",\"")
                        .append("_trustyai_id")
                        .append("\",\"")
                        .append("_trustyai_timestamp")
                        .append("\"");
            }
            output.append("\n");
        }
        AtomicInteger i = new AtomicInteger();
        dataframe.getRows().forEach(values -> {
            final String rowStr = values.stream().map(value -> {
                final Object obj = value.getUnderlyingObject();
                if (obj instanceof String) {
                    return "\"" + obj + "\"";
                } else if (obj instanceof Tensor tensor) {
                    try {
                        return tensor.serialize();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return obj.toString();
                }
            }).collect(Collectors.joining(","));
            output.append(rowStr);
            if (includeInternalData) {
                output.append(",\"")
                        .append(dataframe.getTags().get(i.get()))
                        .append("\",\"")
                        .append(dataframe.getIds().get(i.get()))
                        .append("\",\"")
                        .append(dataframe.getTimestamps().get(i.get()).toInstant(ZONE_OFFSET).toEpochMilli())
                        .append("\"");
            }
            output.append("\n");
            i.getAndIncrement();
        });
        return output.toString();
    }

    private ByteBuffer convertToByteBuffer(Dataframe dataframe, boolean includeHeader) {
        final String inputsStr = convertToString(dataframe, includeHeader, false);
        return ByteBuffer.wrap(inputsStr.getBytes(UTF8));
    }

    @Override
    public ByteBuffer toByteBuffer(Dataframe dataframe, boolean includeHeader) {
        return convertToByteBuffer(dataframe, includeHeader);
    }

    @Override
    public ByteBuffer[] toByteBuffers(Dataframe dataframe, boolean includeHeader) {
        ByteBuffer dataBuffer = toByteBuffer(dataframe, includeHeader);
        final StringBuilder output = new StringBuilder();
        if (includeHeader) {
            output.append(String.join(",", "synthetic", "id", "timestamp")).append("\n");
        }
        List<String> datapointTags = dataframe.getTags();
        List<String> ids = dataframe.getIds();
        List<LocalDateTime> timestamps = dataframe.getTimestamps();
        for (int i = 0; i < ids.size(); i++) {
            output.append(String.join(",", datapointTags.get(i), ids.get(i),
                    timestamps.get(i).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))).append("\n");
        }
        String outputString = output.toString();
        return new ByteBuffer[] { dataBuffer, ByteBuffer.wrap(outputString.getBytes(UTF8)) };
    }
}
