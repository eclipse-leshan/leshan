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

import java.util.Arrays;

import org.eclipse.leshan.util.Validate;

/**
 * A resource is an information made available by the LWM2M Client.
 * <p>
 * A resource may have a single {@link Value} or consist of multiple instances.
 * </p>
 */
public class LwM2mResource implements LwM2mNode {

    private final int id;

    private final Value<?>[] values;

    private final boolean isMultiInstances;

    /**
     * New single instance resource
     */
    public LwM2mResource(int id, Value<?> value) {
        Validate.notNull(value);
        this.id = id;
        this.values = new Value[] { value };
        this.isMultiInstances = false;
    }

    /**
     * New multiple instances resource
     */
    public LwM2mResource(int id, Value<?>[] values) {
        Validate.notEmpty(values);
        this.id = id;
        this.values = Arrays.copyOf(values, values.length);
        this.isMultiInstances = true;
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
     * @return the resource value (for a single instance resource) or the first value (for a multi-instance resource)
     */
    public Value<?> getValue() {
        return values[0];
    }

    /**
     * @return the resource values
     */
    public Value<?>[] getValues() {
        return values;
    }

    /**
     * @return <code>true</code> if this is a resource supporting multiple instances and <code>false</code> otherwise
     */
    public boolean isMultiInstances() {
        return isMultiInstances;
    }

    @Override
    public String toString() {
        return String.format("LwM2mResource [id=%s, values=%s, isMultiInstances=%s]", id, Arrays.toString(values),
                isMultiInstances);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + (isMultiInstances ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(values);
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
        LwM2mResource other = (LwM2mResource) obj;
        if (id != other.id)
            return false;
        if (isMultiInstances != other.isMultiInstances)
            return false;
        if (!Arrays.equals(values, other.values))
            return false;
        return true;
    }

}
