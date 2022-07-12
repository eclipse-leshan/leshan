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

import org.eclipse.leshan.core.link.lwm2m.attributes.AttributeClass;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.WriteAttributesResponse;

public class WriteAttributesRequest extends AbstractSimpleDownlinkRequest<WriteAttributesResponse> {

    private final LwM2mAttributeSet attributes;

    public WriteAttributesRequest(int objectId, LwM2mAttributeSet attributes) throws InvalidRequestException {
        this(newPath(objectId), attributes, null);
    }

    public WriteAttributesRequest(int objectId, int objectInstanceId, LwM2mAttributeSet attributes)
            throws InvalidRequestException {
        this(newPath(objectId, objectInstanceId), attributes, null);
    }

    public WriteAttributesRequest(int objectId, int objectInstanceId, int resourceId, LwM2mAttributeSet attributes)
            throws InvalidRequestException {
        this(newPath(objectId, objectInstanceId, resourceId), attributes, null);
    }

    public WriteAttributesRequest(int objectId, int objectInstanceId, int resourceId, int resourceInstanceId,
            LwM2mAttributeSet attributes) throws InvalidRequestException {
        this(newPath(objectId, objectInstanceId, resourceId, resourceInstanceId), attributes, null);
    }

    public WriteAttributesRequest(String path, LwM2mAttributeSet attributes) {
        this(newPath(path), attributes, null);
    }

    public WriteAttributesRequest(String path, LwM2mAttributeSet attributes, Object coapRequest) {
        this(newPath(path), attributes, coapRequest);
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
        }
        try {
            attributes.validate(path);

        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e, "Some attributes are not valid for the path %s.", path);
        }

        // check some consistency about attribute set
        LwM2mAttribute<Long> pmin = attributes.getLwM2mAttribute(LwM2mAttributes.MINIMUM_PERIOD);
        LwM2mAttribute<Long> pmax = attributes.getLwM2mAttribute(LwM2mAttributes.MAXIMUM_PERIOD);
        if ((pmin != null) && (pmax != null) && pmin.hasValue() && pmax.hasValue()
                && pmin.getValue() > pmax.getValue()) {
            throw new InvalidRequestException("Cannot write attributes where '%s' > '%s'", pmin.getName(),
                    pmax.getName());
        }

        LwM2mAttribute<Long> epmin = attributes.getLwM2mAttribute(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD);
        LwM2mAttribute<Long> epmax = attributes.getLwM2mAttribute(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD);
        if ((epmin != null) && (epmax != null) && epmin.hasValue() && epmax.hasValue()
                && epmin.getValue() > epmax.getValue()) {
            throw new InvalidRequestException("Cannot write attributes where '%s' > '%s'", epmin.getName(),
                    epmax.getName());
        }

    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        WriteAttributesRequest other = (WriteAttributesRequest) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        return true;
    }
}
