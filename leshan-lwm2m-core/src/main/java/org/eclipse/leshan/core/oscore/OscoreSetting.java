/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.Validate;

/**
 * OSCORE Settings.
 * <p>
 * See : https://datatracker.ietf.org/doc/html/rfc8613#section-3.2
 */
public class OscoreSetting implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final AeadAlgorithm DEFAULT_AEAD_ALGORITHM = AeadAlgorithm.AES_CCM_16_64_128;
    public static final HkdfAlgorithm DEFAULT_HKDF_ALGORITHM = HkdfAlgorithm.HKDF_HMAC_SHA_256;
    public static final byte[] DEFAULT_MASTER_SALT = new byte[0];

    private final byte[] senderId;
    private final byte[] recipientId;
    private final byte[] masterSecret;
    private final AeadAlgorithm aeadAlgorithm;
    private final HkdfAlgorithm hkdfAlgorithm;
    private final byte[] masterSalt;

    public OscoreSetting(byte[] senderId, byte[] recipientId, byte[] masterSecret) {
        this(senderId, recipientId, masterSecret, (AeadAlgorithm) null, null, null);
    }

    public OscoreSetting(byte[] senderId, byte[] recipientId, byte[] masterSecret, Integer aeadAlgorithm,
            Integer hkdfAlgorithm, byte[] masterSalt) {
        this(senderId, recipientId, masterSecret, //
                aeadAlgorithm == null ? null : AeadAlgorithm.fromValue(aeadAlgorithm), //
                hkdfAlgorithm == null ? null : HkdfAlgorithm.fromValue(hkdfAlgorithm), //
                masterSalt);
    }

    public OscoreSetting(byte[] senderId, byte[] recipientId, byte[] masterSecret, AeadAlgorithm aeadAlgorithm,
            HkdfAlgorithm hkdfAlgorithm, byte[] masterSalt) {
        Validate.notNull(senderId);
        Validate.notNull(recipientId);
        Validate.notNull(masterSecret);

        this.senderId = senderId;
        this.recipientId = recipientId;
        this.masterSecret = masterSecret;
        this.aeadAlgorithm = aeadAlgorithm == null ? DEFAULT_AEAD_ALGORITHM : aeadAlgorithm;
        this.hkdfAlgorithm = hkdfAlgorithm == null ? DEFAULT_HKDF_ALGORITHM : hkdfAlgorithm;
        this.masterSalt = masterSalt == null ? DEFAULT_MASTER_SALT : masterSalt;
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

    public AeadAlgorithm getAeadAlgorithm() {
        return aeadAlgorithm;
    }

    public HkdfAlgorithm getHkdfAlgorithm() {
        return hkdfAlgorithm;
    }

    public byte[] getMasterSalt() {
        return masterSalt;
    }

    @Override
    public String toString() {
        // Note : oscoreMasterSecret and oscoreMasterSalt are explicitly excluded from the display for security
        // purposes
        return String.format("OscoreSetting [senderId=%s, recipientId=%s, aeadAlgorithm=%s, hkdfsAlgorithm=%s]",
                Hex.encodeHexString(senderId), Hex.encodeHexString(recipientId), aeadAlgorithm, hkdfAlgorithm);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OscoreSetting))
            return false;
        OscoreSetting that = (OscoreSetting) o;
        return Arrays.equals(senderId, that.senderId) && Arrays.equals(recipientId, that.recipientId)
                && Arrays.equals(masterSecret, that.masterSecret) && Objects.equals(aeadAlgorithm, that.aeadAlgorithm)
                && Objects.equals(hkdfAlgorithm, that.hkdfAlgorithm) && Arrays.equals(masterSalt, that.masterSalt);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(Arrays.hashCode(senderId), Arrays.hashCode(recipientId), Arrays.hashCode(masterSecret),
                aeadAlgorithm, hkdfAlgorithm, Arrays.hashCode(masterSalt));
    }
}
