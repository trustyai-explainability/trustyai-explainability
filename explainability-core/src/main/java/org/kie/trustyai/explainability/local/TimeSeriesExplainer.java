package org.kie.trustyai.explainability.local;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionProvider;

/**
 * Interface for time series local explainers.
 * Time-series local explainers are explainers that can explain list of time-series predictions, typically a subset of a time-series.
 * 
 * @param <T> the type of the time-series local explanation generated
 */
public interface TimeSeriesExplainer<T> extends LocalExplainer<T> {

    default CompletableFuture<T> explainAsync(List<Prediction> prediction, PredictionProvider model) {
        return explainAsync(prediction, model, unused -> {
            /* NOP */
        });
    }

    CompletableFuture<T> explainAsync(List<Prediction> prediction, PredictionProvider model, Consumer<T> intermediateResultsConsumer);

}
