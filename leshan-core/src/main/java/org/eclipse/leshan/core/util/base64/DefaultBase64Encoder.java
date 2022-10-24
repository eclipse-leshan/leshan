package org.eclipse.leshan.core.util.base64;

import java.util.Base64;

public class DefaultBase64Encoder implements Base64Encoder {
    private final boolean urlSafe;

    public DefaultBase64Encoder(boolean urlSafe) {
        this.urlSafe = urlSafe;
    }

    @Override public String encode(byte[] dataToEncode) {
        Base64.Encoder encoder = urlSafe ? Base64.getUrlEncoder() : Base64.getEncoder();
        return encoder.encodeToString(dataToEncode);
    }
}
