package org.eclipse.leshan.core.util.base64;

public interface Base64Decoder {
    // should probably be byte[] decode(String encoded); ?
    String decode(String encoded);

    String decode(byte[] encoded);
}
