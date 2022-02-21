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
package org.eclipse.leshan.server.security.oscore;

import java.io.Serializable;
import java.util.Arrays;

import org.eclipse.leshan.core.util.Hex;

public class OscoreSetting implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] senderId;
    private final byte[] recipientId;
    private final byte[] masterSecret;
    private final Integer aeadAlgorithm;
    private final Integer hmacAlgorithm;
    private final byte[] masterSalt;

    public OscoreSetting(byte[] senderId, byte[] recipientId, byte[] masterSecret, Integer aeadAlgorithm,
            Integer hmacAlgorithm, byte[] masterSalt) {
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

    public Integer getAeadAlgorithm() {
        return aeadAlgorithm;
    }

    public Integer getHmacAlgorithm() {
        return hmacAlgorithm;
    }

    public byte[] getMasterSalt() {
        return masterSalt;
    }

    @Override
    public String toString() {
        // Note : oscoreMasterSecret and oscoreMasterSalt are explicitly excluded from the display for security
        // purposes
        return String.format("OscoreSetting [senderId=%s, recipientId=%s, aeadAlgorithm=%s, hmacAlgorithm=%s]",
                Hex.encodeHexString(senderId), Hex.encodeHexString(recipientId), aeadAlgorithm, hmacAlgorithm);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aeadAlgorithm == null) ? 0 : aeadAlgorithm.hashCode());
        result = prime * result + ((hmacAlgorithm == null) ? 0 : hmacAlgorithm.hashCode());
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
        if (hmacAlgorithm == null) {
            if (other.hmacAlgorithm != null)
                return false;
        } else if (!hmacAlgorithm.equals(other.hmacAlgorithm))
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