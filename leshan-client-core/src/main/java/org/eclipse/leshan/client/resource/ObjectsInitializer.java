/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.client.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.util.Validate;

public class ObjectsInitializer {

    protected Map<Integer, LwM2mInstanceEnablerFactory> factories = new HashMap<>();
    protected Map<Integer, LwM2mInstanceEnabler[]> instances = new HashMap<>();
    protected Map<Integer, ContentFormat> defaultContentFormat = new HashMap<>();
    protected LwM2mModel model;

    public ObjectsInitializer() {
        this(null);
    }

    public ObjectsInitializer(LwM2mModel model) {
        if (model == null) {
            this.model = new StaticModel(ObjectLoader.loadDefault());
        } else {
            this.model = model;
        }
    }

    public void setFactoryForObject(int objectId, LwM2mInstanceEnablerFactory factory) {
        if (model.getObjectModel(objectId) == null) {
            throw new IllegalArgumentException(
                    "Cannot set Instance Factory for Object " + objectId + " because no model is defined for this id.");
        }
        Validate.notNull(factory);
        factories.put(objectId, factory);
    }

    public void setClassForObject(int objectId, Class<? extends LwM2mInstanceEnabler> clazz) {
        Validate.notNull(clazz);
        // check clazz has a default constructor
        try {
            clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class must have a default constructor");
        }
        setFactoryForObject(objectId, getClassFactory(clazz));
    }

    public void setInstancesForObject(int objectId, LwM2mInstanceEnabler... instances) {
        ObjectModel objectModel = model.getObjectModel(objectId);
        if (objectModel == null) {
            throw new IllegalArgumentException(
                    "Cannot set Instances Class for Object " + objectId + " because no model is defined for this id.");
        }
        Validate.notNull(instances);
        Validate.notEmpty(instances);

        if (!objectModel.multiple) {
            if (instances.length > 1)
                throw new IllegalArgumentException(
                        "Cannot set more than one instance for the single Object " + objectId);
            if (instances[0].getId() != null && instances[0].getId() != 0)
                throw new IllegalArgumentException(String.format(
                        "Invalid instance id for single object %d : single object instance should have an id equals to 0",
                        objectId));
        }

        this.instances.put(objectId, instances);
    }

    /**
     * Add dummy instance for each given <code>objectId</code>. ObjectId can be repeated to create several dummy
     * instances. A dummy instance is just a very simple instance implementation which respect the object model and
     * return some random values. A good way to begin to test Leshan client but not adapted to production environment.
     * 
     * @param objectId
     */
    public void setDummyInstancesForObject(int... objectIds) {
        // create a map (id => nb instances)
        Map<Integer, Integer> idToNbInstance = new HashMap<>();
        for (int objectid : objectIds) {
            // get current number of instance
            Integer nbInstance = idToNbInstance.get(objectid);
            if (nbInstance == null)
                nbInstance = 0;

            // add a new instance
            idToNbInstance.put(objectid, nbInstance + 1);
        }

        // create dummy instances for each object
        for (Map.Entry<Integer, Integer> entry : idToNbInstance.entrySet()) {
            int objectid = entry.getKey();

            // create instance Array;
            Integer nbInstances = entry.getValue();
            DummyInstanceEnabler[] instances = new DummyInstanceEnabler[nbInstances];
            for (int i = 0; i < instances.length; i++) {
                instances[i] = new DummyInstanceEnabler();
            }

            // set instances for current object id
            setInstancesForObject(objectid, instances);
        }
    }

    public void setDefaultContentFormat(int objectId, ContentFormat format) {
        defaultContentFormat.put(objectId, format);
    }

    /**
     * Create an {@link LwM2mObjectEnabler} for each object to which you associated an "instances", "object class" or
     * "factory".
     * 
     * @return a list of LwM2MObjectEnabler
     * 
     * @see ObjectsInitializer#setInstancesForObject(int, LwM2mInstanceEnabler...)
     * @see ObjectsInitializer#setClassForObject(int, Class)
     * @see ObjectsInitializer#setFactoryForObject(int, LwM2mInstanceEnablerFactory)
     */
    public List<LwM2mObjectEnabler> createAll() {
        // collect object ids which is set
        Set<Integer> ids = new HashSet<>();
        ids.addAll(factories.keySet());
        ids.addAll(instances.keySet());

        // create objects
        int[] idArray = new int[ids.size()];
        int i = 0;
        for (Integer id : ids) {
            idArray[i] = id;
            i++;
        }
        return create(idArray);
    }

