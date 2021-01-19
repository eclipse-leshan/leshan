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
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.datatype.ULong;

/**
 * A resource with a single value.
 */
public class LwM2mSingleResource implements LwM2mResource {

    private final int id;

    private final Object value;

    private final Type type;

    protected LwM2mSingleResource(int id, Object value, Type type) {
        LwM2mNodeUtil.validateNotNull(value, "value MUST NOT be null");
        LwM2mNodeUtil.validateResourceId(id);

        this.id = id;
        this.value = value;
        this.type = type;
    }

    public static LwM2mSingleResource newResource(int id, Object value) {
        LwM2mNodeUtil.validateNotNull(value, "value MUST NOT be null");

        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return new LwM2mSingleResource(id, ((Number) value).longValue(), Type.INTEGER);
        }
        if (value instanceof Float || value instanceof Double) {
            return new LwM2mSingleResource(id, ((Number) value).doubleValue(), Type.FLOAT);
        }
        if (value instanceof Boolean) {
            return new LwM2mSingleResource(id, value, Type.BOOLEAN);
        }
        if (value instanceof byte[]) {
            return new LwM2mSingleResource(id, value, Type.OPAQUE);
        }
        if (value instanceof String) {
            return new LwM2mSingleResource(id, value, Type.STRING);
        }
        if (value instanceof Date) {
            return new LwM2mSingleResource(id, value, Type.TIME);
        }
        if (value instanceof ObjectLink) {
            return new LwM2mSingleResource(id, value, Type.OBJLNK);
        }
        if (value instanceof ULong) {
            return new LwM2mSingleResource(id, value, Type.UNSIGNED_INTEGER);
        }
        throw new LwM2mNodeException(
                String.format("Unsupported type %s for resource", value.getClass().getCanonicalName()));
    }

    public static LwM2mSingleResource newResource(int id, Object value, Type type) {
        String doesNotMatchMessage = "Value does not match the given datatype";
        switch (type) {
        case INTEGER:
            if (!(value instanceof Long))
                throw new LwM2mNodeException(doesNotMatchMessage);
            break;
        case FLOAT:
            if (!(value instanceof Double))
                throw new LwM2mNodeException(doesNotMatchMessage);
            break;
        case BOOLEAN:
            if (!(value instanceof Boolean))
                throw new LwM2mNodeException(doesNotMatchMessage);
            break;
        case OPAQUE:
            if (!(value instanceof byte[]))
                throw new LwM2mNodeException(doesNotMatchMessage);
            break;
        case STRING:
            if (!(value instanceof String))
                throw new LwM2mNodeException(doesNotMatchMessage);
            break;
        case TIME:
            if (!(value instanceof Date))
                throw new LwM2mNodeException(doesNotMatchMessage);
            break;
        case OBJLNK:
            if (!(value instanceof ObjectLink))
                throw new LwM2mNodeException(doesNotMatchMessage);
            break;
        case UNSIGNED_INTEGER:
            if (!(value instanceof ULong)) {
                throw new LwM2mNodeException(doesNotMatchMessage);
            }
            break;
        default:
            throw new LwM2mNodeException(String.format("Type %s is not supported", type.name()));
        }
        return new LwM2mSingleResource(id, value, type);
    }

    public static LwM2mSingleResource newStringResource(int id, String value) {
        return new LwM2mSingleResource(id, value, Type.STRING);
    }

    public static LwM2mSingleResource newIntegerResource(int id, long value) {
        return new LwM2mSingleResource(id, value, Type.INTEGER);
    }

    public static LwM2mSingleResource newObjectLinkResource(int id, ObjectLink objlink) {
        return new LwM2mSingleResource(id, objlink, Type.OBJLNK);
    }

    public static LwM2mSingleResource newBooleanResource(int id, boolean value) {
        return new LwM2mSingleResource(id, value, Type.BOOLEAN);
    }

    public static LwM2mSingleResource newFloatResource(int id, double value) {
        return new LwM2mSingleResource(id, value, Type.FLOAT);
    }

    public static LwM2mSingleResource newDateResource(int id, Date value) {
        return new LwM2mSingleResource(id, value, Type.TIME);
    }

    public static LwM2mSingleResource newBinaryResource(int id, byte[] value) {
        return new LwM2mSingleResource(id, value, Type.OPAQUE);
    }

    public static LwM2mSingleResource newUnsignedIntegerResource(int id, ULong value) {
        return new LwM2mSingleResource(id, value, Type.UNSIGNED_INTEGER);
    }

    public static LwM2mSingleResource newUnsignedIntegerResource(int id, Long value) {
        if (value >= 0)
            return new LwM2mSingleResource(id, ULong.valueOf(value), Type.UNSIGNED_INTEGER);
        else
            throw new LwM2mNodeException("Invalid value : positive value expected for UNSIGNED_INTEGER");
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
     * {@inheritDoc}
     */
    @Override
    public Object getValue() {
        return value;
    }

    /**
     * @exception NoSuchElementException use {@link #getValue()} instead.
     */
    @Override
    public Object getValue(int id) {
        throw new NoSuchElementException("There is no 'values' on single resources, use getValue() instead.");
    }

    /**
     * @exception NoSuchElementException use {@link #getValue()} instead.
     */
    @Override
    public LwM2mResourceInstance getInstance(int id) {
        throw new NoSuchElementException("There is no 'instance' on single resources, use getValue() instead.");
    }

    /**
     * @exception NoSuchElementException use {@link #getValue()} instead.
     */
    @Override
    public Map<Integer, LwM2mResourceInstance> getInstances() {
        throw new NoSuchElementException("There is no 'instances' on single resources, use getValue() instead.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultiInstances() {
        return false;
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
        if (type == Type.OPAQUE) {
            // Custom hashcode to handle byte arrays
            result = prime * result + ((value == null) ? 0 : Arrays.hashCode((byte[]) value));
        } else {
            result = prime * result + ((value == null) ? 0 : value.hashCode());
        }
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
        LwM2mSingleResource other = (LwM2mSingleResource) obj;
        if (id != other.id)
            return false;
        if (type != other.type)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else {
            // Custom equals to handle byte arrays
            return type == Type.OPAQUE ? Arrays.equals((byte[]) value, (byte[]) other.value)
                    : value.equals(other.value);
        }
        return true;
    }

    @Override
    public String toString() {
        // We don't print OPAQUE value as this could be credentials one.
        // Not ideal but didn't find better way for now.
        return String.format("LwM2mSingleResource [id=%s, value=%s, type=%s]", id,
                type == Type.OPAQUE ? ((byte[]) value).length + "Bytes" : value, type);
    }

}
