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
 *     Achim Kraus (Bosch Software Innovations GmbH) - deny delete of resource
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity to 
 *                                                     protect the security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - add resource checks for 
 *                                                     REPLACE/UPDAT implementation
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;

/**
 * A abstract implementation of {@link LwM2mObjectEnabler}. It could be use as base for any {@link LwM2mObjectEnabler}
 * implementation.
 */
public abstract class BaseObjectEnabler implements LwM2mObjectEnabler {

    protected final int id;
    protected final TransactionalObjectListener transactionalListener;
    protected final ObjectModel objectModel;

    private LwM2mClient lwm2mClient;

    public BaseObjectEnabler(int id, ObjectModel objectModel) {
        this.id = id;
        this.objectModel = objectModel;
        this.transactionalListener = createTransactionListener();

    }

    protected TransactionalObjectListener createTransactionListener() {
        return new TransactionalObjectListener(this);
    }

    @Override
    public synchronized int getId() {
        return id;
    }

    @Override
    public synchronized ObjectModel getObjectModel() {
        return objectModel;
    }

    @Override
    public List<Integer> getAvailableResourceIds(int instanceId) {
        // By default we consider that all resources defined in the model are supported
        ArrayList<Integer> resourceIds = new ArrayList<>(objectModel.resources.keySet());
        Collections.sort(resourceIds);
        return resourceIds;
    }

    @Override
    public synchronized CreateResponse create(ServerIdentity identity, CreateRequest request) {
        try {
            beginTransaction();

            if (!identity.isSystem()) {
                if (id == LwM2mId.SECURITY) {
                    return CreateResponse.notFound();
                }
            } else if (identity.isLwm2mBootstrapServer()) {
                // create is not supported for bootstrap
                CreateResponse.methodNotAllowed();
            }

            if (request.unknownObjectInstanceId()) {
                if (missingMandatoryResource(request.getResources())) {
                    return CreateResponse.badRequest("mandatory writable resources missing!");
                }
            } else {
                for (LwM2mObjectInstance instance : request.getObjectInstances()) {
                    if (missingMandatoryResource(instance.getResources().values())) {
                        return CreateResponse.badRequest("mandatory writable resources missing!");
                    }
                }
            }

            return doCreate(identity, request);

        } finally {
            endTransaction();
        }
    }

    protected CreateResponse doCreate(ServerIdentity identity, CreateRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return CreateResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, ReadRequest request) {
        LwM2mPath path = request.getPath();

        // read is not supported for bootstrap
        if (identity.isLwm2mBootstrapServer()) {
            return ReadResponse.methodNotAllowed();
        }

        if (!identity.isSystem()) {
            // read the security object is forbidden
            if (id == LwM2mId.SECURITY) {
                return ReadResponse.notFound();
            }

            // check if the resource is readable.
            if (path.isResource()) {
                ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
                if (resourceModel == null) {
                    return ReadResponse.notFound();
                } else if (!resourceModel.operations.isReadable()) {
                    return ReadResponse.methodNotAllowed();
                }
            }
        }

        return doRead(identity, request);

        // TODO we could do a validation of response.getContent by comparing with resourceSpec information
    }

    protected ReadResponse doRead(ServerIdentity identity, ReadRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return ReadResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, WriteRequest request) {
        try {
            beginTransaction();

            LwM2mPath path = request.getPath();

            // write is not supported for bootstrap, use bootstrap write
            if (identity.isLwm2mBootstrapServer()) {
                return WriteResponse.methodNotAllowed();
            }

            // write the security object is forbidden
            if (LwM2mId.SECURITY == id && !identity.isSystem()) {
                return WriteResponse.notFound();
            }

            if (path.isResource()) {
                // resource write:
                // check if the resource is writable
                if (LwM2mId.SECURITY != id) { // security resources are writable by SYSTEM
                    ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
                    if (resourceModel == null) {
                        return WriteResponse.notFound();
                    } else if (!resourceModel.operations.isWritable()) {
                        return WriteResponse.methodNotAllowed();
                    }
                }
            } else if (path.isObjectInstance()) {
                // instance write:
                // check if all resources are writable
                if (LwM2mId.SECURITY != id) { // security resources are writable by SYSTEM
                    ObjectModel model = getObjectModel();
                    for (Integer writeResourceId : ((LwM2mObjectInstance) request.getNode()).getResources().keySet()) {
                        ResourceModel resourceModel = model.resources.get(writeResourceId);
                        if (null != resourceModel && !resourceModel.operations.isWritable()) {
                            return WriteResponse.methodNotAllowed();
                        }
                    }
                }

                if (request.isReplaceRequest()) {
                    if (missingMandatoryResource(((LwM2mObjectInstance) request.getNode()).getResources().values())) {
                        return WriteResponse.badRequest("mandatory writable resources missing!");
                    }
                }
            }

            // TODO we could do a validation of request.getNode() by comparing with resourceSpec information

            return doWrite(identity, request);
        } finally {
            endTransaction();
        }
    }

