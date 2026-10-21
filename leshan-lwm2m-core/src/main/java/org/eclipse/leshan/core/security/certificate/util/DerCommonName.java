/*******************************************************************************
 * Copyright (c) 2026    Sierra Wireless and others.
 *
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.security.certificate.util;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.security.auth.x500.X500Principal;

/**
 * Extracts the Common Name (CN) from an X.500 Name by reading its DER encoding directly.
 * <p>
 * No DN string is ever produced or parsed, so there is nothing to escape or quote and the result does not depend on how
 * any JVM renders a DN. Uses only Java 8 / Android API 26 classes (no javax.naming).
 * <p>
 * This is NOT a full DER validator or an implementation of one specification. It walks the structure of a Name (X.501 /
 * RFC 5280 Appendix A.1) on top of X.690 DER framing, and enforces:
 * <ul>
 * <li>DER framing: definite lengths in minimal form, no trailing bytes, single-byte tags only (X.690 section 10.1)</li>
 * <li>Name structure: SEQUENCE of non-empty SETs of SEQUENCE { OID, value }</li>
 * <li>CN string type is one of RFC 5280's DirectoryString choices that we support: UTF8String, PrintableString,
 * BMPString, UniversalString. TeletexString (allowed by the RFC) is rejected rather than guessed.</li>
 * <li>BMPString must be UCS-2 and UniversalString UCS-4: no surrogate code points, nothing above U+10FFFF. Strings must
 * decode without malformed input.</li>
 * <li>Local policy, not from any specification: exactly one CN in the whole Name, non-empty, no ISO control
 * characters</li>
 * </ul>
 * Deliberately NOT checked: the character repertoire of PrintableString (real certificates put '*' in it), SET OF
 * ordering (X.690 section 11.6), canonical encoding of OIDs, and the RFC 5280 upper bound of 64 characters for a CN.
 * Not done either: Unicode normalisation / confusable detection. Two CNs that look identical but differ in code points
 * are different identities.
 * <p>
 * All violations throw {@link IllegalStateException} (fail closed).
 */
final class DerCommonName {

    private static final byte[] OID_CN = { 0x55, 0x04, 0x03 }; // 2.5.4.3
    private static final int TAG_SEQUENCE = 0x30;
    private static final int TAG_SET = 0x31;
    private static final int TAG_OID = 0x06;
    private static final int TAG_UTF8_STRING = 0x0c;
    private static final int TAG_PRINTABLE_STRING = 0x13;
    private static final int TAG_UNIVERSAL_STRING = 0x1c;
    private static final int TAG_BMP_STRING = 0x1e;
    private static final int ANY_TAG = -1;
    /** Largest number of length octets after the first one that we accept (our own limit, narrower than X.690). */
    private static final int MAX_LENGTH_OCTETS = 3;

    private DerCommonName() {
    }

    static String extractCN(X500Principal subject) {
        if (subject == null) {
            throw fail("principal is null");
        }
        return extractCN(subject.getEncoded());
    }

    static String extractCN(byte[] name) {
        if (name == null) {
            throw fail("name is null");
        }
        // Name ::= SEQUENCE OF RelativeDistinguishedName
        DerElement nameElement = DerElement.read(name, 0, name.length, TAG_SEQUENCE, "Name");
        if (nameElement.valueEnd != name.length) {
            throw fail("trailing data after Name");
        }
        return validateCommonName(findCommonNameInName(nameElement));
    }

    /**
     * @return the only CN of the name, or null if there is none
     * @throws IllegalStateException if the name has more than one CN or is malformed
     */
    private static String findCommonNameInName(DerElement nameElement) {
        String cn = null;
        int rdnPosition = nameElement.valueStart;
        while (rdnPosition < nameElement.valueEnd) {
            // RelativeDistinguishedName ::= SET SIZE (1..MAX) OF AttributeTypeAndValue
            DerElement rdn = nameElement.readChild(rdnPosition, TAG_SET, "RDN");
            cn = findCommonNameInRdn(rdn, cn);
            rdnPosition = rdn.valueEnd;
        }
        return cn;
    }

