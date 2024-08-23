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

    private String name;

    private Number floatValue;

    private Boolean booleanValue;

    private String objectLinkValue;

    private String stringValue;

    private BigDecimal time;

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

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTime() {
        return time;
    }

    public void setTime(BigDecimal time) {
        this.time = time;
    }

    public Number getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(Number floatValue) {
        this.floatValue = floatValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public String getObjectLinkValue() {
        return objectLinkValue;
    }

    public void setObjectLinkValue(String objectLinkValue) {
        this.objectLinkValue = objectLinkValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
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
