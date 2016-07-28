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
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.util.Validate;

/**
 * A Lightweight M2M request for initiate some action, it can only be performed on individual Resources.
 */
public class ExecuteRequest extends AbstractDownlinkRequest<ExecuteResponse> {

    private final String parameters;

    /**
     * Creates a new <em>execute</em> request for a resource that does not require any parameters.
     *
     * @param path the path of the resource to execute
     * @throw IllegalArgumentException if the path is not valid
     */
    public ExecuteRequest(final String path) {
        this(new LwM2mPath(path), null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting parameters encoded as plain text.
     *
     * @param path the path of the resource to execute
     * @param parameters the parameters
     * @throw IllegalArgumentException if the path is not valid
     */
    public ExecuteRequest(final String path, final String parameters) {
        this(new LwM2mPath(path), parameters);
    }

    /**
     * Creates a new <em>execute</em> request for a resource that does not require any parameters.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     */
    public ExecuteRequest(final int objectId, final int objectInstanceId, final int resourceId) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting parameters encoded as plain text.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     * @param parameters the parameters
     */
    public ExecuteRequest(final int objectId, final int objectInstanceId, final int resourceId,
            final String parameters) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), parameters);
    }

    private ExecuteRequest(final LwM2mPath path, final String parameters) {
        super(path);
        Validate.isTrue(path.isResource(), "Only resource can be executed.");
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return String.format("ExecuteRequest [%s]", getPath());
    }

    @Override
    public void accept(final DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public String getParameters() {
        return parameters;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
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
        ExecuteRequest other = (ExecuteRequest) obj;
        if (parameters == null) {
            if (other.parameters != null)
                return false;
        } else if (!parameters.equals(other.parameters))
            return false;
        return true;
    }
}
