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
package org.eclipse.leshan.core.model;

/**
 * A resource description
 * 
 * @see "LWM2M specification D.1 Object Template."
 * @see <a href="http://openmobilealliance.org/tech/profiles/LWM2M.xsd">LWM2M Editor Schema</a>
 */
public class ResourceModel {

    public enum Operations {
        NONE, R, W, RW, E;

        public boolean isReadable() {
            return this == R || this == RW;
        }

        public boolean isWritable() {
            return this == W || this == RW;
        }

        public boolean isExecutable() {
            return this == E;
        }
    }

    public enum Type {
        NONE, STRING, INTEGER, FLOAT, BOOLEAN, OPAQUE, TIME, OBJLNK
    }

    public final Integer id;
    public final String name;
    public final Operations operations;
    public final Boolean multiple;
    public final Boolean mandatory;
    public final Type type;
    public final String rangeEnumeration;
    public final String units;
    public final String description;

    public ResourceModel(Integer id, String name, Operations operations, Boolean multiple, Boolean mandatory, Type type,
            String rangeEnumeration, String units, String description) {
        this.id = id;
        this.name = name;
        this.operations = operations;
        this.multiple = multiple;
        this.mandatory = mandatory;
        this.type = type;
        this.rangeEnumeration = rangeEnumeration;
        this.units = units;
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ResourceDesc [id=").append(id).append(", name=").append(name).append(", operations=")
                .append(operations).append(", multiple=").append(multiple).append(", mandatory=").append(mandatory)
                .append(", type=").append(type).append(", rangeEnumeration=").append(rangeEnumeration)
                .append(", units=").append(units).append(", description=").append(description).append("]");
        return builder.toString();
    }
}
