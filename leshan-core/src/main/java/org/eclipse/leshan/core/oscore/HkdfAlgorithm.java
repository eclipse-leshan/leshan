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
 * Some utility method about code HKDF Algoritm as defined at https://datatracker.ietf.org/doc/html/rfc8152#section-11.1
 * and https://datatracker.ietf.org/doc/html/rfc8152#section-12.1.2
 */
public class HkdfAlgorithm implements Serializable {
    private static final long serialVersionUID = 1L; // TODO not sure we will keep SecurityInfo serializable

    public static final HkdfAlgorithm HKDF_HMAC_SHA_256 = new HkdfAlgorithm("HKDF-SHA-256", -10);//
    public static final HkdfAlgorithm HKDF_HMAC_SHA_512 = new HkdfAlgorithm("HKDF-SHA-512", -11); //
    public static final HkdfAlgorithm HKDF_HMAC_AES_128 = new HkdfAlgorithm("HKDF-AES-128", -12); //
    public static final HkdfAlgorithm HKDF_HMAC_AES_256 = new HkdfAlgorithm("HKDF-AES-256", -13); //

    public static final HkdfAlgorithm knownHkdfAlgorithms[] = new HkdfAlgorithm[] { //
            HKDF_HMAC_SHA_256, HKDF_HMAC_SHA_512, HKDF_HMAC_AES_128, HKDF_HMAC_AES_256 };

    private final String name;
    private final int value;

    public HkdfAlgorithm(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return {@link HkdfAlgorithm} with the given value.
     */
    public static HkdfAlgorithm fromValue(int value) {
        for (HkdfAlgorithm alg : knownHkdfAlgorithms) {
            if (alg.value == value)
                return alg;
        }
        return new HkdfAlgorithm("UNKNOWN", value);
    }

    /**
     * @return {@link HkdfAlgorithm} with the given name, return null if this {@link HkdfAlgorithm} is not known.
     */
    public static HkdfAlgorithm fromName(String name) {
        for (HkdfAlgorithm alg : knownHkdfAlgorithms) {
            if (alg.name.equals(name))
                return alg;
        }
        return null;
    }

    /**
     * @return {@link HkdfAlgorithm} with the given value.
     */
    public static HkdfAlgorithm fromValue(long value) {
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

    public int getValue() {
        return value;
    }

    /**
     * @return <code>true</code> is this is a known {@link HkdfAlgorithm}
     */
    public boolean isKnown() {
        return Arrays.asList(knownHkdfAlgorithms).contains(this);
    }

    @Override
    public String toString() {
        return String.format("%s(%d)", name, value);
    }
}
