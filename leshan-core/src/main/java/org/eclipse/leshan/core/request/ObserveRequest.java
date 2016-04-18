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
import org.eclipse.leshan.core.response.ObserveResponse;

/**
 * A Lightweight M2M request for observing changes of a specific Resource, Resources within an Object Instance or for
 * all the Object Instances of an Object within the LWM2M Client.
 */
public class ObserveRequest extends AbstractDownlinkRequest<ObserveResponse> {

    private ContentFormat format;

    /**
     * Creates a request for observing future changes of all instances of a particular object of a client.
     * 
     * @param objectId the object ID of the resource
     */
    public ObserveRequest(int objectId) {
        this(null, new LwM2mPath(objectId));
    }

    /**
     * Creates a request for observing future changes of all instances of a particular object of a client.
     * 
     * @param format the desired format for the response (TLV or JSON)
     * @param objectId the object ID of the resource
     */
    public ObserveRequest(ContentFormat format, int objectId) {
        this(format, new LwM2mPath(objectId));
    }

    /**
     * Creates a request for observing future changes of a particular object instance of a client.
     * 
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     */
    public ObserveRequest(int objectId, int objectInstanceId) {
        this(null, new LwM2mPath(objectId, objectInstanceId));
    }

    /**
     * Creates a request for observing future changes of a particular object instance of a client.
     * 
     * @param format the desired format for the response (TLV or JSON)
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     */
    public ObserveRequest(ContentFormat format, int objectId, int objectInstanceId) {
        this(format, new LwM2mPath(objectId, objectInstanceId));
    }

    /**
     * Creates a request for observing future changes of a specific resource of a client.
     * 
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     * @param resourceId the (individual) resource's ID
     */
    public ObserveRequest(int objectId, int objectInstanceId, int resourceId) {
        this(null, new LwM2mPath(objectId, objectInstanceId, resourceId));
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
        this(format, new LwM2mPath(objectId, objectInstanceId, resourceId));
    }

    /**
     * Creates a request for observing future changes of a particular LWM2M node (object, object instance or resource).
     * 
     * @param path the path to the LWM2M node to observe
     * @throw IllegalArgumentException if the path is not valid
     */
    public ObserveRequest(String path) {
        this(null, new LwM2mPath(path));
    }

    /**
     * Creates a request for observing future changes of a particular LWM2M node (object, object instance or resource).
     * 
     * @param format the desired format for the response
     * @param path the path to the LWM2M node to observe
     * @throw IllegalArgumentException if the path is not valid
     */
    public ObserveRequest(ContentFormat format, String path) {
        this(format, new LwM2mPath(path));
    }

    private ObserveRequest(ContentFormat format, LwM2mPath target) {
        super(target);
        this.format = format;
    }

    /**
     * @return the desired format of the resource to read
     */
    public ContentFormat getFormat() {
        return format;
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String toString() {
        return String.format("ObserveRequest [path=%s format=%s]", getPath(), format);
    }

}
