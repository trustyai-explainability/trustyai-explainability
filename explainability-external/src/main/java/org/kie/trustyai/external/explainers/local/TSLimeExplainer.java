package org.kie.trustyai.external.explainers.local;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.kie.trustyai.explainability.local.TimeSeriesExplainer;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.external.interfaces.ExternalPythonExplainer;
import org.kie.trustyai.external.interfaces.TsFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jep.SubInterpreter;

public class TSLimeExplainer extends ExternalPythonExplainer<Map<String, Object>> implements TimeSeriesExplainer<TSLimeExplanation> {

    private static final Logger LOG = LoggerFactory.getLogger(TSLimeExplainer.class);
    private final String NAMESPACE = "trustyaiexternal.algorithms.tslime";
    private final String NAME = "TSLimeExplainer";
    private final SubInterpreter interpreter;

    private final String timestampColumn;

    public TSLimeExplainer(Builder builder) {
        super();
        this.interpreter = builder.interpreter;
        addConstructionArg("model_name", builder.modelName);
        addConstructionArg("model_version", builder.modelVersion);
        addConstructionArg("input_length", builder.inputLength);
        builder.nPerturbations.ifPresent(n -> addConstructionArg("n_perturbations", n));
        builder.relevantHistory.ifPresent(n -> addConstructionArg("relevant_history", n));
        builder.perturbers.ifPresent(strings -> addConstructionArg("perturbers", strings));
        addConstructionArg("target", builder.modelTarget);
        this.timestampColumn = builder.timestampColumn;
        LOG.debug("TSICE explainer created");

    }

    public CompletableFuture<TSLimeExplanation> explainAsync(TsFrame dataframe, PredictionProvider model, Consumer<TSLimeExplanation> intermediateResultsConsumer) {

        final Map<String, Object> args = Map.of("point", dataframe.getTsFrame(this.interpreter));
        final Map<String, Object> result;
        try {
            result = this.invoke(args, interpreter);
        } catch (Throwable e) {
            LOG.error("Error while invoking TSLimeExplainer", e);
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(new TSLimeExplanation(result));
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompletableFuture<TSLimeExplanation> explainAsync(Prediction prediction, PredictionProvider model, Consumer<TSLimeExplanation> intermediateResultsConsumer) {
        return explainAsync(List.of(prediction), model, intermediateResultsConsumer);
    }

    @Override
    public CompletableFuture<TSLimeExplanation> explainAsync(List<Prediction> prediction, PredictionProvider model, Consumer<TSLimeExplanation> intermediateResultsConsumer) {
        final Dataframe df = Dataframe.createFrom(prediction);
        final TsFrame tsFrame = new TsFrame(df, this.timestampColumn);
        return explainAsync(tsFrame, model, intermediateResultsConsumer);
    }

    public static class Builder {
        private int inputLength;

        private OptionalInt relevantHistory = OptionalInt.empty();

        private OptionalInt nPerturbations;

        private Optional<String[]> perturbers = Optional.empty();

        private String modelTarget;
        private String modelName;

        private String timestampColumn;

        private String modelVersion;
        private SubInterpreter interpreter;

        public Builder withTimestampColumn(String timestampColumn) {
            this.timestampColumn = timestampColumn;
            return this;
        }

        public Builder withInputLength(int inputLength) {
            this.inputLength = inputLength;
            return this;
        }

        public Builder withRelevantHistory(OptionalInt relevantHistory) {
            this.relevantHistory = relevantHistory;
            return this;
        }

        public Builder withNPerturbations(int nPerturbations) {
            this.nPerturbations = OptionalInt.of(nPerturbations);
            return this;
        }

        public Builder withPerturbers(String[] perturbers) {
            this.perturbers = Optional.of(perturbers);
            return this;
        }

        public TSLimeExplainer build(SubInterpreter interpreter, String modelTarget, String modelName, String version) {
            this.interpreter = interpreter;
            this.modelTarget = modelTarget;
            this.modelName = modelName;
            this.modelVersion = version;
            return new TSLimeExplainer(this);
        }
    }
}
