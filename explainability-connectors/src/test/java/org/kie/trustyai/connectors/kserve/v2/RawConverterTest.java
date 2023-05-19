package org.kie.trustyai.connectors.kserve.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawConverterTest {

    @Test
    void rawBoolean() {
        final List<Boolean> booleanList = List.of(true, false, true, false, true, false, true, false);
        final ByteString byteString = RawConverter.fromBoolean(booleanList);
        final List<Boolean> booleanList2 = RawConverter.toBoolean(byteString);
        assertEquals(booleanList, booleanList2);
    }

    @Test
    void rawInteger() {
        final List<Integer> data = new Random().ints(20, 0, 100).boxed().collect(Collectors.toList());
        final ByteString byteString = RawConverter.fromInteger(data);
        final List<Integer> converted = RawConverter.toInteger(byteString);
        assertEquals(data, converted);
    }

    @Test
    void rawLong() {
        final List<Long> data = new Random().longs(20, 0, 100).boxed().collect(Collectors.toList());
        final ByteString byteString = RawConverter.fromLong(data);
        final List<Long> converted = RawConverter.toLong(byteString);
        assertEquals(data, converted);
    }

    @Test
    void rawFloat() {
        Random random = new Random();
        List<Float> data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            data.add(random.nextFloat());
        }

        final ByteString byteString = RawConverter.fromFloat(data);
        final List<Float> converted = RawConverter.toFloat(byteString);
        assertEquals(data, converted);
    }

    @Test
    void rawDouble() {
        Random random = new Random();
        List<Double> data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            data.add(random.nextDouble());
        }

        final ByteString byteString = RawConverter.fromDouble(data);
        final List<Double> converted = RawConverter.toDouble(byteString);
        assertEquals(data, converted);
    }

}