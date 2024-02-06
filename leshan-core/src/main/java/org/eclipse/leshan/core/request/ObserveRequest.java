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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.ObserveResponse;

/**
 * A Lightweight M2M request for observing changes of a specific Resource, Resources within an Object Instance or for
 * all the Object Instances of an Object within the LWM2M Client.
 */
public class ObserveRequest extends AbstractSimpleDownlinkRequest<ObserveResponse> {

    private final ContentFormat format;

    /* Additional information relative to this observe request */
    private final Map<String, String> context;

    /**
     * Creates a request for observing future changes of all instances of a particular object of a client.
     *
     * @param objectId the object ID of the resource
     */
    public ObserveRequest(int objectId) {
        this(null, newPath(objectId), null, null);
    }

    /**
     * Creates a request for observing future changes of all instances of a particular object of a client.
     *
     * @param format the desired format for the response (TLV or JSON)
     * @param objectId the object ID of the resource
     */
    public ObserveRequest(ContentFormat format, int objectId) {
        this(format, newPath(objectId), null, null);
    }

    /**
     * Creates a request for observing future changes of a particular object instance of a client.
     *
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     */
    public ObserveRequest(int objectId, int objectInstanceId) {
        this(null, newPath(objectId, objectInstanceId), null, null);
    }

    /**
     * Creates a request for observing future changes of a particular object instance of a client.
     *
     * @param format the desired format for the response (TLV or JSON)
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     */
    public ObserveRequest(ContentFormat format, int objectId, int objectInstanceId) {
        this(format, newPath(objectId, objectInstanceId), null, null);
    }

    /**
     * Creates a request for observing future changes of a specific resource of a client.
     *
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     * @param resourceId the (individual) resource's ID
     */
    public ObserveRequest(int objectId, int objectInstanceId, int resourceId) {
        this(null, newPath(objectId, objectInstanceId, resourceId), null, null);
    }

    /**
     * Creates a request for observing future changes of a specific resource of a client.
     *
     * @param format the desired format for the response (TLV, JSON, TEXT or OPAQUE)
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     * @param resourceId the (individual) resource's ID
     */
    public ObserveRequest(ContentFormat format, int objectId, int objectInstanceId, int resourceId) {
        this(format, newPath(objectId, objectInstanceId, resourceId), null, null);
    }

    /**
     * Creates a request for observing future changes of a specific resource instance of a client.
     *
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     * @param resourceId the (individual) resource's ID
     */
    public ObserveRequest(int objectId, int objectInstanceId, int resourceId, int resourceInstanceId) {
        this(null, newPath(objectId, objectInstanceId, resourceId, resourceInstanceId), null, null);
    }

    /**
     * Creates a request for observing future changes of a specific resource instance of a client.
     *
     * @param format the desired format for the response (TLV, JSON, TEXT or OPAQUE)
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     * @param resourceId the (individual) resource's ID
     */
    public ObserveRequest(ContentFormat format, int objectId, int objectInstanceId, int resourceId,
            int resourceInstanceId) {
        this(format, newPath(objectId, objectInstanceId, resourceId, resourceInstanceId), null, null);
    }

    /**
     * Creates a request for observing future changes of a particular LWM2M node (object, object instance or resource).
     *
     * @param path the path to the LWM2M node to observe
     * @exception InvalidRequestException if the path is not valid.
     */
    public ObserveRequest(String path) throws InvalidRequestException {
        this(null, newPath(path), null, null);
    }

    /**
     * Creates a request for observing future changes of a particular LWM2M node (object, object instance or resource).
     *
     * @param format the desired format for the response
     * @param path the path to the LWM2M node to observe
     * @exception InvalidRequestException if the path is not valid.
     */
    public ObserveRequest(ContentFormat format, String path) throws InvalidRequestException {
        this(format, newPath(path), null, null);
    }

    /**
     * Creates a request for observing future changes of a particular LWM2M node (object, object instance or resource).
     *
     * @param format the desired format for the response
     * @param path the path to the LWM2M node to observe
     * @param coapRequest the underlying request
     *
     * @exception InvalidRequestException if the path is not valid.
     */
    public ObserveRequest(ContentFormat format, String path, Object coapRequest) throws InvalidRequestException {
        this(format, newPath(path), null, coapRequest);
    }

    /**
     * Creates a request for observing future changes of a particular LWM2M node (object, object instance or resource).
     *
     * @param format the desired format for the response
     * @param path the path to the LWM2M node to observe
     * @param context additional information about the request. This context will be available via the
     *        {@link Observation} once established.
     * @exception InvalidRequestException if the path is not valid.
     */
    public ObserveRequest(ContentFormat format, String path, Map<String, String> context)
            throws InvalidRequestException {
        this(format, newPath(path), context, null);
    }

    public ObserveRequest(LwM2mPath path) throws InvalidRequestException {
        this(null, path, null, null);
    }

    public ObserveRequest(ContentFormat format, LwM2mPath path, Object coapRequest) throws InvalidRequestException {
        this(format, path, null, coapRequest);
    }

    private ObserveRequest(ContentFormat format, LwM2mPath target, Map<String, String> context, Object coapRequest) {
        super(target, coapRequest);
        if (target.isRoot())
            throw new InvalidRequestException("Observe request cannot target root path");

        this.format = format;
        if (context == null || context.isEmpty())
            this.context = Collections.emptyMap();
        else
            this.context = Collections.unmodifiableMap(new HashMap<>(context));
    }

    /**
     * @return the desired format of the resource to read
     */
    public ContentFormat getContentFormat() {
        return format;
    }

    /**
     * @return an unmodifiable map containing the additional information relative to this observe request.
     */
    public Map<String, String> getContext() {
        return context;
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String toString() {
        return String.format("ObserveRequest [path=%s format=%s]", getPath(), format);
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
        ObserveRequest other = (ObserveRequest) obj;
        if (format != other.format)
            return false;
        return true;
    }
}
