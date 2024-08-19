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
import java.util.Objects;

/**
 * A Type-Length-Value container, can contain multiple TLV entries.
 */
public class Tlv {

    // type of TLV, indicate if it's containing a value or TLV containing more TLV or values
    private final TlvType type;

    // if of type OBJECT_INSTANCE,MULTIPLE_RESOURCE or null
    private final Tlv[] children;

    // if type RESOURCE_VALUE or RESOURCE_INSTANCE => null
    private final byte[] value;

    private final int identifier;

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

    public Tlv[] getChildren() {
        return children;
    }

    public byte[] getValue() {
        return value;
    }

    public int getIdentifier() {
        return identifier;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Tlv))
            return false;
        Tlv tlv = (Tlv) o;
        return identifier == tlv.identifier && type == tlv.type && Objects.deepEquals(children, tlv.children)
                && Arrays.equals(value, tlv.value);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(type, Arrays.hashCode(children), Arrays.hashCode(value), identifier);
    }

    public enum TlvType {
        OBJECT_INSTANCE, RESOURCE_INSTANCE, MULTIPLE_RESOURCE, RESOURCE_VALUE;
    }

    @Override
    public String toString() {
        return String.format("Tlv [type=%s, children=%s, value=%s, identifier=%s]", new Object[] { type.name(),
                Arrays.toString(children), Arrays.toString(value), Integer.toString(identifier) });
    }

}
