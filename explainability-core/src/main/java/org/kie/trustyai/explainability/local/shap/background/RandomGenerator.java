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

package org.kie.trustyai.explainability.local.shap.background;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.explainability.model.PerturbationContext;
import org.kie.trustyai.explainability.model.PredictionInput;

public class RandomGenerator implements BackgroundGenerator {
    List<PredictionInput> seeds;
    PerturbationContext pc;

    /**
     * Create a Random Background Generator
     *
     * @param seeds: All or a subset of the available training {@link PredictionInput}s
     */
    public RandomGenerator(List<PredictionInput> seeds) {
        this.pc = new PerturbationContext(new Random(), 0);
        this.seeds = seeds;
    }

    /**
     * Create a Random Background Generator
     *
     * @param seeds: All or a subset of the available training {@link PredictionInput}s
     * @param pc: A {@link PerturbationContext} object to provide the source of randomness in the sampling
     */
    public RandomGenerator(List<PredictionInput> seeds, PerturbationContext pc) {
        this.pc = pc;
        this.seeds = seeds;
    }

    /**
     * Sample $n random background points from the provided seed points.
     *
     * @param n: The number of points to randomly sample from seeds
     */
    public List<PredictionInput> generate(int n) {
        List<Integer> idx = IntStream.range(0, seeds.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(idx, pc.getRandom());
        return IntStream.range(0, n).mapToObj(i -> seeds.get(idx.get(i))).collect(Collectors.toList());
    }
}
