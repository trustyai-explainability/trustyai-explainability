package org.kie.trustyai.explainability.model.dataframe;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

import static org.junit.jupiter.api.Assertions.*;

class DataframeMetadataTest {

    @Test
    void testColumnAdding() {
        DataframeMetadata dfm = new DataframeMetadata();
        for (int i = 0; i < 100; i++) {
            if (i % 3 == 0) {
                Feature f = FeatureFactory.newNumericalFeature(String.valueOf(i), i);
                dfm.add(f);
            } else if (i % 3 == 1) {
                Output o = new Output(String.valueOf(i), Type.NUMBER, new Value(i), 1.0);
                dfm.add(o);
            } else {
                dfm.add(String.valueOf(i), null, Type.CATEGORICAL, true, null, true);
            }
        }

        // check alignment of cached column names
        assertEquals(dfm.getNameAliases().size(), dfm.getNames().size());
        assertEquals(dfm.getTypes().size(), dfm.getNames().size());
        assertEquals(dfm.getConstrained().size(), dfm.getNames().size());
        assertEquals(dfm.getDomains().size(), dfm.getNames().size());
        assertEquals(dfm.getInputs().size(), dfm.getNames().size());
    }
}
