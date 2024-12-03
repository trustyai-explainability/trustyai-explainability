package org.kie.trustyai.connectors.kserve.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawValueExtractorTest {

    @Test
    void rawBoolean() {
        final List<Boolean> booleanList = List.of(true, false, true, false, true, false, true, false);
        final ByteString byteString = RawValueExtractor.fromBoolean(booleanList);
        final List<Boolean> booleanList2 = RawValueExtractor.toBoolean(byteString);
        assertEquals(booleanList, booleanList2);
    }

    @Test
    void rawInteger() {
        final List<Integer> data = new Random(0).ints(20, 0, 100).boxed().collect(Collectors.toList());
        final ByteString byteString = RawValueExtractor.fromInteger(data);
        final List<Integer> converted = RawValueExtractor.toInteger(byteString);
        assertEquals(data, converted);
    }

    @Test
    void rawLong() {
        final List<Long> data = new Random(0).longs(20, 0, 100).boxed().collect(Collectors.toList());
        final ByteString byteString = RawValueExtractor.fromLong(data);
        final List<Long> converted = RawValueExtractor.toLong(byteString);
        assertEquals(data, converted);
    }

    @Test
    void rawFloat() {
        Random random = new Random(0);
        List<Float> data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            data.add(random.nextFloat());
        }

        final ByteString byteString = RawValueExtractor.fromFloat(data);
        final List<Float> converted = RawValueExtractor.toFloat(byteString);
        assertEquals(data, converted);
    }

    @Test
    void rawDouble() {
        Random random = new Random(0);
        List<Double> data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            data.add(random.nextDouble());
        }

        final ByteString byteString = RawValueExtractor.fromDouble(data);
        final List<Double> converted = RawValueExtractor.toDouble(byteString);
        assertEquals(data, converted);
    }

}