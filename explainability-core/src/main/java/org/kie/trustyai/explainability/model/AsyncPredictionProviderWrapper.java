/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates.
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
package org.kie.trustyai.explainability.model;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AsyncPredictionProviderWrapper implements AsyncPredictionProvider {

    private final SyncPredictionProvider model;

    private AsyncPredictionProviderWrapper(SyncPredictionProvider model) {
        this.model = model;
    }
    @Override
    public CompletableFuture<List<PredictionOutput>> predictAsync(List<PredictionInput> inputs) {
        return CompletableFuture.completedFuture(this.model.predictSync(inputs));
    }

    public static AsyncPredictionProvider from(SyncPredictionProvider model) {
        return new AsyncPredictionProviderWrapper(model);
    }

    public static AsyncPredictionProvider from(AsyncPredictionProvider model) {
        return model;
    }

    public static AsyncPredictionProvider from(PredictionProvider model) {
        if (model instanceof AsyncPredictionProvider) {
            return (AsyncPredictionProvider) model;
        } else if (model instanceof SyncPredictionProvider) {
            return new AsyncPredictionProviderWrapper((SyncPredictionProvider) model);
        } else {
            throw new IllegalArgumentException("Prediction provider must be either AsyncPredictionProvider or SyncPredictionProvider");
        }
    }

}
