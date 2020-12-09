/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.core.util.datatype;

import java.math.BigInteger;

import com.upokecenter.numbers.EInteger;

public class NumberUtil {

    /**
     * Convert the given number to long.
     * 
     * @param number the number to turn in long
     * @return a long value for the given number
     * 
     * @throws IllegalArgumentException if the number can not be store in a long.
     */
    public static Long numberToLong(Number number) throws IllegalStateException {
        // handle INTEGER
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return number.longValue();
        }
        if (number instanceof BigInteger) {
            // check big integer is not too big for a long
            BigInteger bigInt = (BigInteger) number;
            if (bigInt.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0
                    || bigInt.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                throw new IllegalStateException(String.format("%s  : can not be store in a long", bigInt));
            }
            return bigInt.longValue();
        }

        // handle FLOATING-POINT
        // TODO should we support a safe floating-point conversion

        // handle UNSIGNED
        if (number instanceof ULong) {
            long longValue = number.longValue();
            // if long value is negative this means that this is a too long unsigned long
            if (longValue < 0) {
                throw new IllegalStateException(String.format("%s  : can not be store in a long", number));
            }
            return longValue;
        }
        throw new IllegalStateException(String.format("Can not convert %s to long safely : Unsupported number %s",
                number, number.getClass().getCanonicalName()));
    }

    /**
     * Convert the given number to ULong.
     * 
     * @param number the number to turn in long
     * @return a long value for the given number
     * 
     * @throws IllegalArgumentException if the number can not be store in a long.
     */
    public static ULong numberToULong(Number number) throws IllegalStateException {
        // handle INTEGER
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            long longValue = number.longValue();
            if (longValue < 0) {
                throw new IllegalStateException(
                        String.format("%s  : can not convert negative number to an unsigned long", number));
            }
            return ULong.valueOf(longValue);
        }
        if (number instanceof BigInteger) {
            // check big integer is not too big for a long
            BigInteger bigInt = (BigInteger) number;
            if (bigInt.signum() == -1 || bigInt.compareTo(ULong.MAX_VALUE) > 0) {
                throw new IllegalStateException(String.format("%s  : can not be store in an unsigned long", bigInt));
            }
            return ULong.valueOf(bigInt);
        }

        // handle FLOATING-POINT
        // TODO should we support a safe floating-point conversion

        // handle UNSIGNED
        if (number instanceof ULong) {
            return (ULong) number;
        }
        throw new IllegalStateException(String.format("Can not convert %s to long safely : Unsupported number %s",
                number, number.getClass().getCanonicalName()));
    }

    // This will maybe be added in a new version of CBOR-java : https://github.com/peteroupc/CBOR-Java/issues/15
    public static EInteger unsignedLongToEInteger(long v) {
        if (v >= 0) {
            return EInteger.FromInt64(v);
        } else {
            return EInteger.FromInt32(1).ShiftLeft(64).Add(v);
        }
    }
}
