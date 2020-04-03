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
package org.eclipse.leshan.client.resource;

import java.util.Collection;

import org.eclipse.leshan.core.model.ObjectModel;

/**
 * A base class to implement {@link LwM2mInstanceEnablerFactory}.
 */
public abstract class BaseInstanceEnablerFactory implements LwM2mInstanceEnablerFactory {

    @Override
    public LwM2mInstanceEnabler create(ObjectModel model, Integer id, Collection<Integer> alreadyUsedIdentifier) {
        // generate a new id if needed
        if (id == null) {
            id = getNewInstanceId(alreadyUsedIdentifier);
        }

        // create new instance
        LwM2mInstanceEnabler instance = create();

        // set id if not already done
        if (instance.getId() == null) {
            instance.setId(id);
        } else {
            // check id is well set
            if (instance.getId() != id) {
                throw new IllegalStateException(
                        String.format("instance id should be %d but was %d", id, instance.getId()));
            }
        }

        // set model
        instance.setModel(model);

        return instance;
    }

    /**
     * generate a new valid instance id
     * 
     * @param alreadyUsedIdentifier a collection of id already used
     * @return an id which is not contained in <code>alreadyUsedIdentifier</code>
     */
    protected int getNewInstanceId(Collection<Integer> alreadyUsedIdentifier) {
        return generateNewInstanceId(alreadyUsedIdentifier);
    }

    /**
     * Create a new instance enabler.
     * 
     * @return the new instance enabler
     */
    public abstract LwM2mInstanceEnabler create();

    public static int generateNewInstanceId(Collection<Integer> alreadyUsedIdentifier) {
        if (alreadyUsedIdentifier.isEmpty()) {
            return 0;
        } else {
            return java.util.Collections.max(alreadyUsedIdentifier) + 1;
        }
    }
}
