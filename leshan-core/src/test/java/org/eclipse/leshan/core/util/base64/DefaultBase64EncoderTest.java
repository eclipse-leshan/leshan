package org.eclipse.leshan.core.util.base64;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DefaultBase64EncoderTest {
    @Test public void test_encode_url_safe_and_without_padding() {
        //given
        DefaultBase64Encoder encoder = new DefaultBase64Encoder(true, true);
        byte[] input = new byte[256];
        for (int i = -128; i < 128; i++) {
            input[i + 128] = (byte) i;
        }
        String encoded = "gIGCg4SFhoeIiYqLjI2Oj5CRkpOUlZaXmJmam5ydnp-goaKjpKWmp6ipqqusra6vsLGys7S1tre4ubq7vL2-v8DBwsPEx"
                + "cbHyMnKy8zNzs_Q0dLT1NXW19jZ2tvc3d7f4OHi4-Tl5ufo6err7O3u7_Dx8vP09fb3-Pn6-_z9_v8AAQIDBAUGBwgJCgsMDQ4PE"
                + "BESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4_QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW"
                + "1xdXl9gYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXp7fH1-fw";

        //when
        String output = encoder.encode(input);

        //then
        assertEquals(encoded, output);
    }
}