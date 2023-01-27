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
package org.eclipse.leshan.core.senml.cbor;

import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;

public class SenMLTestUtil {

    public static void assertSenMLPackEquals(SenMLPack expected, SenMLPack actual) {
        if (expected.getRecords().size() != actual.getRecords().size()) {
            fail("Pack not equals : number of records differ");
        }

        for (int i = 0; i < expected.getRecords().size(); i++) {
            SenMLRecord r1 = expected.getRecords().get(i);
            SenMLRecord r2 = actual.getRecords().get(i);
            assertSenMLRecordEquals(r1, r2);
        }
    }

    private static void assertSenMLRecordEquals(SenMLRecord expected, SenMLRecord actual) {
        assertFieldEquals("basename", expected.getBaseName(), actual.getBaseName());
        assertFieldEquals("basetime", expected.getBaseTime(), actual.getBaseTime());
        assertFieldEquals("name", expected.getName(), actual.getName());
        assertFieldEquals("time", expected.getTime(), actual.getTime());
        assertFieldEquals("type", expected.getType(), actual.getType());
        assertFieldEquals("boolean value", expected.getBooleanValue(), actual.getBooleanValue());
        assertFieldEquals("objlink value", expected.getObjectLinkValue(), actual.getObjectLinkValue());
        assertFieldEquals("opaque value", expected.getOpaqueValue(), actual.getOpaqueValue());
        assertFieldEquals("string value", expected.getStringValue(), actual.getStringValue());

        if (!equals(expected.getNumberValue(), actual.getNumberValue())) {
            fail(String.format("Records not equals :number value differ expected %s, actual %s",
                    expected.getNumberValue(), actual.getNumberValue()));
        }
    }

    private static void assertFieldEquals(String fieldName, Object expected, Object actual) {
        if (!Objects.deepEquals(expected, actual)) {
            if (expected instanceof byte[]) {
                expected = Hex.encodeHexString((byte[]) expected);
            }
            if (actual instanceof byte[]) {
                actual = Hex.encodeHexString((byte[]) actual);
            }
            fail(String.format("Records not equals : %s differ expected %s, actual %s", fieldName, expected, actual));
        }
    }

    private static boolean equals(Number x, Number y) {
        if (x == null && y == null)
            return true;
        if (x != null && y == null || x == null && y != null)
            return false;
        return compare(x, y) == 0;
    }

    private static int compare(Number x, Number y) {
        if (isSpecial(x) || isSpecial(y))
            return Double.compare(x.doubleValue(), y.doubleValue());
        else
            return toBigDecimal(x).compareTo(toBigDecimal(y));
    }

    private static boolean isSpecial(Number x) {
        boolean specialDouble = x instanceof Double && (Double.isNaN((Double) x) || Double.isInfinite((Double) x));
        boolean specialFloat = x instanceof Float && (Float.isNaN((Float) x) || Float.isInfinite((Float) x));
        return specialDouble || specialFloat;
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal)
            return (BigDecimal) number;
        if (number instanceof BigInteger)
            return new BigDecimal((BigInteger) number);
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long)
            return new BigDecimal(number.longValue());
        if (number instanceof Float || number instanceof Double)
            return new BigDecimal(number.doubleValue());

        try {
            return new BigDecimal(number.toString());
        } catch (final NumberFormatException e) {
            throw new RuntimeException("The given number (\"" + number + "\" of class " + number.getClass().getName()
                    + ") does not have a parsable string representation", e);
        }
    }
}
