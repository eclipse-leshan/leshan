/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.senml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The class representing the SenML Pack.
 *
 * @see <a href="https://tools.ietf.org/html/rfc8428#section-4">rfc8428 - Sensor Measurement Lists (SenML)</a>
 */
public class SenMLPack {

    private final List<SenMLRecord> records;

    public SenMLPack() {
        records = new ArrayList<>();
    }

    public SenMLPack(List<SenMLRecord> records) {
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
    }

    public void addRecord(SenMLRecord record) {
        records.add(record);
    }

    public void addRecords(List<SenMLRecord> records) {
        this.records.addAll(records);
    }

    public List<SenMLRecord> getRecords() {
        if (records == null)
            return Collections.emptyList();
        return records;
    }

    @Override
    public String toString() {
        return String.format("SenMLPack [records=%s]", records);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SenMLPack))
            return false;
        SenMLPack senMLPack = (SenMLPack) o;
        return Objects.equals(records, senMLPack.records);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(records);
    }
}
