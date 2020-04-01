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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.CreateResponse;

/**
 * A Lightweight M2M request for creating Object Instance(s) within the LWM2M Client.
 */
public class CreateRequest extends AbstractDownlinkRequest<CreateResponse> {

    private final List<LwM2mResource> resources;
    private final List<LwM2mObjectInstance> instances;
    private final ContentFormat contentFormat;

    // ***************** constructors without object instance id ******************* /

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance.
     * The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param contentFormat the payload format
     * @param objectId the object id
     * @param resources the resource values for the new instance
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public CreateRequest(ContentFormat contentFormat, int objectId, LwM2mResource... resources)
            throws InvalidRequestException {
        this(contentFormat, new LwM2mPath(objectId), resources, null);
    }

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance
     * and using the TLV content format. The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param objectId the object id
     * @param resources the resource values for the new instance
     */
    public CreateRequest(int objectId, LwM2mResource... resources) {
        this(null, new LwM2mPath(objectId), resources, null);
    }

    /**
     * Creates a request for creating an instance of a particular object without specifying the id of this new instance.
     * The id will be chosen by the client and accessible in the CreateResponse.
     * 
     * @param contentFormat the payload format
     * @param objectId the object id
     * @param resources the resource values for the new instance
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public CreateRequest(ContentFormat contentFormat, int objectId, Collection<LwM2mResource> resources)
            throws InvalidRequestException {
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
     * Creates a request for creating instances of a particular object.
     * 
     * @param contentFormat the payload format
     * @param objectId the object id
     * @param instances the object instances to create
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public CreateRequest(ContentFormat contentFormat, int objectId, LwM2mObjectInstance... instances)
            throws InvalidRequestException {
        this(contentFormat, new LwM2mPath(objectId), null, instances);
    }

    /**
     * Creates a request for creating instances of a particular object using the TLV content format.
     * 
     * @param objectId the object id
     * @param instances the object instances to create
     */
    public CreateRequest(int objectId, LwM2mObjectInstance... instances) {
        this(null, objectId, instances);
    }

    // ***************** string path constructor ******************* /
    /**
     * Creates a request for creating an instance of a particular object using the default TLV content format.
     * <p>
     * The path MUST BE an object path, the instance id will be chosen by the client and accessible in the
     * CreateResponse.
     * 
     * @param path the target object path
     * @param resources the resource values for the new instance
     * @exception InvalidRequestException if the target path is not valid.
     */
    public CreateRequest(String path, Collection<LwM2mResource> resources) throws InvalidRequestException {
        this(path, resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object.
     * <p>
     * The path MUST BE an object path, the instance id will be chosen by the client and accessible in the
     * CreateResponse.
     * 
     * @param contentFormat the payload format (TLV or JSON)
     * @param path the target object path
     * @param resources the resource values for the new instance
     * @exception InvalidRequestException if parameters are invalid.
     */
    public CreateRequest(ContentFormat contentFormat, String path, Collection<LwM2mResource> resources)
            throws InvalidRequestException {
        this(contentFormat, path, resources.toArray(new LwM2mResource[resources.size()]));
    }

    /**
     * Creates a request for creating an instance of a particular object using the default TLV content format.
     * <p>
     * The path MUST BE an object path, the instance id will be chosen by the client and accessible in the
     * CreateResponse.
     * 
     * @param path the target object path
     * @param resources the resource values for the new instance
     * @exception InvalidRequestException if the target path is not valid.
     */
    public CreateRequest(String path, LwM2mResource... resources) throws InvalidRequestException {
        this(null, newPath(path), resources, null);
    }

    /**
     * Creates a request for creating an instance of a particular object.
     * <p>
     * The path MUST BE an object path, the instance id will be chosen by the client and accessible in the
     * CreateResponse.
     * 
     * @param contentFormat the payload format (TLV or JSON)
     * @param path the target object path
     * @param resources the resource values for the new instance
     * @exception InvalidRequestException if parameters are invalid.
     */
    public CreateRequest(ContentFormat contentFormat, String path, LwM2mResource... resources)
            throws InvalidRequestException {
        this(contentFormat, newPath(path), resources, null);
    }

    /**
     * Creates a request for creating instances of a particular object.
     * 
     * @param path the target object path
     * @param instances the object instances to create
     * @exception InvalidRequestException if the target path is not valid.
     */
    public CreateRequest(String path, LwM2mObjectInstance... instances) throws InvalidRequestException {
        this(null, newPath(path), null, instances);
    }

    /**
     * Creates a request for creating instances of a particular object.
     * 
     * @param contentFormat the payload format (TLV or JSON)
     * @param path the target object path
     * @param instances the object instances to create
     * @exception InvalidRequestException if parameters are invalid.
     */
    public CreateRequest(ContentFormat contentFormat, String path, LwM2mObjectInstance... instances)
            throws InvalidRequestException {
        this(contentFormat, newPath(path), null, instances);
    }

    // ***************** generic constructor ******************* /
    private CreateRequest(ContentFormat format, LwM2mPath target, LwM2mResource[] resources,
            LwM2mObjectInstance[] instances) {
        super(target);
        // ensure instances and resources attributes is exclusives
        if ((instances == null && resources == null) || (instances != null && resources != null)) {
            throw new InvalidRequestException("instance or resources must be present (but not both)");
        }
        // accept only object
        if (target.isRoot())
            throw new InvalidRequestException("Create request cannot target root path");

        if (!target.isObject())
            throw new InvalidRequestException("Invalid path %s: Create request must target an object", target);

        // store attributes
        if (resources != null) {
            this.resources = Collections.unmodifiableList(Arrays.asList(resources));
            this.instances = null;
        } else {
            this.resources = null;
            this.instances = Collections.unmodifiableList(Arrays.asList(instances));
        }
        this.contentFormat = format != null ? format : ContentFormat.TLV; // default to TLV

        if (this.contentFormat == ContentFormat.JSON && unknownObjectInstanceId()) {
            throw new InvalidRequestException(
                    "Missing object instance id for CREATE request (%s) using JSON content format.", target);
        }
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public List<LwM2mResource> getResources() {
        return resources;
    }

    public List<LwM2mObjectInstance> getObjectInstances() {
        return instances;
    }

    public boolean unknownObjectInstanceId() {
        return instances == null;
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
        result = prime * result + ((instances == null) ? 0 : instances.hashCode());
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
        if (contentFormat == null) {
            if (other.contentFormat != null)
                return false;
        } else if (!contentFormat.equals(other.contentFormat))
            return false;
        if (instances == null) {
            if (other.instances != null)
                return false;
        } else if (!instances.equals(other.instances))
            return false;
        if (resources == null) {
            if (other.resources != null)
                return false;
        } else if (!resources.equals(other.resources))
            return false;
        return true;
    }
}
