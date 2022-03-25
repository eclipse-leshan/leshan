package org.eclipse.leshan.core.oscore;

import java.io.Serializable;

/**
 * Some utility method about code AEAD Algoritm as defined at https://datatracker.ietf.org/doc/html/rfc8152#section-10
 */
public class AeadAlgorithm implements Serializable {
    private static final long serialVersionUID = 1L; // TODO not sure we will keep SecurityInfo serializable

    public static final AeadAlgorithm AES_GCM_128 = new AeadAlgorithm("A128GCM", 1); //
    public static final AeadAlgorithm AES_GCM_192 = new AeadAlgorithm("A192GCM", 2); //
    public static final AeadAlgorithm AES_GCM_256 = new AeadAlgorithm("A256GCM", 3); //
    public static final AeadAlgorithm AES_CCM_16_64_128 = new AeadAlgorithm("AES-CCM-16-64-128", 10); //
    public static final AeadAlgorithm AES_CCM_16_64_256 = new AeadAlgorithm("AES-CCM-16-64-256", 11); //
    public static final AeadAlgorithm AES_CCM_64_64_128 = new AeadAlgorithm("AES-CCM-64-64-128", 12); //
    public static final AeadAlgorithm AES_CCM_64_64_256 = new AeadAlgorithm("AES-CCM-64-64-256", 13); //
    public static final AeadAlgorithm AES_CCM_16_128_128 = new AeadAlgorithm("AES-CCM-16-128-128", 30); //
    public static final AeadAlgorithm AES_CCM_16_128_256 = new AeadAlgorithm("AES-CCM-16-128-256", 31); //
    public static final AeadAlgorithm AES_CCM_64_128_128 = new AeadAlgorithm("AES-CCM-64-128-128", 32); //
    public static final AeadAlgorithm AES_CCM_64_128_256 = new AeadAlgorithm("AES-CCM-64-128-256", 33); //

    public static final AeadAlgorithm knownAeadAlgorithms[] = new AeadAlgorithm[] { //
            AES_GCM_128, AES_GCM_192, AES_GCM_256, //
            AES_CCM_16_64_128, AES_CCM_16_64_256, AES_CCM_64_64_128, AES_CCM_64_64_256, //
            AES_CCM_16_128_128, AES_CCM_16_128_256, AES_CCM_64_128_128, AES_CCM_64_128_256 };

    private final String name;
    private final int value;

    public AeadAlgorithm(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return {@link AeadAlgorithm} with the given value, return null if this {@link AeadAlgorithm} is not known.
     */
    public static AeadAlgorithm fromValue(int value) {
        for (AeadAlgorithm alg : knownAeadAlgorithms) {
            if (alg.value == value)
                return alg;
        }
        return null;
    }

    /**
     * @return {@link AeadAlgorithm} with the given name, return null if this {@link AeadAlgorithm} is not known.
     */
    public static AeadAlgorithm fromName(String name) {
        for (AeadAlgorithm alg : knownAeadAlgorithms) {
            if (alg.name.equals(name))
                return alg;
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s(%d)", name, value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + value;
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
        AeadAlgorithm other = (AeadAlgorithm) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (value != other.value)
            return false;
        return true;
    }
}
