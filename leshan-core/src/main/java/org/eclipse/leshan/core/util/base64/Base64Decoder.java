package org.eclipse.leshan.core.util.base64;

public interface Base64Decoder {
    String decode(String encoded);

    String decode(byte[] encoded);
}
