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

import org.junit.Assert;
import org.junit.Test;

public class DefaultBase64DecoderTest {
    @Test
    public void should_decoder_reject_unsafe_url_for_safe_url_decoding() {
        // given
        DefaultBase64Decoder decoder = new DefaultBase64Decoder(true, true);
        String encoded = "abc+1/";
        // when
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> {
            decoder.decode(encoded);
        });
        // then
        Assert.assertEquals("Illegal base64 character 2b", e.getMessage());
    }
}
