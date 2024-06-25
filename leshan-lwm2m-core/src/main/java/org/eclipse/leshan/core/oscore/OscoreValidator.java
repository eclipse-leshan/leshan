/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.oscore;

import java.util.Arrays;
import java.util.List;

import org.eclipse.leshan.core.util.Hex;

public class OscoreValidator {

    public void validateOscoreSetting(OscoreSetting oscoreSetting) throws InvalidOscoreSettingException {
        byte[] senderId = oscoreSetting.getSenderId();
        byte[] recipientId = oscoreSetting.getRecipientId();
        byte[] masterSecret = oscoreSetting.getMasterSecret();
        AeadAlgorithm aeadAlgorithm = oscoreSetting.getAeadAlgorithm();
        HkdfAlgorithm hkdfAlgorithm = oscoreSetting.getHkdfAlgorithm();

        // Validate that Algorithm are known.
        if (!aeadAlgorithm.isKnown()) {
            throw new InvalidOscoreSettingException("Unkown AEAD Algorithm (%s) : known AEAD Algorithm are %s",
                    aeadAlgorithm, Arrays.toString(AeadAlgorithm.knownAeadAlgorithms));
        }
        if (!hkdfAlgorithm.isKnown()) {
            throw new InvalidOscoreSettingException("Unkown HKDF Algorithm (%s) : known HKDF Algorithm are %s",
                    hkdfAlgorithm, Arrays.toString(HkdfAlgorithm.knownHkdfAlgorithms));
        }

        // Validate senderId and recipient id length
        // see : https://datatracker.ietf.org/doc/html/rfc8613#section-3.3
        // The maximum length of Sender ID in bytes equals the length of the AEAD nonce minus 6.
        // The Sender IDs can be very short (note that the empty string is a legitimate value).
        int nonceSize = aeadAlgorithm.getNonceSize();
        int maxLength = nonceSize - 6;
        if (senderId.length > maxLength) {
            throw new InvalidOscoreSettingException("Invalid Sender ID (%s) : max length for % algorithm is %s",
                    Hex.encodeHexString(senderId), aeadAlgorithm, maxLength);
        }
        if (recipientId.length > maxLength) {
            throw new InvalidOscoreSettingException("Invalid Recipient ID (%s) : max length for % algorithm is %s",
                    Hex.encodeHexString(recipientId), aeadAlgorithm, maxLength);
        }
        // Validate master key.
        if (masterSecret.length == 0) {
            throw new InvalidOscoreSettingException("Invalid Master Secret : can not be an empty String");
        }

        // Temporary code check for supported Algorithm
        List<AeadAlgorithm> supportedAeadAlgorithm = Arrays.asList(AeadAlgorithm.AES_CCM_16_64_128,
                AeadAlgorithm.AES_CCM_16_128_128, AeadAlgorithm.AES_CCM_64_64_128, AeadAlgorithm.AES_CCM_64_128_128);
        if (!supportedAeadAlgorithm.contains(aeadAlgorithm)) {
            throw new InvalidOscoreSettingException("Invalid AEAD Algorithm (%s) : currently only %s are supported.",
                    aeadAlgorithm, supportedAeadAlgorithm);
        }

        List<HkdfAlgorithm> supportedHkdfAlgorithm = Arrays.asList(HkdfAlgorithm.HKDF_HMAC_SHA_256,
                HkdfAlgorithm.HKDF_HMAC_SHA_512);
        if (!supportedHkdfAlgorithm.contains(hkdfAlgorithm)) {
            throw new InvalidOscoreSettingException("Invalid HKDF Algorithm (%s) : currently only %s are supported.",
                    hkdfAlgorithm, supportedHkdfAlgorithm);
        }
    }
}
