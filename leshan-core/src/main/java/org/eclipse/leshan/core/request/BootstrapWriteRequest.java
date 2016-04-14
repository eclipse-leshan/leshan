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
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.util.Validate;

/**
 * A LWM2M request for writing object instances during the bootstrap phase.
 */
public class BootstrapWriteRequest extends AbstractDownlinkRequest<BootstrapWriteResponse> {

    private final LwM2mNode node;
    private final ContentFormat contentFormat;

    public BootstrapWriteRequest(final LwM2mPath target, final LwM2mNode node, ContentFormat format) {
        super(target);
        Validate.notNull(node);

        // Validate node and path coherence
        if (getPath().isResource()) {
            if (!(node instanceof LwM2mResource)) {
                throw new IllegalArgumentException(String.format("path '%s' and node type '%s' does not match",
                        target.toString(), node.getClass().getSimpleName()));
            }
        } else if (getPath().isObjectInstance()) {
            if (!(node instanceof LwM2mObjectInstance)) {
                throw new IllegalArgumentException(String.format("path '%s' and node type '%s' does not match",
                        target.toString(), node.getClass().getSimpleName()));
            }
        } else if (getPath().isObject()) {
            if (!(node instanceof LwM2mObject)) {
                throw new IllegalArgumentException(String.format("path '%s' and node type '%s' does not match",
                        target.toString(), node.getClass().getSimpleName()));
            }
        }

        // Validate content format
        if (ContentFormat.TEXT == format || ContentFormat.OPAQUE == format) {
            if (!getPath().isResource()) {
                throw new IllegalArgumentException(
                        String.format("%s format must be used only for single resources", format.toString()));
            } else {
                LwM2mResource resource = (LwM2mResource) node;
                if (resource.isMultiInstances()) {
                    throw new IllegalArgumentException(
                            String.format("%s format must be used only for single resources", format.toString()));
                } else {
                    if (resource.getType() == Type.OPAQUE && format == ContentFormat.TEXT) {
                        throw new IllegalArgumentException(
                                "TEXT format must not be used for byte array single resources");
                    } else if (resource.getType() != Type.OPAQUE && format == ContentFormat.OPAQUE) {
                        throw new IllegalArgumentException(
                                "OPAQUE format must be used only for byte array single resources");
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
    public void accept(final DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("BootstrapWriteRequest [node=%s, contentFormat=%s]", node, contentFormat);
    }

}
