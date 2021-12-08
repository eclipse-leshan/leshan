package org.eclipse.leshan.core;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.leshan.core.util.Hex;

public class OSCoreIdentity implements Serializable {

    protected final byte[] senderId;
    protected final byte[] recipientId;

    public OSCoreIdentity(byte[] senderId, byte[] recipientId) {
        this.senderId = senderId;
        this.recipientId = recipientId;
    }

    public OSCoreIdentity(String senderId, String recipientId) {
        this.senderId = new Hex().decode(senderId.getBytes(StandardCharsets.UTF_8));
        this.recipientId = new Hex().decode(recipientId.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] getSenderId() {
        return senderId;
    }

    public byte[] getRecipientId() {
        return recipientId;
    }

    // TODO OSCORE: Generate toString() in Eclipse.
    @Override
    public String toString() {
        return String.format("OSCoreIdentity [oscoreSenderId=%s, oscoreRecipientId=%s]", Hex.encodeHexString(senderId),
                Hex.encodeHexString(recipientId));
    }

    // TODO OSCORE: Generate equals() in Eclipse.
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OSCoreIdentity))
            return false;
        OSCoreIdentity that = (OSCoreIdentity) o;
        return Arrays.equals(senderId, that.senderId) && Arrays.equals(recipientId, that.recipientId);
    }

    // TODO OSCORE: Generate hashCode() in Eclipse.
    @Override
    public int hashCode() {
        int result = Arrays.hashCode(senderId);
        result = 31 * result + Arrays.hashCode(recipientId);
        return result;
    }
}
