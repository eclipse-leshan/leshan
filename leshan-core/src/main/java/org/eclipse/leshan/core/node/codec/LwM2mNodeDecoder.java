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

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonDecoder;
import org.eclipse.leshan.core.node.codec.opaque.LwM2mNodeOpaqueDecoder;
import org.eclipse.leshan.core.node.codec.text.LwM2mNodeTextDecoder;
import org.eclipse.leshan.core.node.codec.tlv.LwM2mNodeTlvDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeDecoder.class);

    /**
     * Deserializes a binary content into a {@link LwM2mNode}.
     *
     * @param content the content
     * @param format the content format
     * @param path the path of the node to build
     * @param model the collection of supported object models
     * @return the resulting node
     * @throws InvalidValueException
     */
    public static LwM2mNode decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model)
            throws InvalidValueException {

        LOG.debug("Decoding value for path {} and format {}: {}", path, format, content);
        Validate.notNull(path);

        // If no format is given, guess the best one to use.
        if (format == null) {
            if (path.isResource()) {
                ResourceModel rDesc = model.getResourceModel(path.getObjectId(), path.getResourceId());
                if (rDesc != null && rDesc.multiple) {
                    format = ContentFormat.TLV;
                } else {
                    if (rDesc != null && rDesc.type == Type.OPAQUE) {
                        format = ContentFormat.OPAQUE;
                    } else {
                        format = ContentFormat.TEXT;
                    }
                }
            } else {
                format = ContentFormat.TLV;
            }
        }

        // Decode content.
        switch (format) {
        case TEXT:
            return LwM2mNodeTextDecoder.decode(content, path, model);
        case TLV:
            return LwM2mNodeTlvDecoder.decode(content, path, model);
        case OPAQUE:
            return LwM2mNodeOpaqueDecoder.decode(content, path, model);
        case JSON:
            return LwM2mNodeJsonDecoder.decode(content, path, model);
        case LINK:
            throw new UnsupportedOperationException("Content format " + format + " not yet implemented '" + path + "'");
        }
        return null;
    }
}
