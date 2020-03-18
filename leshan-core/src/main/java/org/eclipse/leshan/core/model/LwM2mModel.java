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

import java.util.Collection;

/**
 * A collection of LWM2M object definitions. This collection contains only 1 version of a specific object definition.
 */
public interface LwM2mModel {

    /**
     * Returns the description of a given resource.
     *
     * @param objectId the object identifier
     * @param resourceId the resource identifier
     * @return the resource specification or <code>null</code> if not found
     */
    ResourceModel getResourceModel(int objectId, int resourceId);

    /**
     * Returns the description of a given object.
     *
     * @param objectId the object identifier
     * @return the object definition or <code>null</code> if not found
     */
    ObjectModel getObjectModel(int objectId);

    /**
     * @return all the objects descriptions known.
     */
    Collection<ObjectModel> getObjectModels();

}
