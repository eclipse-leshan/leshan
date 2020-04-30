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
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;

/**
 * A LWM2M request for writing object instances during the bootstrap phase.
 */
public class BootstrapWriteRequest extends AbstractDownlinkRequest<BootstrapWriteResponse> {

    private final LwM2mNode node;
    private final ContentFormat contentFormat;

    public BootstrapWriteRequest(LwM2mPath target, LwM2mNode node, ContentFormat format)
            throws InvalidRequestException {
        super(target);
        if (target.isRoot())
            throw new InvalidRequestException("BootstrapWrite request cannot target root path");

        if (node == null)
            throw new InvalidRequestException("new node value is mandatory for %s", target);

        // Validate node and path coherence
        if (getPath().isResource()) {
            if (!(node instanceof LwM2mResource)) {
                throw new InvalidRequestException("path '%s' and node type '%s' do not match", target,
                        node.getClass().getSimpleName());
            }
        } else if (getPath().isObjectInstance()) {
            if (!(node instanceof LwM2mObjectInstance)) {
                throw new InvalidRequestException("path '%s' and node type '%s' do not match", target,
                        node.getClass().getSimpleName());
            }
        } else if (getPath().isObject()) {
            if (!(node instanceof LwM2mObject)) {
                throw new InvalidRequestException("path '%s' and node type '%s' do not match", target,
                        node.getClass().getSimpleName());
            }
        }

        // Validate content format
        if (ContentFormat.TEXT == format || ContentFormat.OPAQUE == format) {
            if (!getPath().isResource()) {
                throw new InvalidRequestException(
                        "Invalid format for path %s: %s format must be used only for single resources", target, format);
            } else {
                LwM2mResource resource = (LwM2mResource) node;
                if (resource.isMultiInstances()) {
                    throw new InvalidRequestException(
                            "Invalid format for path %s: %s format must be used only for single resources", target,
                            format);
                } else {
                    if (resource.getType() == Type.OPAQUE && format == ContentFormat.TEXT) {
                        throw new InvalidRequestException(
                                "Invalid format for path %s: TEXT format must not be used for byte array single resources",
                                target);
                    } else if (resource.getType() != Type.OPAQUE && format == ContentFormat.OPAQUE) {
                        throw new InvalidRequestException(
                                "Invalid format for path %s: OPAQUE format must be used only for byte array single resources",
                                target);
                    }
                }
            }
        }

        this.node = node;
        if (format == null) {
            this.contentFormat = ContentFormat.TLV; // use TLV as default content type
        } else {
            this.contentFormat = format;
        }
    }

    public LwM2mNode getNode() {
        return node;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("BootstrapWriteRequest [path=%s, node=%s, contentFormat=%s]", getPath(), node,
                contentFormat);
    }

}
