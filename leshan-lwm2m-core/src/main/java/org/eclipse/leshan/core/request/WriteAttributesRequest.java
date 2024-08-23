/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

import java.util.Objects;

import org.eclipse.leshan.core.link.lwm2m.attributes.AttributeClass;
import org.eclipse.leshan.core.link.lwm2m.attributes.InvalidAttributesException;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.WriteAttributesResponse;

public class WriteAttributesRequest extends AbstractSimpleDownlinkRequest<WriteAttributesResponse>
        implements DownlinkDeviceManagementRequest<WriteAttributesResponse> {

    private final LwM2mAttributeSet attributes;

    public WriteAttributesRequest(int objectId, LwM2mAttributeSet attributes) throws InvalidRequestException {
        this(newPath(objectId), attributes, null);
    }

    public WriteAttributesRequest(int objectId, int objectInstanceId, LwM2mAttributeSet attributes)
            throws InvalidRequestException {
        this(newPath(objectId, objectInstanceId), attributes);
    }

    public WriteAttributesRequest(int objectId, int objectInstanceId, int resourceId, LwM2mAttributeSet attributes)
            throws InvalidRequestException {
        this(newPath(objectId, objectInstanceId, resourceId), attributes);
    }

    public WriteAttributesRequest(int objectId, int objectInstanceId, int resourceId, int resourceInstanceId,
            LwM2mAttributeSet attributes) throws InvalidRequestException {
        this(newPath(objectId, objectInstanceId, resourceId, resourceInstanceId), attributes);
    }

    public WriteAttributesRequest(String path, LwM2mAttributeSet attributes) {
        this(newPath(path), attributes);
    }

    public WriteAttributesRequest(String path, LwM2mAttributeSet attributes, Object coapRequest) {
        this(newPath(path), attributes, coapRequest);
    }

    public WriteAttributesRequest(LwM2mPath path, LwM2mAttributeSet attributes) {
        this(path, attributes, null);
    }

    private WriteAttributesRequest(LwM2mPath path, LwM2mAttributeSet attributes, Object coapRequest)
            throws InvalidRequestException {
        super(path, coapRequest);
        if (path.isRoot())
            throw new InvalidRequestException("WriteAttributes request cannot target root path");

        if (attributes == null)
            throw new InvalidRequestException("attributes are mandatory for %s", path);
        this.attributes = attributes;

        // validate attribute
        for (LwM2mAttribute<?> attribute : attributes.getLwM2mAttributes()) {
            if (attribute.getModel().getAttributeClass() != AttributeClass.NOTIFICATION) {
                throw new InvalidRequestException(
                        "Attribute %s is of class %s but only NOTIFICATION attribute can be used in WRITE ATTRIBUTE request.",
                        attribute.getName(), attribute.getModel().getAttributeClass());
            } else if (!attribute.isWritable()) {
                throw new InvalidRequestException("Attribute %s is not writable (access mode %s).", attribute.getName(),
                        attribute.getModel().getAccessMode());
            }
            if (attribute.getValue() == null && !(attribute.getModel().queryParamCanBeValueless())) {
                throw new InvalidRequestException("Attribute %s can have null value.", attribute.getName());
            }
        }
        try {
            attributes.validate(path);
        } catch (InvalidAttributesException e) {
            throw new InvalidRequestException(e, "Some attributes are not valid for the path %s.", path);
        }
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(DownlinkDeviceManagementRequestVisitor visitor) {
        visitor.visit(this);
    }

    public LwM2mAttributeSet getAttributes() {
        return this.attributes;
    }

    @Override
    public String toString() {
        return String.format("WriteAttributesRequest [%s, attributes=%s]", getPath(), getAttributes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof WriteAttributesRequest))
            return false;
        if (!super.equals(o))
            return false;
        WriteAttributesRequest that = (WriteAttributesRequest) o;
        return that.canEqual(this) && Objects.equals(attributes, that.attributes);
    }

    public boolean canEqual(Object o) {
        return (o instanceof WriteAttributesRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attributes);
    }
}
