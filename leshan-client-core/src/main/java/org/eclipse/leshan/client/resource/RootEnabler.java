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
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default implementation of {@link LwM2mRootEnabler}.
 */
public class RootEnabler implements LwM2mRootEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(RootEnabler.class);

    private final LwM2mObjectTree tree;
    private final LwM2mModel model;

    public RootEnabler(final LwM2mObjectTree tree) {
        this.tree = tree;
        this.model = new LwM2mModel() {

            @Override
            public ResourceModel getResourceModel(int objectId, int resourceId) {
                ObjectModel objectModel = this.getObjectModel(objectId);
                if (objectModel != null)
                    return objectModel.resources.get(resourceId);
                return null;
            }

            @Override
            public Collection<ObjectModel> getObjectModels() {
                // TODO implements this ?
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public ObjectModel getObjectModel(int objectId) {
                LwM2mObjectEnabler objectEnabler = tree.getObjectEnabler(objectId);
                if (objectEnabler != null)
                    return objectEnabler.getObjectModel();
                return null;
            }
        };
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
    public LwM2mModel getModel() {
        return model;
    }
}
