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
package org.eclipse.leshan.transport.californium.oscore.cf;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.leshan.core.util.Hex;

/**
 * OSCORE Parameters.
 * <p>
 * See : https://datatracker.ietf.org/doc/html/rfc8613#section-3.2
 */
public class OscoreParameters {

    private final byte[] senderId;
    private final byte[] recipientId;
    private final byte[] masterSecret;
    private final AlgorithmID aeadAlgorithm;
    private final AlgorithmID hmacAlgorithm;
    private final byte[] masterSalt;

    public OscoreParameters(byte[] senderId, byte[] recipientId, byte[] masterSecret, AlgorithmID aeadAlgorithm,
            AlgorithmID hmacAlgorithm, byte[] masterSalt) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.masterSecret = masterSecret;
        this.aeadAlgorithm = aeadAlgorithm;
        this.hmacAlgorithm = hmacAlgorithm;
        this.masterSalt = masterSalt;
    }

    public byte[] getSenderId() {
        return senderId;
    }

    public byte[] getRecipientId() {
        return recipientId;
    }

    public byte[] getMasterSecret() {
        return masterSecret;
    }

    public AlgorithmID getAeadAlgorithm() {
        return aeadAlgorithm;
    }

    public AlgorithmID getHmacAlgorithm() {
        return hmacAlgorithm;
    }

    public byte[] getMasterSalt() {
        return masterSalt;
    }

    @Override
    public String toString() {
        // Note : oscoreMasterSecret and oscoreMasterSalt are explicitly excluded from the display for security
        // purposes
        return String.format("OscoreParameters [senderId=%s, recipientId=%s, aeadAlgorithm=%s, hmacAlgorithm=%s]",
                Hex.encodeHexString(senderId), Hex.encodeHexString(recipientId), aeadAlgorithm, hmacAlgorithm);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OscoreParameters))
            return false;
        OscoreParameters that = (OscoreParameters) o;
        return Objects.deepEquals(senderId, that.senderId) && Objects.deepEquals(recipientId, that.recipientId)
                && Objects.deepEquals(masterSecret, that.masterSecret) && aeadAlgorithm == that.aeadAlgorithm
                && hmacAlgorithm == that.hmacAlgorithm && Objects.deepEquals(masterSalt, that.masterSalt);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(Arrays.hashCode(senderId), Arrays.hashCode(recipientId), Arrays.hashCode(masterSecret),
                aeadAlgorithm, hmacAlgorithm, Arrays.hashCode(masterSalt));
    }
}
