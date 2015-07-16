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

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ValueResponse;

public abstract class BaseObjectEnabler implements LwM2mObjectEnabler {

    int id;
    private NotifySender notifySender;
    private ObjectModel objectModel;

    public BaseObjectEnabler(int id, ObjectModel objectModel) {
        this.id = id;
        this.objectModel = objectModel;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public ObjectModel getObjectModel() {
        return objectModel;
    }

    @Override
    public final CreateResponse create(CreateRequest request) {
        // we can not create new instance on single object
        if (objectModel != null && !objectModel.multiple) {
            return new CreateResponse(ResponseCode.METHOD_NOT_ALLOWED);
        }

        // TODO we could do a validation of request.getObjectInstance() by comparing with resourceSpec information.

        return doCreate(request);
    }

    protected CreateResponse doCreate(CreateRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return new CreateResponse(ResponseCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    public final ValueResponse read(ReadRequest request) {
        LwM2mPath path = request.getPath();

        // check if the resource is readable
        if (path.isResource()) {
            ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
            if (resourceModel != null && !resourceModel.operations.isReadable()) {
                return new ValueResponse(ResponseCode.METHOD_NOT_ALLOWED);
            }
        }

        return doRead(request);

        // TODO we could do a validation of response.getContent by comparing with the spec.
    }

    protected ValueResponse doRead(ReadRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return new ValueResponse(ResponseCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    public final LwM2mResponse write(WriteRequest request) {
        LwM2mPath path = request.getPath();

        // check if the resource is writable
        if (path.isResource()) {
            ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
            if (resourceModel != null && !resourceModel.operations.isWritable()) {
                return new LwM2mResponse(ResponseCode.METHOD_NOT_ALLOWED);
            }
        }

        // TODO we could do a validation of request.getNode() by comparing with resourceSpec information

        return doWrite(request);
    }

    protected LwM2mResponse doWrite(WriteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return new LwM2mResponse(ResponseCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    public final LwM2mResponse delete(DeleteRequest request) {
        // we can not create new instance on single object
        if (objectModel != null && !objectModel.multiple) {
            return new CreateResponse(ResponseCode.METHOD_NOT_ALLOWED);
        }

        return doDelete(request);
    }

    protected LwM2mResponse doDelete(DeleteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return new LwM2mResponse(ResponseCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    public final LwM2mResponse execute(ExecuteRequest request) {
        LwM2mPath path = request.getPath();

        // only resource could be executed
        if (!path.isResource()) {
            return new LwM2mResponse(ResponseCode.BAD_REQUEST);
        }

        // check if the resource is writable
        ResourceModel resourceModel = objectModel.resources.get(path.getResourceId());
        if (resourceModel != null && !resourceModel.operations.isExecutable()) {
            return new LwM2mResponse(ResponseCode.METHOD_NOT_ALLOWED);
        }

        return doExecute(request);
    }

    protected LwM2mResponse doExecute(ExecuteRequest request) {
        // This should be a not implemented error, but this is not defined in the spec.
        return new LwM2mResponse(ResponseCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    public LwM2mResponse writeAttributes(WriteAttributesRequest request) {
        // TODO should be implemented here to be available for all object enabler
        // This should be a not implemented error, but this is not defined in the spec.
        return new LwM2mResponse(ResponseCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    public DiscoverResponse discover(DiscoverRequest request) {
        LwM2mPath path = request.getPath();
        if (path.isObject()) {

            // Manage discover on object
            LinkObject[] linkObjects = LinkFormatHelper.getObjectDescription(getObjectModel(), null);
            return new DiscoverResponse(ResponseCode.CONTENT, linkObjects);

        } else if (path.isObjectInstance()) {

            // Manage discover on instance
            if (!getAvailableInstanceIds().contains(path.getObjectInstanceId()))
                return new DiscoverResponse(ResponseCode.NOT_FOUND);

            LinkObject linkObject = LinkFormatHelper.getInstanceDescription(getObjectModel(),
                    path.getObjectInstanceId(), null);
            return new DiscoverResponse(ResponseCode.CONTENT, new LinkObject[] { linkObject });

        } else if (path.isResource()) {
            // Manage discover on resource
            if (!getAvailableInstanceIds().contains(path.getObjectInstanceId()))
                return new DiscoverResponse(ResponseCode.NOT_FOUND);

            ResourceModel resourceModel = getObjectModel().resources.get(path.getResourceId());
            if (resourceModel == null)
                return new DiscoverResponse(ResponseCode.NOT_FOUND);

            LinkObject linkObject = LinkFormatHelper.getResourceDescription(getObjectModel().id,
                    path.getObjectInstanceId(), resourceModel, null);
            return new DiscoverResponse(ResponseCode.CONTENT, new LinkObject[] { linkObject });
        }
        return new DiscoverResponse(ResponseCode.BAD_REQUEST);
    }

    @Override
    public ValueResponse observe(ObserveRequest request) {
        return this.read(new ReadRequest(request.getPath().toString()));
    }

    @Override
    public void setNotifySender(NotifySender sender) {
        notifySender = sender;
    }

    public NotifySender getNotifySender() {
        return notifySender;
    }
}
