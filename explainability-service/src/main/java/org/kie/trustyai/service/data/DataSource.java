package org.kie.trustyai.service.data;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.parsers.DataParser;
import org.kie.trustyai.service.data.storage.Storage;

@Singleton
public class DataSource {
    private static final Logger LOG = Logger.getLogger(DataSource.class);

    @Inject
    Storage storage;

    @Inject
    DataParser parser;

    @Inject
    ServiceConfig serviceConfig;

    public Dataframe getDataframe() throws DataframeCreateException {

        final ByteBuffer inputsBuffer;
        try {
            inputsBuffer = storage.getInputData();
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final ByteBuffer outputsBuffer;
        try {
            outputsBuffer = storage.getOutputData();
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final Dataframe dataframe = parser.toDataframe(inputsBuffer, outputsBuffer);
        if (serviceConfig.batchSize().isPresent()) {
            final int batchSize = serviceConfig.batchSize().getAsInt();
            final int rows = dataframe.getRowDimension();

            if (batchSize >= rows) {
                LOG.info("Batching with " + batchSize + " rows. Passing " + dataframe.getRowDimension() + " rows");
                return dataframe;
            } else {
                final List<Integer> indices = IntStream.range(rows - batchSize, rows).boxed().collect(Collectors.toList());
                final Dataframe batch = dataframe.filterByRowIndex(indices);
                LOG.info("Batching with " + batchSize + " rows. Passing " + batch.getRowDimension() + " rows");
                return batch;
            }
        } else {
            LOG.info("No batching. Passing all of " + dataframe.getRowDimension() + " rows");
            return dataframe;

        }

    }

}
