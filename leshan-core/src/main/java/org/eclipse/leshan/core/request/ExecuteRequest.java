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
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * A request for executing resources on a client.
 */
public class ExecuteRequest extends AbstractDownlinkRequest<LwM2mResponse> {

    private final byte[] parameters;
    private final ContentFormat contentFormat;

    public ExecuteRequest(final String path) {
        this(new LwM2mPath(path), null, null);
    }

    public ExecuteRequest(final String path, final byte[] parameters, final ContentFormat format) {
        this(new LwM2mPath(path), parameters, format);
    }

    /**
     * Creates a new <em>execute</em> request for a resource that does not require any parameters.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     */
    public ExecuteRequest(final int objectId, final int objectInstanceId, final int resourceId) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), null, null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting parameters encoded as plain text or JSON.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     * @param parameters the parameters
     */
    public ExecuteRequest(final int objectId, final int objectInstanceId, final int resourceId,
            final byte[] parameters, final ContentFormat format) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), parameters, format);
    }

    private ExecuteRequest(final LwM2mPath path, final byte[] parameters, final ContentFormat format) {
        super(path);

        this.parameters = parameters;
        this.contentFormat = format;
    }

    @Override
    public String toString() {
        return String.format("ExecuteRequest [%s]", getPath());
    }

    @Override
    public void accept(final DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public byte[] getParameters() {
        return parameters;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

}
