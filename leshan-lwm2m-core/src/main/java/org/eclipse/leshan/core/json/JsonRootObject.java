/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Gemalto M2M GmbH
 *******************************************************************************/

package org.eclipse.leshan.core.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The class representing the JSON format of LWM2M
 */
public class JsonRootObject {

    private final String baseName;

    private final List<JsonArrayEntry> jsonArray;

    private final BigDecimal baseTime;

    public JsonRootObject(String baseName, List<JsonArrayEntry> jsonArray, BigDecimal baseTime) {
        this.baseName = baseName;
        this.jsonArray = Collections.unmodifiableList(new ArrayList<>(jsonArray));
        this.baseTime = baseTime;
    }

    public String getBaseName() {
        return baseName;
    }

    public BigDecimal getBaseTime() {
        return baseTime;
    }

    public List<JsonArrayEntry> getResourceList() {
        if (jsonArray == null)
            return Collections.emptyList();
        return jsonArray;
    }

    @Override
    public String toString() {
        return String.format("LwM2mJsonElement [baseName=%s, baseTime=%s, resourceList=%s]", baseName, baseTime,
                jsonArray);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JsonRootObject))
            return false;
        JsonRootObject that = (JsonRootObject) o;

        boolean comparablyEqual = (baseTime == null && that.baseTime == null)
                || (baseTime != null && that.baseTime != null && baseTime.compareTo(that.baseTime) == 0);

        return Objects.equals(baseName, that.baseName) && Objects.equals(jsonArray, that.jsonArray) && comparablyEqual;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(baseName, jsonArray, baseTime != null ? baseTime.stripTrailingZeros() : null);
    }
}
