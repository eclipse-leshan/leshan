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
import java.util.Objects;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;

public class JsonArrayEntry {

    private final String name;

    private final Number floatValue;

    private final Boolean booleanValue;

    private final String objectLinkValue;

    private final String stringValue;

    private final BigDecimal time;

    public JsonArrayEntry(String name, Number floatValue, Boolean booleanValue, String objectLinkValue,
            String stringValue, BigDecimal time) {
        this.name = name;
        this.floatValue = floatValue;
        this.booleanValue = booleanValue;
        this.objectLinkValue = objectLinkValue;
        this.stringValue = stringValue;
        this.time = time;
    }

    public ResourceModel.Type getType() {
        if (booleanValue != null) {
            return Type.BOOLEAN;
        }
        if (floatValue != null) {
            return Type.FLOAT;
        }
        if (objectLinkValue != null) {
            return Type.OBJLNK;
        }
        if (stringValue != null) {
            return Type.STRING;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTime() {
        return time;
    }

    public Number getFloatValue() {
        return floatValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public String getObjectLinkValue() {
        return objectLinkValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public Object getResourceValue() {

        if (booleanValue != null) {
            return booleanValue;
        }
        if (floatValue != null) {
            return floatValue;
        }
        if (objectLinkValue != null) {
            return objectLinkValue;
        }
        if (stringValue != null) {
            return stringValue;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format(
                "JsonArrayEntry [name=%s, floatValue=%s, booleanValue=%s, objectLinkValue=%s, stringValue=%s, time=%s]",
                name, floatValue, booleanValue, objectLinkValue, stringValue, time);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JsonArrayEntry))
            return false;
        JsonArrayEntry that = (JsonArrayEntry) o;

        boolean comparablyEqual = (time == null && that.time == null)
                || (time != null && that.time != null && time.compareTo(that.time) == 0);

        return Objects.equals(name, that.name) && Objects.equals(floatValue, that.floatValue)
                && Objects.equals(booleanValue, that.booleanValue)
                && Objects.equals(objectLinkValue, that.objectLinkValue)
                && Objects.equals(stringValue, that.stringValue) && comparablyEqual;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(name, floatValue, booleanValue, objectLinkValue, stringValue,
                time != null ? time.stripTrailingZeros() : null);
    }
}
