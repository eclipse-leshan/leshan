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

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.WriteResponse;

/**
 * The request to change the value of a Resource, an array of Resources Instances or multiple Resources from an Object
 * Instance.
 */
public class WriteRequest extends AbstractDownlinkRequest<WriteResponse> {

    /**
     * Define the behavior of a write request.
     */
    public enum Mode {
        /**
         * Replaces the Object Instance or the Resource(s) with the new value provided in the “Write” operation. (see
         * section 5.3.3 of the LW M2M spec).
         */
        REPLACE,
        /**
         * Adds or updates Resources provided in the new value and leaves other existing Resources unchanged. (see
         * section 5.3.3 of the LW M2M spec).
         */
        UPDATE
    }

    private final LwM2mNode node;
    private final ContentFormat contentFormat;
    private final Mode mode;

    // ***************** write instance ****************** //

    /**
     * Request to write an <b>Object Instance</b>.
     * 
     * @param mode the mode of the request : replace or update.
     * @param contentFormat Format of the payload (TLV or JSON).
     * @param objectId the id of the object to write.
     * @param objectInstanceId the id of the object instance to write.
     * @param resources the list of resources to write.
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public WriteRequest(Mode mode, ContentFormat contentFormat, int objectId, int objectInstanceId,
            Collection<LwM2mResource> resources) throws InvalidRequestException {
        this(mode, contentFormat, new LwM2mPath(objectId, objectInstanceId),
                new LwM2mObjectInstance(objectId, resources));
    }

    /**
     * Request to write an <b>Object Instance</b> using the TLV content format.
     * 
     * @param mode the mode of the request : replace or update.
     * @param objectId the id of the object to write.
     * @param objectInstanceId the id of the object instance to write.
     * @param resources the list of resources to write.
     */
    public WriteRequest(Mode mode, int objectId, int objectInstanceId, Collection<LwM2mResource> resources) {
        this(mode, ContentFormat.TLV, new LwM2mPath(objectId, objectInstanceId),
                new LwM2mObjectInstance(objectId, resources));
    }

    /**
     * Request to write an <b>Object Instance</b>.
     * 
     * @param mode the mode of the request : replace or update.
     * @param contentFormat Format of the payload (TLV or JSON).
     * @param objectId the id of the object to write.
     * @param objectInstanceId the id of the object instance to write.
     * @param resources the list of resources to write.
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public WriteRequest(Mode mode, ContentFormat contentFormat, int objectId, int objectInstanceId,
            LwM2mResource... resources) throws InvalidRequestException {
        this(mode, contentFormat, new LwM2mPath(objectId, objectInstanceId),
                new LwM2mObjectInstance(objectId, resources));
    }

    /**
     * Request to write an <b>Object Instance</b> using the TLV content format.
     * 
     * @param mode the mode of the request : replace or update.
     * @param objectId the id of the object to write.
     * @param objectInstanceId the id of the object instance to write.
     * @param resources the list of resources to write.
     */
    public WriteRequest(Mode mode, int objectId, int objectInstanceId, LwM2mResource... resources) {
        this(mode, ContentFormat.TLV, new LwM2mPath(objectId, objectInstanceId),
                new LwM2mObjectInstance(objectId, resources));
    }

