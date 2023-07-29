package org.kie.trustyai.external.explainers.local;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.local.TimeSeriesExplainer;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.external.interfaces.ExternalPythonExplainer;
import org.kie.trustyai.external.interfaces.TsFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jep.SubInterpreter;

public class TSICEExplainer extends ExternalPythonExplainer<Map<String, Object>> implements TimeSeriesExplainer<TSICEExplanation> {

    private static final Logger LOG = LoggerFactory.getLogger(TSICEExplainer.class);
    private final String NAMESPACE = "trustyaiexternal.algorithms.tsice";
    private final String NAME = "TSICEExplainer";
    private final SubInterpreter interpreter;

    private final String timestampColumn;

    public TSICEExplainer(Builder builder) {
        super();
        this.interpreter = builder.interpreter;
        addConstructionArg("model_name", builder.modelName);
        addConstructionArg("model_version", builder.modelVersion);
        addConstructionArg("input_length", builder.inputLength);
        addConstructionArg("forecast_lookahead", builder.forecastLookahead);
        builder.nVariables.ifPresent(integer -> addConstructionArg("n_variables", integer));
        builder.nExogs.ifPresent(integer -> addConstructionArg("n_exogs", integer));
        addConstructionArg("n_perturbations", builder.nPerturbations);
        addConstructionArg("features_to_analyze", builder.featuresToAnalyze.stream().map(AnalyseFeature::toString).collect(Collectors.toList()));
        builder.perturbers.ifPresent(strings -> addConstructionArg("perturbers", strings));
        addConstructionArg("explanation_window_start", builder.explanationWindowStart);
        addConstructionArg("explanation_window_length", builder.explanationWindowLength);
        addConstructionArg("target", builder.modelTarget);
        this.timestampColumn = builder.timestampColumn;
        LOG.info("TSICE explainer created");

    }

    public CompletableFuture<TSICEExplanation> explainAsync(TsFrame dataframe, PredictionProvider model, Consumer<TSICEExplanation> intermediateResultsConsumer) {

        final Map<String, Object> args = Map.of("point", dataframe.getTsFrame(this.interpreter));
        final Map<String, Object> result;
        try {
            result = this.invoke(args, interpreter);
        } catch (Throwable e) {
            LOG.error("Error while invoking TSICE", e);
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(new TSICEExplanation(result));
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
    public CompletableFuture<TSICEExplanation> explainAsync(Prediction prediction, PredictionProvider model, Consumer<TSICEExplanation> intermediateResultsConsumer) {
        return explainAsync(List.of(prediction), model, intermediateResultsConsumer);
    }

    @Override
    public CompletableFuture<TSICEExplanation> explainAsync(List<Prediction> prediction, PredictionProvider model, Consumer<TSICEExplanation> intermediateResultsConsumer) {
        final Dataframe df = Dataframe.createFrom(prediction);
        final TsFrame tsFrame = new TsFrame(df, this.timestampColumn);
        return explainAsync(tsFrame, model, intermediateResultsConsumer);
    }

    public enum AnalyseFeature {
        MEDIAN("median"),
        MEAN("mean"),
        MIN("min"),
        MAX("max"),
        STD("std"),
        RANGE("range"),
        INTERCEPT("intercept"),
        TREND("trend"),
        RSQUARED("rsquared"),
        MAX_VARIATION("max_variation");

        private final String feature;

        AnalyseFeature(String feature) {
            this.feature = feature;
        }

        @Override
        public String toString() {
            return this.feature;
        }
    }

    public static class Builder {
        private int inputLength;
        private int forecastLookahead;
        private OptionalInt nVariables = OptionalInt.empty();
        private OptionalInt nExogs = OptionalInt.empty();
        private int nPerturbations;
        private List<AnalyseFeature> featuresToAnalyze;
        private Optional<String[]> perturbers = Optional.empty();
        private int explanationWindowStart;
        private int explanationWindowLength;
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

        public Builder withForecastLookahead(int forecastLookahead) {
            this.forecastLookahead = forecastLookahead;
            return this;
        }

        public Builder withNVariables(int nVariables) {
            this.nVariables = OptionalInt.of(nVariables);
            return this;
        }

        public Builder withNExogs(int nExogs) {
            this.nExogs = OptionalInt.of(nExogs);
            return this;
        }

        public Builder withNPerturbations(int nPerturbations) {
            this.nPerturbations = nPerturbations;
            return this;
        }

        public Builder withFeaturesToAnalyze(List<AnalyseFeature> featuresToAnalyze) {
            this.featuresToAnalyze = featuresToAnalyze;
            return this;
        }

        public Builder withPerturbers(String[] perturbers) {
            this.perturbers = Optional.of(perturbers);
            return this;
        }

        public Builder withExplanationWindowStart(int explanationWindowStart) {
            this.explanationWindowStart = explanationWindowStart;
            return this;
        }

        public Builder withExplanationWindowLength(int explanationWindowLength) {
            this.explanationWindowLength = explanationWindowLength;
            return this;
        }

        public TSICEExplainer build(SubInterpreter interpreter, String modelTarget, String modelName, String version) {
            this.interpreter = interpreter;
            this.modelTarget = modelTarget;
            this.modelName = modelName;
            this.modelVersion = version;
            return new TSICEExplainer(this);
        }
    }
}
