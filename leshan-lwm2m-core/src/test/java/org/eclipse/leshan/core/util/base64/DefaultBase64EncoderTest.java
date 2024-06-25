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

import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderAlphabet;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderPadding;
import org.junit.jupiter.api.Test;

public class DefaultBase64EncoderTest {
    @Test
    public void test_encode_url_safe_and_without_padding() {
        // given
        DefaultBase64Encoder encoder = new DefaultBase64Encoder(EncoderAlphabet.BASE64URL, EncoderPadding.WITHOUT);
        byte[] input = new byte[128];
        for (int i = -128; i < 0; i++) {
            input[i + 128] = (byte) i;
        }
        String encoded = "gIGCg4SFhoeIiYqLjI2Oj5CRkpOUlZaXmJmam5ydnp-goaKjpKWmp6ipqqusra6vsLGys7S1tre4ubq7vL2-v8DBws"
                + "PExcbHyMnKy8zNzs_Q0dLT1NXW19jZ2tvc3d7f4OHi4-Tl5ufo6err7O3u7_Dx8vP09fb3-Pn6-_z9_v8";

        // when
        String output = encoder.encode(input);

        // then
        assertEquals(encoded, output);
    }

    @Test
    public void test_encode_url_safe_and_with_padding() {
        // given
        DefaultBase64Encoder encoder = new DefaultBase64Encoder(EncoderAlphabet.BASE64URL, EncoderPadding.WITH);
        byte[] input = new byte[128];
        for (int i = -128; i < 0; i++) {
            input[i + 128] = (byte) i;
        }
        String encoded = "gIGCg4SFhoeIiYqLjI2Oj5CRkpOUlZaXmJmam5ydnp-goaKjpKWmp6ipqqusra6vsLGys7S1tre4ubq7vL2-v8DBwsP"
                + "ExcbHyMnKy8zNzs_Q0dLT1NXW19jZ2tvc3d7f4OHi4-Tl5ufo6err7O3u7_Dx8vP09fb3-Pn6-_z9_v8=";

        // when
        String output = encoder.encode(input);

        // then
        assertEquals(encoded, output);
    }

    @Test
    public void test_encode_url_unsafe_and_without_padding() {
        // given
        DefaultBase64Encoder encoder = new DefaultBase64Encoder(EncoderAlphabet.BASE64, EncoderPadding.WITHOUT);
        byte[] input = new byte[128];
        for (int i = -128; i < 0; i++) {
            input[i + 128] = (byte) i;
        }
        String encoded = "gIGCg4SFhoeIiYqLjI2Oj5CRkpOUlZaXmJmam5ydnp+goaKjpKWmp6ipqqusra6vsLGys7S1tre4ubq7vL2+v8DBwsP"
                + "ExcbHyMnKy8zNzs/Q0dLT1NXW19jZ2tvc3d7f4OHi4+Tl5ufo6err7O3u7/Dx8vP09fb3+Pn6+/z9/v8";

        // when
        String output = encoder.encode(input);

        // then
        assertEquals(encoded, output);
    }

    @Test
    public void test_encode_url_unsafe_and_with_padding() {
        // given
        DefaultBase64Encoder encoder = new DefaultBase64Encoder(EncoderAlphabet.BASE64, EncoderPadding.WITH);
        byte[] input = new byte[128];
        for (int i = -128; i < 0; i++) {
            input[i + 128] = (byte) i;
        }
        String encoded = "gIGCg4SFhoeIiYqLjI2Oj5CRkpOUlZaXmJmam5ydnp+goaKjpKWmp6ipqqusra6vsLGys7S1tre4ubq7vL2+v8DBwsP"
                + "ExcbHyMnKy8zNzs/Q0dLT1NXW19jZ2tvc3d7f4OHi4+Tl5ufo6err7O3u7/Dx8vP09fb3+Pn6+/z9/v8=";

        // when
        String output = encoder.encode(input);

        // then
        assertEquals(encoded, output);
    }

}
