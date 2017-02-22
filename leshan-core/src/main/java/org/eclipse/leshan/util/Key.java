package org.eclipse.leshan.util;

import java.util.Arrays;

public class Key {

    private final byte[] bytes;

    public Key(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return Hex.encodeHexString(bytes);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bytes);
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
        Key other = (Key) obj;
        if (!Arrays.equals(bytes, other.bytes))
            return false;
        return true;
    }
}