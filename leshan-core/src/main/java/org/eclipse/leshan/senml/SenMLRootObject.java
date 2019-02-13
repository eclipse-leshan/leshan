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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The class representing the SenML format of LWM2M
 */
public class SenMLRootObject {

    private String baseName = null;

    private List<SenMLDataPoint> resourceList;

    private Long baseTime;

    public SenMLRootObject() {
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
    
    public void addResource(SenMLDataPoint dataPoint) {
        if(resourceList ==null) {
            resourceList = new ArrayList<>();
        }
        
        resourceList.add(dataPoint);
    }

    public void setResourceList(List<SenMLDataPoint> resourceList) {
        this.resourceList = resourceList;
    }

    public List<SenMLDataPoint> getResourceList() {
        if (resourceList == null)
            return Collections.emptyList();
        return resourceList;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseName == null) ? 0 : baseName.hashCode());
        result = prime * result + ((baseTime == null) ? 0 : baseTime.hashCode());
        result = prime * result + ((resourceList == null) ? 0 : resourceList.hashCode());
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
        SenMLRootObject other = (SenMLRootObject) obj;
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
        if (resourceList == null) {
            if (other.resourceList != null)
                return false;
        } else if (!resourceList.equals(other.resourceList))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("SenMLObj [baseName=%s, baseTime=%d, resourceList=%s]", baseName, baseTime,
                resourceList);
    }
}
