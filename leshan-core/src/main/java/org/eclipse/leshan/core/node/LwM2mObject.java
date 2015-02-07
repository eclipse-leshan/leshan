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
 *******************************************************************************/
package org.eclipse.leshan.core.node;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.util.Validate;

/**
 * The top level element in the LWM2M resource tree.
 * <p>
 * An Objects defines a grouping of Resources and may consist of multiple instances.
 * </p>
 */
public class LwM2mObject implements LwM2mNode {

    private int id;

    private final Map<Integer, LwM2mObjectInstance> instances;

    public LwM2mObject(int id, LwM2mObjectInstance[] instances) {
        Validate.notNull(instances);

        this.id = id;
        this.instances = new HashMap<>(instances.length);
        for (LwM2mObjectInstance instance : instances) {
            this.instances.put(instance.getId(), instance);
        }
    }

    @Override
    public void accept(LwM2mNodeVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return id;
    }

    /**
     * Returns a map of object intances by id.
     *
     * @return the instances
     */
    public Map<Integer, LwM2mObjectInstance> getInstances() {
        return Collections.unmodifiableMap(instances);
    }

    @Override
    public String toString() {
        return String.format("LwM2mObject [id=%s, instances=%s]", id, instances);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((instances == null) ? 0 : instances.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LwM2mObject other = (LwM2mObject) obj;
        if (id != other.id) {
            return false;
        }
        if (instances == null) {
            if (other.instances != null) {
                return false;
            }
        } else if (!instances.equals(other.instances)) {
            return false;
        }
        return true;
    }

}
