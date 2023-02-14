package org.kie.trustyai.service.data.parsers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
    public Dataframe parse(ByteBuffer inputs, ByteBuffer outputs) throws DataframeCreateException {
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
}
