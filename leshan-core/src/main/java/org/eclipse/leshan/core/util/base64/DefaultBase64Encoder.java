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

public class DefaultBase64Encoder implements Base64Encoder {
    private final Base64.Encoder encoder;

    public DefaultBase64Encoder(boolean urlSafe, boolean withoutPadding) {

        Base64.Encoder encoder = urlSafe ? Base64.getUrlEncoder() : Base64.getEncoder();
        if (withoutPadding) {
            encoder = encoder.withoutPadding();
        }
        this.encoder = encoder;
    }

    @Override public String encode(byte[] dataToEncode) {
        return encoder.encodeToString(dataToEncode);
    }
}
