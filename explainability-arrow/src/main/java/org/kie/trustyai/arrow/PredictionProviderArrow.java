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
package org.kie.trustyai.arrow;

import java.util.concurrent.CompletableFuture;

/**
 * A provider of predictions.
 * This can be any model, service or function, like (local / remote) DMN, PMML services or any other ML model.
 */
@FunctionalInterface
public interface PredictionProviderArrow {
    CompletableFuture<byte[]> predictAsync(byte[] outBytes);
}
