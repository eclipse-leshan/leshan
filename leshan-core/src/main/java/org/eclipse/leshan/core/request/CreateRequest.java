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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.CreateResponse;

/**
 * A Lightweight M2M request for creating Object Instance(s) within the LWM2M Client.
 */
public class CreateRequest extends AbstractDownlinkRequest<CreateResponse> {

    private final List<LwM2mResource> resources;
    private final ContentFormat contentFormat;

    // ***************** constructor without object instance id ******************* /

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance.
     * The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param contentFormat the payload format
     * @param objectId the object id
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, int objectId, LwM2mResource... resources) {
        this(contentFormat, new LwM2mPath(objectId), resources);
    }

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance
     * and using the TLV content format. The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param objectId the object id
     * @param resources the resource values for the new instance
     */
    public CreateRequest(int objectId, LwM2mResource... resources) {
        this(null, new LwM2mPath(objectId), resources);
    }

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance.
     * The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param contentFormat the payload format
     * @param objectId the object id
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, int objectId, Collection<LwM2mResource> resources) {
        this(contentFormat, new LwM2mPath(objectId), resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance
     * and using the TLV content format. The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param objectId the object id
     * @param resources the resource values for the new instance
     */
    public CreateRequest(int objectId, Collection<LwM2mResource> resources) {
        this(null, objectId, resources.toArray(new LwM2mResource[resources.size()]));
    }

    // ***************** constructor with object instance id ******************* /

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param contentFormat the payload format
     * @param objectId the object id
     * @param objectInstanceId the id of the new instance
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, int objectId, int objectInstanceId, LwM2mResource... resources) {
        this(contentFormat, new LwM2mPath(objectId, objectInstanceId), resources);
    }

    /**
     * Creates a request for creating an instance of a particular object using the TLV content format.
     * 
     * @param objectId the object id
     * @param objectInstanceId the id of the new instance
     * @param resources the resource values for the new instance
     */
    public CreateRequest(int objectId, int objectInstanceId, LwM2mResource... resources) {
        this(null, new LwM2mPath(objectId, objectInstanceId), resources);
    }

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param contentFormat the payload format
     * @param objectId the object id
     * @param objectInstanceId the id of the new instance
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, int objectId, int objectInstanceId,
            Collection<LwM2mResource> resources) {
        this(contentFormat, new LwM2mPath(objectId, objectInstanceId), resources.toArray(new LwM2mResource[resources
                .size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object using the TLV content format.
     * 
     * @param objectId the object id
     * @param objectInstanceId the id of the new instance
     * @param resources the resource values for the new instance
     */
    public CreateRequest(int objectId, int objectInstanceId, Collection<LwM2mResource> resources) {
        this(null, new LwM2mPath(objectId, objectInstanceId), resources.toArray(new LwM2mResource[resources.size()]));
    }

    // ***************** string path constructor ******************* /
    /**
     * Creates a request for creating an instance of a particular object using the default TLV content format.
     * 
     * @param path the target path
     * @param resources the resource values for the new instance
     */
    public CreateRequest(String path, Collection<LwM2mResource> resources) {
        this(null, new LwM2mPath(path), resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param contentFormat the payload format
     * @param path the target path
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, String path, Collection<LwM2mResource> resources) {
        this(contentFormat, new LwM2mPath(path), resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object using the default TLV content format.
     * 
     * @param path the target path
     * @param resources the resource values for the new instance
     */
    public CreateRequest(String path, LwM2mResource... resources) {
        this(null, new LwM2mPath(path), resources);
    }

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param contentFormat the payload format
     * @param path the target path
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, String path, LwM2mResource... resources) {
        this(contentFormat, new LwM2mPath(path), resources);
    }

    // ***************** generic constructor ******************* /

    private CreateRequest(ContentFormat format, LwM2mPath target, LwM2mResource[] resources) {
        super(target);

        if (target.isResource()) {
            throw new IllegalArgumentException("Cannot create a resource node");
        }

        this.resources = Collections.unmodifiableList(Arrays.asList(resources));
        this.contentFormat = format != null ? format : ContentFormat.TLV; // default to TLV
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public List<LwM2mResource> getResources() {
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
