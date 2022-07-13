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
package org.eclipse.leshan.core.datatype;

import static org.eclipse.leshan.core.util.datatype.NumberUtil.longToInt;
import static org.eclipse.leshan.core.util.datatype.NumberUtil.numberToLong;
import static org.eclipse.leshan.core.util.datatype.NumberUtil.numberToULong;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.leshan.core.util.datatype.ULong;
import org.junit.Test;

public class NumberUtilTest {

    @Test
    public void convert_number_to_long() {
        assertEquals(Long.valueOf(-128l), numberToLong(Byte.valueOf("-128")));
        assertEquals(Long.valueOf(127l), numberToLong(Byte.valueOf("127")));

        assertEquals(Long.valueOf(-32768l), numberToLong(Short.valueOf("-32768")));
        assertEquals(Long.valueOf(32767l), numberToLong(Short.valueOf("32767")));

        assertEquals(Long.valueOf(-2147483648l), numberToLong(Integer.valueOf("-2147483648")));
        assertEquals(Long.valueOf(2147483647l), numberToLong(Integer.valueOf("2147483647")));

        assertEquals(Long.valueOf(-9223372036854775808l), numberToLong(Long.valueOf("-9223372036854775808")));
        assertEquals(Long.valueOf(9223372036854775807l), numberToLong(Long.valueOf("9223372036854775807")));

        assertEquals(Long.valueOf(-9223372036854775808l), numberToLong(new BigInteger("-9223372036854775808")));
        assertEquals(Long.valueOf(9223372036854775807l), numberToLong(new BigInteger("9223372036854775807")));

        assertEquals(Long.valueOf(0l), numberToLong(ULong.valueOf("0")));
        assertEquals(Long.valueOf(9223372036854775807l), numberToLong(ULong.valueOf("9223372036854775807")));

        // floating point
        assertEquals(Long.valueOf(-9223371487098961920l), numberToLong(Float.valueOf("-9223371487098961920")));
        assertEquals(Long.valueOf(9223371487098961920l), numberToLong(Float.valueOf("9223371487098961920")));

        assertEquals(Long.valueOf(-9223372036854775808l), numberToLong(Double.valueOf("-9223372036854775808")));
        assertEquals(Long.valueOf(9223372036854774784l), numberToLong(Double.valueOf("9223372036854774800")));

        assertEquals(Long.valueOf(-9223372036854775808l), numberToLong(new BigDecimal("-9223372036854775808")));
        assertEquals(Long.valueOf(9223372036854775807l), numberToLong(new BigDecimal("9223372036854775807")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void biginteger_too_small_for_long() {
        numberToLong(new BigInteger("-9223372036854775809"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void biginteger_too_big_for_long() {
        numberToLong(new BigInteger("9223372036854775808"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ulong_too_big_for_long() {
        numberToLong(ULong.valueOf("9223372036854775808"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void float_too_small_for_long() {
        numberToLong(Float.valueOf("-9223373136366403584"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void float_too_big_for_long() {
        numberToLong(Float.valueOf("9223372036854775808"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void float_with_decimal_for_long() {
        numberToLong(Float.valueOf(30.50f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void double_too_small_for_long() {
        numberToLong(Double.valueOf("-9223373136366403584"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void double_too_big_for_long() {
        numberToLong(Double.valueOf("9223372036854775808"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void double_with_decimal() {
        numberToLong(Double.valueOf(30.50d));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bigdecimal_too_small_for_long() {
        numberToLong(new BigDecimal("-9223372036854775809"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bigdecimal_too_big_for_long() {
        numberToLong(new BigDecimal("9223372036854775808"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bigdecimal_with_decimal_for_long() {
        numberToLong(new BigDecimal(30.50f));
    }

    @Test
    public void convert_number_to_ulong() {
        assertEquals(ULong.valueOf("0"), numberToULong(Byte.valueOf("0")));
        assertEquals(ULong.valueOf("127"), numberToULong(Byte.valueOf("127")));

        assertEquals(ULong.valueOf("0"), numberToULong(Short.valueOf("0")));
        assertEquals(ULong.valueOf("32767"), numberToULong(Short.valueOf("32767")));

        assertEquals(ULong.valueOf("0"), numberToULong(Integer.valueOf("0")));
        assertEquals(ULong.valueOf("2147483647"), numberToULong(Integer.valueOf("2147483647")));

        assertEquals(ULong.valueOf("0"), numberToULong(Long.valueOf("0")));
        assertEquals(ULong.valueOf("9223372036854775807"), numberToULong(Long.valueOf("9223372036854775807")));

        assertEquals(ULong.valueOf("0"), numberToULong(new BigInteger("0")));
        assertEquals(ULong.valueOf("18446744073709551615"), numberToULong(new BigInteger("18446744073709551615")));

        assertEquals(ULong.valueOf("0"), numberToULong(ULong.valueOf("0")));
        assertEquals(ULong.valueOf("18446744073709551615"), numberToULong(ULong.valueOf("18446744073709551615")));

        // floating point
        assertEquals(ULong.valueOf("0"), numberToULong(Float.valueOf("0")));
        assertEquals(ULong.valueOf("18446742974197923840"), numberToULong(Float.valueOf("18446742974197923840")));

        assertEquals(ULong.valueOf("0"), numberToULong(Double.valueOf("0")));
        assertEquals(ULong.valueOf("18446742974197923840"), numberToULong(Double.valueOf("18446742974197923840")));

        assertEquals(ULong.valueOf("0"), numberToULong(new BigDecimal("0")));
        assertEquals(ULong.valueOf("18446744073709551615"), numberToULong(new BigDecimal("18446744073709551615")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void byte_negative_for_ulong() {
        numberToULong(Byte.valueOf("-1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void short_negative_for_ulong() {
        numberToULong(Short.valueOf("-1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void integer_negative_for_ulong() {
        numberToULong(Integer.valueOf("-1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void long_negative_for_ulong() {
        numberToULong(Long.valueOf("-1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void biginteger_negative_for_ulong() {
        numberToULong(new BigInteger("-1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void float_negative_for_ulong() {
        numberToULong(Float.valueOf("-1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void double_negative_for_ulong() {
        numberToULong(Double.valueOf("-1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bigdecimal_negative_for_ulong() {
        numberToULong(new BigDecimal("-1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void float_with_decimal_for_ulong() {
        numberToULong(Float.valueOf(30.50f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void biginteger_to_big_for_ulong() {
        numberToULong(new BigInteger("18446744073709551616"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void float_too_big_for_ulong() {
        numberToULong(Float.valueOf("18446744073709551616"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void double_too_big_for_ulong() {
        numberToULong(Double.valueOf("18446744073709551616"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bigdecimal_to_big_for_ulong() {
        numberToULong(new BigDecimal("18446744073709551616"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void too_long_to_int() {
        longToInt(2147483648l);
    }
}
