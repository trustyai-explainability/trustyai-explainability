package org.kie.trustyai.connectors.kserve.v2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;

public class RawConverter {

    public static List<Boolean> toBoolean(ByteString raw) {
        byte[] bytes = raw.toByteArray();
        final List<Boolean> booleanList = new ArrayList<>();
        for (byte b : bytes) {
            for (int i = 7; i >= 0; --i) {
                booleanList.add((b & (1 << i)) != 0);
            }
        }
        return booleanList;
    }

    public static List<Integer> toInteger(ByteString raw) {
        List<Integer> integerList = new ArrayList<>();
        ByteBuffer byteBuffer = ByteBuffer.wrap(raw.toByteArray());
        while (byteBuffer.hasRemaining()) {
            integerList.add(byteBuffer.getInt());
        }
        return integerList;
    }

    public static List<Long> toLong(ByteString raw) {
        List<Long> longList = new ArrayList<>();
        ByteBuffer byteBuffer = ByteBuffer.wrap(raw.toByteArray());
        while (byteBuffer.hasRemaining()) {
            longList.add(byteBuffer.getLong());
        }
        return longList;
    }

    public static List<Float> toFloat(ByteString raw) {
        List<Float> floatList = new ArrayList<>();
        ByteBuffer byteBuffer = ByteBuffer.wrap(raw.toByteArray());
        while (byteBuffer.hasRemaining()) {
            floatList.add(byteBuffer.getFloat());
        }
        return floatList;
    }

    public static List<Double> toDouble(ByteString raw) {
        List<Double> doubleList = new ArrayList<>();
        ByteBuffer byteBuffer = ByteBuffer.wrap(raw.toByteArray());
        while (byteBuffer.hasRemaining()) {
            doubleList.add(byteBuffer.getDouble());
        }
        return doubleList;
    }
}
