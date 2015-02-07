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
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.response.CreateResponse;

/**
 * A Lightweight M2M request for creating resources on a client.
 */
public class CreateRequest extends AbstractDownlinkRequest<CreateResponse> {

    private final LwM2mObjectInstance instance;

    private final ContentFormat contentFormat;

    /**
     * Creates a request for creating the (only) instance of a particular object.
     * 
     * @param objectId the object ID
     * @param values the TLV encoded resource values of the object instance
     */
    public CreateRequest(int objectId, LwM2mObjectInstance instance, ContentFormat contentFormat) {
        this(new LwM2mPath(objectId), instance, contentFormat);
    }

    /**
     * Creates a request for creating the (only) instance of a particular object.
     * 
     * @param objectId the object ID
     * @param objectInstanceId the ID of the new object instance
     * @param values the TLV encoded resource values of the object instance
     */
    public CreateRequest(int objectId, int objectInstanceId, LwM2mObjectInstance instance, ContentFormat contentFormat) {
        this(new LwM2mPath(objectId, objectInstanceId), instance, contentFormat);
    }

    /**
     * Creates a request for creating the (only) instance of a particular object.
     * 
     * @param path the target path
     * @param values the TLV encoded resource values of the object instance
     */
    public CreateRequest(String path, LwM2mObjectInstance instance, ContentFormat contentFormat) {
        this(new LwM2mPath(path), instance, contentFormat);
    }

    private CreateRequest(LwM2mPath target, LwM2mObjectInstance instance, ContentFormat format) {
        super(target);

        if (target.isResource()) {
            throw new IllegalArgumentException("Cannot create a resource node");
        }

        this.instance = instance;
        this.contentFormat = format != null ? format : ContentFormat.TLV; // default to TLV
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public LwM2mNode getObjectInstance() {
        return instance;
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
