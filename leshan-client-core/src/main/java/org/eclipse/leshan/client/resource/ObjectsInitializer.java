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
    protected Map<Integer, LwM2mInstanceEnabler> instances = new HashMap<Integer, LwM2mInstanceEnabler>();
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
        Validate.notNull(clazz);
        if (instances.containsKey(objectId)) {
            throw new IllegalStateException("Cannot set Instance Class for Object " + objectId
                    + " when Instance already exists. Can only have one or the other.");
        }
        classes.put(objectId, clazz);
    }

    public void setInstanceForObject(int objectId, LwM2mInstanceEnabler instance) {
        Validate.notNull(instance);
        if (classes.containsKey(objectId)) {
            throw new IllegalStateException("Cannot set Instance for Object " + objectId
                    + " when Instance Class already exists.  Can only have one or the other.");
        }
        instances.put(objectId, instance);
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

    public List<ObjectEnabler> create(int... objectId) {
        List<ObjectEnabler> enablers = new ArrayList<ObjectEnabler>();
        for (int i = 0; i < objectId.length; i++) {
            ObjectModel objectModel = model.getObjectModel(objectId[i]);
            ObjectEnabler objectEnabler = createNodeEnabler(objectModel);
            if (objectEnabler != null)
                enablers.add(objectEnabler);

        }
        return enablers;
    }

    protected ObjectEnabler createNodeEnabler(ObjectModel objectModel) {
        final Map<Integer, LwM2mInstanceEnabler> instances = new HashMap<Integer, LwM2mInstanceEnabler>();
        if (!objectModel.multiple) {
            LwM2mInstanceEnabler newInstance = createInstance(objectModel);
            if (newInstance != null) {
                instances.put(0, newInstance);
                return new ObjectEnabler(objectModel.id, objectModel, instances, SimpleInstanceEnabler.class);
            }
        }
        return new ObjectEnabler(objectModel.id, objectModel, instances, SimpleInstanceEnabler.class);
    }

    protected LwM2mInstanceEnabler createInstance(ObjectModel objectModel) {
        LwM2mInstanceEnabler instance;
        if (instances.containsKey(objectModel.id)) {
            instance = instances.get(objectModel.id);
        } else {
            Class<? extends LwM2mInstanceEnabler> clazz = classes.get(objectModel.id);
            if (clazz == null)
                clazz = SimpleInstanceEnabler.class;

            try {
                instance = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        instance.setObjectModel(objectModel);
        return instance;
    }
}
