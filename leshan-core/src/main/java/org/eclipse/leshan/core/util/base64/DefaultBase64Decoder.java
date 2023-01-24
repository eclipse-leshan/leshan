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

import java.util.Base64;

public class DefaultBase64Decoder implements Base64Decoder {

    public enum DecoderPadding {
        REQUIRED, FORBIDEN, OPTIONAL
    }

    public enum DecoderAlphabet {
        BASE64, BASE64URL, BASE64URL_OR_BASE64
    }

    protected final DecoderPadding padding;
    protected final DecoderAlphabet alphabet;

    public DefaultBase64Decoder(DecoderAlphabet alphabet, DecoderPadding padding) {
        this.padding = padding;
        this.alphabet = alphabet;
    }

    public boolean supportUrlSafeEncoding() {
        return alphabet == DecoderAlphabet.BASE64URL || alphabet == DecoderAlphabet.BASE64URL_OR_BASE64;
    }

    public boolean supportUrlUnSafeEncoding() {
        return alphabet == DecoderAlphabet.BASE64 || alphabet == DecoderAlphabet.BASE64URL_OR_BASE64;
    }

    public boolean requirePadding() {
        return padding == DecoderPadding.REQUIRED;
    }

    public boolean supportPadding() {
        return padding == DecoderPadding.REQUIRED || padding == DecoderPadding.OPTIONAL;
    }

    @Override
    public byte[] decode(String encoded) throws InvalidBase64Exception {
        // String are UTF-8 encoded in Java
        // In UTF-8 some char can be encoded on several bytes.
        // So first ensure there is only 1 byte char in the string.

        byte[] bytesToDecode = encoded.getBytes();
        if (encoded.length() != bytesToDecode.length) {
            throw new InvalidBase64Exception("Base64 String contain non ascii char [%s]", encoded);
        }
        return decode(bytesToDecode);
    }

    @Override
    public byte[] decode(byte[] encoded) throws InvalidBase64Exception {
        validateEncodedData(encoded);
        try {
            return innerDecode(encoded);
        } catch (IllegalArgumentException e) {
            throw new InvalidBase64Exception(e, "Unable to decode Base64");
        }
    }

    protected byte[] innerDecode(byte[] encoded) {
        switch (alphabet) {
        case BASE64:
            return Base64.getDecoder().decode(encoded);
        case BASE64URL:
            return Base64.getUrlDecoder().decode(encoded);
        case BASE64URL_OR_BASE64:
            return seemsUrlSafe(encoded) ? Base64.getUrlDecoder().decode(encoded) : Base64.getDecoder().decode(encoded);
        }
        throw new IllegalStateException("Unexpected alphabet " + alphabet);
    }

    protected boolean seemsUrlSafe(byte[] encoded) {
        for (int i = 0; i < encoded.length; i++) {
            if (encoded[i] == ((byte) '-') || encoded[i] == ((byte) '_')) {
                return true;
            }
        }
        return false;
    }

    protected void validateEncodedData(byte[] encoded) throws InvalidBase64Exception {
        // empty string is valid base64
        if (encoded.length == 0)
            return;
        validatePadding(encoded);
        validateIsCanonical(encoded);
    }

    protected void validatePadding(byte[] encoded) throws InvalidBase64Exception {
        if (padding == DecoderPadding.REQUIRED) {
            if (encoded.length % 4 != 0) {
                throw new InvalidBase64Exception("Missing base64 valid padding [%s]", new String(encoded));
            }
        } else if (padding == DecoderPadding.FORBIDEN) {
            if (encoded[encoded.length - 1] == (byte) '=') {
                throw new InvalidBase64Exception("Unexpected padding in [%s]", new String(encoded));
            }
        }
    }

    protected void validateIsCanonical(byte[] encoded) throws InvalidBase64Exception {
        // We validate that base64 is in canonical form.
        // See :
        // - https://www.rfc-editor.org/rfc/rfc4648#section-3.5
        // - https://www.rfc-editor.org/rfc/rfc4648#section-4

        // find last significant char & final quantum size
        char lastchar;
        int nbCharInFinalQuantum;
        boolean usePadding;
        if (padding == DecoderPadding.OPTIONAL) {
            // Guess if padding is used.
            usePadding = encoded[encoded.length - 1] == (byte) '=';
        } else if (padding == DecoderPadding.REQUIRED) {
            usePadding = true;
        } else if (padding == DecoderPadding.FORBIDEN) {
            usePadding = false;
        } else {
            throw new IllegalStateException(String.format("Not implemented padding mode : %s", padding));
        }

        if (!usePadding) {
            // without padding
            nbCharInFinalQuantum = encoded.length % 4;
            lastchar = (char) encoded[encoded.length - 1];
        } else {
            // with padding
            // Count number of padding char
            int nbPaddingChar = 0;
            for (int i = encoded.length - 1; i >= 0; i--) {
                if (encoded[i] == (byte) '=') {
                    nbPaddingChar++;
                } else {
                    break;
                }
            }
            if (nbPaddingChar == encoded.length) {
                throw new InvalidBase64Exception("Invalid Base64 string [%s]", new String(encoded));
            }

            // Get significant char and final quantum size
            if (nbPaddingChar == 0) {
                // if final quantum size is 24 bits, all bits are significants
                // all base64 char are valid
                return;
            } else if (nbPaddingChar == 1) {
                nbCharInFinalQuantum = 3;
                lastchar = (char) encoded[encoded.length - 1 - nbPaddingChar];
            } else if (nbPaddingChar == 2) {
                nbCharInFinalQuantum = 2;
                lastchar = (char) encoded[encoded.length - 1 - nbPaddingChar];
            } else {
                throw new InvalidBase64Exception("Base64 string is not in canonical form : too many padding char [%s]",
                        new String(encoded));
            }
        }

        // Get all valid chars for canonical form
        char[] validChar;
        if (nbCharInFinalQuantum == 0) {
            // if final quantum size is 24 bits, all bits are significants
            // all base64 char are valid
            return;
        } else if (nbCharInFinalQuantum == 2) {
            // if final quantum size is 8 bits, only the first two bits of the last char are used, 4 others should be 0.
            // So only valid char are :
            // - A(000000), Q(010000), g(100000), W(110000).
            // See : https://en.wikipedia.org/wiki/Base64#Base64_table_from_RFC_4648
            validChar = new char[] { 'A', 'Q', 'g', 'W' };

        } else if (nbCharInFinalQuantum == 3) {
            // if final quantum size is 16 bits, only the first four bits of the last char are used, 2 other should be
            // 0.
            // So only valid char are :
            // - A(000000), Q(010000), g(100000), W(110000),
            // - E(000100), U(010100), k(100100), 0(110100),
            // - I(001000), Y(011000), o(101000), 4(111000)
            // - M(001100), c(011100), s(101100), 8(111100).
            // See : https://en.wikipedia.org/wiki/Base64#Base64_table_from_RFC_4648

            validChar = new char[] { 'A', 'Q', 'g', 'W', 'E', 'U', 'k', '0', 'I', 'Y', 'o', '4', 'M', 'c', 's', '8' };
        } else {
            throw new InvalidBase64Exception("Invlaid number of character is final quantum must be 2 or 3 [%s]",
                    new String(encoded));
        }

        // Ensure last char is valid for a canonical form.
        for (int i = 0; i < validChar.length; i++) {
            if (lastchar == validChar[i]) {
                return;
            }
        }
        throw new InvalidBase64Exception("Base64 string %s is not in canonical form.", new String(encoded));
    }

    @Override
    public String toString() {
        return String.format("%s Decoder - Padding %s", alphabet, padding);
    }
}
