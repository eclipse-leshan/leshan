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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.eclipse.leshan.core.model.ResourceModel.Type;

/**
 * A resource which contains several resource instances.
 * 
 * A resource instance is defined by a numeric identifier and a value. There are accessible via {@link #getValues()}
 */
public class LwM2mMultipleResource implements LwM2mResource {

    private final int id;

    private final Map<Integer, Object> values;

    private final Type type;

    protected LwM2mMultipleResource(int id, Map<Integer, ?> values, Type type) {
        LwM2mNodeUtil.validateNotNull(values, "values MUST NOT be null");
        LwM2mNodeUtil.validateResourceId(id);

        for (Integer instanceId : values.keySet()) {
            LwM2mNodeUtil.validateResourceInstanceId(instanceId);
        }
        this.id = id;
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
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
     * @exception NoSuchElementException use {@link #getValue(int)} or {@link #getValue(int)} instead.
     */
    @Override
    public Object getValue() {
        throw new NoSuchElementException("There is no 'value' on multiple resources, use getValues() instead.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, ?> getValues() {
        return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue(int id) {
        return values.get(id);
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
        result = prime * result + ((values == null) ? 0 : internalHashCode(values));
        return result;
    }

    /**
     * This is a copy of {@link AbstractMap#hashCode()} with custom code to handle byte array equality
     */
    private int internalHashCode(Map<?, ?> m) {
        int h = 0;
        Iterator<?> i = m.entrySet().iterator();
        if (type == Type.OPAQUE) {
            // Custom hashcode to handle byte arrays
            while (i.hasNext()) {
                Entry<?, ?> e = (Entry<?, ?>) i.next();
                h += Objects.hashCode(e.getKey()) ^ Arrays.hashCode((byte[]) e.getValue());
            }
        } else {
            while (i.hasNext()) {
                Entry<?, ?> e = (Entry<?, ?>) i.next();
                h += Objects.hashCode(e.getKey()) ^ Objects.hashCode(e.getValue());
            }
        }
        return h;
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
        if (values == null) {
            if (other.values != null)
                return false;
            // Custom equals to handle byte arrays
        } else if (!internalMapEquals(values, other.values))
            return false;
        return true;
    }

    /**
     * This is a copy of {@link AbstractMap#equals(Object)} with custom code to handle byte array equality
     */
    private boolean internalMapEquals(Map<?, ?> m1, Object o2) {
        if (o2 == this)
            return true;

        if (!(o2 instanceof Map))
            return false;
        Map<?, ?> m2 = (Map<?, ?>) o2;
        if (m2.size() != m1.size())
            return false;

        try {
            for (Object o : m1.entrySet()) {
                Entry<?, ?> e = (Entry<?, ?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                if (value == null) {
                    if (!(m2.get(key) == null && m2.containsKey(key)))
                        return false;
                } else {
                    // Custom equals to handle byte arrays
                    return type == Type.OPAQUE ? Arrays.equals((byte[]) value, (byte[]) m2.get(key))
                            : value.equals(m2.get(key));
                }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        Object printableValue;
        if (type == Type.OPAQUE) {
            // We don't print OPAQUE value as this could be credentials one.
            // Not ideal but didn't find better way for now.
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            Iterator<Entry<Integer, Object>> iter = values.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<Integer, Object> entry = iter.next();
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(((byte[]) entry.getValue()).length + "Bytes");
                if (iter.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            printableValue = sb.toString();
        } else {
            printableValue = values;
        }
        return String.format("LwM2mMultipleResource [id=%s, values=%s, type=%s]", id, printableValue, type);
    }

}
