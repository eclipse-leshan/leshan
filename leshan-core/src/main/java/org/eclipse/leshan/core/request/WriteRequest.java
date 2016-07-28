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
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.util.Validate;

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
     */
    public WriteRequest(final Mode mode, final ContentFormat contentFormat, final int objectId,
            final int objectInstanceId, final Collection<LwM2mResource> resources) {
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
    public WriteRequest(final Mode mode, final int objectId, final int objectInstanceId,
            final Collection<LwM2mResource> resources) {
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
     */
    public WriteRequest(final Mode mode, final ContentFormat contentFormat, final int objectId,
            final int objectInstanceId, final LwM2mResource... resources) {
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
    public WriteRequest(final Mode mode, final int objectId, final int objectInstanceId,
            final LwM2mResource... resources) {
        this(mode, ContentFormat.TLV, new LwM2mPath(objectId, objectInstanceId),
                new LwM2mObjectInstance(objectId, resources));
    }

    // ***************** write single value resource ****************** //
    /**
     * Request to write a <b>String Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(final int objectId, final int objectInstanceId, final int resourceId, String value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b>String Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     */
    public WriteRequest(final ContentFormat contentFormat, final int objectId, final int objectInstanceId,
            final int resourceId, String value) {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newStringResource(resourceId, value));
    }

    /**
     * Request to write a <b>Boolean Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(final int objectId, final int objectInstanceId, final int resourceId, boolean value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b>Boolean Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     */
    public WriteRequest(final ContentFormat contentFormat, final int objectId, final int objectInstanceId,
            final int resourceId, boolean value) {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newBooleanResource(resourceId, value));
    }

    /**
     * Request to write a <b>Integer Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(final int objectId, final int objectInstanceId, final int resourceId, long value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b>Integer Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     */
    public WriteRequest(final ContentFormat contentFormat, final int objectId, final int objectInstanceId,
            final int resourceId, long value) {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newIntegerResource(resourceId, value));
    }

    /**
     * Request to write a <b> Float Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(final int objectId, final int objectInstanceId, final int resourceId, double value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b> Float Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     */
    public WriteRequest(final ContentFormat contentFormat, final int objectId, final int objectInstanceId,
            final int resourceId, double value) {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newFloatResource(resourceId, value));
    }

    /**
     * Request to write a <b> Date Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(final int objectId, final int objectInstanceId, final int resourceId, Date value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b> Date Single-Instance Resource</b> using the given content format (TEXT, TLV, JSON).
     */
    public WriteRequest(final ContentFormat contentFormat, final int objectId, final int objectInstanceId,
            final int resourceId, Date value) {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newDateResource(resourceId, value));
    }

    /**
     * Request to write a <b> Binary Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(final int objectId, final int objectInstanceId, final int resourceId, byte[] value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b> Binary Single-Instance Resource</b> using the given content format (OPAQUE, TLV, JSON).
     */
    public WriteRequest(final ContentFormat contentFormat, final int objectId, final int objectInstanceId,
            final int resourceId, byte[] value) {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mSingleResource.newBinaryResource(resourceId, value));
    }

    /**
     * Request to write a <b> Objlnk Single-Instance Resource</b> using the TLV content format.
     */
    public WriteRequest(final int objectId, final int objectInstanceId, final int resourceId, ObjectLink value) {
        this(ContentFormat.TLV, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Request to write a <b> Objlnk Single-Instance Resource</b> using the given content format (OPAQUE, TLV, JSON,
     * TEXT).
     */
    public WriteRequest(final ContentFormat contentFormat, final int objectId, final int objectInstanceId,
            final int resourceId, ObjectLink value) {
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
     * @param values the list of resource instance (id->value) to write.
     * @param type the data type of the resource.
     */
    public WriteRequest(final ContentFormat contentFormat, final int objectId, final int objectInstanceId,
            final int resourceId, final Map<Integer, ?> values, Type type) {
        this(Mode.REPLACE, contentFormat, new LwM2mPath(objectId, objectInstanceId, resourceId),
                LwM2mMultipleResource.newResource(resourceId, values, type));
    }

    /**
     * Request to write a <b>Multi-Instance Resource</b> using the TLV content format.
     * 
     * @param objectId the id of the object to write.
     * @param objectInstanceId the id of the object instance to write.
     * @param resourceId the id of the resource to write.
     * @param values the list of resource instance (id->value) to write.
     * @param type the data type of the resource.
     */
    public WriteRequest(final int objectId, final int objectInstanceId, final int resourceId,
            final Map<Integer, ?> values, Type type) {
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
     */
    public WriteRequest(final Mode mode, final ContentFormat contentFormat, final String path, final LwM2mNode node) {
        this(mode, contentFormat, new LwM2mPath(path), node);
    }

    private WriteRequest(final Mode mode, ContentFormat format, final LwM2mPath target, final LwM2mNode node) {
        super(target);
        Validate.notNull(mode);
        Validate.notNull(node);

        // Validate Mode
        if (getPath().isResource() && mode == Mode.UPDATE) {
            throw new IllegalArgumentException(
                    String.format("Invalid mode for '%s': update is not allowed on resource", target.toString()));
        }

        // Validate node and path coherence
        if (getPath().isResource()) {
            if (!(node instanceof LwM2mResource)) {
                throw new IllegalArgumentException(String.format("path '%s' and node type '%s' does not match",
                        target.toString(), node.getClass().getSimpleName()));
            }
        } else if (getPath().isObjectInstance()) {
            if (!(node instanceof LwM2mObjectInstance)) {
                throw new IllegalArgumentException(String.format("path '%s' and node type '%s' does not match",
                        target.toString(), node.getClass().getSimpleName()));
            }
        } else if (getPath().isObject()) {
            throw new IllegalArgumentException("write request cannot target an object: " + target.toString());
        }

        // Validate content format
        if (ContentFormat.TEXT == format || ContentFormat.OPAQUE == format) {
            if (!getPath().isResource()) {
                throw new IllegalArgumentException(
                        String.format("%s format must be used only for single resources", format.toString()));
            } else {
                LwM2mResource resource = (LwM2mResource) node;
                if (resource.isMultiInstances()) {
                    throw new IllegalArgumentException(
                            String.format("%s format must be used only for single resources", format.toString()));
                } else {
                    if (resource.getType() == Type.OPAQUE && format == ContentFormat.TEXT) {
                        throw new IllegalArgumentException(
                                "TEXT format must not be used for byte array single resources");
                    } else if (resource.getType() != Type.OPAQUE && format == ContentFormat.OPAQUE) {
                        throw new IllegalArgumentException(
                                "OPAQUE format must be used only for byte array single resources");
                    }
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
    public void accept(final DownlinkRequestVisitor visitor) {
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