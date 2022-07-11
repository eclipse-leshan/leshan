/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.client.resource.listener.ObjectsListener;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default implementation of {@link LwM2mRootEnabler}.
 */
public class RootEnabler implements LwM2mRootEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(RootEnabler.class);

    private final LwM2mObjectTree tree;

    public RootEnabler(final LwM2mObjectTree tree) {
        this.tree = tree;
    }

    @Override
    public void addListener(ObjectsListener listener) {
        tree.addListener(listener);
    }

    @Override
    public void removeListener(ObjectsListener listener) {
        tree.removeListener(listener);
    }

    @Override
    public ReadCompositeResponse read(ServerIdentity identity, ReadCompositeRequest request) {
        List<LwM2mPath> paths = request.getPaths();
        if (paths.size() == 1 && paths.get(0).isRoot()) {
            // TODO implement read for "/" use case.
            return ReadCompositeResponse.internalServerError("Not implemented yet");
        }

        // Read Nodes
        Map<LwM2mPath, LwM2mNode> content = new HashMap<>();
        boolean isEmpty = true; // true if don't succeed to read any of requested path
        for (LwM2mPath path : paths) {
            // Get corresponding object enabler
            Integer objectId = path.getObjectId();
            LwM2mObjectEnabler objectEnabler = tree.getObjectEnabler(objectId);

            LwM2mNode node = null;
            if (objectEnabler != null) {
                ReadResponse response = objectEnabler.read(identity,
                        new ReadRequest(request.getResponseContentFormat(), path, request.getCoapRequest()));
                if (response.isSuccess()) {
                    node = response.getContent();
                    isEmpty = false;
                } else {
                    LOG.debug("Server {} try to read node {} in a Read-Composite Request {} but it failed for {} {}",
                            identity, path, paths, response.getCode(), response.getErrorMessage());
                }
            } else {
                LOG.debug(
                        "Server {} try to read node {} in a Read-Composite Request {} but it failed because Object {} is not supported",
                        identity, path, paths, objectId);
            }
            // LWM2M specification says that "Read-Composite operation is treated as
            // non-atomic and handled as best effort by the client. That is, if any of the requested resources do not
            // have a valid value to return, they will not be included in the response".
            // So If we are not able to read a node (error or not supported we just ignore it) and add NULL to the list.
            content.put(path, node);
        }
        if (isEmpty) {
            return ReadCompositeResponse.notFound();
        } else {
            return ReadCompositeResponse.success(content);
        }
    }

    @Override
    public WriteCompositeResponse write(ServerIdentity identity, WriteCompositeRequest request) {
        // We first need to check if targeted object and instance exist and if there are writable.
        Map<Integer, LwM2mObjectEnabler> enablers = new HashMap<>();
        for (Entry<LwM2mPath, LwM2mNode> entry : request.getNodes().entrySet()) {
            LwM2mPath path = entry.getKey();
            LwM2mObjectEnabler objectEnabler = tree.getObjectEnabler(path.getObjectId());

            // check if object is supported
            if (objectEnabler == null) {
                return WriteCompositeResponse.notFound(String.format("object %s not found", path.toObjectPath()));
            }

            // check if instance is available
            if (!objectEnabler.getAvailableInstanceIds().contains(path.getObjectInstanceId())) {
                return WriteCompositeResponse
                        .notFound(String.format("object instance %s not found", path.toObjectInstancePath()));
            }

            // check if resource is writable
            ObjectModel model = objectEnabler.getObjectModel();
            ResourceModel resourceModel = model.resources.get(path.getResourceId());
            if (!resourceModel.operations.isWritable()) {
                return WriteCompositeResponse
                        .methodNotAllowed(String.format("resource %s is not writable", path.toResourcePath()));
            }

            // check about single/multi instance resource
            LwM2mNode node = entry.getValue();
            if (path.isResource()) {
                if (resourceModel.multiple) {
                    return WriteCompositeResponse.badRequest(String.format("resource %s is multi instance", path));
                }
                if (!(node instanceof LwM2mSingleResource)) {
                    return WriteCompositeResponse
                            .badRequest(String.format("invalid path %s for %s", path, node.getClass().getSimpleName()));
                }
            } else if (path.isResourceInstance()) {
                if (!resourceModel.multiple) {
                    return WriteCompositeResponse.badRequest(String.format("resource %s is single instance", path));
                }
                if (!(node instanceof LwM2mResourceInstance)) {
                    return WriteCompositeResponse
                            .badRequest(String.format("invalid path %s for %s", path, node.getClass().getSimpleName()));
                }
            }
            enablers.put(path.getObjectId(), objectEnabler);
        }

        // TODO all of this should be done in an atomic way. So I suppose if we want to support this we need to add a
        // kind of transaction mechanism with rollback feature and also a way to lock objectTree?
        // current transaction mechanism is just about regroup event and so observe notification.

        // Write Nodes
        try {
            tree.beginTransaction(enablers);
            for (Entry<LwM2mPath, LwM2mNode> entry : request.getNodes().entrySet()) {
                // Get corresponding object enabler
                LwM2mPath path = entry.getKey();
                LwM2mNode node = entry.getValue();
                LwM2mObjectEnabler objectEnabler = enablers.get(path.getObjectId());

                // WriteComposite is about patching so we need to use write UPDATE Mode
                // which is only available on instance.
                // So we create an instance from resource or resource instance.
                LwM2mObjectInstance instance;
                if (node instanceof LwM2mResource) {
                    instance = new LwM2mObjectInstance(path.getObjectInstanceId(), (LwM2mResource) node);
                } else if (node instanceof LwM2mResourceInstance) {
                    LwM2mResourceInstance resourceInstance = (LwM2mResourceInstance) node;
                    instance = new LwM2mObjectInstance(path.getObjectInstanceId(), new LwM2mMultipleResource(
                            path.getResourceId(), resourceInstance.getType(), resourceInstance));
                } else {
                    return WriteCompositeResponse.internalServerError(
                            String.format("node %s should be a resource or a resource instance, not a %s", path,
                                    node.getClass().getSimpleName()));
                }

                WriteResponse response = objectEnabler.write(identity, new WriteRequest(Mode.UPDATE,
                        request.getContentFormat(), path.toObjectInstancePath(), instance, request.getCoapRequest()));

                if (response.isFailure()) {
                    return new WriteCompositeResponse(response.getCode(), response.getErrorMessage(), null);
                }
            }
        } finally {
            tree.endTransaction(enablers);
        }
        return WriteCompositeResponse.success();

    }

    @Override
    public synchronized ObserveCompositeResponse observe(ServerIdentity identity, ObserveCompositeRequest request) {
        List<LwM2mPath> paths = request.getPaths();

        // Read Nodes
        Map<LwM2mPath, LwM2mNode> content = new HashMap<>();
        boolean isEmpty = true; // true if don't succeed to read any of requested path
        for (LwM2mPath path : paths) {
            // Get corresponding object enabler
            Integer objectId = path.getObjectId();
            LwM2mObjectEnabler objectEnabler = tree.getObjectEnabler(objectId);

            LwM2mNode node = null;
            if (objectEnabler != null) {
                ReadResponse response = objectEnabler.observe(identity,
                        new ObserveRequest(request.getResponseContentFormat(), path, request.getCoapRequest()));
                if (response.isSuccess()) {
                    node = response.getContent();
                    isEmpty = false;
                } else {
                    LOG.debug("Server {} try to read node {} in a Observe-Composite Request {} but it failed for {} "
                            + "{}", identity, path, paths, response.getCode(), response.getErrorMessage());
                }
            } else {
                LOG.debug("Server {} try to read node {} in a Observe-Composite Request {} but it failed because "
                        + "Object {} is not supported", identity, path, paths, objectId);
            }

            content.put(path, node);
        }
        if (isEmpty) {
            return ObserveCompositeResponse.notFound();
        } else {
            return ObserveCompositeResponse.success(content);
        }
    }

    @Override
    public LwM2mModel getModel() {
        return tree.getModel();
    }
}
