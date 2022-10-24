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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder.DecoderAlphabet;
import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder.DecoderPadding;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DefaultBase64DecoderTest {

    @Parameterized.Parameters(name = "{0} - Padding {1}")
    public static Iterable<Object[]> base64Decoder() {
        return Arrays.asList(new Object[][] { //
                { DecoderAlphabet.BASE64, DecoderPadding.REQUIRED }, //
                { DecoderAlphabet.BASE64, DecoderPadding.FORBIDEN }, //
                { DecoderAlphabet.BASE64, DecoderPadding.OPTIONAL }, //
                { DecoderAlphabet.BASE64URL, DecoderPadding.REQUIRED }, //
                { DecoderAlphabet.BASE64URL, DecoderPadding.FORBIDEN }, //
                { DecoderAlphabet.BASE64URL, DecoderPadding.OPTIONAL }, //
                { DecoderAlphabet.BASE64URL_OR_BASE64, DecoderPadding.REQUIRED }, //
                { DecoderAlphabet.BASE64URL_OR_BASE64, DecoderPadding.FORBIDEN }, //
                { DecoderAlphabet.BASE64URL_OR_BASE64, DecoderPadding.OPTIONAL }, //
        });
    }

    private final Base64Decoder decoder;
    private final DecoderAlphabet alphabet;
    private final DecoderPadding padding;

    public DefaultBase64DecoderTest(DecoderAlphabet alphabet, DecoderPadding padding) {
        this.decoder = new DefaultBase64Decoder(alphabet, padding);
        this.alphabet = alphabet;
        this.padding = padding;
    }

    public boolean decoderSupportUrlSafeEncoding() {
        return alphabet == DecoderAlphabet.BASE64URL || alphabet == DecoderAlphabet.BASE64URL_OR_BASE64;
    }

    public boolean decoderSupportUrlUnSafeEncoding() {
        return alphabet == DecoderAlphabet.BASE64 || alphabet == DecoderAlphabet.BASE64URL_OR_BASE64;
    }

    public boolean decoderRequirePadding() {
        return padding == DecoderPadding.REQUIRED;
    }

    public boolean decoderSupportPadding() {
        return padding == DecoderPadding.REQUIRED || padding == DecoderPadding.OPTIONAL;
    }

    @Test
    public void decode_valid_base64() throws InvalidBase64Exception {
        // Test value from : https://www.rfc-editor.org/rfc/rfc4648#section-10

        // given
        String[] valuesToDecode = new String[] { "", "Zg==", "Zm8=", "Zm9v", "Zm9vYg==", "Zm9vYmE=", "Zm9vYmFy" };
        String[] expectedDecodedValue = new String[] { "", "f", "fo", "foo", "foob", "fooba", "foobar" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            // when
            String valueToDecode = valuesToDecode[i];
            if (!decoderSupportPadding()) {
                // remove padding
                valueToDecode = valueToDecode.replaceAll("=", "");
            }
            byte[] decodedValue = decoder.decode(valueToDecode);

            // then
            assertEquals(expectedDecodedValue[i], new String(decodedValue));
        }
    }

    @Test
    public void reject_string_containing_non_ascii_char() {

        // given
        String[] valuesToDecode = new String[] { "€", "ù==", "€=", "Z€", "€ùg==", "€€E=", "€€ù" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode;
            if (!decoderSupportPadding()) {
                // remove padding
                valueToDecode = valuesToDecode[i].replaceAll("=", "");
            } else {
                valueToDecode = valuesToDecode[i];
            }

            // then
            Assert.assertThrows(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @Test
    public void reject_string_containing_illegal_base64_char() {
        // given
        String[] valuesToDecode = new String[] { "Z!==", "Z,8=", "Zm9>", ">m9vYg==", "Zm<vYmE=", "Zm9%YmFy" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode;
            if (!decoderSupportPadding()) {
                // remove padding
                valueToDecode = valuesToDecode[i].replaceAll("=", "");
            } else {
                valueToDecode = valuesToDecode[i];
            }

            // then
            Assert.assertThrows(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @Test
    public void test_url_unsafe_char() throws InvalidBase64Exception {
        // given
        String[] valuesToDecode = new String[] { "A+BB", "+g==", "/m8=", "Z/9v", "Z++vYg==", "Zm/v+mE=", "Zm/v+mFy" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode;
            if (!decoderSupportPadding()) {
                // remove padding
                valueToDecode = valuesToDecode[i].replaceAll("=", "");
            } else {
                valueToDecode = valuesToDecode[i];
            }

            // then
            if (!decoderSupportUrlUnSafeEncoding()) {
                Assert.assertThrows(InvalidBase64Exception.class, () -> {
                    decoder.decode(valueToDecode);
                });
            } else {
                decoder.decode(valueToDecode);
            }
        }
    }

    @Test
    public void test_url_safe_char() throws InvalidBase64Exception {
        // given
        String[] valuesToDecode = new String[] { "A-BB", "-g==", "_m8=", "Z_9v", "Z--vYg==", "Zm_v-mE=", "Zm_v-mFy" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode;
            if (!decoderSupportPadding()) {
                // remove padding
                valueToDecode = valuesToDecode[i].replaceAll("=", "");
            } else {
                valueToDecode = valuesToDecode[i];
            }

            // then
            if (!decoderSupportUrlSafeEncoding()) {
                Assert.assertThrows(InvalidBase64Exception.class, () -> {
                    decoder.decode(valueToDecode);
                });
            } else {
                decoder.decode(valueToDecode);
            }
        }
    }

    @Test
    public void test_without_padding() throws InvalidBase64Exception {
        // given
        String[] valuesToDecode = new String[] { "ABA", "Zg", "Zm8", "Zm9vYg", "Zm9vYmE" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];
            // then
            if (decoderRequirePadding()) {
                Assert.assertThrows(InvalidBase64Exception.class, () -> {
                    decoder.decode(valueToDecode);
                });
            } else {
                decoder.decode(valueToDecode);
            }
        }
    }

    @Test
    public void reject_if_not_canonical_with_padding() {
        // given
        String[] valuesToDecode = new String[] { "ABC=", "Zh==", "Zm9=", "Zm9vYi==", "Zm9vYmF=" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            Assert.assertThrows(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @Test
    public void reject_if_not_canonical_without_padding() {
        // given
        String[] valuesToDecode = new String[] { "ABC", "Zh", "Zm9", "Zm9vYi", "Zm9vYmF" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            Assert.assertThrows(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @Test
    public void reject_too_many_padding() {
        // given
        String[] valuesToDecode = new String[] { "Zg===", "Zm8==", "Zm9v=", "Zm9vYg====", "Zm9vYmE===",
                "Zm9vYmFy====" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            Assert.assertThrows(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @Test
    public void reject_invalid_number_of_char() {
        // given
        String[] valuesToDecode = new String[] { "Z", "Zm8ac", "Zm9va", "Zm9vYmFya" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            Assert.assertThrows(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }

    @Test
    public void reject_char_after_padding() {
        // given
        String[] valuesToDecode = new String[] { "Zg==aaaa", "Zm8=aaaa", "Zm9v====aaaa", "Zm9vYg==aaaa", "Zm9vYmE=aaaa",
                "Zm9vYmFy====aaaa" };

        for (int i = 0; i < valuesToDecode.length; i++) {
            final String valueToDecode = valuesToDecode[i];

            // then
            Assert.assertThrows(InvalidBase64Exception.class, () -> {
                decoder.decode(valueToDecode);
            });
        }
    }
}