    /**
     * @param cnFoundSoFar the CN found in the previous RDNs of the name, or null
     * @return the CN of this RDN, or <code>cnFoundSoFar</code> if this RDN has none
     * @throws IllegalStateException if this RDN has a CN while <code>cnFoundSoFar</code> is not null, or if it is
     *         malformed
     */
    @SuppressWarnings("java:S125")
    private static String findCommonNameInRdn(DerElement rdn, String cnFoundSoFar) {
        if (rdn.hasEmptyValue()) {
            throw fail("empty RDN");
        }
        String cn = cnFoundSoFar;
        int attributePosition = rdn.valueStart;
        while (attributePosition < rdn.valueEnd) {
            // AttributeTypeAndValue ::= SEQUENCE { type OID, value ANY }
            DerElement attribute = rdn.readChild(attributePosition, TAG_SEQUENCE, "AttributeTypeAndValue");
            DerElement type = attribute.readChild(attribute.valueStart, TAG_OID, "attribute type");
            DerElement value = attribute.readChild(type.valueEnd, ANY_TAG, "attribute value");
            if (value.valueEnd != attribute.valueEnd) {
                throw fail("trailing data in AttributeTypeAndValue");
            }
            if (type.valueEquals(OID_CN)) {
                if (cn != null) {
                    throw fail("more than one CN in name");
                }
                cn = decodeString(value);
            }
            attributePosition = attribute.valueEnd;
        }
        return cn;
    }

    /**
     * Local policy on top of the encoding rules: a usable CN exists, is not empty and has no control character.
     */
    private static String validateCommonName(String cn) {
        if (cn == null) {
            throw fail("no CN in name");
        }
        if (cn.isEmpty()) {
            throw fail("empty CN");
        }
        int i = 0;
        while (i < cn.length()) {
            int codePoint = cn.codePointAt(i);
            if (Character.isISOControl(codePoint)) {
                throw fail("CN contains a control character");
            }
            i += Character.charCount(codePoint);
        }
        return cn;
    }

