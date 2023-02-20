package org.kie.trustyai.service.data.parsers;

import java.nio.ByteBuffer;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;

public interface DataParser {

    Dataframe toDataframe(ByteBuffer inputs, ByteBuffer outputs) throws DataframeCreateException;

    ByteBuffer toInputByteBuffer(Dataframe dataframe, boolean includeHeader);

    ByteBuffer toOutputByteBuffer(Dataframe dataframe, boolean includeHeader);
}
