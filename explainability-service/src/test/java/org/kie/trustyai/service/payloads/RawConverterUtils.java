package org.kie.trustyai.service.payloads;

import java.nio.ByteBuffer;
import java.util.List;

import com.google.protobuf.ByteString;

public class RawConverterUtils {
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
