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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add reset() for
 *                                                     REPLACE/UPDATE implementation
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.resource.listener.ResourceListener;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

/**
 * A abstract implementation of {@link LwM2mInstanceEnabler}. It could be use as base for any
 * {@link LwM2mInstanceEnabler} implementation.
 */
public class BaseInstanceEnabler implements LwM2mInstanceEnabler {

    protected List<ResourceListener> listeners = new ArrayList<>();
    protected Integer id = null;
    protected ObjectModel model;
    protected LwM2mClient lwm2mClient;

    public BaseInstanceEnabler() {
    }

    public BaseInstanceEnabler(int id) {
        setId(id);
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setModel(ObjectModel model) {
        this.model = model;
    }

    public ObjectModel getModel() {
        return model;
    }

    @Override
    public void setLwM2mClient(LwM2mClient client) {
        this.lwm2mClient = client;
    }

    public LwM2mClient getLwM2mClient() {
        return lwm2mClient;
    }

    @Override
    public void addResourceListener(ResourceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeResourceListener(ResourceListener listener) {
        listeners.remove(listener);
    }

    /**
     * To be used to notify that 1 or several resources change.
     * <p>
     * This method SHOULD NOT be called in a synchronize block or any thread synchronization tools to avoid any risk of
     * deadlock.
     * <p>
     * Calling this method is needed to trigger NOTIFICATION when an observe relation is established.
     *
     * @param resources the list of path of resource and resource instances which changes.
     *
     * @see #getResourcePath(int)
     * @see #getResourceInstancePath(int, int)
     */
    public void fireResourcesChange(LwM2mPath... resources) {
        for (ResourceListener listener : listeners) {
            listener.resourceChanged(resources);
        }
    }

    /**
     * @see #fireResourcesChange(LwM2mPath...)
     */
    public void fireResourceChange(int resourceId) {
        fireResourcesChange(getResourcePath(resourceId));
    }

    /**
     * @see #fireResourcesChange(LwM2mPath...)
     */
    public void fireResourceInstanceChange(int resourceId, int resourceInstanceId) {
        fireResourcesChange(getResourceInstancePath(resourceId, resourceInstanceId));
    }

    protected LwM2mPath getResourcePath(int resourceId) {
        return new LwM2mPath(getModel().id, getId(), resourceId);
    }

    protected LwM2mPath[] getResourcePaths(int... resourceIds) {
        LwM2mPath[] paths = new LwM2mPath[resourceIds.length];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = getResourcePath(resourceIds[i]);
        }
        return paths;
    }

    protected LwM2mPath getResourceInstancePath(int resourceId, int resourceInstanceId) {
        return new LwM2mPath(getModel().id, getId(), resourceId, resourceInstanceId);
    }

    @Override
    public ReadResponse read(ServerIdentity identity) {
        List<LwM2mResource> resources = new ArrayList<>();
        for (ResourceModel resourceModel : model.resources.values()) {
            // check, if internal request (SYSTEM) or readable
            if (identity.isSystem() || resourceModel.operations.isReadable()) {
                ReadResponse response = read(identity, resourceModel.id);
                if (response.isSuccess() && response.getContent() instanceof LwM2mResource)
                    resources.add((LwM2mResource) response.getContent());
            }
        }
        return ReadResponse.success(new LwM2mObjectInstance(id, resources));
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        return ReadResponse.notFound();
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceId, int resourceInstance) {
        ReadResponse response = read(identity, resourceId);
        if (response.isFailure())
            return response;
        LwM2mMultipleResource resource = (LwM2mMultipleResource) response.getContent();
        LwM2mResourceInstance resourceIn = resource.getInstance(resourceInstance);
        if (resourceIn == null) {
            return ReadResponse.notFound();
        } else {
            return ReadResponse.success(resourceIn);
        }
    }

    @Override
    public WriteResponse write(ServerIdentity identity, boolean replace, LwM2mObjectInstance value) {
        Map<Integer, LwM2mResource> resourcesToWrite = new HashMap<>(value.getResources());

        if (replace) {
            // REPLACE
            for (ResourceModel resourceModel : model.resources.values()) {
                if (identity.isSystem() || identity.isLwm2mBootstrapServer()) {
                    // For request comes from System or Bootstrap server we handle all resources.
                    LwM2mResource writeResource = resourcesToWrite.remove(resourceModel.id);
                    if (null != writeResource) {
                        write(identity, true, resourceModel.id, writeResource);
                    } else {
                        reset(resourceModel.id);
                    }

                } else if (identity.isLwm2mServer()) {
                    if (resourceModel.operations.isWritable()) {
                        // For writable resource, Write resource if there something to write, if not reset value
                        LwM2mResource writeResource = resourcesToWrite.remove(resourceModel.id);
                        if (null != writeResource) {
                            write(identity, true, resourceModel.id, writeResource);
                        } else {
                            reset(resourceModel.id);
                        }
                    } else if (resourceModel.operations.isReadable()) {
                        // For readable value, just reset value
                        reset(resourceModel.id);
                    }
                } else {
                    throw new IllegalStateException("Unsupported type of server identity " + identity);
                }
            }
        }
        // UPDATE and resources currently not in the model
        for (LwM2mResource resource : resourcesToWrite.values()) {
            write(identity, false, resource.getId(), resource);
        }
        return WriteResponse.success();
    }

    @Override
    public WriteResponse write(ServerIdentity identity, boolean replace, int resourceid, LwM2mResource value) {
        return WriteResponse.notFound();
    }

    @Override
    public WriteResponse write(ServerIdentity identity, boolean addIfAbsent, int resourceid, int resourceInstanceId,
            LwM2mResourceInstance value) {

        // this is a sub-optimal default implementation
        if (!addIfAbsent) {
            ReadResponse response = read(ServerIdentity.SYSTEM, resourceid, resourceInstanceId);
            if (!response.isSuccess()) {
                return WriteResponse.notFound();
            }
        }
        return write(identity, false, resourceid, new LwM2mMultipleResource(resourceid, value.getType(), value));
    }

    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, Arguments arguments) {
        return ExecuteResponse.notFound();
    }

    @Override
    public ObserveResponse observe(ServerIdentity identity) {
        // Perform a read by default
        ReadResponse readResponse = this.read(identity);
        return new ObserveResponse(readResponse.getCode(), readResponse.getContent(), null, null,
                readResponse.getErrorMessage());
    }

    @Override
    public ObserveResponse observe(ServerIdentity identity, int resourceid) {
        // Perform a read by default
        ReadResponse readResponse = this.read(identity, resourceid);
        return new ObserveResponse(readResponse.getCode(), readResponse.getContent(), null, null,
                readResponse.getErrorMessage());
    }

    @Override
    public ObserveResponse observe(ServerIdentity identity, int resourceid, int resourceInstanceId) {
        // Perform a read by default
        ReadResponse readResponse = this.read(identity, resourceid, resourceInstanceId);
        return new ObserveResponse(readResponse.getCode(), readResponse.getContent(), null, null,
                readResponse.getErrorMessage());
    }

    @Override
    public void onDelete(ServerIdentity identity) {
        // No default behavior
    }

    @Override
    public void reset(int resourceid) {
        // No default behavior
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        // By default we consider that all resources defined in the model are supported
        ArrayList<Integer> resourceIds = new ArrayList<>(model.resources.keySet());
        Collections.sort(resourceIds);
        return resourceIds;
    }
}
