/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.senml;

import java.math.BigDecimal;
import java.time.Instant;

import org.eclipse.leshan.core.util.TimestampUtil;

/**
 * Utility class used to resolve SenML record.
 *
 * @see ResolvedSenMLRecord
 */
public abstract class SenMLResolver<T extends ResolvedSenMLRecord> {

    private BigDecimal currentTimestampInSeconds = TimestampUtil.fromInstant(Instant.now());
    private String currentBasename = null;
    private BigDecimal currentBasetime = null;

    public T resolve(SenMLRecord record) throws SenMLException {
        // Resolve SenML name (see https://tools.ietf.org/html/rfc8428#section-4.5.1)
        if (record.getBaseName() != null)
            currentBasename = record.getBaseName();

        String resolvedName = null;
        if (currentBasename != null || record.getName() != null) {
            String baseName = currentBasename != null ? currentBasename : "";
            resolvedName = record.getName() == null ? baseName : baseName + record.getName();
        }

        // Resolve SenML time (https://tools.ietf.org/html/rfc8428#section-4.5.3)
        BigDecimal resolvedTimestamp = null;
        if (record.getBaseTime() != null)
            currentBasetime = record.getBaseTime();
        if (currentBasetime != null || record.getTime() != null) {
            BigDecimal basetime = currentBasetime != null ? currentBasetime : BigDecimal.ZERO;
            resolvedTimestamp = record.getTime() != null ? basetime.add(record.getTime()) : basetime;

            // Values less than 268,435,456 (2**28) represent time relative to the current time.
            // A negative value indicates seconds in the past from roughly "now".
            // Positive values up to 2**28 indicate seconds in the future from "now".
            if (resolvedTimestamp.compareTo(BigDecimal.valueOf(268_435_456)) < 0) {
                resolvedTimestamp = currentTimestampInSeconds.add(resolvedTimestamp);
            }
            // else
            // Values greater than or equal to 2**28 represent an absolute time relative to the Unix epoch
            // (1970-01-01T00:00Z in UTC time)
        }

        return createResolvedRecord(record, resolvedName, resolvedTimestamp);
    }

    protected abstract T createResolvedRecord(SenMLRecord record, String resolvedName, BigDecimal resolvedTimestamp)
            throws SenMLException;
}
