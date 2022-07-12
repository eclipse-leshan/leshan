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
public class ExecuteRequest extends AbstractSimpleDownlinkRequest<ExecuteResponse> {

    private final Arguments arguments;

    /**
     * Creates a new <em>execute</em> request for a resource that does not require any arguments.
     *
     * @param path the path of the resource to execute
     * @exception InvalidRequestException if the path is not valid.
     */
    public ExecuteRequest(String path) throws InvalidRequestException {
        this(path, (Arguments) null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting arguments encoded as plain text.
     *
     * @param path the path of the resource to execute
     * @param arguments the arguments
     * @exception InvalidRequestException if the path is not valid.
     */
    public ExecuteRequest(String path, String arguments) throws InvalidRequestException {
        this(newPath(path), newArguments(arguments), null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting arguments encoded as plain text.
     *
     * @param path the path of the resource to execute
     * @param arguments the arguments
     * @exception InvalidRequestException if the path is not valid.
     */
    public ExecuteRequest(String path, Arguments arguments) throws InvalidRequestException {
        this(newPath(path), arguments, null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting arguments encoded as plain text.
     *
     * @param path the path of the resource to execute
     * @param arguments the arguments
     * @param coapRequest the underlying request
     *
     * @exception InvalidRequestException if the path is not valid.
     */
    public ExecuteRequest(String path, String arguments, Object coapRequest) throws InvalidRequestException {
        this(newPath(path), newArguments(arguments), coapRequest);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting arguments encoded as plain text.
     *
     * @param path the path of the resource to execute
     * @param arguments the arguments
     * @param coapRequest the underlying request
     *
     * @exception InvalidRequestException if the path is not valid.
     */
    public ExecuteRequest(String path, Arguments arguments, Object coapRequest) throws InvalidRequestException {
        this(newPath(path), arguments, coapRequest);
    }

    /**
     * Creates a new <em>execute</em> request for a resource that does not require any arguments.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     */
    public ExecuteRequest(int objectId, int objectInstanceId, int resourceId) {
        this(newPath(objectId, objectInstanceId, resourceId), null, null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting arguments encoded as plain text.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     * @param arguments the arguments
     */
    public ExecuteRequest(int objectId, int objectInstanceId, int resourceId, String arguments) {
        this(newPath(objectId, objectInstanceId, resourceId), newArguments(arguments), null);
    }

    /**
     * Creates a new <em>execute</em> request for a resource accepting arguments encoded as plain text.
     *
     * @param objectId the resource's object ID
     * @param objectInstanceId the resource's object instance ID
     * @param resourceId the resource's ID
     * @param arguments the arguments
     */
    public ExecuteRequest(int objectId, int objectInstanceId, int resourceId, Arguments arguments) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), arguments, null);
    }

    private ExecuteRequest(LwM2mPath path, Arguments arguments, Object coapRequest) {
        super(path, coapRequest);
        if (path.isRoot())
            throw new InvalidRequestException("Execute request cannot target root path");

        if (!path.isResource())
            throw new InvalidRequestException("Invalid path %s : Only resource can be executed.", path);

        if (arguments == null) {
            this.arguments = Arguments.emptyArguments();
        } else {
            this.arguments = arguments;
        }
    }

    private static Arguments newArguments(String arguments) {
        try {
            return Arguments.parse(arguments);
        } catch (InvalidArgumentException e) {
            throw new InvalidRequestException(e, "Invalid Execute request : [%s]", e.getMessage());
        }
    }

    @Override
    public String toString() {
        return String.format("ExecuteRequest [path=%s, arguments=%s]", getPath(), getArguments());
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public Arguments getArguments() {
        return arguments;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((arguments == null) ? 0 : arguments.hashCode());
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
        if (arguments == null) {
            if (other.arguments != null)
                return false;
        } else if (!arguments.equals(other.arguments))
            return false;
        return true;
    }
}
