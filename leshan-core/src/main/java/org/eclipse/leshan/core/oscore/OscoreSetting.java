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

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.Validate;

/**
 * OSCORE Settings.
 * <p>
 * See : https://datatracker.ietf.org/doc/html/rfc8613#section-3.2
 */
public class OscoreSetting implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] senderId;
    private final byte[] recipientId;
    private final byte[] masterSecret;
    private final AeadAlgorithm aeadAlgorithm;
    private final HkdfAlgorithm hkdfAlgorithm;
    private final byte[] masterSalt;

    public OscoreSetting(byte[] senderId, byte[] recipientId, byte[] masterSecret) {
        this(senderId, recipientId, masterSecret, null, null, null);
    }

    public OscoreSetting(byte[] senderId, byte[] recipientId, byte[] masterSecret, AeadAlgorithm aeadAlgorithm,
            HkdfAlgorithm hkdfAlgorithm, byte[] masterSalt) {
        Validate.notNull(senderId);
        Validate.notNull(recipientId);
        Validate.notNull(masterSecret);

        this.senderId = senderId;
        this.recipientId = recipientId;
        this.masterSecret = masterSecret;
        this.aeadAlgorithm = aeadAlgorithm == null ? AeadAlgorithm.AES_CCM_16_64_128 : aeadAlgorithm;
        this.hkdfAlgorithm = hkdfAlgorithm == null ? HkdfAlgorithm.HKDF_HMAC_SHA_256 : hkdfAlgorithm;
        this.masterSalt = masterSalt == null ? new byte[0] : masterSalt;

        // Validate senderId and recipient id length
        // see : https://datatracker.ietf.org/doc/html/rfc8613#section-3.3
        // The maximum length of Sender ID in bytes equals the length of the AEAD nonce minus 6.
        // The Sender IDs can be very short (note that the empty string is a legitimate value).
        int nonceSize = this.aeadAlgorithm.getNonceSize();
        int maxLength = nonceSize - 6;
        if (this.senderId.length > maxLength) {
            throw new IllegalArgumentException(
                    String.format("Invalid Sender ID (%s) : max length for % algorithm is %s",
                            Hex.encodeHexString(this.senderId), this.aeadAlgorithm, maxLength));
        }
        if (this.recipientId.length > maxLength) {
            throw new IllegalArgumentException(
                    String.format("Invalid Recipient ID (%s) : max length for % algorithm is %s",
                            Hex.encodeHexString(this.recipientId), this.aeadAlgorithm, maxLength));
        }
        // Validate master key.
        if (this.masterSecret.length == 0) {
            throw new IllegalArgumentException("Invalid Master Secret : can not be an empty String");
        }
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

    public OscoreIdentity getOscoreIdentity() {
        return new OscoreIdentity(recipientId);
    }

    @Override
    public String toString() {
        // Note : oscoreMasterSecret and oscoreMasterSalt are explicitly excluded from the display for security
        // purposes
        return String.format("OscoreSetting [senderId=%s, recipientId=%s, aeadAlgorithm=%s, hkdfsAlgorithm=%s]",
                Hex.encodeHexString(senderId), Hex.encodeHexString(recipientId), aeadAlgorithm, hkdfAlgorithm);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aeadAlgorithm == null) ? 0 : aeadAlgorithm.hashCode());
        result = prime * result + ((hkdfAlgorithm == null) ? 0 : hkdfAlgorithm.hashCode());
        result = prime * result + Arrays.hashCode(masterSalt);
        result = prime * result + Arrays.hashCode(masterSecret);
        result = prime * result + Arrays.hashCode(recipientId);
        result = prime * result + Arrays.hashCode(senderId);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OscoreSetting other = (OscoreSetting) obj;
        if (aeadAlgorithm == null) {
            if (other.aeadAlgorithm != null)
                return false;
        } else if (!aeadAlgorithm.equals(other.aeadAlgorithm))
            return false;
        if (hkdfAlgorithm == null) {
            if (other.hkdfAlgorithm != null)
                return false;
        } else if (!hkdfAlgorithm.equals(other.hkdfAlgorithm))
            return false;
        if (!Arrays.equals(masterSalt, other.masterSalt))
            return false;
        if (!Arrays.equals(masterSecret, other.masterSecret))
            return false;
        if (!Arrays.equals(recipientId, other.recipientId))
            return false;
        if (!Arrays.equals(senderId, other.senderId))
            return false;
        return true;
    }
}