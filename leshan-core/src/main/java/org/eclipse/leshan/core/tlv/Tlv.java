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
 *******************************************************************************/
package org.eclipse.leshan.core.tlv;

import java.util.Arrays;

/**
 * A Type-Length-Value container, can contain multiple TLV entries.
 */
public class Tlv {

    // type of TLV, indicate if it's containing a value or TLV containing more TLV or values
    private TlvType type;

    // if of type OBJECT_INSTANCE,MULTIPLE_RESOURCE or null
    private Tlv[] children;

    // if type RESOURCE_VALUE or RESOURCE_INSTANCE => null
    private byte[] value;

    private int identifier;

    /**
     * Creates a TLV container.
     * 
     * @param type the type of TLV
     * @param children the list of children TLV (must be <code>null</code> for type {@link TlvType#RESOURCE_VALUE}
     * @param value the raw contained value, only for type {@link TlvType#RESOURCE_VALUE} <code>null</code> for the
     *        other types
     * @param identifier the TLV identifier (resource id, instance id,..)
     */
    public Tlv(TlvType type, Tlv[] children, byte[] value, int identifier) {
        this.type = type;
        this.children = children;
        this.value = value;
        this.identifier = identifier;

        if (type == TlvType.RESOURCE_VALUE || type == TlvType.RESOURCE_INSTANCE) {
            if (value == null) {
                throw new IllegalArgumentException("a " + type.name() + " must have a value");
            } else if (children != null) {
                throw new IllegalArgumentException("a " + type.name() + " can't have children");
            }
        } else {
            if (value != null) {
                throw new IllegalArgumentException("a " + type.name() + " can't have a value");
            } else if (children == null) {
                throw new IllegalArgumentException("a " + type.name() + " must have children");
            }
        }
    }

    public TlvType getType() {
        return type;
    }

    public void setType(TlvType type) {
        this.type = type;
    }

    public Tlv[] getChildren() {
        return children;
    }

    public void setChildren(Tlv[] children) {
        this.children = children;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public enum TlvType {
        OBJECT_INSTANCE, RESOURCE_INSTANCE, MULTIPLE_RESOURCE, RESOURCE_VALUE;
    }

    @Override
    public String toString() {
        return String.format("Tlv [type=%s, children=%s, value=%s, identifier=%s]", new Object[] { type.name(),
                Arrays.toString(children), Arrays.toString(value), Integer.toString(identifier) });
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(children);
        result = prime * result + identifier;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + Arrays.hashCode(value);
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
        Tlv other = (Tlv) obj;
        if (!Arrays.equals(children, other.children))
            return false;
        if (identifier != other.identifier)
            return false;
        if (type != other.type)
            return false;
        if (!Arrays.equals(value, other.value))
            return false;
        return true;
    }
}