    private static String decodeString(DerElement value) {
        byte[] raw = value.valueBytes();
        Charset charset;
        switch (value.tag) {
        case TAG_UTF8_STRING:
            charset = StandardCharsets.UTF_8;
            break;
        case TAG_PRINTABLE_STRING:
            charset = StandardCharsets.US_ASCII;
            break;
        case TAG_BMP_STRING:
            checkBmpString(raw);
            charset = StandardCharsets.UTF_16BE;
            break;
        case TAG_UNIVERSAL_STRING:
            checkUniversalString(raw);
            charset = utf32BigEndian();
            break;
        default:
            throw fail(String.format("unsupported CN string type 0x%02x", value.tag));
        }
        try {
            return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(raw)).toString();
        } catch (CharacterCodingException e) {
            throw fail("CN is not a valid " + charset.name() + " string");
        }
    }

    /**
     * BMPString is UCS-2 (X.680): every 16-bit unit is one character, and surrogates (D800-DFFF) are not characters.
     * UTF-16 would accept a surrogate pair as one supplementary character, so the units are checked here.
     */
    private static void checkBmpString(byte[] raw) {
        if (raw.length % 2 != 0) {
            throw fail("BMPString with odd length");
        }
        for (int i = 0; i < raw.length; i += 2) {
            int unit = ((raw[i] & 0xff) << 8) | (raw[i + 1] & 0xff);
            if (isSurrogate(unit)) {
                throw fail("BMPString contains a surrogate code unit");
            }
        }
    }

    /**
     * UniversalString is UCS-4 (X.680). Java's UTF-32 decoder happily returns a lone surrogate, so the 32-bit units are
     * checked here.
     */
    private static void checkUniversalString(byte[] raw) {
        if (raw.length % 4 != 0) {
            throw fail("UniversalString length not a multiple of 4");
        }
        for (int i = 0; i < raw.length; i += 4) {
            long unit = ((raw[i] & 0xffL) << 24) | ((raw[i + 1] & 0xffL) << 16) | ((raw[i + 2] & 0xffL) << 8)
                    | (raw[i + 3] & 0xffL);
            if (unit > 0x10FFFFL || isSurrogate(unit)) {
                throw fail("UniversalString contains an invalid code point");
            }
        }
    }

    private static boolean isSurrogate(long codeUnit) {
        return codeUnit >= 0xD800L && codeUnit <= 0xDFFFL;
    }

    private static Charset utf32BigEndian() {
        try {
            return Charset.forName("UTF-32BE");
        } catch (RuntimeException e) {
            throw fail("UTF-32BE not supported on this platform");
        }
    }

    /**
     * One DER element (identifier octet, length octets, value octets) located inside a byte array. It keeps positions
     * into that array, not a copy of the data. Reading is strict: see the class comment for what is enforced.
     * <p>
     * Named "DerElement" on purpose: org.eclipse.leshan.core.tlv.Tlv is the unrelated LwM2M TLV format.
     */
    private static final class DerElement {

        private final byte[] data;
        private final int tag;
        /** Position of the first value octet. */
        private final int valueStart;
        /** Position after the last value octet, which is also where the next sibling element starts. */
        private final int valueEnd;

        private DerElement(byte[] data, int tag, int valueStart, int valueEnd) {
            this.data = data;
            this.tag = tag;
            this.valueStart = valueStart;
            this.valueEnd = valueEnd;
        }

        /**
         * Reads the element that starts at <code>position</code> and must end before <code>limit</code>.
         *
         * @param expectedTag tag the element must have, or {@link #ANY_TAG}
         * @param what name used in error messages
         * @throws IllegalStateException if the bytes are not a well formed DER element of the expected tag
         */
        static DerElement read(byte[] data, int position, int limit, int expectedTag, String what) {
            if (position >= limit) {
                throw fail("truncated " + what);
            }
            int tag = data[position] & 0xff;
            checkTag(tag, expectedTag, what);

            int lengthPosition = position + 1;
            if (lengthPosition >= limit) {
                throw fail("truncated length of " + what);
            }
            int firstLengthOctet = data[lengthPosition] & 0xff;
            int extraLengthOctets = countExtraLengthOctets(firstLengthOctet, what);
            int valueStart = lengthPosition + 1 + extraLengthOctets;
            if (valueStart > limit) {
                throw fail("invalid length encoding of " + what);
            }
            int length = extraLengthOctets == 0 ? firstLengthOctet
                    : readLongFormLength(data, lengthPosition + 1, extraLengthOctets, what);
            if (length > limit - valueStart) {
                throw fail("length of " + what + " exceeds available data");
            }
            return new DerElement(data, tag, valueStart, valueStart + length);
        }

        private static void checkTag(int tag, int expectedTag, String what) {
            if ((tag & 0x1f) == 0x1f) {
                throw fail("high-tag-number form not supported (" + what + ")");
            }
            if (expectedTag >= 0 && tag != expectedTag) {
                throw fail(String.format("unexpected tag 0x%02x for %s", tag, what));
            }
        }

        /**
         * @return how many length octets follow the first one: 0 in short form, 1 to {@link #MAX_LENGTH_OCTETS} in long
         *         form
         */
        private static int countExtraLengthOctets(int firstLengthOctet, String what) {
            if (firstLengthOctet < 0x80) {
                return 0; // short form: this octet is the length itself
            }
            if (firstLengthOctet == 0x80) {
                throw fail("indefinite length is not allowed in DER (" + what + ")");
            }
            int extraOctets = firstLengthOctet & 0x7f;
            if (extraOctets > MAX_LENGTH_OCTETS) {
                throw fail("invalid length encoding of " + what);
            }
            return extraOctets;
        }

        private static int readLongFormLength(byte[] data, int position, int octets, String what) {
            int length = 0;
            for (int i = 0; i < octets; i++) {
                length = (length << 8) | (data[position + i] & 0xff);
            }
            // DER: long form only when needed, and without leading zero bytes
            if (length < 0x80 || (length >> (8 * (octets - 1))) == 0) {
                throw fail("non-minimal length encoding of " + what);
            }
            return length;
        }

        /** Reads the element that starts at <code>position</code> inside the value of this element. */
        DerElement readChild(int position, int expectedTag, String what) {
            return read(data, position, valueEnd, expectedTag, what);
        }

        boolean hasEmptyValue() {
            return valueStart == valueEnd;
        }

        boolean valueEquals(byte[] expected) {
            if (valueEnd - valueStart != expected.length) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if (data[valueStart + i] != expected[i]) {
                    return false;
                }
            }
            return true;
        }

        byte[] valueBytes() {
            return Arrays.copyOfRange(data, valueStart, valueEnd);
        }
    }

    private static IllegalStateException fail(String reason) {
        return new IllegalStateException("Unable to extract common name : " + reason);
    }
}
