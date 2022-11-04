package org.eclipse.leshan.core.util.base64;

import java.util.Base64;

public class DefaultBase64Decoder implements Base64Decoder {
    private final boolean urlSafe;
    private final boolean withoutPadding;
    public DefaultBase64Decoder(boolean urlSafe, boolean withoutPadding) {
        this.urlSafe = urlSafe;
        this.withoutPadding = withoutPadding;
    }

    @Override public String decode(String encoded) {
        if (!this.withoutPadding)
        {
            validateEncodedData(encoded);
        }
        Base64.Decoder decoder = urlSafe ? Base64.getUrlDecoder() : Base64.getDecoder();
        byte[] decoded = decoder.decode(encoded);
        return new String(decoded);
    }

    @Override public String decode(byte[] encoded) {
        if (!this.withoutPadding)
        {
            validateEncodedData(new String(encoded));
        }

        Base64.Decoder decoder = urlSafe ? Base64.getUrlDecoder() : Base64.getDecoder();
        byte[] decoded = decoder.decode(encoded);
        return new String(decoded);
    }

    private void validateEncodedData(String encoded) {
        if (encoded.length() % 4 != 0) {
            throw new IllegalArgumentException(String.format("Base64 string %s is missing valid padding.", encoded));
        }
    }
}
