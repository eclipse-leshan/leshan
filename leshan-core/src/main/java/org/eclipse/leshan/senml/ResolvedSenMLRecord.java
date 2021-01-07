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

/**
 * A resolved SenML record where name and time is resolved for each record, meaning that time or name is no more
 * relative to a previous <code>basename</code> or <code>basetime</code>.
 */
public class ResolvedSenMLRecord {
    private SenMLRecord record;
    private String name;
    private Long timestamp;

    public ResolvedSenMLRecord(SenMLRecord unresolvedRecord, String resolvedName, Long resolvedTimestamp) {
        super();
        this.record = unresolvedRecord;
        this.name = resolvedName;
        this.timestamp = resolvedTimestamp;
    }

    /**
     * @return the original record before we resolve it.
     */
    public SenMLRecord getRecord() {
        return record;
    }

    /**
     * @return the resolved name. (see
     *         <a href="https://tools.ietf.org/html/rfc8428#section-4.5.1">rfc8428#section-4.5.1</a>)
     */
    public String getName() {
        return name;
    }

    /**
     * @return the resolved timestamp (see
     *         <a href="https://tools.ietf.org/html/rfc8428#section-4.5.3">rfc8428#section-4.5.3</a>)
     */
    public Long getTimeStamp() {
        return timestamp;
    }
}