package org.kie.trustyai.explainability.local.tssaliency;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.utils.TimeseriesUtils;

/**
 * A wrapper for a {@link PredictionProvider} that converts inputs between time-series vector format and
 * list of features format
 */
public class TSSaliencyModelWrapper implements PredictionProvider {

    private final PredictionProvider model;

    public TSSaliencyModelWrapper(PredictionProvider model) {
        this.model = model;
    }

    /**
     * Performs inference on a list of inputs in the vector format.
     *
     * @param inputs the input batch
     * @return
     */
    @Override
    public CompletableFuture<List<PredictionOutput>> predictAsync(List<PredictionInput> inputs) {
        final List<PredictionInput> transformedInputs = TimeseriesUtils.featureVectorTofeatureList(inputs);
        return model.predictAsync(transformedInputs);
    }
}
