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

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.request.argument.InvalidArgumentException;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.ExecuteResponse;

/**
 * A Lightweight M2M request for initiate some action, it can only be performed on individual Resources.
 */
public class ExecuteRequest extends AbstractDownlinkRequest<ExecuteResponse> {

    private final String parameters;

    /**
     * Creates a new <em>execute</em> request for a resource that does not require any parameters.
     *
     * @param path the path of the resource to execute
     * @exception InvalidRequestException if the path is not valid.
     */
    public ExecuteRequest(String path) throws InvalidRequestException {
        this(newPath(path), null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting parameters encoded as plain text.
     *
     * @param path the path of the resource to execute
     * @param parameters the parameters
     * 
     * @see Arguments to create valid parameters string.
     * @exception InvalidRequestException if the path is not valid or use {@link #ExecuteRequest(String, Arguments)}
     *            instead.
     */
    public ExecuteRequest(String path, String parameters) throws InvalidRequestException {
        this(newPath(path), parameters);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting parameters encoded as plain text.
     *
     * @param path the path of the resource to execute
     * @param arguments the arguments
     * 
     * @exception InvalidRequestException if the path is not valid.
     * 
     * @since 1.4
     */
    public ExecuteRequest(String path, Arguments arguments) throws InvalidRequestException {
        this(newPath(path), arguments.serialize());
    }

    /**
     * Creates a new <em>execute</em> request for a resource that does not require any parameters.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     */
    public ExecuteRequest(int objectId, int objectInstanceId, int resourceId) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting parameters encoded as plain text.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     * @param parameters the parameters
     * 
     * @see Arguments to create valid parameters string or use {@link #ExecuteRequest(int, int, int, Arguments)}
     *      instead.
     */
    public ExecuteRequest(int objectId, int objectInstanceId, int resourceId, String parameters) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), parameters);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting parameters encoded as plain text.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     * @param arguments the arguments
     * 
     * @since 1.4
     */
    public ExecuteRequest(int objectId, int objectInstanceId, int resourceId, Arguments arguments) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), arguments.serialize());
    }

    private ExecuteRequest(LwM2mPath path, String parameters) {
        super(path);
        if (path.isRoot())
            throw new InvalidRequestException("Execute request cannot target root path");

        if (!path.isResource())
            throw new InvalidRequestException("Invalid path %s : Only resource can be executed.", path);
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return String.format("ExecuteRequest [path=%s, parameters=%s]", getPath(), getParameters());
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * @see Arguments#parse(String) to parse arguments or use {@link #getArguments()} instead.
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * @since 1.4
     */
    public Arguments getArguments() throws InvalidArgumentException {
        return Arguments.parse(parameters);
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
