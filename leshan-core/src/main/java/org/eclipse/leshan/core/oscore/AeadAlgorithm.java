/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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

import org.eclipse.leshan.core.util.datatype.NumberUtil;

/**
 * Some utility method about code AEAD Algoritm as defined at https://datatracker.ietf.org/doc/html/rfc8152#section-10
 */
public class AeadAlgorithm implements Serializable {
    private static final long serialVersionUID = 1L; // TODO not sure we will keep SecurityInfo serializable

    public static final AeadAlgorithm AES_GCM_128 = new AeadAlgorithm("A128GCM", 1, 12); //
    public static final AeadAlgorithm AES_GCM_192 = new AeadAlgorithm("A192GCM", 2, 12); //
    public static final AeadAlgorithm AES_GCM_256 = new AeadAlgorithm("A256GCM", 3, 12); //
    public static final AeadAlgorithm AES_CCM_16_64_128 = new AeadAlgorithm("AES-CCM-16-64-128", 10, 13); //
    public static final AeadAlgorithm AES_CCM_16_64_256 = new AeadAlgorithm("AES-CCM-16-64-256", 11, 13); //
    public static final AeadAlgorithm AES_CCM_64_64_128 = new AeadAlgorithm("AES-CCM-64-64-128", 12, 7); //
    public static final AeadAlgorithm AES_CCM_64_64_256 = new AeadAlgorithm("AES-CCM-64-64-256", 13, 7); //
    public static final AeadAlgorithm AES_CCM_16_128_128 = new AeadAlgorithm("AES-CCM-16-128-128", 30, 13); //
    public static final AeadAlgorithm AES_CCM_16_128_256 = new AeadAlgorithm("AES-CCM-16-128-256", 31, 13); //
    public static final AeadAlgorithm AES_CCM_64_128_128 = new AeadAlgorithm("AES-CCM-64-128-128", 32, 7); //
    public static final AeadAlgorithm AES_CCM_64_128_256 = new AeadAlgorithm("AES-CCM-64-128-256", 33, 7); //

    public static final AeadAlgorithm knownAeadAlgorithms[] = new AeadAlgorithm[] { //
            AES_GCM_128, AES_GCM_192, AES_GCM_256, //
            AES_CCM_16_64_128, AES_CCM_16_64_256, AES_CCM_64_64_128, AES_CCM_64_64_256, //
            AES_CCM_16_128_128, AES_CCM_16_128_256, AES_CCM_64_128_128, AES_CCM_64_128_256 };

    private final String name;
    private final int value;
    private final int nonceSize; // in bytes

    public AeadAlgorithm(String name, int value, int nonceSize) {
        this.name = name;
        this.value = value;
        this.nonceSize = nonceSize;
    }

    /**
     * @return {@link AeadAlgorithm} with the given value.
     */
    public static AeadAlgorithm fromValue(int value) {
        for (AeadAlgorithm alg : knownAeadAlgorithms) {
            if (alg.value == value)
                return alg;
        }
        return new AeadAlgorithm("UNKNOWN", value, 0);
    }

    /**
     * @return {@link AeadAlgorithm} with the given value.
     */
    public static AeadAlgorithm fromValue(long value) {
        try {
            int intValue = NumberUtil.longToInt(value);
            return fromValue(intValue);
        } catch (IllegalArgumentException e) {
            if (value >= 0)
                return fromValue(Integer.MAX_VALUE);
            else
                return fromValue(Integer.MIN_VALUE);
        }
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

    /**
     * @return nonce size in bytes.
     */
    public int getNonceSize() {
        return nonceSize;
    }

    /**
     * @return <code>true</code> is this is a known {@link AeadAlgorithm}
     */
    public boolean isKnown() {
        return Arrays.asList(knownAeadAlgorithms).contains(this);
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
        result = prime * result + nonceSize;
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
        if (nonceSize != other.nonceSize)
            return false;
        if (value != other.value)
            return false;
        return true;
    }
}
