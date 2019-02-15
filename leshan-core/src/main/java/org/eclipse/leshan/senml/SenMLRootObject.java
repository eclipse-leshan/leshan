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
 *     Cavenaghi9 - initial API and implementation
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

    private List<SenMLDataPoint> dataPoints;

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
    
    public void addDataPoint(SenMLDataPoint dataPoint) {
        if(dataPoints ==null) {
            dataPoints = new ArrayList<>();
        }
        
        dataPoints.add(dataPoint);
    }

    public void setDataPoints(List<SenMLDataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public List<SenMLDataPoint> getDataPoints() {
        if (dataPoints == null)
            return Collections.emptyList();
        return dataPoints;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseName == null) ? 0 : baseName.hashCode());
        result = prime * result + ((baseTime == null) ? 0 : baseTime.hashCode());
        result = prime * result + ((dataPoints == null) ? 0 : dataPoints.hashCode());
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
        if (dataPoints == null) {
            if (other.dataPoints != null)
                return false;
        } else if (!dataPoints.equals(other.dataPoints))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("SenMLObj [baseName=%s, baseTime=%d, dataPoints=%s]", baseName, baseTime,
                dataPoints);
    }
}
