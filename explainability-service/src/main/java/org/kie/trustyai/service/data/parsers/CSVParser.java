package org.kie.trustyai.service.data.parsers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionMetadata;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.data.utils.CSVUtils;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.context.ApplicationScoped;

@LookupIfProperty(name = "service.data.format", stringValue = "CSV")
@ApplicationScoped
public class CSVParser implements DataParser {

    private static final Logger LOG = Logger.getLogger(CSVParser.class);
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    @Override
    public Dataframe toDataframe(ByteBuffer byteBuffer, Metadata metadata) throws DataframeCreateException {
        final String data = UTF8.decode(byteBuffer).toString();

        final List<Prediction> predictions;
        try {
            predictions = CSVUtils.parse(data, metadata);
        } catch (IOException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        LOG.info("Creating dataframe from CSV data");
        return Dataframe.createFrom(predictions);
    }

    @Override
    public Dataframe toDataframe(ByteBuffer dataByteBuffer, ByteBuffer internalDataByteBuffer, Metadata metadata)
            throws DataframeCreateException {
        final String data = UTF8.decode(dataByteBuffer).toString();

        final List<Prediction> predictions;
        try {
            predictions = CSVUtils.parse(data, metadata);
        } catch (IOException e) {
            throw new DataframeCreateException(e.getMessage());
        }
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

        LOG.info("Creating dataframe from CSV data");
        return Dataframe.createWithMetadata(predictions, predictionsMetadata);
    }

    public String convertToString(Dataframe dataframe, boolean includeHeader) {
        final StringBuilder output = new StringBuilder();
        if (includeHeader) {
            output
                    .append(
                            String.join(",",
                                    dataframe.getColumnNames().stream().map(name -> "\"" + name + "\"").collect(Collectors.toList())))
                    .append("\n");
        }
        dataframe.getRows().forEach(values -> {
            final String rowStr = String.join(",", values.stream().map(value -> {
                final Object obj = value.getUnderlyingObject();
                if (obj instanceof String) {
                    return "\"" + obj + "\"";
                } else {
                    return obj.toString();
                }
            }).collect(Collectors.toList()));
            output.append(rowStr).append("\n");
        });
        return output.toString();
    }

    private ByteBuffer convertToByteBuffer(Dataframe dataframe, boolean includeHeader) {
        final String inputsStr = convertToString(dataframe, includeHeader);
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
