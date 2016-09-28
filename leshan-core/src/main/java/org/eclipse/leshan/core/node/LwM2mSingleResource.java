/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.util.Validate;

/**
 * A resource with a single value.
 */
public class LwM2mSingleResource implements LwM2mResource {

    private final int id;

    private final Object value;

    private final Type type;

    protected LwM2mSingleResource(int id, Object value, Type type) {
        Validate.notNull(value);
        this.id = id;
        this.value = value;
        this.type = type;
    }

    public static LwM2mSingleResource newResource(int id, Object value, Type type) {
        String doesNotMatchMessage = "Value does not match the given datatype";
        switch (type) {
        case INTEGER:
            if (!(value instanceof Long))
                throw new IllegalArgumentException(doesNotMatchMessage);
            break;
        case FLOAT:
            if (!(value instanceof Double))
                throw new IllegalArgumentException(doesNotMatchMessage);
            break;
        case BOOLEAN:
            if (!(value instanceof Boolean))
                throw new IllegalArgumentException(doesNotMatchMessage);
            break;
        case OPAQUE:
            if (!(value instanceof byte[]))
                throw new IllegalArgumentException(doesNotMatchMessage);
            break;
        case STRING:
            if (!(value instanceof String))
                throw new IllegalArgumentException(doesNotMatchMessage);
            break;
        case TIME:
            if (!(value instanceof Date))
                throw new IllegalArgumentException(doesNotMatchMessage);
            break;
        case OBJLNK:
            if (!(value instanceof ObjectLink))
                throw new IllegalArgumentException(doesNotMatchMessage);
            break;
        default:
            throw new IllegalArgumentException(String.format("Type %s is not supported", type.name()));
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
     * @exception raise a {@link NoSuchElementException}
     */
    @Override
    public Map<Integer, ?> getValues() {
        throw new NoSuchElementException("There is no 'values' on single resources, use getValue() instead.");
    }

    /**
     * @exception raise a {@link NoSuchElementException}
     */
    @Override
    public Object getValue(int id) {
        throw new NoSuchElementException("There is no 'values' on single resources, use getValue() instead.");
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
            return type == Type.OPAQUE ? Arrays.equals((byte[]) value, (byte[]) other.value) : value
                    .equals(other.value);
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("LwM2mSingleResource [id=%s, value=%s, type=%s]", id, value, type);
    }

}
