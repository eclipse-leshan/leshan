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

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.CreateResponse;

/**
 * A Lightweight M2M request for creating resources on a client.
 */
public class CreateRequest extends AbstractDownlinkRequest<CreateResponse> {

    private final LwM2mResource[] resources;

    private final ContentFormat contentFormat;

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param objectId the object ID
     * @param resources the resource values for the new instance
     * @param contentFormat the payload format
     */
    public CreateRequest(int objectId, LwM2mResource[] resources, ContentFormat contentFormat) {
        this(new LwM2mPath(objectId), resources, contentFormat);
    }

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param objectId the object ID
     * @param objectInstanceId the ID of the new object instance
     * @param resources the resource values for the new instance
     * @param contentFormat the payload format
     */
    public CreateRequest(int objectId, int objectInstanceId, LwM2mResource[] resources, ContentFormat contentFormat) {
        this(new LwM2mPath(objectId, objectInstanceId), resources, contentFormat);
    }

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param path the target path
     * @param resources the resource values for the new instance
     * @param contentFormat the payload format
     */
    public CreateRequest(String path, LwM2mResource[] resources, ContentFormat contentFormat) {
        this(new LwM2mPath(path), resources, contentFormat);
    }

    private CreateRequest(LwM2mPath target, LwM2mResource[] resources, ContentFormat format) {
        super(target);

        if (target.isResource()) {
            throw new IllegalArgumentException("Cannot create a resource node");
        }

        this.resources = resources;
        this.contentFormat = format != null ? format : ContentFormat.TLV; // default to TLV
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public LwM2mResource[] getResources() {
        return resources;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CreateRequest [").append(getPath()).append("]");
        return builder.toString();
    }
}
