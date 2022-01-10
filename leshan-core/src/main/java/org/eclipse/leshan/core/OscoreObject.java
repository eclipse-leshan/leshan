package org.eclipse.leshan.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import org.eclipse.leshan.core.util.Hex;

public class OscoreObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] senderId;
    private final byte[] recipientId;
    private final byte[] masterSecret;
    private final Integer aeadAlgorithm;
    private final Integer hmacAlgorithm;
    private final byte[] masterSalt;

    public OscoreObject(byte[] senderId, byte[] recipientId, byte[] masterSecret, Integer aeadAlgorithm,
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
        // Note : oscoreMasterSecret and oscoreMasterSalt are explicitly excluded from the display for security purposes
        return "OscoreObject [senderId=" + Arrays.toString(senderId) + ", recipientId=" + Arrays.toString(recipientId)
                + ", aeadAlgorithm=" + aeadAlgorithm + ", hmacAlgorithm=" + hmacAlgorithm + "]";
    }

    public OSCoreIdentity getOSCoreIdentity() {
        return new OSCoreIdentity(senderId, recipientId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OscoreObject other = (OscoreObject) obj;
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
}