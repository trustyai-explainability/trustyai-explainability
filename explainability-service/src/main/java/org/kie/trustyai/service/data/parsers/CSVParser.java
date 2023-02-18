package org.kie.trustyai.service.data.parsers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.utils.CSVUtils;

import io.quarkus.arc.lookup.LookupIfProperty;

@LookupIfProperty(name = "service.data.format", stringValue = "CSV")
@ApplicationScoped
public class CSVParser implements DataParser {
    private static final Logger LOG = Logger.getLogger(CSVParser.class);
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    @Override
    public Dataframe toDataframe(ByteBuffer inputs, ByteBuffer outputs) throws DataframeCreateException {
        final String inputData = UTF8.decode(inputs).toString();
        final String outputData = UTF8.decode(outputs).toString();

        final List<PredictionInput> predictionInputs;
        try {
            predictionInputs = CSVUtils.parseInputs(inputData);
        } catch (IOException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final List<PredictionOutput> predictionOutputs;
        try {
            predictionOutputs = CSVUtils.parseOutputs(outputData);
        } catch (IOException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        LOG.info("Creating dataframe from CSV data");
        return Dataframe.createFrom(predictionInputs, predictionOutputs);
    }

    private String convertToString(Dataframe dataframe, boolean includeHeader) {
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
    public ByteBuffer toInputByteBuffer(Dataframe dataframe, boolean includeHeader) {
        return convertToByteBuffer(dataframe.getInputDataframe(), includeHeader);
    }

    @Override
    public ByteBuffer toOutputByteBuffer(Dataframe dataframe, boolean includeHeader) {
        return convertToByteBuffer(dataframe.getOutputDataframe(), includeHeader);
    }

}
