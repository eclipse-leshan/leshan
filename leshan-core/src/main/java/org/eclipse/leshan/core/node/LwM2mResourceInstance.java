/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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

import org.eclipse.leshan.core.model.ResourceModel.Type;

/**
 * An instance of {@link LwM2mMultipleResource}.
 */
public class LwM2mResourceInstance implements LwM2mNode {

    private final int id;
    private final Object value;
    private final Type type;

    protected LwM2mResourceInstance(int id, Object value, Type type) {
        LwM2mNodeUtil.validateNotNull(value, "value MUST NOT be null");
        LwM2mNodeUtil.validateResourceInstanceId(id);

        this.id = id;
        this.value = value;
        this.type = type;
    }

    public static LwM2mResourceInstance newInstance(int id, Object value, Type type) {
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
        default:
            throw new LwM2mNodeException(String.format("Type %s is not supported", type.name()));
        }
        return new LwM2mResourceInstance(id, value, type);
    }

    public static LwM2mResourceInstance newStringInstance(int id, String value) {
        return new LwM2mResourceInstance(id, value, Type.STRING);
    }

    public static LwM2mResourceInstance newIntegerInstance(int id, long value) {
        return new LwM2mResourceInstance(id, value, Type.INTEGER);
    }

    public static LwM2mResourceInstance newObjectLinkInstance(int id, ObjectLink objlink) {
        return new LwM2mResourceInstance(id, objlink, Type.OBJLNK);
    }

    public static LwM2mResourceInstance newBooleanInstance(int id, boolean value) {
        return new LwM2mResourceInstance(id, value, Type.BOOLEAN);
    }

    public static LwM2mResourceInstance newFloatInstance(int id, double value) {
        return new LwM2mResourceInstance(id, value, Type.FLOAT);
    }

    public static LwM2mResourceInstance newDateInstance(int id, Date value) {
        return new LwM2mResourceInstance(id, value, Type.TIME);
    }

    public static LwM2mResourceInstance newBinaryInstance(int id, byte[] value) {
        return new LwM2mResourceInstance(id, value, Type.OPAQUE);
    }

    @Override
    public int getId() {
        return id;
    }

    public Object getValue() {
        return value;
    }

    public Type getType() {
        return type;
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
        LwM2mResourceInstance other = (LwM2mResourceInstance) obj;
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
        return String.format("LwM2mResourceInstance [id=%s, value=%s, type=%s]", id,
                type == Type.OPAQUE ? ((byte[]) value).length + "Bytes" : value, type);
    }
}
