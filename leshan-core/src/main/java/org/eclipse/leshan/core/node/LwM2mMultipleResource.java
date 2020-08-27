/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.core.node;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.core.util.datatype.ULong;

/**
 * A resource which contains several resource instances.
 * 
 * A resource instance is defined by a numeric identifier and a value. There are accessible via {@link #getInstances()}
 */
public class LwM2mMultipleResource implements LwM2mResource {

    private final int id;

    private final Map<Integer, LwM2mResourceInstance> instances;

    private final Type type;

    public LwM2mMultipleResource(int id, Type type, Collection<LwM2mResourceInstance> instances) {
        Validate.notNull(instances);
        Validate.notNull(type);
        LwM2mNodeUtil.validateResourceId(id);

        this.id = id;
        this.type = type;
        Map<Integer, LwM2mResourceInstance> instancesMap = new HashMap<>(instances.size());
        for (LwM2mResourceInstance instance : instances) {
            if (type != instance.getType())
                new LwM2mNodeException("Invalid resource instance %d, type is %s but resource is %s.", id,
                        instance.getType(), type);
            instancesMap.put(instance.getId(), instance);
        }
        this.instances = Collections.unmodifiableMap(instancesMap);
    }

    public LwM2mMultipleResource(int id, Type type, LwM2mResourceInstance... instances) {
        this(id, type, Arrays.asList(instances));
    }

    protected LwM2mMultipleResource(int id, Map<Integer, ?> values, Type type) {
        LwM2mNodeUtil.validateNotNull(values, "values MUST NOT be null");
        LwM2mNodeUtil.validateResourceId(id);

        for (Integer instanceId : values.keySet()) {
            LwM2mNodeUtil.validateResourceInstanceId(instanceId);
        }
        this.id = id;
        Map<Integer, LwM2mResourceInstance> val = new HashMap<>();
        for (Entry<Integer, ?> entry : values.entrySet()) {
            val.put(entry.getKey(), LwM2mResourceInstance.newInstance(entry.getKey(), entry.getValue(), type));
        }
        this.instances = Collections.unmodifiableMap(val);
        this.type = type;
    }

    public static LwM2mMultipleResource newResource(int id, Map<Integer, ?> values, Type type) {
        LwM2mNodeUtil.validateNotNull(values, "values MUST NOT be null");
        switch (type) {
        case INTEGER:
            LwM2mNodeUtil.allElementsOfType(values.values(), Long.class);
            break;
        case FLOAT:
            LwM2mNodeUtil.allElementsOfType(values.values(), Double.class);
            break;
        case BOOLEAN:
            LwM2mNodeUtil.allElementsOfType(values.values(), Boolean.class);
            break;
        case OPAQUE:
            LwM2mNodeUtil.allElementsOfType(values.values(), byte[].class);
            break;
        case STRING:
            LwM2mNodeUtil.allElementsOfType(values.values(), String.class);
            break;
        case TIME:
            LwM2mNodeUtil.allElementsOfType(values.values(), Date.class);
            break;
        case OBJLNK:
            LwM2mNodeUtil.allElementsOfType(values.values(), ObjectLink.class);
            break;
        case UNSIGNED_INTEGER:
            LwM2mNodeUtil.allElementsOfType(values.values(), ULong.class);
            break;
        default:
            throw new LwM2mNodeException(String.format("Type %s is not supported", type.name()));
        }
        return new LwM2mMultipleResource(id, values, type);
    }

    public static LwM2mMultipleResource newStringResource(int id, Map<Integer, String> values) {
        LwM2mNodeUtil.noNullElements(values.values());
        return new LwM2mMultipleResource(id, values, Type.STRING);
    }

    public static LwM2mMultipleResource newIntegerResource(int id, Map<Integer, Long> values) {
        LwM2mNodeUtil.noNullElements(values.values());
        return new LwM2mMultipleResource(id, values, Type.INTEGER);
    }

    public static LwM2mMultipleResource newBooleanResource(int id, Map<Integer, Boolean> values) {
        LwM2mNodeUtil.noNullElements(values.values());
        return new LwM2mMultipleResource(id, values, Type.BOOLEAN);
    }

    public static LwM2mMultipleResource newFloatResource(int id, Map<Integer, Double> values) {
        LwM2mNodeUtil.noNullElements(values.values());
        return new LwM2mMultipleResource(id, values, Type.FLOAT);
    }

    public static LwM2mMultipleResource newDateResource(int id, Map<Integer, Date> values) {
        LwM2mNodeUtil.noNullElements(values.values());
        return new LwM2mMultipleResource(id, values, Type.TIME);
    }

    public static LwM2mMultipleResource newObjectLinkResource(int id, Map<Integer, ObjectLink> values) {
        LwM2mNodeUtil.noNullElements(values.values());
        return new LwM2mMultipleResource(id, values, Type.OBJLNK);
    }

    public static LwM2mMultipleResource newBinaryResource(int id, Map<Integer, byte[]> values) {
        LwM2mNodeUtil.noNullElements(values.values());
        return new LwM2mMultipleResource(id, values, Type.OPAQUE);
    }

    public static LwM2mMultipleResource newUnsignedIntegerResource(int id, Map<Integer, ULong> values) {
        LwM2mNodeUtil.noNullElements(values.values());
        return new LwM2mMultipleResource(id, values, Type.UNSIGNED_INTEGER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return type;
    }

    /**
     * @exception NoSuchElementException use {@link #getValue(int)} or {@link #getInstances()} or
     *            {@link #getInstance(int)} instead.
     */
    @Override
    public Object getValue() {
        throw new NoSuchElementException("There is no 'value' on multiple resources, use getValues() instead.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue(int id) {
        LwM2mResourceInstance resourceInstance = instances.get(id);
        if (resourceInstance != null)
            return resourceInstance.getValue();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LwM2mResourceInstance getInstance(int id) {
        return instances.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, LwM2mResourceInstance> getInstances() {
        return instances;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultiInstances() {
        return true;
    }

    @Override
    public void accept(LwM2mNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((instances == null) ? 0 : instances.hashCode());
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
        LwM2mMultipleResource other = (LwM2mMultipleResource) obj;
        if (id != other.id)
            return false;
        if (type != other.type)
            return false;
        if (instances == null) {
            if (other.instances != null)
                return false;
        } else if (!instances.equals(other.instances))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("LwM2mMultipleResource [id=%s, values=%s, type=%s]", id, instances, type);
    }

}
