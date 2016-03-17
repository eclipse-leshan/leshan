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
 *     Achim Kraus (Bosch Software Innovations GmbH) - deny delete of resource
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity to 
 *                                                     protect the security object
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public abstract class BaseObjectEnabler implements LwM2mObjectEnabler {

    int id;
    private NotifySender notifySender;
    private ObjectModel objectModel;

    public BaseObjectEnabler(int id, ObjectModel objectModel) {
        this.id = id;
        this.objectModel = objectModel;
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
    public synchronized final CreateResponse create(ServerIdentity identity, CreateRequest request) {
        // we can not create new instance on single object

        if (!identity.isSystem()) {
            if (objectModel != null && !objectModel.multiple) {
                return CreateResponse.methodNotAllowed();
            }

            if (id == LwM2mId.SECURITY) {
                return CreateResponse.notFound();
            }
        }

        // TODO we could do a validation of request.getObjectInstance() by comparing with resourceSpec information.

        return doCreate(request);
    }

    protected CreateResponse doCreate(CreateRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return CreateResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized final ReadResponse read(ServerIdentity identity, ReadRequest request) {
        LwM2mPath path = request.getPath();

        if (identity.isLwm2mBootstrapServer()) {
            // read is not supported for bootstrap
            return ReadResponse.methodNotAllowed();
        }

        if (!identity.isSystem()) {
            if (id == LwM2mId.SECURITY) {
                // read the security object is forbidden
                return ReadResponse.notFound();
            }

            // check if the resource is readable.
            if (path.isResource()) {
                ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
                if (resourceModel != null && !resourceModel.operations.isReadable()) {
                    return ReadResponse.methodNotAllowed();
                }
            }
        }

        return doRead(request, identity);

        // TODO we could do a validation of response.getContent by comparing with the spec.
    }

    protected ReadResponse doRead(ReadRequest request, ServerIdentity identity) {
        // This should be a not implemented error, but this is not defined in the spec.
        return ReadResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized final WriteResponse write(ServerIdentity identity, WriteRequest request) {
        LwM2mPath path = request.getPath();

        if (identity.isLwm2mBootstrapServer()) {
            // write is not supported for bootstrap, use bootstrap write
            return WriteResponse.methodNotAllowed();
        }

        if (!identity.isSystem()) {
            if (id == LwM2mId.SECURITY) {
                return WriteResponse.notFound();
            }

            // check if the resource is writable
            if (path.isResource()) {
                ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
                if (resourceModel != null && !resourceModel.operations.isWritable()) {
                    return WriteResponse.methodNotAllowed();
                }
            }
        }

        // TODO we could do a validation of request.getNode() by comparing with resourceSpec information

        return doWrite(request);
    }

    protected WriteResponse doWrite(WriteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return WriteResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized final BootstrapWriteResponse write(ServerIdentity identity, BootstrapWriteRequest request) {
        LwM2mPath path = request.getPath();

        if (!identity.isLwm2mBootstrapServer()) {
            // write is not supported for bootstrap, use bootstrap write
            return BootstrapWriteResponse.internalServerError("bootstrap write request from LWM2M server");
        }

        // check if the resource is writable
        if (path.isResource()) {
            ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
            if (resourceModel != null && !resourceModel.operations.isWritable()) {
                return BootstrapWriteResponse.badRequest(null);
            }
        }

        // TODO we could do a validation of request.getNode() by comparing with resourceSpec information

        return doWrite(request);
    }

    protected BootstrapWriteResponse doWrite(BootstrapWriteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return BootstrapWriteResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized final DeleteResponse delete(ServerIdentity identity, DeleteRequest request) {
        if (!identity.isLwm2mBootstrapServer() && !identity.isSystem()) {

            if (id == LwM2mId.SECURITY) {
                return DeleteResponse.notFound();
            }

            LwM2mPath path = request.getPath();
            if (path.isResource()) {
                return DeleteResponse.methodNotAllowed();
            }

            // we can not delete instance on single object
            if (objectModel != null && !objectModel.multiple) {
                return DeleteResponse.methodNotAllowed();
            }
        }

        return doDelete(request);
    }

    protected DeleteResponse doDelete(DeleteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return DeleteResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized final ExecuteResponse execute(ServerIdentity identity, ExecuteRequest request) {
        LwM2mPath path = request.getPath();

        if (identity.isLwm2mBootstrapServer()) {
            // execute is not supported for bootstrap
            return ExecuteResponse.methodNotAllowed();
        }

        if (id == LwM2mId.SECURITY) {
            return ExecuteResponse.notFound();
        }

        // only resource could be executed
        if (!path.isResource()) {
            return ExecuteResponse.badRequest(null);
        }

        // check if the resource is writable
        ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
        if (resourceModel != null && !resourceModel.operations.isExecutable()) {
            return ExecuteResponse.methodNotAllowed();
        }

        return doExecute(request);
    }

    protected ExecuteResponse doExecute(ExecuteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return ExecuteResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized WriteAttributesResponse writeAttributes(ServerIdentity identity,
            WriteAttributesRequest request) {
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

        LwM2mPath path = request.getPath();
        if (path.isObject()) {

            // Manage discover on object
            LinkObject[] linkObjects = LinkFormatHelper.getObjectDescription(getObjectModel(), null);
            return DiscoverResponse.success(linkObjects);

        } else if (path.isObjectInstance()) {

            // Manage discover on instance
            if (!getAvailableInstanceIds().contains(path.getObjectInstanceId()))
                return DiscoverResponse.notFound();

            LinkObject linkObject = LinkFormatHelper.getInstanceDescription(getObjectModel(),
                    path.getObjectInstanceId(), null);
            return DiscoverResponse.success(new LinkObject[] { linkObject });

        } else if (path.isResource()) {
            // Manage discover on resource
            if (!getAvailableInstanceIds().contains(path.getObjectInstanceId()))
                return DiscoverResponse.notFound();

            ResourceModel resourceModel = getObjectModel().resources.get(path.getResourceId());
            if (resourceModel == null)
                return DiscoverResponse.notFound();

            LinkObject linkObject = LinkFormatHelper.getResourceDescription(getObjectModel().id,
                    path.getObjectInstanceId(), resourceModel, null);
            return DiscoverResponse.success(new LinkObject[] { linkObject });
        }
        return DiscoverResponse.badRequest(null);
    }

    @Override
    public synchronized ObserveResponse observe(ServerIdentity identity, ObserveRequest request) {
        ReadResponse readResponse = this.read(identity, new ReadRequest(request.getPath().toString()));
        return new ObserveResponse(readResponse.getCode(), readResponse.getContent(), null,
                readResponse.getErrorMessage());
    }

    @Override
    public void setNotifySender(NotifySender sender) {
        notifySender = sender;
    }

    public NotifySender getNotifySender() {
        return notifySender;
    }
}
