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
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.BootstrapReadResponse;

/**
 * A Lightweight M2M request for retrieving the values of resources from a LWM2M Client.
 *
 * The "Bootstrap-Read" operation in the Bootstrap Interface is a restricted form of the "Read" operation found in the
 * Device Management and Service Enablement interface, and MUST be limited to target Objects that are strictly necessary
 * to setup a proper configuration in a LwM2M Client.
 */
public class BootstrapReadRequest extends AbstractSimpleDownlinkRequest<BootstrapReadResponse>
        implements BootstrapDownlinkRequest<BootstrapReadResponse> {

    private final ContentFormat format;

    /**
     * Creates a request for reading all instances of a particular object from a client.
     *
     * @param objectId the object ID of the resource
     */
    public BootstrapReadRequest(int objectId) {
        this(null, newPath(objectId), null);
    }

    /**
     * Creates a request for reading all instances of a particular object from a client.
     *
     * @param format the desired format for the response
     * @param objectId the object ID of the resource
     */
    public BootstrapReadRequest(ContentFormat format, int objectId) {
        this(format, newPath(objectId), null);
    }

    /**
     * Creates a request for reading a particular object instance from a client.
     *
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     */
    public BootstrapReadRequest(int objectId, int objectInstanceId) {
        this(null, newPath(objectId, objectInstanceId), null);
    }

    /**
     * Creates a request for reading a particular object instance from a client.
     *
     * @param format the desired format for the response
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     */
    public BootstrapReadRequest(ContentFormat format, int objectId, int objectInstanceId) {
        this(format, newPath(objectId, objectInstanceId), null);
    }

    /**
     * Create a request for reading an object/instance targeted by a specific path.
     *
     * @param path the path to the LWM2M node to read
     * @throws IllegalArgumentException if the target path is not valid
     */
    public BootstrapReadRequest(String path) {
        this(null, newPath(path), null);
    }

    /**
     * Create a request for reading an object/instance targeted by a specific path.
     *
     * @param format the desired format for the response
     * @param path the path to the LWM2M node to read
     * @throws IllegalArgumentException if the target path is not valid
     */
    public BootstrapReadRequest(ContentFormat format, String path) {
        this(format, newPath(path), null);
    }

    /**
     * Create a request for reading an object/instance targeted by a specific path.
     *
     * @param format the desired format for the response
     * @param path the path to the LWM2M node to read
     * @param coapRequest the underlying request
     *
     * @throws IllegalArgumentException if the target path is not valid
     */
    public BootstrapReadRequest(ContentFormat format, String path, Object coapRequest) {
        this(format, newPath(path), coapRequest);
    }

    /**
     * Create a request for reading an object/instance targeted by a specific path.
     * <p>
     * This constructor is mainly for internal purpose.
     *
     * @param format the desired format for the response
     * @param target the path to the LWM2M node to read
     * @param coapRequest the underlying request
     *
     * @throws IllegalArgumentException if the target path is not valid
     */
    public BootstrapReadRequest(ContentFormat format, LwM2mPath target, Object coapRequest) {
        super(target, coapRequest);
        if (!target.isObject() && !target.isObjectInstance())
            throw new InvalidRequestException("Bootstrap Read request cannot only target Object and Object instance");
        this.format = format;
    }

    /**
     * @return the desired format of the resource to read
     */
    public ContentFormat getContentFormat() {
        return format;
    }

    @Override
    public final String toString() {
        return String.format("BootstrapReadRequest [path=%s format=%s]", getPath(), format);
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((format == null) ? 0 : format.hashCode());
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
        BootstrapReadRequest other = (BootstrapReadRequest) obj;
        if (format != other.format)
            return false;
        return true;
    }
}
