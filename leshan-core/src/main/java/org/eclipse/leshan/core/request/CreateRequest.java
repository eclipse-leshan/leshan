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

import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.util.Validate;

/**
 * A Lightweight M2M request for creating Object Instance(s) within the LWM2M Client.
 */
public class CreateRequest extends AbstractDownlinkRequest<CreateResponse> {

    private final Integer instanceId;
    private final List<LwM2mResource> resources;
    private final ContentFormat contentFormat;

    // ***************** constructors without object id ******************* /

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance.
     * The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param contentFormat the payload format
     * @param objectId the object id
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, int objectId, LwM2mResource... resources) {
        this(contentFormat, new LwM2mPath(objectId), null, resources);
    }

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance
     * and using the TLV content format. The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param objectId the object id
     * @param resources the resource values for the new instance
     */
    public CreateRequest(int objectId, LwM2mResource... resources) {
        this(null, new LwM2mPath(objectId), null, resources);
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
        this(contentFormat, objectId, resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance
     * and using the TLV content format. The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param objectId the object id
     * @param resources the resource values for the new instance
     */
    public CreateRequest(int objectId, Collection<LwM2mResource> resources) {
        this(objectId, resources.toArray(new LwM2mResource[resources.size()]));
    }

    // ***************** constructor with object instance ******************* /

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param contentFormat the payload format
     * @param objectId the object id
     * @param instance the object instance
     */
    public CreateRequest(ContentFormat contentFormat, int objectId, LwM2mObjectInstance instance) {
        this(contentFormat, new LwM2mPath(objectId), instance.getId(), instance.getResources().values()
                .toArray(new LwM2mResource[0]));
    }

    /**
     * Creates a request for creating an instance of a particular object using the TLV content format.
     * 
     * @param objectId the object id
     * @param instance the object instance
     */
    public CreateRequest(int objectId, LwM2mObjectInstance instance) {
        this(null, objectId, instance);
    }

    // ***************** string path constructor ******************* /
    /**
     * Creates a request for creating an instance of a particular object using the default TLV content format.
     * 
     * @param path the target path
     * @param resources the resource values for the new instance
     */
    public CreateRequest(String path, Collection<LwM2mResource> resources) {
        this(path, resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param contentFormat the payload format
     * @param path the target path
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, String path, Collection<LwM2mResource> resources) {
        this(contentFormat, path, resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object using the default TLV content format.
     * 
     * @param path the target path
     * @param resources the resource values for the new instance
     */
    public CreateRequest(String path, LwM2mResource... resources) {
        this(null, new LwM2mPath(path), null, resources);
    }

    /**
     * Creates a request for creating an instance of a particular object.
     * 
     * @param contentFormat the payload format
     * @param path the target path
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, String path, LwM2mResource... resources) {
        this(contentFormat, new LwM2mPath(path), null, resources);
    }

    // ***************** generic constructor ******************* /

    private CreateRequest(ContentFormat format, LwM2mPath target, Integer instanceId, LwM2mResource[] resources) {
        super(target);

        if (!target.isObject()) {
            throw new IllegalArgumentException("Create request must target an object");
        }

        Validate.isTrue(instanceId == null || instanceId >= 0, "Invalid instance id: " + instanceId);
        this.instanceId = instanceId;
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

    /**
     * @return the id of the new instance. <code>null</code> if not assigned by the server.
     */
    public Integer getInstanceId() {
        return instanceId;
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
