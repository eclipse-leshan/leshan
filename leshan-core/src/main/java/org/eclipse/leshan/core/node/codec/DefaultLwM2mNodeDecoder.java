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
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.cbor.LwM2mNodeCborDecoder;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonDecoder;
import org.eclipse.leshan.core.node.codec.opaque.LwM2mNodeOpaqueDecoder;
import org.eclipse.leshan.core.node.codec.senml.LwM2mNodeSenMLDecoder;
import org.eclipse.leshan.core.node.codec.text.LwM2mNodeTextDecoder;
import org.eclipse.leshan.core.node.codec.tlv.LwM2mNodeTlvDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.senml.cbor.upokecenter.SenMLCborUpokecenterEncoderDecoder;
import org.eclipse.leshan.senml.json.jackson.SenMLJsonJacksonEncoderDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default {@link LwM2mNodeDecoder} which support default {@link ContentFormat} :
 * <ul>
 * <li>{@link ContentFormat#TLV}</li>
 * <li>{@link ContentFormat#JSON}</li>
 * <li>{@link ContentFormat#TEXT}</li>
 * <li>{@link ContentFormat#OPAQUE}</li>
 * </ul>
 */
public class DefaultLwM2mNodeDecoder implements LwM2mNodeDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLwM2mNodeDecoder.class);

    public static Map<ContentFormat, NodeDecoder> getDefaultDecoders(boolean supportDeprecatedContentFormat) {
        Map<ContentFormat, NodeDecoder> decoders = new HashMap<>();
        decoders.put(ContentFormat.TEXT, new LwM2mNodeTextDecoder());
        decoders.put(ContentFormat.OPAQUE, new LwM2mNodeOpaqueDecoder());
        decoders.put(ContentFormat.CBOR, new LwM2mNodeCborDecoder());
        decoders.put(ContentFormat.SENML_JSON, new LwM2mNodeSenMLDecoder(new SenMLJsonJacksonEncoderDecoder()));
        decoders.put(ContentFormat.SENML_CBOR, new LwM2mNodeSenMLDecoder(new SenMLCborUpokecenterEncoderDecoder()));

        // tlv
        LwM2mNodeTlvDecoder tlvDecoder = new LwM2mNodeTlvDecoder();
        decoders.put(ContentFormat.TLV, tlvDecoder);
        if (supportDeprecatedContentFormat)
            decoders.put(new ContentFormat(ContentFormat.OLD_TLV_CODE), tlvDecoder);

        // deprecated json
        LwM2mNodeJsonDecoder jsonDecoder = new LwM2mNodeJsonDecoder();
        decoders.put(ContentFormat.JSON, jsonDecoder);
        if (supportDeprecatedContentFormat)
            decoders.put(new ContentFormat(ContentFormat.OLD_JSON_CODE), jsonDecoder);
        return decoders;
    }

    protected final Map<ContentFormat, NodeDecoder> decoders;

    /**
     * Create {@link DefaultLwM2mNodeDecoder} without support of old TLV and JSON code.
     */
    public DefaultLwM2mNodeDecoder() {
        this(false);
    }

    /**
     * Create {@link DefaultLwM2mNodeDecoder} allowing to enable support for old TLV and JSON code.
     * <p>
     * Those old codes was used by the LWM2M specification before the official v1.0.0 release and could still be needed
     * for backward compatibility.
     * 
     * @param supportDeprecatedContentFormat True to accept to decode old code.
     */
    public DefaultLwM2mNodeDecoder(boolean supportDeprecatedContentFormat) {
        this(getDefaultDecoders(supportDeprecatedContentFormat));
    }

    public DefaultLwM2mNodeDecoder(Map<ContentFormat, NodeDecoder> decoders) {
        this.decoders = decoders;
    }

    @Override
    public LwM2mNode decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model)
            throws CodecException {
        return decode(content, format, path, model, nodeClassFromPath(path));
    }

    @Override
    public <T extends LwM2mNode> T decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model,
            Class<T> nodeClass) throws CodecException {

        LOG.debug("Decoding value for path {} and format {}: {}", path, format, content);
        Validate.notNull(path);

        if (format == null) {
            throw new CodecException("Content format is mandatory. [%s]", path);
        }

        NodeDecoder decoder = decoders.get(format);
        if (decoder == null) {
            throw new CodecException("Content format %s is not supported [%s]", format, path);
        }
        return decoder.decode(content, path, model, nodeClass);
    }

    @Override
    public List<TimestampedLwM2mNode> decodeTimestampedData(byte[] content, ContentFormat format, LwM2mPath path,
            LwM2mModel model) throws CodecException {
        LOG.debug("Decoding value for path {} and format {}: {}", path, format, content);
        Validate.notNull(path);

        if (format == null) {
            throw new CodecException("Content format is mandatory. [%s]", path);
        }

        NodeDecoder decoder = decoders.get(format);
        if (decoder == null) {
            throw new CodecException("Content format %s is not supported [%s]", format, path);
        }

        if (decoder instanceof TimestampedNodeDecoder) {
            return ((TimestampedNodeDecoder) decoder).decodeTimestampedData(content, path, model,
                    nodeClassFromPath(path));
        } else {
            return toTimestampedNodes(decoder.decode(content, path, model, nodeClassFromPath(path)));
        }
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
        } else if (path.isResourceInstance()) {
            return LwM2mResourceInstance.class;
        }
        throw new IllegalArgumentException("invalid path level: " + path);
    }

    @Override
    public boolean isSupported(ContentFormat format) {
        return decoders.get(format) != null;
    }
}
