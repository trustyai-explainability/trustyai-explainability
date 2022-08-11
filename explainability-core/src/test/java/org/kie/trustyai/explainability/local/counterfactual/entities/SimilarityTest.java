/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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
package org.kie.trustyai.explainability.local.counterfactual.entities;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.model.FeatureFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimilarityTest {

    private static double HIGHEST_SIMILARITY = 1.0;
    private static double LOWEST_SIMILARITY = 0.0;

    @Test
    void integerSimpleSimilarity() {
        final int value = 20;
        final IntegerEntity x = IntegerEntity.from(FeatureFactory.newNumericalFeature("x", value), 0, 100);

        x.setProposedValue(value + 9);
        final double similarity1 = x.similarity();

        x.setProposedValue(value - 9);
        final double similarity2 = x.similarity();

        assertEquals(similarity1, similarity2);
        assertTrue(similarity1 > 0.9);
    }

    @Test
    void doubleSimpleSimilarity() {
        final double value = 500.0;
        final DoubleEntity x = DoubleEntity.from(FeatureFactory.newNumericalFeature("x", value), 0.0, 1000.0);

        x.setProposedValue(value + 90.0);
        final double similarity1 = x.similarity();

        x.setProposedValue(value - 90.0);
        final double similarity2 = x.similarity();

        assertEquals(similarity1, similarity2);
        assertTrue(similarity1 > 0.9);
    }

    @Test
    void longSimpleSimilarity() {
        final long value = 20;
        final LongEntity x = LongEntity.from(FeatureFactory.newNumericalFeature("x", value), 0, 100);

        x.setProposedValue(value + 9);
        final double similarity1 = x.similarity();

        x.setProposedValue(value - 9);
        final double similarity2 = x.similarity();

        assertEquals(similarity1, similarity2);
        assertTrue(similarity1 > 0.9);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    void booleanSimpleSimilarity(int seed) {
        final Random random = new Random(seed);
        final boolean originalValue = random.nextBoolean();
        final BooleanEntity x = BooleanEntity.from(FeatureFactory.newBooleanFeature("x", originalValue));

        x.setProposedValue(!originalValue);
        assertEquals(LOWEST_SIMILARITY, x.similarity());
        assertEquals(1, x.distance());

        x.setProposedValue(originalValue);
        assertEquals(HIGHEST_SIMILARITY, x.similarity());
        assertEquals(0, x.distance());
    }

    @Test
    void categoricalSimpleSimilarity() {
        final String value = "foo";
        final Set<String> categories = Set.of("foo", "bar", "baz");
        final CategoricalEntity x = CategoricalEntity.from(FeatureFactory.newCategoricalFeature("x", value), categories);

        x.setProposedValue("bar");
        assertEquals(LOWEST_SIMILARITY, x.similarity());
        assertEquals(1, x.distance());

        x.setProposedValue("baz");
        assertEquals(LOWEST_SIMILARITY, x.similarity());
        assertEquals(1, x.distance());

        x.setProposedValue("foo");
        assertEquals(HIGHEST_SIMILARITY, x.similarity());
        assertEquals(0, x.distance());
    }

    @Test
    void uriSimpleSimilarity() throws URISyntaxException {
        final URI value = new URI("https://kogito.kie.org/trustyai/");
        final Set<URI> uris = Set.of(
                new URI("https://example.com/foo"),
                new URI("https://example.com/bar"));
        final URIEntity x = URIEntity.from(FeatureFactory.newURIFeature("uri", value), uris);

        x.setProposedValue(new URI("https://example.com/bar"));
        assertEquals(LOWEST_SIMILARITY, x.similarity());
        assertEquals(1, x.distance());

        x.setProposedValue(new URI("https://example.com/baz"));
        assertEquals(LOWEST_SIMILARITY, x.similarity());
        assertEquals(1, x.distance());

        x.setProposedValue(new URI("https://kogito.kie.org/trustyai/"));
        assertEquals(HIGHEST_SIMILARITY, x.similarity());
        assertEquals(0, x.distance());
    }

    @Test
    void currencySimpleSimilarity() {
        final Currency value = Currency.getInstance("GBP");
        final Set<Currency> categories = Set.of(
                Currency.getInstance("GBP"),
                Currency.getInstance("EUR"),
                Currency.getInstance("USD"));

        final CurrencyEntity x = CurrencyEntity.from(FeatureFactory.newCurrencyFeature("currency", value), categories);

        x.setProposedValue(Currency.getInstance("EUR"));
        assertEquals(LOWEST_SIMILARITY, x.similarity());
        assertEquals(1, x.distance());

        x.setProposedValue(Currency.getInstance("USD"));
        assertEquals(LOWEST_SIMILARITY, x.similarity());
        assertEquals(1, x.distance());

        x.setProposedValue(Currency.getInstance("GBP"));
        assertEquals(HIGHEST_SIMILARITY, x.similarity());
        assertEquals(0, x.distance());
    }

    @Test
    void binarySimpleSimilarity() {
        ByteBuffer bytes = ByteBuffer.wrap("foo".getBytes());

        final List<ByteBuffer> categories = Stream.of(
                "bar".getBytes(), "baz".getBytes(), "fun".getBytes())
                .map(ByteBuffer::wrap).collect(Collectors.toList());

        final BinaryEntity x = BinaryEntity.from(FeatureFactory.newBinaryFeature("f", bytes), new HashSet<>(categories));

        x.setProposedValue(categories.get(0));
        assertEquals(LOWEST_SIMILARITY, x.similarity());
        assertEquals(1, x.distance());

        x.setProposedValue(categories.get(1));
        assertEquals(LOWEST_SIMILARITY, x.similarity());
        assertEquals(1, x.distance());

        x.setProposedValue(bytes);
        assertEquals(HIGHEST_SIMILARITY, x.similarity());
        assertEquals(0, x.distance());
    }

    @Test
    void durationSimpleSimilarity() {
        final Long days = 365L;
        final Duration duration = Duration.ofDays(days);
        final DurationEntity x =
                DurationEntity.from(FeatureFactory.newDurationFeature("x", duration), Duration.ZERO, Duration.ofDays(3 * days));

        assertEquals(HIGHEST_SIMILARITY, x.similarity());

        x.setProposedValue(Duration.ofDays(2 * days));
        assertEquals(0.6666, x.similarity(), 1e-3);
        assertEquals(Math.abs(Duration.ofDays(days).minus(Duration.ofDays(2 * days)).getSeconds()), x.distance());

        x.setProposedValue(Duration.ofDays(-days));
        assertEquals(0.333, x.similarity(), 1e-3);
        assertEquals(Math.abs(Duration.ofDays(days).minus(Duration.ofDays(-days)).getSeconds()), x.distance());

        x.setProposedValue(Duration.ofDays(days));
        assertEquals(HIGHEST_SIMILARITY, x.similarity());
        assertEquals(0, x.distance());

        x.setProposedValue(Duration.ofSeconds(-5L));
        assertEquals(0.6666, x.similarity(), 1e-3);
        assertEquals(Math.abs(Duration.ofDays(days).minus(Duration.ofSeconds(-5)).getSeconds()), x.distance());

    }

    @Test
    void timeSimpleSimilarity() {
        final LocalTime value = LocalTime.of(17, 17);
        final TimeEntity x = TimeEntity.from(FeatureFactory.newTimeFeature("x", value),
                LocalTime.of(10, 0), LocalTime.of(21, 18));

        assertEquals(HIGHEST_SIMILARITY, x.similarity());

        x.setProposedValue(LocalTime.of(17, 59));
        assertEquals(0.938, x.similarity(), 1e-3);
        assertEquals(2520, x.distance());

        x.setProposedValue(LocalTime.of(9, 23));
        assertEquals(0.300, x.similarity(), 1e-3);
        assertEquals(28440, x.distance());

        x.setProposedValue(value);
        assertEquals(HIGHEST_SIMILARITY, x.similarity());
        assertEquals(0, x.distance());
    }

    @Test
    void objectSimpleSimilarity() {
        Long value = 20L;

        final List<Object> categories = List.of(30L, "test");

        final ObjectEntity x = ObjectEntity.from(FeatureFactory.newObjectFeature("f", value), new HashSet<>(categories));

        x.setProposedValue(categories.get(0));
        assertEquals(LOWEST_SIMILARITY, x.similarity());

        x.setProposedValue(categories.get(1));
        assertEquals(LOWEST_SIMILARITY, x.similarity());

        x.setProposedValue(20L);
        assertEquals(HIGHEST_SIMILARITY, x.similarity());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    void constantRelativeSimilarityIntegerEntity(int seed) {
        final Random random = new Random(seed);
        final int MAX_UPPER_BOUND_RANGE = 10_000;
        // generate 3 random points
        int[] values = new int[3];
        values[0] = random.nextInt(1000);
        values[1] = values[0] + random.nextInt(100);
        values[2] = values[1] + random.nextInt(100);

        // create non-overlapping bounds
        int lowerBound = random.nextInt(values[0] - 1);
        int upperBound = values[2] + random.nextInt(MAX_UPPER_BOUND_RANGE) + 1;
        final IntegerEntity x =
                IntegerEntity.from(FeatureFactory.newNumericalFeature("x", values[0]), lowerBound, upperBound);

        // similarity to point #2
        x.setProposedValue(values[1]);
        final double x_similarity1 = x.similarity();
        // similarity to point #3
        x.setProposedValue(values[2]);
        final double x_similarity2 = x.similarity();

        // change bounds
        lowerBound = random.nextInt(values[0] - 1);
        upperBound = values[2] + random.nextInt(MAX_UPPER_BOUND_RANGE) + 1;

        final IntegerEntity y =
                IntegerEntity.from(FeatureFactory.newNumericalFeature("y", values[0]), lowerBound, upperBound);
        // similarity to point #2 with new range
        y.setProposedValue(values[1]);
        final double y_similarity1 = y.similarity();
        // similarity to point #3 with new range
        y.setProposedValue(values[2]);
        final double y_similarity2 = y.similarity();

        // relative similarities must stay the same
        assertTrue(x_similarity1 > x_similarity2);
        assertTrue(y_similarity1 > y_similarity2);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    void constantRelativeSimilarityDoubleEntity(int seed) {
        final Random random = new Random(seed);
        final double MAX_UPPER_BOUND_RANGE = 10_000.0;
        // generate 3 random points
        double[] values = new double[3];
        values[0] = random.nextDouble() * 1000.0;
        values[1] = values[0] + random.nextDouble() * 100.0;
        values[2] = values[1] + random.nextDouble() * 100.0;

        // create non-overlapping bounds
        double lowerBound = random.nextDouble() * values[0];
        double upperBound = values[2] + random.nextDouble() * MAX_UPPER_BOUND_RANGE;
        final DoubleEntity x =
                DoubleEntity.from(FeatureFactory.newNumericalFeature("x", values[0]), lowerBound, upperBound);

        // similarity to point #2
        x.setProposedValue(values[1]);
        final double x_similarity1 = x.similarity();
        // similarity to point #3
        x.setProposedValue(values[2]);
        final double x_similarity2 = x.similarity();

        // change bounds
        lowerBound = random.nextDouble() * values[0];
        upperBound = values[2] + random.nextDouble() * MAX_UPPER_BOUND_RANGE;

        final DoubleEntity y =
                DoubleEntity.from(FeatureFactory.newNumericalFeature("y", values[0]), lowerBound, upperBound);
        // similarity to point #2 with new range
        y.setProposedValue(values[1]);
        final double y_similarity1 = y.similarity();
        // similarity to point #3 with new range
        y.setProposedValue(values[2]);
        final double y_similarity2 = y.similarity();

        // relative similarities must stay the same
        assertTrue(x_similarity1 > x_similarity2);
        assertTrue(y_similarity1 > y_similarity2);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    void constantRelativeSimilarityLongEntity(int seed) {
        final Random random = new Random(seed);
        final int MAX_UPPER_BOUND_RANGE = 10_000;
        // generate 3 random points
        long[] values = new long[3];
        values[0] = random.nextInt(1000);
        values[1] = values[0] + random.nextInt(100);
        values[2] = values[1] + random.nextInt(100);

        // create non-overlapping bounds
        long lowerBound = random.nextInt((int) values[0] - 1);
        long upperBound = values[2] + random.nextInt(MAX_UPPER_BOUND_RANGE) + 1;
        final LongEntity x =
                LongEntity.from(FeatureFactory.newNumericalFeature("x", values[0]), lowerBound, upperBound);

        // similarity to point #2
        x.setProposedValue(values[1]);
        final double x_similarity1 = x.similarity();
        // similarity to point #3
        x.setProposedValue(values[2]);
        final double x_similarity2 = x.similarity();

        // change bounds
        lowerBound = random.nextInt((int) values[0] - 1);
        upperBound = values[2] + random.nextInt(MAX_UPPER_BOUND_RANGE) + 1;

        final LongEntity y =
                LongEntity.from(FeatureFactory.newNumericalFeature("y", values[0]), lowerBound, upperBound);
        // similarity to point #2 with new range
        y.setProposedValue(values[1]);
        final double y_similarity1 = y.similarity();
        // similarity to point #3 with new range
        y.setProposedValue(values[2]);
        final double y_similarity2 = y.similarity();

        // relative similarities must stay the same
        assertTrue(x_similarity1 > x_similarity2);
        assertTrue(y_similarity1 > y_similarity2);
    }
}
