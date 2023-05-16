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

    public static ByteString fromBoolean(List<Boolean> booleanList) {
        byte[] bytes = new byte[(booleanList.size() + 7) / 8];
        for (int i = 0; i < booleanList.size(); i++) {
            if (booleanList.get(i)) {
                bytes[i / 8] |= 1 << (7 - i % 8);
            }
        }
        return ByteString.copyFrom(bytes);
    }

    public static ByteString fromInteger(List<Integer> integerList) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(integerList.size() * 4);
        for (int value : integerList) {
            byteBuffer.putInt(value);
        }
        return ByteString.copyFrom(byteBuffer.array());
    }

    public static ByteString fromLong(List<Long> longList) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(longList.size() * 8);
        for (long value : longList) {
            byteBuffer.putLong(value);
        }
        return ByteString.copyFrom(byteBuffer.array());
    }

    public static ByteString fromFloat(List<Float> floatList) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(floatList.size() * 4);
        for (float value : floatList) {
            byteBuffer.putFloat(value);
        }
        return ByteString.copyFrom(byteBuffer.array());
    }

    public static ByteString fromDouble(List<Double> doubleList) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(doubleList.size() * 8);
        for (double value : doubleList) {
            byteBuffer.putDouble(value);
        }
        return ByteString.copyFrom(byteBuffer.array());
    }
}
