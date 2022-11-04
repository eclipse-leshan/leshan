package org.eclipse.leshan.core.util.base64;

import java.util.Base64;

public class DefaultBase64Encoder implements Base64Encoder {
    private final boolean urlSafe;
    private final boolean withoutPadding;
    public DefaultBase64Encoder(boolean urlSafe, boolean withoutPadding ) {
        this.urlSafe = urlSafe;
        this.withoutPadding = withoutPadding;
    }

    @Override public String encode(byte[] dataToEncode) {
        Base64.Encoder encoder;
        if (this.withoutPadding) {
            encoder = urlSafe ? Base64.getUrlEncoder().withoutPadding() : Base64.getEncoder().withoutPadding();
        }
        else
            encoder = urlSafe ? Base64.getUrlEncoder() : Base64.getEncoder();

        return encoder.encodeToString(dataToEncode);
    }
}
