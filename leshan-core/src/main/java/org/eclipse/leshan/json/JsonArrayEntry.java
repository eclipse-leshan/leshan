/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Gemalto M2M GmbH
 *******************************************************************************/

package org.eclipse.leshan.json;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;

import com.google.gson.annotations.SerializedName;

public class JsonArrayEntry {

    @SerializedName("n")
    private String name;

    @SerializedName("v")
    private Number floatValue;

    @SerializedName("bv")
    private Boolean booleanValue;

    @SerializedName("ov")
    private String objectLinkValue;

    @SerializedName("sv")
    private String stringValue;

    @SerializedName("t")
    private Long time;

    public ResourceModel.Type getType() {
        if (booleanValue != null) {
            return Type.BOOLEAN;
        }
        if (floatValue != null) {
            return Type.FLOAT;
        }
        if (objectLinkValue != null) {
            // TODO handle object link or not ..
            return null;
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

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public void setFloatValue(Number floatValue) {
        this.floatValue = floatValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public void setObjectLinkValue(String objectLinkValue) {
        this.objectLinkValue = objectLinkValue;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((booleanValue == null) ? 0 : booleanValue.hashCode());
        result = prime * result + ((floatValue == null) ? 0 : floatValue.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((objectLinkValue == null) ? 0 : objectLinkValue.hashCode());
        result = prime * result + ((stringValue == null) ? 0 : stringValue.hashCode());
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonArrayEntry other = (JsonArrayEntry) obj;
        if (booleanValue == null) {
            if (other.booleanValue != null)
                return false;
        } else if (!booleanValue.equals(other.booleanValue))
            return false;
        if (floatValue == null) {
            if (other.floatValue != null)
                return false;
        } else if (!floatValue.equals(other.floatValue))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (objectLinkValue == null) {
            if (other.objectLinkValue != null)
                return false;
        } else if (!objectLinkValue.equals(other.objectLinkValue))
            return false;
        if (stringValue == null) {
            if (other.stringValue != null)
                return false;
        } else if (!stringValue.equals(other.stringValue))
            return false;
        if (time != other.time)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "JsonArrayElement [name=%s, floatValue=%s, booleanValue=%s, objectLinkValue=%s, stringValue=%s, time=%s]",
                name, floatValue, booleanValue, objectLinkValue, stringValue, time);
    }

}
