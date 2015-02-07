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
package org.eclipse.leshan.core.objectspec;

import java.util.Collections;
import java.util.Map;

/**
 * An object description
 */
public class ObjectSpec {

    public final int id;
    public final String name;
    public final String description;
    public final boolean multiple;
    public final boolean mandatory;

    public final Map<Integer, ResourceSpec> resources; // resources by ID

    public ObjectSpec(int id, String name, String description, boolean multiple, boolean mandatory,
            Map<Integer, ResourceSpec> resources) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.multiple = multiple;
        this.mandatory = mandatory;
        this.resources = Collections.unmodifiableMap(resources);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ObjectDesc [id=").append(id).append(", name=").append(name).append(", description=")
                .append(description).append(", multiple=").append(multiple).append(", mandatory=").append(mandatory)
                .append(", resources=").append(resources).append("]");
        return builder.toString();
    }
}
