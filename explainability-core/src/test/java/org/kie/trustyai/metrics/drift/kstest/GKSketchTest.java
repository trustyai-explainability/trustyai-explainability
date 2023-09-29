package org.kie.trustyai.metrics.drift.kstest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GKSketchTest {
    public List<Integer> getRandomIntList(int size, long seed) {
        List<Integer> sampleInts = IntStream.range(0, size).boxed().collect(Collectors.toList());
        Collections.shuffle(sampleInts, new Random(seed));
        return sampleInts;
    }

    public List<Double> getRandomDoubleList(int size, long seed) {
        Random rnd = new Random(seed);
        List<Double> sampleList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            sampleList.add(rnd.nextDouble());
        }
        return sampleList;
    }

    public <T extends Number & Comparable<T>> boolean isEpsQuantile(List<T> data, double prob, double eps, T approxQuantile) {
        // Check if an approx quantile meets the definition of epsilon error boundry
        int N = data.size();
        ArrayList<T> sortedList = new ArrayList<>(data);
        Collections.sort(sortedList);
        int trueRank = sortedList.indexOf(approxQuantile) + 1;
        int lowerRank = (int) Math.floor((prob - eps) * N);
        int upperRank = (int) Math.ceil((prob + eps) * N);
        boolean isEpsQ = (trueRank >= lowerRank) && (trueRank <= upperRank);

        return isEpsQ;

    }

    @Test
    void testGKSketchInit() {
        long seed = new Random().nextLong();
        List<Integer> intSeq = getRandomIntList(100, seed);
        double eps = 0.1;
        GKSketch sketch = new GKSketch(eps);
        for (int v : intSeq) {
            sketch.insert(v);
        }
        assertEquals(sketch.getNumx(), intSeq.size());
        assertEquals(sketch.getXmin(), 0);
        assertEquals(sketch.getXmax(), 99);

    }

    @Test
    void testGKSketchInsert() {
        List<Integer> intSeq = List.of(12, 10, 11, 10, 1, 10, 11, 9, 6, 7, 8, 11, 4, 5, 2, 3);
        double eps = 0.25;
        GKSketch sketch = new GKSketch(eps);
        for (int i = 0; i < 4; i++) {
            sketch.insert(intSeq.get(i));
        }
        assertEquals(sketch.getSummary().size(), 4);
        assertEquals(sketch.getXmin(), 10);
        assertEquals(sketch.getXmax(), 12);
        assertEquals(sketch.getSummary().get(2), Triple.of(11.0, 1, 0));
        sketch.insert(intSeq.get(4));
        sketch.insert(intSeq.get(5));
        sketch.insert(intSeq.get(6)); //compress before 7th item inserted
        assertEquals(sketch.getSummary().size(), 5);
        assertEquals(sketch.getSummary().get(4), Triple.of(12.0, 2, 0));
        sketch.insert(intSeq.get(7));
        sketch.insert(intSeq.get(8)); //compress before 9th item inserted and add 9th item
        assertEquals(sketch.getSummary().size(), 5);
        assertEquals(sketch.getSummary().get(0), Triple.of(1.0, 1, 0));
        assertEquals(sketch.getSummary().get(4), Triple.of(12.0, 3, 0));
        assertEquals(sketch.getSummary().get(3), Triple.of(10.0, 3, 0));
    }

    @Test
    void testGKSketchQuantilesCompliesEpsBounds() {
        long seed = new Random().nextLong();
        List<Integer> intSeq = getRandomIntList(100, seed);
        double eps = 0.1;
        GKSketch sketch = new GKSketch(eps);
        for (int v : intSeq) {
            sketch.insert(v);
        }
        assertEquals(sketch.getNumx(), intSeq.size());
        double[] probs = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };
        for (double p : probs) {
            double approxQ = sketch.quantile(p);
            assertTrue(isEpsQuantile(intSeq, p, eps, Integer.valueOf((int) approxQ)));
        }
    }

    @Test
    void testGKSketchQuantilesCompliesEpsBounds500K() {
        long seed = new Random().nextInt();
        List<Integer> intSeq = getRandomIntList(500000, seed);
        double eps = 0.01;
        GKSketch sketch = new GKSketch(eps);
        for (int v : intSeq) {
            sketch.insert(v);
        }
        assertEquals(sketch.getNumx(), intSeq.size());
        double[] probs = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };
        for (double p : probs) {
            double approxQ = sketch.quantile(p);
            assertTrue(isEpsQuantile(intSeq, p, eps, Integer.valueOf((int) approxQ)));
        }
    }

    @Test
    void testGKSketchQuantilesCompliesEpsBounds1KDoubles() {
        long seed = new Random().nextLong();
        List<Double> doubleSeq = getRandomDoubleList(1000, seed);
        double eps = 0.1;
        GKSketch sketch = new GKSketch(eps);
        for (double v : doubleSeq) {
            sketch.insert(v);
        }
        assertEquals(sketch.getNumx(), doubleSeq.size());
        double[] probs = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };
        for (double p : probs) {
            double approxQ = sketch.quantile(p);
            assertTrue(isEpsQuantile(doubleSeq, p, eps, Double.valueOf(approxQ)));
        }
    }

}
