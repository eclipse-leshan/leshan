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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.cbor.LwM2mNodeCborEncoder;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonEncoder;
import org.eclipse.leshan.core.node.codec.opaque.LwM2mNodeOpaqueEncoder;
import org.eclipse.leshan.core.node.codec.senml.LwM2mNodeSenMLEncoder;
import org.eclipse.leshan.core.node.codec.text.LwM2mNodeTextEncoder;
import org.eclipse.leshan.core.node.codec.tlv.LwM2mNodeTlvEncoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.senml.cbor.upokecenter.SenMLCborUpokecenterEncoderDecoder;
import org.eclipse.leshan.senml.json.jackson.SenMLJsonJacksonEncoderDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default {@link LwM2mNodeEncoder} which support default {@link ContentFormat} :
 * <ul>
 * <li>{@link ContentFormat#TLV}</li>
 * <li>{@link ContentFormat#JSON}</li>
 * <li>{@link ContentFormat#TEXT}</li>
 * <li>{@link ContentFormat#OPAQUE}</li>
 * </ul>
 */
public class DefaultLwM2mNodeEncoder implements LwM2mNodeEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLwM2mNodeEncoder.class);

    public static Map<ContentFormat, NodeEncoder> getDefaultEncoders(boolean supportDeprecatedContentFormat) {
        Map<ContentFormat, NodeEncoder> encoders = new HashMap<>();
        encoders.put(ContentFormat.TEXT, new LwM2mNodeTextEncoder());
        encoders.put(ContentFormat.OPAQUE, new LwM2mNodeOpaqueEncoder());
        encoders.put(ContentFormat.CBOR, new LwM2mNodeCborEncoder());
        encoders.put(ContentFormat.SENML_JSON, new LwM2mNodeSenMLEncoder(new SenMLJsonJacksonEncoderDecoder()));
        encoders.put(ContentFormat.SENML_CBOR, new LwM2mNodeSenMLEncoder(new SenMLCborUpokecenterEncoderDecoder()));

        // tlv
        LwM2mNodeTlvEncoder tlvDecoder = new LwM2mNodeTlvEncoder();
        encoders.put(ContentFormat.TLV, tlvDecoder);
        if (supportDeprecatedContentFormat)
            encoders.put(new ContentFormat(ContentFormat.OLD_TLV_CODE), tlvDecoder);

        // deprecated json
        LwM2mNodeJsonEncoder jsonDecoder = new LwM2mNodeJsonEncoder();
        encoders.put(ContentFormat.JSON, jsonDecoder);
        if (supportDeprecatedContentFormat)
            encoders.put(new ContentFormat(ContentFormat.OLD_JSON_CODE), jsonDecoder);
        return encoders;
    }

    protected final LwM2mValueConverter converter;
    protected final Map<ContentFormat, NodeEncoder> encoders;

    /**
     * Create {@link DefaultLwM2mNodeEncoder} without support of old TLV and JSON code.
     */
    public DefaultLwM2mNodeEncoder() {
        this(new LwM2mValueChecker());
    }

    public DefaultLwM2mNodeEncoder(LwM2mValueConverter converter) {
        this(converter, false);
    }

    /**
     * Create {@link DefaultLwM2mNodeEncoder} allowing to enable support for old TLV and JSON code.
     * <p>
     * Those old codes was used by the LWM2M specification before the official v1.0.0 release and could still be needed
     * for backward compatibility.
     * 
     * @param supportDeprecatedContentFormat True to accept to encode old code.
     */
    public DefaultLwM2mNodeEncoder(boolean supportDeprecatedContentFormat) {
        this(new LwM2mValueChecker(), supportDeprecatedContentFormat);
    }

    public DefaultLwM2mNodeEncoder(LwM2mValueConverter converter, boolean supportDeprecatedContentFormat) {
        this(getDefaultEncoders(supportDeprecatedContentFormat), converter);
    }

    public DefaultLwM2mNodeEncoder(Map<ContentFormat, NodeEncoder> encoders, LwM2mValueConverter converter) {
        this.encoders = encoders;
        this.converter = converter;
    }

    @Override
    public byte[] encode(LwM2mNode node, ContentFormat format, LwM2mPath path, LwM2mModel model) throws CodecException {
        Validate.notNull(node);

        if (format == null) {
            throw new CodecException("Content format is mandatory. [%s]", path);
        }

        NodeEncoder encoder = encoders.get(format);
        if (encoder == null) {
            throw new CodecException("Content format %s is not supported [%s]", format, path);
        }
        LOG.trace("Encoding node {} for path {} and format {}", node, path, format);
        byte[] encoded = encoder.encode(node, path, model, converter);
        LOG.trace("Encoded node {}: {}", node, encoded);
        return encoded;
    }

    @Override
    public byte[] encodeTimestampedData(List<TimestampedLwM2mNode> timestampedNodes, ContentFormat format,
            LwM2mPath path, LwM2mModel model) throws CodecException {
        Validate.notEmpty(timestampedNodes);
        if (format == null) {
            throw new CodecException("Content format is mandatory. [%s]", path);
        }

        NodeEncoder encoder = encoders.get(format);
        if (encoder == null) {
            throw new CodecException("Content format %s is not supported [%s]", format, path);
        }
        if (!(encoder instanceof TimestampedNodeEncoder)) {
            throw new CodecException("Cannot encode timestampedNode with format %s. [%s]", format, path);
        }
        LOG.trace("Encoding time-stamped nodes for path {} and format {}", timestampedNodes, path, format);
        byte[] encoded = ((TimestampedNodeEncoder) encoder).encodeTimestampedData(timestampedNodes, path, model,
                converter);
        LOG.trace("Encoded node timestampedNode: {}", timestampedNodes, encoded);
        return encoded;

    }

    @Override
    public boolean isSupported(ContentFormat format) {
        return encoders.get(format) != null;
    }
}
