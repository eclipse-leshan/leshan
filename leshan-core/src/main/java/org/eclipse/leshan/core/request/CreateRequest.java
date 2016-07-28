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

    // ***************** constructors without object instance id ******************* /

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
        this(contentFormat, new LwM2mPath(objectId), instance.getId(),
                instance.getResources().values().toArray(new LwM2mResource[0]));
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
     * Creates a request for creating an instance of a particular object using the default TLV content format. </br>
     * If the path is an object path, the instance id will be chosen by the client and accessible in the CreateResponse.
     * To choose instance id at server side, the path must be an object instance path.
     * 
     * @param path the target path (object or object instance)
     * @param resources the resource values for the new instance
     */
    public CreateRequest(String path, Collection<LwM2mResource> resources) {
        this(path, resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object. </br>
     * If the path is an object path, the instance id will be chosen by the client and accessible in the CreateResponse.
     * To choose instance id at server side, the path must be an object instance path.
     * 
     * @param contentFormat the payload format (TLV or JSON)
     * @param path the target path (object or object instance)
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, String path, Collection<LwM2mResource> resources) {
        this(contentFormat, path, resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object using the default TLV content format. </br>
     * If the path is an object path, the instance id will be chosen by the client and accessible in the CreateResponse.
     * To choose instance id at server side, the path must be an object instance path.
     * 
     * @param path the target path (object or object instance)
     * @param resources the resource values for the new instance
     */
    public CreateRequest(String path, LwM2mResource... resources) {
        this(null, new LwM2mPath(path), null, resources);
    }

    /**
     * Creates a request for creating an instance of a particular object.</br>
     * If the path is an object path, the instance id will be chosen by the client and accessible in the CreateResponse.
     * To choose instance id at server side, the path must be an object instance path.
     * 
     * @param contentFormat the payload format (TLV or JSON)
     * @param path the target path (object or object instance)
     * @param resources the resource values for the new instance
     */
    public CreateRequest(ContentFormat contentFormat, String path, LwM2mResource... resources) {
        this(contentFormat, new LwM2mPath(path), null, resources);
    }

    /**
     * Creates a request for creating an instance of a particular object.</br>
     * If the path is an object path, the instance id will be chosen by the client and accessible in the CreateResponse.
     * To choose instance id at server side, the path must be an object instance path.
     * 
     * @param path the target path (object or object instance)
     * @param instance the object instance
     */
    public CreateRequest(String path, LwM2mObjectInstance instance) {
        this(null, new LwM2mPath(path), instance.getId(),
                instance.getResources().values().toArray((new LwM2mResource[instance.getResources().size()])));
    }

    /**
     * Creates a request for creating an instance of a particular object.</br>
     * If the path is an object path, the instance id will be chosen by the client and accessible in the CreateResponse.
     * To choose instance id at server side, the path must be an object instance path.
     * 
     * @param contentFormat the payload format (TLV or JSON)
     * @param path the target path (object or object instance)
     * @param instance the object instance
     */
    public CreateRequest(ContentFormat contentFormat, String path, LwM2mObjectInstance instance) {
        this(contentFormat, new LwM2mPath(path), instance.getId(),
                instance.getResources().values().toArray((new LwM2mResource[instance.getResources().size()])));
    }

    // ***************** generic constructor ******************* /
    private CreateRequest(ContentFormat format, LwM2mPath target, Integer instanceId, LwM2mResource[] resources) {
        super(target);

        // accept only object and object instance path
        if (!target.isObject() && !target.isObjectInstance()) {
            throw new IllegalArgumentException("Create request must target an object or object instance");
        }

        // validate instance id
        if (instanceId != null && instanceId == LwM2mObjectInstance.UNDEFINED) {
            instanceId = null;
        }
        if (target.isObjectInstance()) {
            if (instanceId == null) {
                instanceId = target.getObjectInstanceId();
            } else {
                if (instanceId != target.getObjectInstanceId()) {
                    throw new IllegalArgumentException("Conflict between path instance id and node instance id");
                }
            }
        }
        Validate.isTrue(instanceId == null || instanceId >= 0, "Invalid instance id: " + instanceId);

        // store attributes
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((contentFormat == null) ? 0 : contentFormat.hashCode());
        result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
        result = prime * result + ((resources == null) ? 0 : resources.hashCode());
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
        CreateRequest other = (CreateRequest) obj;
        if (contentFormat != other.contentFormat)
            return false;
        if (instanceId == null) {
            if (other.instanceId != null)
                return false;
        } else if (!instanceId.equals(other.instanceId))
            return false;
        if (resources == null) {
            if (other.resources != null)
                return false;
        } else if (!resources.equals(other.resources))
            return false;
        return true;
    }
}
