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
 *     Bartosz Stolarczyk
 *     Orange Polska S.A. - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.core.util.base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder.DecoderAlphabet;
import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder.DecoderPadding;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DefaultBase64DecoderTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("base64Decoder")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllDecoder {
    }

    static Stream<DefaultBase64Decoder> base64Decoder() {
        return Stream.of(//
                new DefaultBase64Decoder(DecoderAlphabet.BASE64, DecoderPadding.REQUIRED), //
                new DefaultBase64Decoder(DecoderAlphabet.BASE64, DecoderPadding.FORBIDEN), //
                new DefaultBase64Decoder(DecoderAlphabet.BASE64, DecoderPadding.OPTIONAL), //
                new DefaultBase64Decoder(DecoderAlphabet.BASE64URL, DecoderPadding.REQUIRED), //
                new DefaultBase64Decoder(DecoderAlphabet.BASE64URL, DecoderPadding.FORBIDEN), //
                new DefaultBase64Decoder(DecoderAlphabet.BASE64URL, DecoderPadding.OPTIONAL), //
                new DefaultBase64Decoder(DecoderAlphabet.BASE64URL_OR_BASE64, DecoderPadding.REQUIRED), //
                new DefaultBase64Decoder(DecoderAlphabet.BASE64URL_OR_BASE64, DecoderPadding.FORBIDEN), //
                new DefaultBase64Decoder(DecoderAlphabet.BASE64URL_OR_BASE64, DecoderPadding.OPTIONAL) //
        );
    }

    @TestAllDecoder
    public void decode_valid_base64(DefaultBase64Decoder decoder) throws InvalidBase64Exception {
        // Test value from : https://www.rfc-editor.org/rfc/rfc4648#section-10

        // given
        String[] valuesToDecode = new String[] { "", "Zg==", "Zm8=", "Zm9v", "Zm9vYg==", "Zm9vYmE=", "Zm9vYmFy" };
        String[] expectedDecodedValue = new String[] { "", "f", "fo", "foo", "foob", "fooba", "foobar" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            // when
            String valueToDecode = valuesToDecode[i];
            if (!decoder.supportPadding()) {
                // remove padding
                valueToDecode = valueToDecode.replaceAll("=", "");
            }
            byte[] decodedValue = decoder.decode(valueToDecode);

            // then
            assertEquals(expectedDecodedValue[i], new String(decodedValue));
        }
    }

    @TestAllDecoder
    public void reject_string_containing_non_ascii_char(DefaultBase64Decoder decoder) {

        // given
        String[] valuesToDecode = new String[] { "€", "ù==", "€=", "Z€", "€ùg==", "€€E=", "€€ù" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode;
            if (!decoder.supportPadding()) {
                // remove padding
                valueToDecode = valuesToDecode[i].replaceAll("=", "");
            } else {
                valueToDecode = valuesToDecode[i];
            }

            // then
            assertThrowsExactly(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @TestAllDecoder
    public void reject_string_containing_illegal_base64_char(DefaultBase64Decoder decoder) {
        // given
        String[] valuesToDecode = new String[] { "Z!==", "Z,8=", "Zm9>", ">m9vYg==", "Zm<vYmE=", "Zm9%YmFy" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode;
            if (!decoder.supportPadding()) {
                // remove padding
                valueToDecode = valuesToDecode[i].replaceAll("=", "");
            } else {
                valueToDecode = valuesToDecode[i];
            }

            // then
            assertThrowsExactly(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @TestAllDecoder
    public void test_url_unsafe_char(DefaultBase64Decoder decoder) throws InvalidBase64Exception {
        // given
        String[] valuesToDecode = new String[] { "A+BB", "+g==", "/m8=", "Z/9v", "Z++vYg==", "Zm/v+mE=", "Zm/v+mFy" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode;
            if (!decoder.supportPadding()) {
                // remove padding
                valueToDecode = valuesToDecode[i].replaceAll("=", "");
            } else {
                valueToDecode = valuesToDecode[i];
            }

            // then
            if (!decoder.supportUrlUnSafeEncoding()) {
                assertThrowsExactly(InvalidBase64Exception.class, () -> {
                    decoder.decode(valueToDecode);
                });
            } else {
                decoder.decode(valueToDecode);
            }
        }
    }

    @TestAllDecoder
    public void test_url_safe_char(DefaultBase64Decoder decoder) throws InvalidBase64Exception {
        // given
        String[] valuesToDecode = new String[] { "A-BB", "-g==", "_m8=", "Z_9v", "Z--vYg==", "Zm_v-mE=", "Zm_v-mFy" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode;
            if (!decoder.supportPadding()) {
                // remove padding
                valueToDecode = valuesToDecode[i].replaceAll("=", "");
            } else {
                valueToDecode = valuesToDecode[i];
            }

            // then
            if (!decoder.supportUrlSafeEncoding()) {
                assertThrowsExactly(InvalidBase64Exception.class, () -> {
                    decoder.decode(valueToDecode);
                });
            } else {
                decoder.decode(valueToDecode);
            }
        }
    }

    @TestAllDecoder
    public void test_without_padding(DefaultBase64Decoder decoder) throws InvalidBase64Exception {
        // given
        String[] valuesToDecode = new String[] { "ABA", "Zg", "Zm8", "Zm9vYg", "Zm9vYmE" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];
            // then
            if (decoder.requirePadding()) {
                assertThrowsExactly(InvalidBase64Exception.class, () -> {
                    decoder.decode(valueToDecode);
                });
            } else {
                decoder.decode(valueToDecode);
            }
        }
    }

    @TestAllDecoder
    public void reject_if_not_canonical_with_padding(DefaultBase64Decoder decoder) {
        // given
        String[] valuesToDecode = new String[] { "ABC=", "Zh==", "Zm9=", "Zm9vYi==", "Zm9vYmF=" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            assertThrowsExactly(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @TestAllDecoder
    public void reject_if_not_canonical_without_padding(DefaultBase64Decoder decoder) {
        // given
        String[] valuesToDecode = new String[] { "ABC", "Zh", "Zm9", "Zm9vYi", "Zm9vYmF" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            assertThrowsExactly(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @TestAllDecoder
    public void reject_too_many_padding(DefaultBase64Decoder decoder) {
        // given
        String[] valuesToDecode = new String[] { "Zg===", "Zm8==", "Zm9v=", "Zm9vYg====", "Zm9vYmE===",
                "Zm9vYmFy====" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            assertThrowsExactly(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @TestAllDecoder
    public void reject_invalid_number_of_char(DefaultBase64Decoder decoder) {
        // given
        String[] valuesToDecode = new String[] { "Z", "Zm8ac", "Zm9va", "Zm9vYmFya" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            assertThrowsExactly(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @TestAllDecoder
    public void reject_char_after_padding(DefaultBase64Decoder decoder) {
        // given
        String[] valuesToDecode = new String[] { "Zg==aaaa", "Zm8=aaaa", "Zm9v====aaaa", "Zm9vYg==aaaa", "Zm9vYmE=aaaa",
                "Zm9vYmFy====aaaa" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            assertThrowsExactly(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }
}
