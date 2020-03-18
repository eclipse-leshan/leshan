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
package org.eclipse.leshan.core.node;

import java.util.Date;
import java.util.Map;

import org.eclipse.leshan.core.model.ResourceModel.Type;

/**
 * A resource is an information made available by the LWM2M Client.
 * <p>
 * A resource could be a {@link LwM2mSingleResource} or a {@link LwM2mMultipleResource}.
 * </p>
 */
public interface LwM2mResource extends LwM2mNode {

    /**
     * @return the type of the resource.
     */
    Type getType();

    /**
     * @return true if this resource contains severals resources instances.
     */
    boolean isMultiInstances();

    /**
     * This method is only available if {@link #isMultiInstances()} return <code>false</code>.
     * 
     * The type of the returned value depends on the {@link #getType()} method.
     * 
     * If {@link #getType()} returns {@link Type#BOOLEAN}, the value is a {@link Boolean}.<br>
     * If {@link #getType()} returns {@link Type#STRING}, the value is a {@link String}.<br>
     * If {@link #getType()} returns {@link Type#OPAQUE}, the value is a byte array.<br>
     * If {@link #getType()} returns {@link Type#TIME}, the value is a {@link Date}.<br>
     * If {@link #getType()} returns {@link Type#INTEGER}, the value is a {@link Long}.<br>
     * If {@link #getType()} returns {@link Type#FLOAT}, the value is a {@link Double}.<br>
     * 
     * @return the value of the resource.
     */
    Object getValue();

    /**
     * This method is only available if {@link #isMultiInstances()} return <code>true</code>.
     * 
     * The type of the right part of the returned map depends on the {@link #getType()} method.
     * 
     * If {@link #getType()} returns {@link Type#BOOLEAN}, the value is a {@link Boolean}.<br>
     * If {@link #getType()} returns {@link Type#STRING}, the value is a {@link String}.<br>
     * If {@link #getType()} returns {@link Type#OPAQUE}, the value is a byte array.<br>
     * If {@link #getType()} returns {@link Type#TIME}, the value is a {@link Date}.<br>
     * If {@link #getType()} returns {@link Type#INTEGER}, the value is a {@link Long}.<br>
     * If {@link #getType()} returns {@link Type#FLOAT}, the value is a {@link Double}.<br>
     * 
     * @return the values of each resource instances (key is the resource instance identifier).
     */
    Map<Integer, ?> getValues();

    /**
     * This method is only available if {@link #isMultiInstances()} return <code>true</code>.
     * 
     * The type of the returned value depends on the {@link #getType()} method.
     * 
     * If {@link #getType()} returns {@link Type#BOOLEAN}, the value is a {@link Boolean}.<br>
     * If {@link #getType()} returns {@link Type#STRING}, the value is a {@link String}.<br>
     * If {@link #getType()} returns {@link Type#OPAQUE}, the value is a byte array.<br>
     * If {@link #getType()} returns {@link Type#TIME}, the value is a {@link Date}.<br>
     * If {@link #getType()} returns {@link Type#INTEGER}, the value is a {@link Long}.<br>
     * If {@link #getType()} returns {@link Type#FLOAT}, the value is a {@link Double}.<br>
     * 
     * @return the value a resource instance with the given identifier.
     */
    Object getValue(int id);
}
