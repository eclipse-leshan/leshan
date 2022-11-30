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
