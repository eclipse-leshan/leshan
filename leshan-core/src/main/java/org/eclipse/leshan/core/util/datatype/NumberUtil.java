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

import java.math.BigDecimal;
import java.math.BigInteger;

import com.upokecenter.numbers.EInteger;

public class NumberUtil {

    /**
     * Convert the given number to long without loss allowing Floating-point number conversion
     *
     * @param number the number to turn in long
     * @return a long value for the given number
     *
     * @throws IllegalArgumentException if the number can not be store in a long.
     */
    public static Long numberToLong(Number number) throws IllegalArgumentException {
        return numberToLong(number, true);
    }

    /**
     * Convert the given number to long without loss.
     *
     * @param number the number to turn in long
     * @param permissiveNumberConversion this will allow Floating-Point number to be converted in Long, else an
     *        exception is raised.
     * @return a long value for the given number
     *
     * @throws IllegalArgumentException if the number can not be store in a long.
     */
    public static Long numberToLong(Number number, boolean permissiveNumberConversion) throws IllegalArgumentException {
        // handle INTEGER
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return number.longValue();
        }
        if (number instanceof BigInteger) {
            // check big integer is not too big for a long
            BigInteger bigInt = (BigInteger) number;
            if (bigInt.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0
                    || bigInt.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException(String.format("%s  : can not be store in a long", bigInt));
            }
            return bigInt.longValue();
        }

        // handle FLOATING-POINT
        if (permissiveNumberConversion) {
            BigDecimal bigDec = null;
            if (number instanceof Float || number instanceof Double) {
                bigDec = new BigDecimal(number.doubleValue());
            } else if (number instanceof BigDecimal) {
                bigDec = (BigDecimal) number;
            }
            if (bigDec != null) {
                try {
                    return bigDec.longValueExact();
                } catch (ArithmeticException e) {
                    throw new IllegalArgumentException(String.format("%s  : can not be store in a long", bigDec));
                }
            }
        }

        // handle UNSIGNED
        if (number instanceof ULong) {
            long longValue = number.longValue();
            // if long value is negative this means that this is a too long unsigned long
            if (longValue < 0) {
                throw new IllegalArgumentException(String.format("%s  : can not be store in a long", number));
            }
            return longValue;
        }
        throw new IllegalArgumentException(String.format("Can not convert %s to long safely : Unsupported number %s",
                number, number.getClass().getCanonicalName()));
    }

    /**
     * Convert the given number to ULong without loss allowing Floating-point number conversion.
     *
     * @param number the number to turn in long
     * @return a Ulong value for the given number
     *
     * @throws IllegalArgumentException if the number can not be store in a Ulong.
     */
    public static ULong numberToULong(Number number) {
        return numberToULong(number, true);
    }

    /**
     * Convert the given number to ULong without loss.
     *
     * @param number the number to turn in long
     * @param permissiveNumberConversion this will allow Floating-Point number to be converted in Long, else an
     *        exception is raised.
     * @return a Ulong value for the given number
     *
     * @throws IllegalArgumentException if the number can not be store in a Ulong.
     */
    public static ULong numberToULong(Number number, boolean permissiveNumberConversion)
            throws IllegalArgumentException {
        // handle INTEGER
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            long longValue = number.longValue();
            if (longValue < 0) {
                throw new IllegalArgumentException(
                        String.format("%s  : can not convert negative number to an unsigned long", number));
            }
            return ULong.valueOf(longValue);
        }
        if (number instanceof BigInteger) {
            // check big integer is not too big for a long
            BigInteger bigInt = (BigInteger) number;
            if (bigInt.signum() == -1 || bigInt.compareTo(ULong.MAX_VALUE) > 0) {
                throw new IllegalArgumentException(String.format("%s  : can not be store in an unsigned long", bigInt));
            }
            return ULong.valueOf(bigInt);
        }

        // handle FLOATING-POINT
        if (permissiveNumberConversion) {
            BigDecimal bigDec = null;
            if (number instanceof Float || number instanceof Double) {
                bigDec = new BigDecimal(number.doubleValue());
            } else if (number instanceof BigDecimal) {
                bigDec = (BigDecimal) number;
            }
            if (bigDec != null) {
                if (bigDec.signum() == -1) {
                    throw new IllegalArgumentException(
                            String.format("%s  : can not be store in an unsigned long", bigDec));
                } else {
                    try {
                        BigInteger bigInt = bigDec.toBigIntegerExact();
                        if (bigInt.compareTo(ULong.MAX_VALUE) > 0) {
                            throw new IllegalArgumentException(
                                    String.format("%s  : can not be store in an unsigned long", bigInt));
                        }
                        return ULong.valueOf(bigInt);
                    } catch (ArithmeticException e) {
                        throw new IllegalArgumentException(String.format("%s  : can not be store in a long", bigDec));
                    }
                }
            }
        }

        // handle UNSIGNED
        if (number instanceof ULong) {
            return (ULong) number;
        }
        throw new IllegalArgumentException(String.format("Can not convert %s to long safely : Unsupported number %s",
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

    /**
     * Convert the given number to Double with potential precision loss because rounding could be involved.
     *
     * @param number the number to turn in double
     * @param permissiveNumberConversion this will allow Integer to be converted in Double, else an exception is raised.
     * @return a double value for the given number
     *
     * @throws IllegalArgumentException if the number can not be store in a long.
     */
    public static Double numberToDouble(Number number, boolean permissiveNumberConversion) {
        if (permissiveNumberConversion)
            return number.doubleValue();

        if (number instanceof Float || number instanceof Double || number instanceof BigDecimal)
            return number.doubleValue();

        throw new IllegalArgumentException(String.format("Floating-point number expected but was %s (%s)", number,
                number.getClass().getCanonicalName()));
    }

    /**
     * Convert the given long to integer without loss.
     *
     * @throws IllegalArgumentException if the long can not be store in an integer.
     */
    public static int longToInt(long longValue) {
        int intValue = (int) longValue;
        if (intValue != longValue) {
            throw new IllegalArgumentException(String.format("%d cannot be cast to int.", longValue));
        }
        return intValue;
    }
}