    protected WriteResponse doWrite(ServerIdentity identity, WriteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return WriteResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized BootstrapWriteResponse write(ServerIdentity identity, BootstrapWriteRequest request) {

        // We should not get a bootstrapWriteRequest from a LWM2M server
        if (identity.isLwm2mServer()) {
            return BootstrapWriteResponse.internalServerError("bootstrap write request from LWM2M server");
        }

        return doWrite(identity, request);
    }

    protected BootstrapWriteResponse doWrite(ServerIdentity identity, BootstrapWriteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return BootstrapWriteResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized DeleteResponse delete(ServerIdentity identity, DeleteRequest request) {
        if (!identity.isSystem()) {
            if (identity.isLwm2mBootstrapServer())
                return DeleteResponse.methodNotAllowed();

            // delete the security object is forbidden
            if (id == LwM2mId.SECURITY) {
                return DeleteResponse.notFound();
            }

            if (id == LwM2mId.DEVICE) {
                return DeleteResponse.methodNotAllowed();
            }
        }

        return doDelete(identity, request);
    }

    protected DeleteResponse doDelete(ServerIdentity identity, DeleteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return DeleteResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized BootstrapDeleteResponse delete(ServerIdentity identity, BootstrapDeleteRequest request) {
        if (!identity.isSystem()) {
            if (identity.isLwm2mServer()) {
                return BootstrapDeleteResponse.internalServerError("bootstrap delete request from LWM2M server");
            }
            if (id == LwM2mId.DEVICE) {
                return BootstrapDeleteResponse.badRequest("Device object instance is not deletable");
            }
        }
        return doDelete(identity, request);
    }

    protected BootstrapDeleteResponse doDelete(ServerIdentity identity, BootstrapDeleteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return BootstrapDeleteResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, ExecuteRequest request) {
        LwM2mPath path = request.getPath();

        // execute is not supported for bootstrap
        if (identity.isLwm2mBootstrapServer()) {
            return ExecuteResponse.methodNotAllowed();
        }

        // execute on security object is forbidden
        if (id == LwM2mId.SECURITY) {
            return ExecuteResponse.notFound();
        }

        // only resource could be executed
        if (!path.isResource()) {
            return ExecuteResponse.badRequest(null);
        }

        // check if the resource is writable
        ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
        if (resourceModel == null) {
            return ExecuteResponse.notFound();
        } else if (!resourceModel.operations.isExecutable()) {
            return ExecuteResponse.methodNotAllowed();
        }

        return doExecute(identity, request);
    }

    protected ExecuteResponse doExecute(ServerIdentity identity, ExecuteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return ExecuteResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized WriteAttributesResponse writeAttributes(ServerIdentity identity,
            WriteAttributesRequest request) {
        // execute is not supported for bootstrap
        if (identity.isLwm2mBootstrapServer()) {
            return WriteAttributesResponse.methodNotAllowed();
        }
        // TODO should be implemented here to be available for all object enabler
        // This should be a not implemented error, but this is not defined in the spec.
        return WriteAttributesResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized DiscoverResponse discover(ServerIdentity identity, DiscoverRequest request) {

        if (identity.isLwm2mBootstrapServer()) {
            // discover is not supported for bootstrap
            return DiscoverResponse.methodNotAllowed();
        }

        if (id == LwM2mId.SECURITY) {
            return DiscoverResponse.notFound();
        }
        return doDiscover(identity, request);

    }

    protected DiscoverResponse doDiscover(ServerIdentity identity, DiscoverRequest request) {

        LwM2mPath path = request.getPath();
        if (path.isObject()) {
            // Manage discover on object
            Link[] ObjectLinks = LinkFormatHelper.getObjectDescription(this, null);
            return DiscoverResponse.success(ObjectLinks);

        } else if (path.isObjectInstance()) {
            // Manage discover on instance
            if (!getAvailableInstanceIds().contains(path.getObjectInstanceId()))
                return DiscoverResponse.notFound();

            Link[] instanceLink = LinkFormatHelper.getInstanceDescription(this, path.getObjectInstanceId(), null);
            return DiscoverResponse.success(instanceLink);

        } else if (path.isResource()) {
            // Manage discover on resource
            if (!getAvailableInstanceIds().contains(path.getObjectInstanceId()))
                return DiscoverResponse.notFound();

            ResourceModel resourceModel = getObjectModel().resources.get(path.getResourceId());
            if (resourceModel == null)
                return DiscoverResponse.notFound();

            if (!getAvailableResourceIds(path.getObjectInstanceId()).contains(path.getResourceId()))
                return DiscoverResponse.notFound();

            Link resourceLink = LinkFormatHelper.getResourceDescription(this, path.getObjectInstanceId(),
                    path.getResourceId(), null);
            return DiscoverResponse.success(new Link[] { resourceLink });
        }
        return DiscoverResponse.badRequest(null);
    }

    @Override
    public synchronized BootstrapDiscoverResponse discover(ServerIdentity identity, BootstrapDiscoverRequest request) {

        if (!identity.isLwm2mBootstrapServer()) {
            return BootstrapDiscoverResponse.badRequest("not a bootstrap server");
        }

        return doDiscover(identity, request);
    }

    protected BootstrapDiscoverResponse doDiscover(ServerIdentity identity, BootstrapDiscoverRequest request) {

        LwM2mPath path = request.getPath();
        if (path.isObject()) {
            // Manage discover on object
            Link[] ObjectLinks = LinkFormatHelper.getBootstrapObjectDescription(this);
            return BootstrapDiscoverResponse.success(ObjectLinks);
        }
        return BootstrapDiscoverResponse.badRequest("invalid path");
    }

    @Override
    public synchronized ObserveResponse observe(ServerIdentity identity, ObserveRequest request) {
        LwM2mPath path = request.getPath();

        // observe is not supported for bootstrap
        if (identity.isLwm2mBootstrapServer())
            return ObserveResponse.methodNotAllowed();

        if (!identity.isSystem()) {
            // observe or read of the security object is forbidden
            if (id == LwM2mId.SECURITY)
                return ObserveResponse.notFound();

            // check if the resource is readable.
            if (path.isResource()) {
                ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
                if (resourceModel == null) {
                    return ObserveResponse.notFound();
                } else if (!resourceModel.operations.isReadable()) {
                    return ObserveResponse.methodNotAllowed();
                }
            }
        }
        return doObserve(identity, request);
    }

    protected ObserveResponse doObserve(ServerIdentity identity, ObserveRequest request) {
        ReadResponse readResponse = this.read(identity, new ReadRequest(request.getPath().toString()));
        return new ObserveResponse(readResponse.getCode(), readResponse.getContent(), null, null,
                readResponse.getErrorMessage());
    }

    protected boolean missingMandatoryResource(Collection<LwM2mResource> resources) {
        // check, if all mandatory writable resources are provided
        // Collect all mandatory writable resource IDs from the model
        Set<Integer> mandatoryResources = new HashSet<>();
        for (ResourceModel resourceModel : getObjectModel().resources.values()) {
            if (resourceModel.mandatory && (LwM2mId.SECURITY == id || resourceModel.operations.isWritable()))
                mandatoryResources.add(resourceModel.id);
        }
        // Afterwards remove the provided resource IDs from that set
        for (LwM2mResource resource : resources) {
            mandatoryResources.remove(resource.getId());
        }
        return !mandatoryResources.isEmpty();
    }

    @Override
    public void addListener(ObjectListener listener) {
        transactionalListener.addListener(listener);
    }

    @Override
    public void removeListener(ObjectListener listener) {
        transactionalListener.removeListener(listener);
    }

    protected void beginTransaction() {
        transactionalListener.beginTransaction();
    }

    protected void endTransaction() {
        transactionalListener.endTransaction();
    }

    @Override
    public void setLwM2mClient(LwM2mClient client) {
        this.lwm2mClient = client;
    }

    public LwM2mClient getLwm2mClient() {
        return lwm2mClient;
    }

    protected void fireInstancesAdded(int... instanceIds) {
        transactionalListener.objectInstancesAdded(this, instanceIds);
    }

    protected void fireInstancesRemoved(int... instanceIds) {
        transactionalListener.objectInstancesRemoved(this, instanceIds);
    }

    protected void fireResourcesChanged(int instanceid, int... resourceIds) {
        transactionalListener.resourceChanged(this, instanceid, resourceIds);
    }

    @Override
    public ContentFormat getDefaultEncodingFormat(DownlinkRequest<?> request) {
        return ContentFormat.DEFAULT;
    }
}
