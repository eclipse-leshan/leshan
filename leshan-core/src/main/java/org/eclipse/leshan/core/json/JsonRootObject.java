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

import java.util.Collections;
import java.util.List;

/**
 * The class representing the JSON format of LWM2M
 */
public class JsonRootObject {

    private String baseName = null;

    private List<JsonArrayEntry> jsonArray;

    private Long baseTime;

    public JsonRootObject() {
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

    public void setResourceList(List<JsonArrayEntry> jsonArray) {
        this.jsonArray = jsonArray;
    }

    public List<JsonArrayEntry> getResourceList() {
        if (jsonArray == null)
            return Collections.emptyList();
        return jsonArray;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseName == null) ? 0 : baseName.hashCode());
        result = prime * result + ((baseTime == null) ? 0 : baseTime.hashCode());
        result = prime * result + ((jsonArray == null) ? 0 : jsonArray.hashCode());
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
        JsonRootObject other = (JsonRootObject) obj;
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
        if (jsonArray == null) {
            if (other.jsonArray != null)
                return false;
        } else if (!jsonArray.equals(other.jsonArray))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("LwM2mJsonElement [baseName=%s, baseTime=%d, resourceList=%s]", baseName, baseTime,
                jsonArray);
    }

}
