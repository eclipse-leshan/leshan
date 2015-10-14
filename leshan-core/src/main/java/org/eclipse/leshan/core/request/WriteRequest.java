/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.util.Validate;

/**
 * The request to change the value of a Resource, an array of Resources Instances or multiple Resources from an Object
 * Instance.
 */
public class WriteRequest extends AbstractDownlinkRequest<WriteResponse> {

    private final LwM2mNode node;
    private final ContentFormat contentFormat;

    private final boolean replaceRequest;

    public WriteRequest(final int objectId, final int objectInstanceId, final int resourceId, final LwM2mNode node,
            final ContentFormat contentFormat, final boolean replaceResources) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), node, contentFormat, replaceResources);
    }

    public WriteRequest(final String target, final LwM2mNode node, final ContentFormat contentFormat,
            final boolean replaceResources) {
        this(new LwM2mPath(target), node, contentFormat, replaceResources);
    }

    private WriteRequest(final LwM2mPath target, final LwM2mNode node, ContentFormat format,
            final boolean replaceResources) {
        super(target);
        Validate.notNull(node);

        // Validate node and path coherence
        if (getPath().isResource()) {
            if (!(node instanceof LwM2mResource)) {
                throw new IllegalArgumentException(String.format("path '%s' and node type '%s' does not match",
                        target.toString(), node.getClass().getSimpleName()));
            }
        }
        if (getPath().isObjectInstance()) {
            if (!(node instanceof LwM2mObjectInstance)) {
                throw new IllegalArgumentException(String.format("path '%s' and node type '%s' does not match",
                        target.toString(), node.getClass().getSimpleName()));
            }
        }
        if (getPath().isObject()) {
            if (!(node instanceof LwM2mObject)) {
                throw new IllegalArgumentException(String.format("path '%s' and node type '%s' does not match",
                        target.toString(), node.getClass().getSimpleName()));
            }
        }

        // Validate content format
        if (ContentFormat.TEXT == format || ContentFormat.OPAQUE == format) {
            if (!getPath().isResource()) {
                throw new IllegalArgumentException(String.format("%s format must be used only for single resources",
                        format.toString()));
            } else {
                if (((LwM2mResource) node).isMultiInstances()) {
                    throw new IllegalArgumentException(String.format(
                            "%s format must be used only for single resources", format.toString()));
                }
            }
        }

        this.node = node;
        this.contentFormat = format;
        this.replaceRequest = replaceResources;
    }

    /**
     * Checks whether this write request is supposed to replace all resources or do a partial update only (see section
     * 5.3.3 of the LW M2M spec).
     *
     * @return <code>true</code> if all resources are to be replaced
     */
    public boolean isReplaceRequest() {
        return this.replaceRequest;
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
        return String.format("WriteRequest [replaceRequest=%s, getPath()=%s]", replaceRequest, getPath());
    }

}