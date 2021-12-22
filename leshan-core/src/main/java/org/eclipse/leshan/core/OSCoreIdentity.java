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
    
    @Override
    public String toString() {
        return "OSCoreIdentity [senderId=" + Arrays.toString(senderId) + ", recipientId=" + Arrays.toString(recipientId)
                + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OSCoreIdentity other = (OSCoreIdentity) obj;
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
        result = prime * result + Arrays.hashCode(recipientId);
        result = prime * result + Arrays.hashCode(senderId);
        return result;
    }
}
