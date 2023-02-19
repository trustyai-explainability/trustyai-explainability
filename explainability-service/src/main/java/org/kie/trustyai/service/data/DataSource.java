package org.kie.trustyai.service.data;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.cache.DataframeCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.parsers.DataParser;
import org.kie.trustyai.service.data.storage.Storage;

import io.quarkus.cache.CacheResult;

@Singleton
public class DataSource {
    private static final Logger LOG = Logger.getLogger(DataSource.class);

    @Inject
    Instance<Storage> storage;

    @Inject
    DataParser parser;

    @Inject
    ServiceConfig serviceConfig;

    @CacheResult(cacheName = "dataframe", keyGenerator = DataframeCacheKeyGen.class)
    public Dataframe getDataframe() throws DataframeCreateException {

        LOG.info("Cache miss! Reading dataframe");

        final ByteBuffer inputsBuffer;
        try {
            inputsBuffer = storage.get().getInputData();
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final ByteBuffer outputsBuffer;
        try {
            outputsBuffer = storage.get().getOutputData();
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

    public void appendDataframe(Dataframe dataframe) {
        if (!storage.get().inputExists()) {
            storage.get().saveInputData(parser.toInputByteBuffer(dataframe, true));
        } else {
            storage.get().appendInputData(parser.toInputByteBuffer(dataframe, false));
        }

        if (!storage.get().outputExists()) {
            storage.get().saveOutputData(parser.toOutputByteBuffer(dataframe, true));
        } else {
            storage.get().appendOutputData(parser.toOutputByteBuffer(dataframe, false));
        }
    }
}
