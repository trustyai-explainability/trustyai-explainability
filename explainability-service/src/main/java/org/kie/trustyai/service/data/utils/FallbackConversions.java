package org.kie.trustyai.service.data.utils;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class FallbackConversions {
    private FallbackConversions() {
        // utility class
    }

    static long int64Parser(ByteString token){
        byte[] byteArray = token.toByteArray();
        ArrayUtils.reverse(byteArray);
        return ByteBuffer.wrap(byteArray).getLong();
    }


}
