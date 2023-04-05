package org.kie.trustyai.connectors.kserve.v2;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.ArrayUtils;
import org.kie.trustyai.explainability.model.Value;

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

    static int int32Parser(ByteString token){
        byte[] byteArray = token.toByteArray();
        ArrayUtils.reverse(byteArray);
        return ByteBuffer.wrap(byteArray).getInt();
    }

    static double fp64Parser(ByteString token){
        byte[] byteArray = token.toByteArray();
        ArrayUtils.reverse(byteArray);
        return ByteBuffer.wrap(byteArray).getDouble();
    }

    static float fp32Parser(ByteString token){
        byte[] byteArray = token.toByteArray();
        ArrayUtils.reverse(byteArray);
        return ByteBuffer.wrap(byteArray).getFloat();
    }

}
