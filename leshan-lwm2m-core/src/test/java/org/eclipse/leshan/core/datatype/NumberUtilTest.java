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
import static org.eclipse.leshan.core.util.datatype.NumberUtil.numberToDouble;
import static org.eclipse.leshan.core.util.datatype.NumberUtil.numberToLong;
import static org.eclipse.leshan.core.util.datatype.NumberUtil.numberToULong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.leshan.core.util.datatype.ULong;
import org.junit.jupiter.api.Test;

class NumberUtilTest {

    @Test
    void convert_number_to_long() {
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

    @Test
    void biginteger_too_small_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(new BigInteger("-9223372036854775809"));
        });
    }

    @Test
    void biginteger_too_big_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(new BigInteger("9223372036854775808"));
        });
    }

    @Test
    void ulong_too_big_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(ULong.valueOf("9223372036854775808"));
        });
    }

    @Test
    void float_too_small_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(Float.valueOf("-9223373136366403584"));
        });
    }

    @Test
    void float_too_big_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(Float.valueOf("9223372036854775808"));
        });
    }

    @Test
    void float_with_decimal_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(Float.valueOf(30.50f));
        });
    }

    @Test
    void double_too_small_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(Double.valueOf("-9223373136366403584"));
        });
    }

    @Test
    void double_too_big_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(Double.valueOf("9223372036854775808"));
        });
    }

    @Test
    void double_with_decimal() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(Double.valueOf(30.50d));
        });
    }

    @Test
    void bigdecimal_too_small_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(new BigDecimal("-9223372036854775809"));
        });
    }

    @Test
    void bigdecimal_too_big_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(new BigDecimal("9223372036854775808"));
        });
    }

    @Test
    void bigdecimal_with_decimal_for_long() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToLong(new BigDecimal(30.50f));
        });
    }

    @Test
    void convert_number_to_ulong() {
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

    @Test
    void byte_negative_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(Byte.valueOf("-1"));
        });
    }

    @Test
    void short_negative_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(Short.valueOf("-1"));
        });
    }

    @Test
    void integer_negative_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(Integer.valueOf("-1"));
        });
    }

    @Test
    void long_negative_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(Long.valueOf("-1"));
        });
    }

    @Test
    void biginteger_negative_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(new BigInteger("-1"));
        });
    }

    @Test
    void float_negative_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(Float.valueOf("-1"));
        });
    }

    @Test
    void double_negative_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(Double.valueOf("-1"));
        });
    }

    @Test
    void bigdecimal_negative_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(new BigDecimal("-1"));
        });
    }

    @Test
    void float_with_decimal_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(Float.valueOf(30.50f));
        });
    }

    @Test
    void biginteger_to_big_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(new BigInteger("18446744073709551616"));
        });
    }

    @Test
    void float_too_big_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(Float.valueOf("18446744073709551616"));
        });
    }

    @Test
    void double_too_big_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(Double.valueOf("18446744073709551616"));
        });
    }

    @Test
    void bigdecimal_to_big_for_ulong() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToULong(new BigDecimal("18446744073709551616"));
        });
    }

    @Test
    void too_long_to_int() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            longToInt(2147483648l);
        });
    }

    @Test
    void convert_number_to_double() {
        assertEquals(-128d, numberToDouble(Byte.valueOf("-128"), true));
        assertEquals(127d, numberToDouble(Byte.valueOf("127"), true));

        assertEquals(-32768d, numberToDouble(Short.valueOf("-32768"), true));
        assertEquals(32767d, numberToDouble(Short.valueOf("32767"), true));

        assertEquals(-2147483648d, numberToDouble(Integer.valueOf("-2147483648"), true));
        assertEquals(2147483647d, numberToDouble(Integer.valueOf("2147483647"), true));

        // safe limit
        assertEquals(-9007199254740991d, numberToDouble(Long.valueOf("-9007199254740991"), true));
        assertEquals(9007199254740991d, numberToDouble(Long.valueOf("9007199254740991"), true));
        // outside safe limit
        // see for more details : https://observablehq.com/@benaubin/floating-point
        assertEquals(9007199254740992d, numberToDouble(Long.valueOf("9007199254740992"), true));
        assertEquals(9007199254740992d, numberToDouble(Long.valueOf("9007199254740992"), true));
        assertEquals(9007199254740994d, numberToDouble(Long.valueOf("9007199254740994"), true));

        // safe limit
        assertEquals(-9007199254740991d, numberToDouble(new BigInteger("-9007199254740991"), true));
        assertEquals(9007199254740991d, numberToDouble(new BigInteger("9007199254740991"), true));
        // outside safe limit
        // see for more details : https://observablehq.com/@benaubin/floating-point
        assertEquals(-9007199254740992d, numberToDouble(new BigInteger("-9007199254740992"), true));
        assertEquals(9007199254740992d, numberToDouble(new BigInteger("9007199254740992"), true));
        assertEquals(9007199254740994d, numberToDouble(new BigInteger("9007199254740994"), true));

        // floating point
        assertEquals(-340282346638528859811704183484516925440d, numberToDouble(new Float(-Float.MAX_VALUE), true));
        assertEquals(340282346638528859811704183484516925440d, numberToDouble(new Float(Float.MAX_VALUE), true));

        assertEquals(
                -179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368d,
                numberToDouble(new Double(-Double.MAX_VALUE), true));
        assertEquals(
                179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368d,
                numberToDouble(new Double(Double.MAX_VALUE), true));

        assertEquals(
                -179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368d,
                numberToDouble(new BigDecimal(-Double.MAX_VALUE), true));
        assertEquals(
                179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368d,
                numberToDouble(new BigDecimal(Double.MAX_VALUE), true));
    }

    @Test
    void long_does_not_fit_in_for_double() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToDouble(Long.valueOf("9007199254740993"), true);
        });
    }

    @Test
    void biginteger_does_not_fit_in_double() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToDouble(new BigInteger("-9007199254740993"), true);
        });
    }

    @Test
    void bigdecimal_does_not_fit_in_for_double() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToDouble(new BigDecimal(Double.MAX_VALUE).add(new BigDecimal(Double.MAX_VALUE)), true);
        });

        assertThrowsExactly(IllegalArgumentException.class, () -> {
            numberToDouble(new BigDecimal(-Double.MAX_VALUE).add(new BigDecimal(-Double.MAX_VALUE)), true);
        });
    }
}
