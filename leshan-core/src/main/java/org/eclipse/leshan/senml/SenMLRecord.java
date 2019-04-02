/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.senml;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;

public class SenMLRecord {

    private String baseName = null;
    private Long baseTime;

    private String name;
    private Long time;

    private Number floatValue;
    private Boolean booleanValue;
    private String objectLinkValue;
    private String stringValue;
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

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public Long getBaseTime() {
        return baseTime;
    }

    public void setBaseTime(Long baseTime) {
        this.baseTime = baseTime;
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
        if (opaqueValue != null) {
            return opaqueValue;
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseName == null) ? 0 : baseName.hashCode());
        result = prime * result + ((baseTime == null) ? 0 : baseTime.hashCode());
        result = prime * result + ((booleanValue == null) ? 0 : booleanValue.hashCode());
        result = prime * result + ((floatValue == null) ? 0 : floatValue.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((objectLinkValue == null) ? 0 : objectLinkValue.hashCode());
        result = prime * result + ((stringValue == null) ? 0 : stringValue.hashCode());
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
        SenMLRecord other = (SenMLRecord) obj;

        if (baseName == null) {
            if (other.baseName != null)
                return false;
        } else if (!baseName.equals(other.baseName))
            return false;
        if (baseTime == null) {
            if (other.baseTime != null)
                return false;
        } else if (!baseTime.equals(other.baseTime))
            return false;

        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;

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
                "SenMLRecord [baseName=%s, baseTime=%d, name=%s, time=%d, floatValue=%s, booleanValue=%s, objectLinkValue=%s, stringValue=%s, opaque=%s]",
                baseName, baseTime, name, time, floatValue, booleanValue, objectLinkValue, stringValue, opaqueValue);
    }
}
