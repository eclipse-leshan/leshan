/*******************************************************************************
 * Copyright (c) 2022 Orange.
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
 *     Orange - Timestamp utilities
 *******************************************************************************/
package org.eclipse.leshan.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;

/**
 * <p>
 * This class contains methods for converting timestamps between <code>BigDecimal</code> and <code>Instant</code>
 * format.
 * </p>
 */
public class TimestampUtil {

    /**
     * <p>
     * Converts an <code>Instant</code> object to a <code>BigDecimal</code> - its value represents epoch time in seconds
     * with additional sub-second precision. Maximal supported precision is nanoseconds.
     * </p>
     *
     * @param timestamp timestamp to be converted
     * @return converted timestamp in seconds
     */
    public static BigDecimal fromInstant(Instant timestamp) {
        if (timestamp == null) {
            return null;
        }

        BigDecimal decimalPoints = new BigDecimal(timestamp.getNano()).divide(new BigDecimal(1_000_000_000));
        return BigDecimal.valueOf(timestamp.getEpochSecond()).add(decimalPoints);
    }

    /**
     * <p>
     * Converts a <code>BigDecimal</code> object to an <code>Instant</code> - the input's integer part should represent
     * epoch time in seconds and the decimal part should represent sub-second precision of the timestamp. Maximal
     * supported precision is nanoseconds - any decimal point beyond that will be cut off and not present in the output
     * value.
     * </p>
     *
     * @param timestampInSeconds timestamp to be converted
     * @return converted timestamp
     * @throws IllegalArgumentException if the timestamp value is larger than maximum of minimum number of epoch seconds
     *         for creating an <code>Instant</code> or the timestamp value precision is more than nanoseconds
     */
    public static Instant fromSeconds(BigDecimal timestampInSeconds) {
        if (timestampInSeconds == null) {
            return null;
        }

        long seconds;
        try {
            seconds = timestampInSeconds.setScale(0, RoundingMode.DOWN).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    String.format("Provided timestamp value: %s is too large or too small to be converted to Double",
                            timestampInSeconds));
        }

        BigDecimal nanos = timestampInSeconds.subtract(BigDecimal.valueOf(seconds))
                .multiply(BigDecimal.valueOf(1_000_000_000));
        if (nanos.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(String.format(
                    "Provided timestamp value: %s is too precise - maximum allowed precision is nanoseconds",
                    timestampInSeconds));
        }

        Instant converted;
        try {
            converted = Instant.ofEpochSecond(seconds, nanos.longValue());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    String.format("Provided timestamp value: %s is too large or too small to be converted to Instant",
                            timestampInSeconds));
        }
        return converted;
    }
}
