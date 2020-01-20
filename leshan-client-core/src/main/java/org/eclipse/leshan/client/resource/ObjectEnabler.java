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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *     Achim Kraus (Bosch Software Innovations GmbH) - implement REPLACE/UPDATE
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class ObjectEnabler extends BaseObjectEnabler {

    private Map<Integer, LwM2mInstanceEnabler> instances;
    private LwM2mInstanceEnablerFactory instanceFactory;
    private ContentFormat defaultContentFormat;

    public ObjectEnabler(int id, ObjectModel objectModel, Map<Integer, LwM2mInstanceEnabler> instances,
            LwM2mInstanceEnablerFactory instanceFactory, ContentFormat defaultContentFormat) {
        super(id, objectModel);
        this.instances = new HashMap<>(instances);
        this.instanceFactory = instanceFactory;
        this.defaultContentFormat = defaultContentFormat;
        for (Entry<Integer, LwM2mInstanceEnabler> entry : this.instances.entrySet()) {
            instances.put(entry.getKey(), entry.getValue());
            listenInstance(entry.getValue(), entry.getKey());
        }
    }

    @Override
    public synchronized List<Integer> getAvailableInstanceIds() {
        List<Integer> ids = new ArrayList<>(instances.keySet());
        Collections.sort(ids);
        return ids;
    }

    @Override
    public synchronized List<Integer> getAvailableResourceIds(int instanceId) {
        LwM2mInstanceEnabler instanceEnabler = instances.get(instanceId);
        if (instanceEnabler != null) {
            return instanceEnabler.getAvailableResourceIds(getObjectModel());
        } else {
            return Collections.emptyList();
        }
    }

    public synchronized void addInstance(int instanceId, LwM2mInstanceEnabler newInstance) {
        instances.put(instanceId, newInstance);
        listenInstance(newInstance, instanceId);
        fireInstancesAdded(instanceId);
    }

    public synchronized LwM2mInstanceEnabler getInstance(int instanceId) {
        return instances.get(instanceId);
    }

    public synchronized LwM2mInstanceEnabler removeInstance(int instanceId) {
        LwM2mInstanceEnabler removedInstance = instances.remove(instanceId);
        if (removedInstance != null) {
            fireInstancesRemoved(removedInstance.getId());
        }
        return removedInstance;
    }

    @Override
    protected CreateResponse doCreate(ServerIdentity identity, CreateRequest request) {
        if (!getObjectModel().multiple && instances.size() > 0) {
            return CreateResponse.badRequest("an instance already exist for this single instance object");
        }

        if (request.unknownObjectInstanceId()) {
            // create instance
            LwM2mInstanceEnabler newInstance = createInstance(identity, getObjectModel().multiple ? null : 0,
                    request.getResources());

            // add new instance to this object
            instances.put(newInstance.getId(), newInstance);
            listenInstance(newInstance, newInstance.getId());
            fireInstancesAdded(newInstance.getId());

            return CreateResponse
                    .success(new LwM2mPath(request.getPath().getObjectId(), newInstance.getId()).toString());
        } else {
            List<LwM2mObjectInstance> instanceNodes = request.getObjectInstances();

            // checks single object instances
            if (!getObjectModel().multiple) {
                if (request.getObjectInstances().size() > 1) {
                    return CreateResponse.badRequest("can not create several instances on this single instance object");
                }
                if (request.getObjectInstances().get(0).getId() != 0) {
                    return CreateResponse.badRequest("single instance object must use 0 as ID");
                }
            }
            // ensure instance does not already exists
            for (LwM2mObjectInstance instance : instanceNodes) {
                if (instances.containsKey(instance.getId())) {
                    return CreateResponse.badRequest(String.format("instance %d already exists", instance.getId()));
                }
            }

            // create the new instances
            int[] instanceIds = new int[request.getObjectInstances().size()];
            int i = 0;
            for (LwM2mObjectInstance instance : request.getObjectInstances()) {
                // create instance
                LwM2mInstanceEnabler newInstance = createInstance(identity, instance.getId(),
                        instance.getResources().values());

                // add new instance to this object
                instances.put(newInstance.getId(), newInstance);
                listenInstance(newInstance, newInstance.getId());

                // store instance ids
                instanceIds[i] = newInstance.getId();
                i++;
            }
            fireInstancesAdded(instanceIds);
            return CreateResponse.success();
        }
    }

    protected LwM2mInstanceEnabler createInstance(ServerIdentity identity, Integer instanceId,
            Collection<LwM2mResource> resources) {
        // create the new instance
        LwM2mInstanceEnabler newInstance = instanceFactory.create(getObjectModel(), instanceId, instances.keySet());

        // add/write resource
        for (LwM2mResource resource : resources) {
            newInstance.write(identity, resource.getId(), resource);
        }

        return newInstance;
    }

    @Override
    protected ReadResponse doRead(ServerIdentity identity, ReadRequest request) {
        LwM2mPath path = request.getPath();

        // Manage Object case
        if (path.isObject()) {
            List<LwM2mObjectInstance> lwM2mObjectInstances = new ArrayList<>();
            for (LwM2mInstanceEnabler instance : instances.values()) {
                ReadResponse response = instance.read(identity);
                if (response.isSuccess()) {
                    lwM2mObjectInstances.add((LwM2mObjectInstance) response.getContent());
                }
            }
            return ReadResponse.success(new LwM2mObject(getId(), lwM2mObjectInstances));
        }

        // Manage Instance case
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null)
            return ReadResponse.notFound();

        if (path.getResourceId() == null) {
            return instance.read(identity);
        }

        // Manage Resource case
        return instance.read(identity, path.getResourceId());
    }

    @Override
    protected ObserveResponse doObserve(final ServerIdentity identity, final ObserveRequest request) {
        final LwM2mPath path = request.getPath();

        // Manage Object case
        if (path.isObject()) {
            List<LwM2mObjectInstance> lwM2mObjectInstances = new ArrayList<>();
            for (LwM2mInstanceEnabler instance : instances.values()) {
                ReadResponse response = instance.observe(identity);
                if (response.isSuccess()) {
                    lwM2mObjectInstances.add((LwM2mObjectInstance) response.getContent());
                }
            }
            return ObserveResponse.success(new LwM2mObject(getId(), lwM2mObjectInstances));
        }

        // Manage Instance case
        final LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null)
            return ObserveResponse.notFound();

        if (path.getResourceId() == null) {
            return instance.observe(identity);
        }

        // Manage Resource case
        return instance.observe(identity, path.getResourceId());
    }

    @Override
    protected WriteResponse doWrite(ServerIdentity identity, WriteRequest request) {
        LwM2mPath path = request.getPath();

        // Manage Instance case
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null)
            return WriteResponse.notFound();

        if (path.isObjectInstance()) {
            return instance.write(identity, request.isReplaceRequest(), (LwM2mObjectInstance) request.getNode());
        }

        // Manage Resource case
        return instance.write(identity, path.getResourceId(), (LwM2mResource) request.getNode());
    }

    @Override
    protected BootstrapWriteResponse doWrite(ServerIdentity identity, BootstrapWriteRequest request) {
        LwM2mPath path = request.getPath();

        // Manage Object case
        if (path.isObject()) {
            for (LwM2mObjectInstance instanceNode : ((LwM2mObject) request.getNode()).getInstances().values()) {
                LwM2mInstanceEnabler instanceEnabler = instances.get(instanceNode.getId());
                if (instanceEnabler == null) {
                    doCreate(identity, new CreateRequest(path.getObjectId(), instanceNode));
                } else {
                    doWrite(identity, new WriteRequest(Mode.REPLACE, path.getObjectId(), instanceEnabler.getId(),
                            instanceNode.getResources().values()));
                }
            }
            return BootstrapWriteResponse.success();
        }

        // Manage Instance case
        if (path.isObjectInstance()) {
            LwM2mObjectInstance instanceNode = (LwM2mObjectInstance) request.getNode();
            LwM2mInstanceEnabler instanceEnabler = instances.get(path.getObjectInstanceId());
            if (instanceEnabler == null) {
                doCreate(identity, new CreateRequest(path.getObjectId(), instanceNode));
            } else {
                doWrite(identity, new WriteRequest(Mode.REPLACE, request.getContentFormat(), path.getObjectId(),
                        path.getObjectInstanceId(), instanceNode.getResources().values()));
            }
            return BootstrapWriteResponse.success();
        }

        // Manage resource case
        LwM2mResource resource = (LwM2mResource) request.getNode();
        LwM2mInstanceEnabler instanceEnabler = instances.get(path.getObjectInstanceId());
        if (instanceEnabler == null) {
            doCreate(identity, new CreateRequest(path.getObjectId(),
                    new LwM2mObjectInstance(path.getObjectInstanceId(), resource)));
        } else {
            instanceEnabler.write(identity, path.getResourceId(), resource);
        }
        return BootstrapWriteResponse.success();
    }

    @Override
    protected ExecuteResponse doExecute(ServerIdentity identity, ExecuteRequest request) {
        LwM2mPath path = request.getPath();
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null) {
            return ExecuteResponse.notFound();
        }
        return instance.execute(identity, path.getResourceId(), request.getParameters());
    }

    @Override
    protected DeleteResponse doDelete(ServerIdentity identity, DeleteRequest request) {
        LwM2mInstanceEnabler deletedInstance = instances.remove(request.getPath().getObjectInstanceId());
        if (deletedInstance != null) {
            deletedInstance.onDelete(identity);
            fireInstancesRemoved(deletedInstance.getId());
            return DeleteResponse.success();
        }
        return DeleteResponse.notFound();
    }

    @Override
    public BootstrapDeleteResponse doDelete(ServerIdentity identity, BootstrapDeleteRequest request) {
        if (request.getPath().isRoot() || request.getPath().isObject()) {
            if (id == LwM2mId.SECURITY) {
                // For security object, we clean everything except bootstrap Server account.
                Entry<Integer, LwM2mInstanceEnabler> bootstrapServerAccount = null;
                int[] instanceIds = new int[instances.size()];
                int i = 0;
                for (Entry<Integer, LwM2mInstanceEnabler> instance : instances.entrySet()) {
                    if (ServersInfoExtractor.isBootstrapServer(instance.getValue())) {
                        bootstrapServerAccount = instance;
                    } else {
                        // store instance ids
                        instanceIds[i] = instance.getKey();
                        i++;
                    }
                }
                instances.clear();
                if (bootstrapServerAccount != null) {
                    instances.put(bootstrapServerAccount.getKey(), bootstrapServerAccount.getValue());
                }
                fireInstancesRemoved(instanceIds);
                return BootstrapDeleteResponse.success();
            } else {
                instances.clear();
                // fired instances removed
                int[] instanceIds = new int[instances.size()];
                int i = 0;
                for (Entry<Integer, LwM2mInstanceEnabler> instance : instances.entrySet()) {
                    instanceIds[i] = instance.getKey();
                    i++;
                }
                fireInstancesRemoved(instanceIds);

                return BootstrapDeleteResponse.success();
            }
        } else if (request.getPath().isObjectInstance()) {
            if (id == LwM2mId.SECURITY) {
                // For security object, deleting bootstrap Server account is not allowed
                LwM2mInstanceEnabler instance = instances.get(request.getPath().getObjectInstanceId());
                if (ServersInfoExtractor.isBootstrapServer(instance)) {
                    return BootstrapDeleteResponse.badRequest("bootstrap server can not be deleted");
                }
            }
            if (null != instances.remove(request.getPath().getObjectInstanceId())) {
                fireInstancesRemoved(request.getPath().getObjectInstanceId());
                return BootstrapDeleteResponse.success();
            } else {
                return BootstrapDeleteResponse.badRequest(String.format("Instance %s not found", request.getPath()));
            }
        }
        return BootstrapDeleteResponse.badRequest(String.format("unexcepted path %s", request.getPath()));
    }

    private void listenInstance(LwM2mInstanceEnabler instance, final int instanceId) {
        instance.addResourceChangedListener(new ResourceChangedListener() {

            @Override
            public void resourcesChanged(int... resourceIds) {
                fireResourcesChanged(instanceId, resourceIds);

                // TODO remove Notify sender
                NotifySender sender = getNotifySender();
                if (null != sender) {
                    // check, if sender is available
                    sender.sendNotify(getId() + "");
                    sender.sendNotify(getId() + "/" + instanceId);
                    for (int resourceId : resourceIds) {
                        sender.sendNotify(getId() + "/" + instanceId + "/" + resourceId);
                    }
                }
            }
        });
    }

    @Override
    public ContentFormat getDefaultEncodingFormat(DownlinkRequest<?> request) {
        return defaultContentFormat;
    }
}