    // ***************** write single value resource ****************** //
    /**
     * Request to write a <b>String Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(int objectId, int objectInstanceId, int resourceId, String value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b>String Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     * 
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public WriteRequest(ContentFormat contentFormat, int objectId, int objectInstanceId, int resourceId, String value)
            throws InvalidRequestException {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newStringResource(resourceId, value));
    }

    /**
     * Request to write a <b>Boolean Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(int objectId, int objectInstanceId, int resourceId, boolean value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b>Boolean Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     * 
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public WriteRequest(ContentFormat contentFormat, int objectId, int objectInstanceId, int resourceId, boolean value)
            throws InvalidRequestException {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newBooleanResource(resourceId, value));
    }

    /**
     * Request to write a <b>Integer Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(int objectId, int objectInstanceId, int resourceId, long value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b>Integer Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     * 
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public WriteRequest(ContentFormat contentFormat, int objectId, int objectInstanceId, int resourceId, long value)
            throws InvalidRequestException {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newIntegerResource(resourceId, value));
    }

    /**
     * Request to write a <b> Float Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(int objectId, int objectInstanceId, int resourceId, double value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b> Float Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     * 
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public WriteRequest(ContentFormat contentFormat, int objectId, int objectInstanceId, int resourceId, double value)
            throws InvalidRequestException {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newFloatResource(resourceId, value));
    }

    /**
     * Request to write a <b> Date Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(int objectId, int objectInstanceId, int resourceId, Date value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b> Date Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     * 
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public WriteRequest(ContentFormat contentFormat, int objectId, int objectInstanceId, int resourceId, Date value)
            throws InvalidRequestException {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newDateResource(resourceId, value));
    }

    /**
     * Request to write a <b> Binary Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(int objectId, int objectInstanceId, int resourceId, byte[] value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b> Binary Single-Instance Resource</b> using the given content format (OPAQUE, TLV, JSON).
     * 
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public WriteRequest(ContentFormat contentFormat, int objectId, int objectInstanceId, int resourceId, byte[] value)
            throws InvalidRequestException {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newBinaryResource(resourceId, value));
    }

    /**
     * Request to write a <b> Objlnk Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(int objectId, int objectInstanceId, int resourceId, ObjectLink value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b> Objlnk Single-Instance Resource</b> using the given content format (TLV, JSON, TEXT).
     * 
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     */
    public WriteRequest(ContentFormat contentFormat, int objectId, int objectInstanceId, int resourceId,
            ObjectLink value) throws InvalidRequestException {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newObjectLinkResource(resourceId, value));
    }

    // ***************** write multi instance resource ****************** //
    /**
     * Request to write a <b>Multi-Instance Resource</b>.
     * 
     * @param contentFormat Format of the payload (TLV or JSON).
     * @param objectId the id of the object to write.
     * @param objectInstanceId the id of the object instance to write.
     * @param resourceId the id of the resource to write.
     * @param values the list of resource instance (id-&gt;value) to write.
     * @param type the data type of the resource.
     * @exception InvalidRequestException if bad @{link ContentFormat} format was used.
     * 
     */
    public WriteRequest(ContentFormat contentFormat, int objectId, int objectInstanceId, int resourceId,
            Map<Integer, ?> values, Type type) throws InvalidRequestException {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mMultipleResource.newResource(resourceId, values, type));
    }

    /**
     * Request to write a <b>Multi-Instance Resource</b> using the TLV content format.
     * 
     * @param objectId the id of the object to write.
     * @param objectInstanceId the id of the object instance to write.
     * @param resourceId the id of the resource to write.
     * @param values the list of resource instance (id-&gt;value) to write.
     * @param type the data type of the resource.
     * @exception InvalidRequestException if parameters are invalid.
     */
    public WriteRequest(int objectId, int objectInstanceId, int resourceId, Map<Integer, ?> values, Type type)
            throws InvalidRequestException {
        this(Mode.REPLACE, ContentFormat.TLV, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mMultipleResource.newResource(resourceId, values, type));
    }

    // ***************** generic constructor ****************** //
    /**
     * A generic constructor to write request.
     * 
     * @param mode the mode of the request : replace or update.
     * @param contentFormat Format of the payload (TLV,JSON,TEXT,OPAQUE ..).
     * @param path the path of the LWM2M node to write (object instance or resource).
     * @param node the {@link LwM2mNode} to write.
     * @exception InvalidRequestException if parameters are invalid.
     */
    public WriteRequest(Mode mode, ContentFormat contentFormat, String path, LwM2mNode node)
            throws InvalidRequestException {
        this(mode, contentFormat, newPath(path), node);
    }

    private WriteRequest(Mode mode, ContentFormat format, LwM2mPath target, LwM2mNode node) {
        super(target);
        if (target.isRoot())
            throw new InvalidRequestException("Write request cannot target root path");
        if (mode == null)
            throw new InvalidRequestException("mode is mandatory for %s", target);
        if (node == null)
            throw new InvalidRequestException("new node value is mandatory for %s", target);

        // Validate Mode
        if (getPath().isResource() && mode == Mode.UPDATE)
            throw new InvalidRequestException("Invalid mode for '%s': update is not allowed on resource", target);

        // Validate node and path coherence
        if (getPath().isResource()) {
            if (!(node instanceof LwM2mResource)) {
                throw new InvalidRequestException("path '%s' and node type '%s' do not match", target,
                        node.getClass().getSimpleName());
            }
        } else if (getPath().isObjectInstance()) {
            if (!(node instanceof LwM2mObjectInstance)) {
                throw new InvalidRequestException("path '%s' and node type '%s' do not match", target,
                        node.getClass().getSimpleName());
            }
        } else if (getPath().isObject()) {
            throw new InvalidRequestException("write request %s cannot target an object", target);
        }

        // Validate content format
        if (ContentFormat.TEXT == format || ContentFormat.OPAQUE == format) {
            if (!getPath().isResource()) {
                throw new InvalidRequestException(
                        "Invalid format for %s: %s format must be used only for single resources", target, format);
            } else {
                LwM2mResource resource = (LwM2mResource) node;
                if (resource.isMultiInstances()) {
                    throw new InvalidRequestException(
                            "Invalid format for path %s: format must be used only for single resources", target,
                            format);
                } else if (resource.getType() != Type.OPAQUE && format == ContentFormat.OPAQUE) {
                    throw new InvalidRequestException(
                            "Invalid format for %s: OPAQUE format must be used only for byte array single resources",
                            target);
                }
            }
        }

        this.node = node;
        if (format == null) {
            this.contentFormat = ContentFormat.TLV; // use TLV as default content type
        } else {
            this.contentFormat = format;
        }
        this.mode = mode;
    }

    /**
     * @return <code>true</code> if all resources are to be replaced (see section 5.3.3 of the LW M2M spec).
     */
    public boolean isReplaceRequest() {
        return mode == Mode.REPLACE;
    }

    /**
     * @return <code>true</code> if this is a partial update request (see section 5.3.3 of the LW M2M spec).
     */
    public boolean isPartialUpdateRequest() {
        return mode == Mode.UPDATE;
    }

    public LwM2mNode getNode() {
        return node;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("WriteRequest [mode=%s, path=%s, format=%s, node=%s]", mode, getPath(), contentFormat,
                node);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((contentFormat == null) ? 0 : contentFormat.hashCode());
        result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        result = prime * result + ((node == null) ? 0 : node.hashCode());
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
        WriteRequest other = (WriteRequest) obj;
        if (contentFormat != other.contentFormat)
            return false;
        if (mode != other.mode)
            return false;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        return true;
    }
}
