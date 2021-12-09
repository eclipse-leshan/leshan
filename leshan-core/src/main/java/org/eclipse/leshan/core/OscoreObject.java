package org.eclipse.leshan.core;

import java.io.Serializable;

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
        // Note : oscoreMasterSecret and oscoreMasterSalt are explicitly excluded from the display for security
        // purposes
        return String.format(
                "OscoreObject [oscoreSenderId=%s, oscoreRecipientId=%s, oscoreAeadAlgorithm=%s, oscoreHmacAlgorithm=%s]",
                Hex.encodeHexString(senderId), Hex.encodeHexString(recipientId), aeadAlgorithm, hmacAlgorithm);
    }

    public OSCoreIdentity getOSCoreIdentity() {
        return new OSCoreIdentity(senderId, recipientId);
    }
}