    /**
     * Create an {@link LwM2mObjectEnabler} for the given <code>objectId</code>.
     * 
     * An "instances", "object class" or "factory" MUST have been associated before.
     * 
     * @return a LwM2MObjectEnabler
     * 
     * @see ObjectsInitializer#setInstancesForObject(int, LwM2mInstanceEnabler...)
     * @see ObjectsInitializer#setClassForObject(int, Class)
     * @see ObjectsInitializer#setFactoryForObject(int, LwM2mInstanceEnablerFactory)
     */
    public LwM2mObjectEnabler create(int objectId) {
        ObjectModel objectModel = model.getObjectModel(objectId);
        if (objectModel == null) {
            throw new IllegalArgumentException(
                    "Cannot create object for id " + objectId + " because no model is defined for this id.");
        }
        return createNodeEnabler(objectModel);
    }

    /**
     * Create an {@link LwM2mObjectEnabler} for each given <code>objectId</code>.
     * 
     * An "instances", "object class" or "factory" MUST have been associated before.
     * 
     * @return a list of LwM2MObjectEnabler
     * 
     * @see ObjectsInitializer#setClassForObject(int, Class)
     * @see ObjectsInitializer#setFactoryForObject(int, LwM2mInstanceEnablerFactory)
     * @see ObjectsInitializer#setInstancesForObject(int, LwM2mInstanceEnabler...)
     */
    public List<LwM2mObjectEnabler> create(int... objectId) {
        List<LwM2mObjectEnabler> enablers = new ArrayList<>();
        for (int anObjectId : objectId) {
            LwM2mObjectEnabler objectEnabler = create(anObjectId);
            if (objectEnabler != null)
                enablers.add(objectEnabler);
        }
        return enablers;
    }

    protected LwM2mInstanceEnablerFactory getFactoryFor(ObjectModel objectModel) {
        // if we have a factory for this object id, return it
        LwM2mInstanceEnablerFactory instanceFactory = factories.get(objectModel.id);
        if (instanceFactory != null)
            return instanceFactory;

        // if there are no factory for this object check in instance list.
        LwM2mInstanceEnabler[] instances = this.instances.get(objectModel.id);
        if (instances != null) {
            for (LwM2mInstanceEnabler instance : instances) {
                // check if the class of this instance has a default constructors;
                try {
                    Class<? extends LwM2mInstanceEnabler> clazz = instance.getClass();
                    clazz.getConstructor();
                    return getClassFactory(clazz);
                } catch (NoSuchMethodException e) {
                    // no default constructor.
                }
            }
        }
        throw new IllegalStateException(String.format(
                "Unable to create factory for %s object (%d) : a factory, a class or an instance with a default constructor should be associated to",
                objectModel.name, objectModel.id));
    }

    protected ObjectEnabler createNodeEnabler(ObjectModel objectModel) {
        Map<Integer, LwM2mInstanceEnabler> instances = new HashMap<>();
        LwM2mInstanceEnabler[] newInstances = createInstances(objectModel);
        for (LwM2mInstanceEnabler instance : newInstances) {
            // set id if not already set
            if (instance.getId() == null) {
                int id = BaseInstanceEnablerFactory.generateNewInstanceId(instances.keySet());
                instance.setId(id);
            }
            instance.setModel(objectModel);
            instances.put(instance.getId(), instance);
        }
        return new ObjectEnabler(objectModel.id, objectModel, instances, getFactoryFor(objectModel),
                getContentFormat(objectModel.id));
    }

    protected ContentFormat getContentFormat(int id) {
        ContentFormat contentFormat = defaultContentFormat.get(id);
        if (contentFormat != null) {
            return contentFormat;
        }
        return ContentFormat.DEFAULT;
    }

    protected LwM2mInstanceEnabler[] createInstances(ObjectModel objectModel) {
        LwM2mInstanceEnabler[] newInstances = new LwM2mInstanceEnabler[0];
        if (instances.containsKey(objectModel.id)) {
            newInstances = instances.get(objectModel.id);
        }
        return newInstances;
    }

    protected LwM2mInstanceEnablerFactory getClassFactory(final Class<? extends LwM2mInstanceEnabler> clazz) {
        LwM2mInstanceEnablerFactory factory = new BaseInstanceEnablerFactory() {
            @Override
            public LwM2mInstanceEnabler create() {
                try {
                    return clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return factory;
    }
}
