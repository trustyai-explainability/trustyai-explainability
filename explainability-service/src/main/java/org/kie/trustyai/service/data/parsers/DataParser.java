package org.kie.trustyai.service.data.parsers;

import java.nio.ByteBuffer;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.metadata.Metadata;

public interface DataParser {

    Dataframe toDataframe(ByteBuffer inputs, Metadata metadata) throws DataframeCreateException;

    ByteBuffer toByteBuffer(Dataframe dataframe, boolean includeHeader);
}
