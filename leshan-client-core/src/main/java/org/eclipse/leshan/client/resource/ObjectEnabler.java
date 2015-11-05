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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class ObjectEnabler extends BaseObjectEnabler {

    // TODO we should manage that in a threadsafe way
    private Map<Integer, LwM2mInstanceEnabler> instances;
    private LwM2mInstanceEnablerFactory instanceFactory;

    public ObjectEnabler(int id, ObjectModel objectModel, Map<Integer, LwM2mInstanceEnabler> instances,
            LwM2mInstanceEnablerFactory instanceFactory) {
        super(id, objectModel);
        this.instances = new HashMap<Integer, LwM2mInstanceEnabler>(instances);
        this.instanceFactory = instanceFactory;
        for (Entry<Integer, LwM2mInstanceEnabler> entry : this.instances.entrySet()) {
            addInstance(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public List<Integer> getAvailableInstanceIds() {
        List<Integer> ids = new ArrayList<Integer>(instances.keySet());
        Collections.sort(ids);
        return ids;
    }

    public void addInstance(int instanceId, LwM2mInstanceEnabler newInstance) {
        instances.put(instanceId, newInstance);
        listenInstance(newInstance, instanceId);
    }

    public LwM2mInstanceEnabler getInstance(int instanceId) {
        return instances.get(instanceId);
    }

    public LwM2mInstanceEnabler removeInstance(int instanceId) {
        return instances.remove(instanceId);
    }

    @Override
    protected synchronized CreateResponse doCreate(CreateRequest request) {
        Integer instanceId = request.getPath().getObjectInstanceId();
        if (instanceId == null) {
            // the client is in charge to generate the id of the new instance
            if (instances.isEmpty()) {
                instanceId = 0;
            } else {
                instanceId = Collections.max(instances.keySet()) + 1;
            }
        }

        LwM2mInstanceEnabler newInstance = instanceFactory.create(getObjectModel());

        for (LwM2mResource resource : request.getResources()) {
            newInstance.write(resource.getId(), resource);
        }
        instances.put(instanceId, newInstance);
        listenInstance(newInstance, instanceId);

        return CreateResponse.success(new LwM2mPath(request.getPath().getObjectId(), instanceId).toString());
    }

    @Override
    protected ReadResponse doRead(ReadRequest request) {
        LwM2mPath path = request.getPath();

        // Manage Object case
        if (path.isObject()) {
            List<LwM2mObjectInstance> lwM2mObjectInstances = new ArrayList<>();
            for (Entry<Integer, LwM2mInstanceEnabler> entry : instances.entrySet()) {
                lwM2mObjectInstances.add(getLwM2mObjectInstance(entry.getKey(), entry.getValue()));
            }
            return ReadResponse.success(new LwM2mObject(getId(), lwM2mObjectInstances));
        }

        // Manage Instance case
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null)
            return ReadResponse.notFound();

        if (path.getResourceId() == null) {
            return ReadResponse.success(getLwM2mObjectInstance(path.getObjectInstanceId(), instance));
        }

        // Manage Resource case
        return instance.read(path.getResourceId());
    }

    LwM2mObjectInstance getLwM2mObjectInstance(int instanceid, LwM2mInstanceEnabler instance) {
        List<LwM2mResource> resources = new ArrayList<>();
        for (ResourceModel resourceModel : getObjectModel().resources.values()) {
            if (resourceModel.operations.isReadable()) {
                ReadResponse response = instance.read(resourceModel.id);
                if (response.getCode() == ResponseCode.CONTENT && response.getContent() instanceof LwM2mResource)
                    resources.add((LwM2mResource) response.getContent());
            }
        }
        return new LwM2mObjectInstance(instanceid, resources);
    }

    @Override
    protected WriteResponse doWrite(WriteRequest request) {
        LwM2mPath path = request.getPath();

        // Manage Instance case
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null)
            return WriteResponse.notFound();

        if (path.getResourceId() == null) {
            for (LwM2mResource resource : ((LwM2mObjectInstance) request.getNode()).getResources().values()) {
                instance.write(resource.getId(), resource);
            }
            return WriteResponse.success();
        }

        // Manage Resource case
        return instance.write(path.getResourceId(), (LwM2mResource) request.getNode());
    }

    @Override
    protected ExecuteResponse doExecute(ExecuteRequest request) {
        LwM2mPath path = request.getPath();
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null) {
            return ExecuteResponse.notFound();
        }
        return instance.execute(path.getResourceId(), request.getParameters());
    }

    @Override
    protected DeleteResponse doDelete(DeleteRequest request) {
        LwM2mPath path = request.getPath();
        if (!instances.containsKey(path.getObjectInstanceId())) {
            return DeleteResponse.notFound();
        }
        instances.remove(request.getPath().getObjectInstanceId());
        return DeleteResponse.success();
    }

    private void listenInstance(LwM2mInstanceEnabler instance, final int instanceId) {
        instance.addResourceChangedListener(new ResourceChangedListener() {
            @Override
            public void resourcesChanged(int... resourceIds) {
                getNotifySender().sendNotify(getId() + "");
                getNotifySender().sendNotify(getId() + "/" + instanceId);
                for (int resourceId : resourceIds) {
                    getNotifySender().sendNotify(getId() + "/" + instanceId + "/" + resourceId);
                }
            }
        });
    }

}
