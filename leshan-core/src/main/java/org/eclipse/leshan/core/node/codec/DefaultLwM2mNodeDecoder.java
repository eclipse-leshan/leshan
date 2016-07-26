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
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonDecoder;
import org.eclipse.leshan.core.node.codec.opaque.LwM2mNodeOpaqueDecoder;
import org.eclipse.leshan.core.node.codec.text.LwM2mNodeTextDecoder;
import org.eclipse.leshan.core.node.codec.tlv.LwM2mNodeTlvDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLwM2mNodeDecoder implements LwM2mNodeDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLwM2mNodeDecoder.class);

    @Override
    public LwM2mNode decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model)
            throws InvalidValueException {
        return decode(content, format, path, model, nodeClassFromPath(path));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LwM2mNode> T decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model,
            Class<T> nodeClass) throws InvalidValueException {

        LOG.debug("Decoding value for path {} and format {}: {}", path, format, content);
        Validate.notNull(path);
        Validate.notNull(format);

        // Decode content.
        switch (format.getCode()) {
        case ContentFormat.TEXT_CODE:
            return (T) LwM2mNodeTextDecoder.decode(content, path, model);
        case ContentFormat.TLV_CODE:
            return LwM2mNodeTlvDecoder.decode(content, path, model, nodeClass);
        case ContentFormat.OPAQUE_CODE:
            return (T) LwM2mNodeOpaqueDecoder.decode(content, path, model);
        case ContentFormat.JSON_CODE:
            return LwM2mNodeJsonDecoder.decode(content, path, model, nodeClass);
        case ContentFormat.LINK_CODE:
            throw new UnsupportedOperationException("Content format " + format + " not yet implemented '" + path + "'");
        }
        return null;
    }

    @Override
    public List<TimestampedLwM2mNode> decodeTimestampedData(byte[] content, ContentFormat format, LwM2mPath path,
            LwM2mModel model) throws InvalidValueException {
        LOG.debug("Decoding value for path {} and format {}: {}", path, format, content);
        Validate.notNull(path);
        Validate.notNull(format);

        // Decode content.
        switch (format.getCode()) {
        case ContentFormat.TEXT_CODE:
            return toTimestampedNodes(LwM2mNodeTextDecoder.decode(content, path, model));
        case ContentFormat.TLV_CODE:
            return toTimestampedNodes(LwM2mNodeTlvDecoder.decode(content, path, model, nodeClassFromPath(path)));
        case ContentFormat.OPAQUE_CODE:
            return toTimestampedNodes(LwM2mNodeOpaqueDecoder.decode(content, path, model));
        case ContentFormat.JSON_CODE:
            return LwM2mNodeJsonDecoder.decodeTimestamped(content, path, model, nodeClassFromPath(path));
        case ContentFormat.LINK_CODE:
            throw new UnsupportedOperationException("Content format " + format + " not yet implemented '" + path + "'");
        }
        return null;
    }

    private static List<TimestampedLwM2mNode> toTimestampedNodes(LwM2mNode node) {
        if (node == null)
            return Collections.emptyList();

        ArrayList<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>(1);
        timestampedNodes.add(new TimestampedLwM2mNode(null, node));
        return Collections.unmodifiableList(timestampedNodes);
    }

    public static Class<? extends LwM2mNode> nodeClassFromPath(LwM2mPath path) {
        if (path.isObject()) {
            return LwM2mObject.class;
        } else if (path.isObjectInstance()) {
            return LwM2mObjectInstance.class;
        } else if (path.isResource()) {
            return LwM2mResource.class;
        }
        throw new IllegalArgumentException("invalid path level: " + path);
    }

    @Override
    public boolean isSupported(ContentFormat format) {
        switch (format.getCode()) {
        case ContentFormat.TEXT_CODE:
        case ContentFormat.TLV_CODE:
        case ContentFormat.OPAQUE_CODE:
        case ContentFormat.JSON_CODE:
            return true;
        default:
            return false;
        }
    }
}
