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

package org.eclipse.leshan.senml;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;

public class SenMLDataPoint {

    private String name;
    private Long time;
    
    private Number floatValue;
    private Boolean booleanValue;
    private String objectLinkValue;
    private String stringValue;
    private Long timeValue;
    private byte[] opaqueValue;

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
        if (timeValue != null) {
            return Type.TIME;
        }
        if (opaqueValue != null) {
            return Type.OPAQUE;
        }
        return null;
    }
    
    public void setTime(Long time) {
        this.time = time;
    }
    
    public Long getTime() {
        return time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTimeValue() {
        return timeValue;
    }

    public void setTimeValue(Long time) {
        this.timeValue = time;
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

    public void setOpaqueValue(byte[] opaqueValue) {
        this.opaqueValue = opaqueValue;
    }

    public byte[] getOpaqueValue() {
        return opaqueValue;
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
        if (timeValue != null) {
            return timeValue;
        }
        if (opaqueValue != null) {
            return opaqueValue;
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
        result = prime * result + ((timeValue == null) ? 0 : timeValue.hashCode());
        result = prime * result + ((opaqueValue == null) ? 0 : opaqueValue.hashCode());
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
        SenMLDataPoint other = (SenMLDataPoint) obj;
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
        if (timeValue == null) {
            if (other.timeValue != null)
                return false;
        } else if (!timeValue.equals(other.timeValue))
            return false;
        
        if (opaqueValue == null) {
            if (other.opaqueValue != null)
                return false;
        } else if (!opaqueValue.equals(other.opaqueValue))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "SenMLDataPoint [name=%s, floatValue=%s, booleanValue=%s, objectLinkValue=%s, stringValue=%s, timeValue=%s, opaque=%s]",
                name, floatValue, booleanValue, objectLinkValue, stringValue, timeValue, opaqueValue);
    }
}
