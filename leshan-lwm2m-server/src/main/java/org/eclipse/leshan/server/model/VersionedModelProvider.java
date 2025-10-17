/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.server.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.server.registration.IRegistration;

/**
 * A LwM2mModelProvider which supports object versioning. It returns a LwM2mModel taking into account object version
 * given in registration.
 */
public class VersionedModelProvider implements LwM2mModelProvider {

    private final LwM2mModelRepository repository;

    public VersionedModelProvider(Collection<ObjectModel> objectModels) {
        this.repository = new LwM2mModelRepository(objectModels);
    }

    public VersionedModelProvider(LwM2mModelRepository repository) {
        this.repository = repository;
    }

    @Override
    public LwM2mModel getObjectModel(IRegistration registration) {
        return new DynamicModel(registration);
    }

    private class DynamicModel implements LwM2mModel {

        private final IRegistration registration;

        public DynamicModel(IRegistration registration) {
            this.registration = registration;
        }

        @Override
        public ResourceModel getResourceModel(int objectId, int resourceId) {
            ObjectModel objectModel = getObjectModel(objectId);
            if (objectModel != null)
                return objectModel.resources.get(resourceId);
            else
                return null;
        }

        @Override
        public ObjectModel getObjectModel(int objectId) {
            Version version = registration.getSupportedVersion(objectId);
            if (version != null) {
                return repository.getObjectModel(objectId, version);
            }
            return null;
        }

        @Override
        public Collection<ObjectModel> getObjectModels() {
            Map<Integer, Version> supportedObjects = registration.getSupportedObject();
            Collection<ObjectModel> result = new ArrayList<>(supportedObjects.size());
            for (Entry<Integer, Version> supportedObject : supportedObjects.entrySet()) {
                ObjectModel objectModel = repository.getObjectModel(supportedObject.getKey(),
                        supportedObject.getValue());
                if (objectModel != null)
                    result.add(objectModel);
            }
            return result;
        }

        @Override
        public boolean hasChildrenModels() {
            return registration.isGateway();
        }

        @Override
        public LwM2mModel getChildModel(String prefix) {
            IRegistration childRegistration = registration.getChildEndDevices(prefix);
            if (childRegistration == null) {
                return null;
            }
            return new DynamicModel(childRegistration);
        }
    }
}
