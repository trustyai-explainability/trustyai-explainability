package org.kie.trustyai.connectors.kserve.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.kie.trustyai.connectors.kserve.v2.RawConverterUtils.*;

class RawConverterTest {

    @Test
    void rawBoolean() {
        final List<Boolean> booleanList = List.of(true, false, true, false, true, false, true, false);
        final ByteString byteString = fromBoolean(booleanList);
        final List<Boolean> booleanList2 = RawConverter.toBoolean(byteString);
        System.out.println(booleanList2);
        assertEquals(booleanList, booleanList2);
    }

    @Test
    void rawInteger() {
        final List<Integer> data = new Random().ints(20, 0, 100).boxed().collect(Collectors.toList());
        final ByteString byteString = fromInteger(data);
        final List<Integer> converted = RawConverter.toInteger(byteString);
        System.out.println(converted);
        assertEquals(data, converted);
    }

    @Test
    void rawLong() {
        final List<Long> data = new Random().longs(20, 0, 100).boxed().collect(Collectors.toList());
        final ByteString byteString = fromLong(data);
        final List<Long> converted = RawConverter.toLong(byteString);
        System.out.println(converted);
        assertEquals(data, converted);
    }

    @Test
    void rawFloat() {
        Random random = new Random();
        List<Float> data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            data.add(random.nextFloat());
        }

        final ByteString byteString = fromFloat(data);
        final List<Float> converted = RawConverter.toFloat(byteString);
        System.out.println(converted);
        assertEquals(data, converted);
    }

    @Test
    void rawDouble() {
        Random random = new Random();
        List<Double> data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            data.add(random.nextDouble());
        }

        final ByteString byteString = fromDouble(data);
        final List<Double> converted = RawConverter.toDouble(byteString);
        System.out.println(converted);
        assertEquals(data, converted);
    }

}