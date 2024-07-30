package org.kie.trustyai.service.data.parsers;

import java.nio.ByteBuffer;
import java.util.List;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.payloads.service.InferenceId;

public interface DataParser {

    Dataframe toDataframe(ByteBuffer inputs, StorageMetadata storageMetadata) throws DataframeCreateException;

    Dataframe toDataframe(ByteBuffer inputs, ByteBuffer internalData, StorageMetadata storageMetadata) throws DataframeCreateException;

    List<InferenceId> toInferenceIds(ByteBuffer byteBuffer);

    ByteBuffer toByteBuffer(Dataframe dataframe, boolean includeHeader);

    ByteBuffer[] toByteBuffers(Dataframe dataframe, boolean includeHeader);
}
