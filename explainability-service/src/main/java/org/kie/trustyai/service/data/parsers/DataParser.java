package org.kie.trustyai.service.data.parsers;

import java.nio.ByteBuffer;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;

public interface DataParser {

    Dataframe parse(ByteBuffer inputs, ByteBuffer outputs) throws DataframeCreateException;
}
