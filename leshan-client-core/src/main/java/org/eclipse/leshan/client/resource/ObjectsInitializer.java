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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.util.Validate;

public class ObjectsInitializer {

    protected Map<Integer, Class<? extends LwM2mInstanceEnabler>> classes = new HashMap<Integer, Class<? extends LwM2mInstanceEnabler>>();
    protected Map<Integer, LwM2mInstanceEnabler[]> instances = new HashMap<Integer, LwM2mInstanceEnabler[]>();
    protected LwM2mModel model;

    public ObjectsInitializer() {
        this(null);
    }

    public ObjectsInitializer(LwM2mModel model) {
        if (model == null) {
            List<ObjectModel> objects = ObjectLoader.loadDefault();
            HashMap<Integer, ObjectModel> map = new HashMap<Integer, ObjectModel>();
            for (ObjectModel objectModel : objects) {
                map.put(objectModel.id, objectModel);
            }
            this.model = new LwM2mModel(map);
        } else {
            this.model = model;
        }
    }

    public void setClassForObject(int objectId, Class<? extends LwM2mInstanceEnabler> clazz) {
        if (model.getObjectModel(objectId) == null) {
            throw new IllegalStateException("Cannot set Instance Class for Object " + objectId
                    + " because no model is defined for this id.");
        }

        Validate.notNull(clazz);
        if (instances.containsKey(objectId)) {
            throw new IllegalStateException("Cannot set Instance Class for Object " + objectId
                    + " when Instance already exists. Can only have one or the other.");
        }

        // check clazz has a default constructor
        try {
            clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Class must have a default constructor");
        }
        classes.put(objectId, clazz);
    }

    public void setInstancesForObject(int objectId, LwM2mInstanceEnabler... instances) {
        ObjectModel objectModel = model.getObjectModel(objectId);
        if (objectModel == null) {
            throw new IllegalStateException("Cannot set Instances Class for Object " + objectId
                    + " because no model is defined for this id.");
        }
        Validate.notNull(instances);
        Validate.notEmpty(instances);

        if (classes.containsKey(objectId)) {
            throw new IllegalStateException("Cannot set Instances for Object " + objectId
                    + " when Instance Class already exists.  Can only have one or the other.");
        }

        if (instances.length > 1 && !objectModel.multiple)
            throw new IllegalStateException("Cannot set more than one instance for the single Object " + objectId);

        // check class of the instance has a default constructor
        try {
            instances[0].getClass().getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Instance must have a class with a default constructor");
        }
        this.instances.put(objectId, instances);
    }

    public List<ObjectEnabler> createMandatory() {
        Collection<ObjectModel> objectModels = model.getObjectModels();

        List<ObjectEnabler> enablers = new ArrayList<ObjectEnabler>();
        for (ObjectModel objectModel : objectModels) {
            if (objectModel.mandatory) {
                ObjectEnabler objectEnabler = createNodeEnabler(objectModel);
                if (objectEnabler != null)
                    enablers.add(objectEnabler);
            }
        }
        return enablers;
    }

    public ObjectEnabler create(int objectId) {
        ObjectModel objectModel = model.getObjectModel(objectId);
        if (objectModel == null) {
            throw new IllegalStateException("Cannot create object for id " + objectId
                    + " because no model is defined for this id.");
        }

        return createNodeEnabler(objectModel);

    }

    public List<ObjectEnabler> create(int... objectId) {
        List<ObjectEnabler> enablers = new ArrayList<ObjectEnabler>();
        for (int i = 0; i < objectId.length; i++) {
            ObjectEnabler objectEnabler = create(objectId[i]);
            if (objectEnabler != null)
                enablers.add(objectEnabler);
        }
        return enablers;
    }

    protected Class<? extends LwM2mInstanceEnabler> getClassFor(ObjectModel objectModel) {
        // if we have a class for this object id, return it
        Class<? extends LwM2mInstanceEnabler> clazz = classes.get(objectModel.id);
        if (clazz != null)
            return clazz;

        // if there are no class for this object check in instance list.
        LwM2mInstanceEnabler[] instances = this.instances.get(objectModel.id);
        if (instances != null && instances.length > 0)
            return instances[0].getClass();

        // default class :
        return SimpleInstanceEnabler.class;
    }

    protected ObjectEnabler createNodeEnabler(ObjectModel objectModel) {
        final Map<Integer, LwM2mInstanceEnabler> instances = new HashMap<Integer, LwM2mInstanceEnabler>();
        LwM2mInstanceEnabler[] newInstances = createInstances(objectModel);
        for (int i = 0; i < newInstances.length; i++) {
            instances.put(i, newInstances[i]);
        }
        return new ObjectEnabler(objectModel.id, objectModel, instances, getClassFor(objectModel));
    }

    protected LwM2mInstanceEnabler[] createInstances(ObjectModel objectModel) {
        LwM2mInstanceEnabler[] newInstances = new LwM2mInstanceEnabler[0];
        if (instances.containsKey(objectModel.id)) {
            newInstances = instances.get(objectModel.id);
        } else {
            // we create instance from class only for single object
            if (!objectModel.multiple) {
                Class<? extends LwM2mInstanceEnabler> clazz = getClassFor(objectModel);
                try {
                    newInstances = new LwM2mInstanceEnabler[] { clazz.newInstance() };
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return newInstances;
    }
}
