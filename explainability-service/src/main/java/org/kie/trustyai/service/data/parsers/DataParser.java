package org.kie.trustyai.service.data.parsers;

import java.nio.ByteBuffer;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;

public interface DataParser {

    Dataframe toDataframe(ByteBuffer inputs, StorageMetadata storageMetadata) throws DataframeCreateException;

    Dataframe toDataframe(ByteBuffer inputs, ByteBuffer internalData, StorageMetadata storageMetadata) throws DataframeCreateException;

    ByteBuffer toByteBuffer(Dataframe dataframe, boolean includeHeader);

    ByteBuffer[] toByteBuffers(Dataframe dataframe, boolean includeHeader);
}
