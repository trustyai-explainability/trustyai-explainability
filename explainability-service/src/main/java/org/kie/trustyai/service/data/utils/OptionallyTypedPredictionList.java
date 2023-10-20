package org.kie.trustyai.service.data.utils;

import java.util.List;

import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.service.payloads.values.DataType;

class OptionallyTypedPredictionList {
    /**
     * In the case where the InferenceReconciler can directly provide the DATAFRAME TYPES of the inbound payloads,
     * the OptionallyTypedPredictionList stores these types to skip type inference during dataframe metadata creation.
     * For simplicity, this object is (currently) restricted to holding *either*
     * a) One prediction and n_feats + n_outputs types
     * b) N predictions, with no types
     */
    List<DataType> types;
    List<Prediction> predictions;

    public OptionallyTypedPredictionList(List<DataType> types, List<Prediction> predictions) {
        if (types != null) {
            if (predictions.size() != 1) {
                throw new IllegalArgumentException("When passing a list of types, the prediction list must contain only one element, but " + predictions.size() + " predictions were passed.");
            }

            int nTypes = types.size();
            int nFeatsPlusOutputs = predictions.get(0).getInput().getFeatures().size() + predictions.get(0).getOutput().getOutputs().size();
            if (predictions.size() == 1 && nTypes != nFeatsPlusOutputs) {
                throw new IllegalArgumentException(String.format(
                        "Size mismatch between number of passed types (%d) and total number of combined features and outputs in prediction (%d)",
                        nTypes, nFeatsPlusOutputs));
            }
        }

        this.types = types;
        this.predictions = predictions;

    }

    public OptionallyTypedPredictionList(List<Prediction> predictions) {
        this.predictions = predictions;
        types = null;
    }
}
