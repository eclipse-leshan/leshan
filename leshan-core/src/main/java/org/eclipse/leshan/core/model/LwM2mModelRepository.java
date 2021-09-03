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
package org.eclipse.leshan.core.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.node.LwM2mNodeUtil;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of LWM2M object definitions which could contained several definitions of the same object in different
 * version.
 */
public class LwM2mModelRepository {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mModelRepository.class);

    private static class Key implements Comparable<Key> {
        Integer id;
        Version version;

        public Key(Integer id, Version version) {
            this.id = id;
            this.version = version;
        }

        public void validate() {
            LwM2mNodeUtil.validateObjectId(id);
            String err = version.validate();
            if (err != null) {
                throw new IllegalStateException(
                        String.format("Invalid version %s for object %d : %s", version, id, err));
            }
        }

        @Override
        public int compareTo(Key o) {
            // handle null
            if (o == null) {
                return 1;
            }
            // compare id
            int res = Integer.compare(this.id, o.id);
            if (res != 0) {
                return res;
            }
            // compare version
            else {
                return this.version.compareTo(o.version);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
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
            Key other = (Key) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return String.format("key[%s/%s]", id, version);
        }

    }

    // This map contains all the object models available. Different version could be used.
    private final NavigableMap<Key, ObjectModel> objects;

    public LwM2mModelRepository(ObjectModel... objectModels) {
        this(Arrays.asList(objectModels));
    }

    public LwM2mModelRepository(Collection<ObjectModel> objectModels) {
        if (objectModels == null) {
            objects = new TreeMap<>();
        } else {
            NavigableMap<Key, ObjectModel> map = new TreeMap<>();
            for (ObjectModel model : objectModels) {
                Key key = getKey(model);
                if (key == null) {
                    throw new IllegalArgumentException(
                            String.format("Model %s is invalid : object id is missing.", model));
                }
                key.validate();

                ObjectModel old = map.put(key, model);
                if (old != null) {
                    LOG.debug("Model already exists for object {} in version {}. Overriding it.", model.id,
                            model.version);
                }
            }
            // TODO use unmodifiableNavigableMap when we pass to java8.
            objects = map;
        }
    }

    public ObjectModel getObjectModel(Integer objectId, String version) {
        Validate.notNull(objectId, "objectid must not be null");
        Validate.notNull(version, "version must not be null");

        return objects.get(getKey(objectId, version));
    }

    public ObjectModel getObjectModel(Integer objectId, Version version) {
        Validate.notNull(objectId, "objectid must not be null");
        Validate.notNull(version, "version must not be null");

        return objects.get(getKey(objectId, version));
    }

    /**
     * @return most recent version of the model.
     */
    public ObjectModel getObjectModel(Integer objectId) {
        Validate.notNull(objectId, "objectid must not be null");

        Key floorKey = objects.floorKey(getKey(objectId, Version.MAX));
        if (floorKey == null || floorKey.id != objectId) {
            return null;
        }
        return objects.get(floorKey);
    }

    private Key getKey(ObjectModel objectModel) {
        return getKey(objectModel.id, objectModel.version);
    }

    private Key getKey(Integer objectId, String version) {
        return getKey(objectId, new Version(version));
    }

    private Key getKey(Integer objectId, Version version) {
        if (objectId == null) {
            return null;
        }
        return new Key(objectId, version);
    }

    /**
     * Create a {@link LwM2mModel} with the last version of each Objects.
     */
    public LwM2mModel getLwM2mModel() {
        return new LwM2mModel() {
            @Override
            public ResourceModel getResourceModel(int objectId, int resourceId) {
                ObjectModel objectModel = getObjectModel(objectId);
                if (objectModel == null)
                    return null;

                return objectModel.resources.get(resourceId);
            }

            @Override
            public Collection<ObjectModel> getObjectModels() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public ObjectModel getObjectModel(int objectId) {
                return LwM2mModelRepository.this.getObjectModel(objectId);
            }
        };
    }
}