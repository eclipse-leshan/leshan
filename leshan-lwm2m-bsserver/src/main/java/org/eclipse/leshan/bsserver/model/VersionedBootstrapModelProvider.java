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

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;

/**
 * A {@link LwM2mBootstrapModelProvider} which supports object versioning.
 */
public class VersionedBootstrapModelProvider implements LwM2mBootstrapModelProvider {

    private LwM2mModelRepository repository;

    public VersionedBootstrapModelProvider(Collection<ObjectModel> objectModels) {
        this.repository = new LwM2mModelRepository(objectModels);
    }

    public VersionedBootstrapModelProvider(LwM2mModelRepository repository) {
        this.repository = repository;
    }

    @Override
    public LwM2mModel getObjectModel(BootstrapSession session, Map<Integer, String> supportedObjects) {
        return new DynamicModel(supportedObjects);
    }

    private class DynamicModel implements LwM2mModel {

        private final Map<Integer, String> supportedObjects;

        public DynamicModel(Map<Integer, String> supportedObjects) {
            this.supportedObjects = supportedObjects;
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
            String version = supportedObjects.get(objectId);
            if (version != null) {
                return repository.getObjectModel(objectId, version);
            }
            return null;
        }

        @Override
        public Collection<ObjectModel> getObjectModels() {
            Collection<ObjectModel> result = new ArrayList<>(supportedObjects.size());
            for (Entry<Integer, String> supportedObject : supportedObjects.entrySet()) {
                ObjectModel objectModel = repository.getObjectModel(supportedObject.getKey(),
                        supportedObject.getValue());
                if (objectModel != null)
                    result.add(objectModel);
            }
            return result;
        }
    }
}
