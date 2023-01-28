/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.trustyai.explainability.local.shap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.linear.RealVector;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.AsyncPredictionProvider;
import org.kie.trustyai.explainability.model.SimplePrediction;

public class ShapDataCarrier {
    private AsyncPredictionProvider model;
    private CompletableFuture<RealVector> linkNull;
    private CompletableFuture<RealVector> fnull;
    private CompletableFuture<Map<String, Double>> nullOutput;
    private int rows;
    private int cols;
    private CompletableFuture<Integer> outputSize;
    private Integer numSamples;

    private List<ShapSyntheticDataSample> samplesAdded;
    private int samplesAddedSize = 0;
    private List<Integer> varyingFeatureGroups;
    private int numVarying;
    private HashMap<Integer, Integer> masksUsed;
    private List<Pair<SimplePrediction, boolean[]>> availableCounterfactuals = new ArrayList<>();

    // data statistics ======================================================
    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public CompletableFuture<Integer> getOutputSize() {
        return outputSize;
    }

    public void setOutputSize(CompletableFuture<Integer> outputSize) {
        this.outputSize = outputSize;
    }

    // model information ========================================================

    public AsyncPredictionProvider getModel() {
        return model;
    }

    public void setModel(AsyncPredictionProvider model) {
        this.model = model;
    }

    public CompletableFuture<RealVector> getLinkNull() {
        return linkNull;
    }

    public void setLinkNull(CompletableFuture<RealVector> linkNull) {
        this.linkNull = linkNull;
    }

    public CompletableFuture<RealVector> getFnull() {
        return fnull;
    }

    public void setFnull(CompletableFuture<RealVector> fnull) {
        this.fnull = fnull;
    }

    public CompletableFuture<Map<String, Double>> getNullOutput() {
        return nullOutput;
    }

    public void setNullOutput(CompletableFuture<Map<String, Double>> nullOutput) {
        this.nullOutput = nullOutput;
    }

    // shap configuration =========================================================
    public Integer getNumSamples() {
        return numSamples;
    }

    public void setNumSamples(Integer numSamples) {
        this.numSamples = numSamples;
    }

    // runtime accumulators ==========================================================
    //sample trackers
    public ShapSyntheticDataSample getSamplesAdded(int i) {
        return samplesAdded.get(i);
    }

    public Integer getSamplesAddedSize() {
        return samplesAddedSize;
    }

    public void setSamplesAdded(List<ShapSyntheticDataSample> samplesAdded) {
        this.samplesAdded = samplesAdded;
    }

    public void addSample(ShapSyntheticDataSample sample) {
        this.samplesAdded.add(sample);
        this.samplesAddedSize += 1;
    }

    public void addAvailableCounterfactual(List<PredictionInput> pis, List<PredictionOutput> pos, List<boolean[]> masks,
            List<Integer> batchSizes, int index) {
        int currentBatch = 0;
        int currentPredictionIndex = 0;
        for (int i = 0; i < pis.size(); i++) {
            if (currentPredictionIndex > batchSizes.get(currentBatch)) {
                currentBatch++;
                currentPredictionIndex = 0;
            }
            this.availableCounterfactuals.add(Pair.of(new SimplePrediction(pis.get(i), pos.get(i)), masks.get(currentBatch)));
            currentPredictionIndex++;
        }
    }

    public void addAvailableCounterfactual(List<PredictionInput> pis, List<PredictionOutput> pos, boolean[] mask, int index) {
        for (int i = 0; i < pis.size(); i++) {
            this.availableCounterfactuals.add(Pair.of(new SimplePrediction(pis.get(i), pos.get(i)), mask));
        }
    }

    public List<Pair<SimplePrediction, boolean[]>> getAvailableCounterfactuals() {
        return this.availableCounterfactuals;
    }

    //varying feature groups getters and setters
    public List<Integer> getVaryingFeatureGroups() {
        return varyingFeatureGroups;
    }

    public Integer getVaryingFeatureGroups(int i) {
        return varyingFeatureGroups.get(i);
    }

    public void setVaryingFeatureGroups(List<Integer> varyingFeatureGroups) {
        this.varyingFeatureGroups = varyingFeatureGroups;
    }

    public int getNumVarying() {
        return numVarying;
    }

    public void setNumVarying(int numVarying) {
        this.numVarying = numVarying;
    }

    // mask hash getters and setters
    public HashMap<Integer, Integer> getMasksUsed() {
        return masksUsed;
    }

    public Integer getMasksUsed(Integer key) {
        return masksUsed.get(key);
    }

    public void setMasksUsed(HashMap<Integer, Integer> masksUsed) {
        this.masksUsed = masksUsed;
    }

    public void addMask(Integer key, Integer value) {
        this.masksUsed.put(key, value);
    }

    public ShapDataCarrier() {
        // empty
    }
}
