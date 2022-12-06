/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Adam Serodzinski, Jarosław Legierski
 *     Orange Polska S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.util.base64;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DefaultBase64Decoder implements Base64Decoder {
    private final boolean withoutPadding;
    private final Base64.Decoder decoder;

    public DefaultBase64Decoder(boolean urlSafe, boolean withoutPadding) {
        this.withoutPadding = withoutPadding;
        this.decoder = urlSafe ? Base64.getUrlDecoder() : Base64.getDecoder();
    }

    @Override
    public byte[] decode(String encoded) {
        return decode(encoded.getBytes(StandardCharsets.ISO_8859_1)).getBytes(StandardCharsets.ISO_8859_1);
    }

    @Override
    public String decode(byte[] encoded) {
        if (!this.withoutPadding) {
            validateEncodedData(new String(encoded));
        }
        byte[] decoded = decoder.decode(encoded);
        return new String(decoded, StandardCharsets.ISO_8859_1);
    }

    private void validateEncodedData(String encoded) {
        if (encoded.length() % 4 != 0) {
            throw new IllegalArgumentException(String.format("Base64 string %s is missing valid padding.", encoded));
        }
    }
}
