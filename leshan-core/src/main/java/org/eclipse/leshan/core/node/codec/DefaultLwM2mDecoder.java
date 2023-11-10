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
import java.util.Set;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mRoot;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.cbor.LwM2mNodeCborDecoder;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonDecoder;
import org.eclipse.leshan.core.node.codec.opaque.LwM2mNodeOpaqueDecoder;
import org.eclipse.leshan.core.node.codec.senml.LwM2mNodeSenMLDecoder;
import org.eclipse.leshan.core.node.codec.senml.LwM2mPathSenMLDecoder;
import org.eclipse.leshan.core.node.codec.text.LwM2mNodeTextDecoder;
import org.eclipse.leshan.core.node.codec.tlv.LwM2mNodeTlvDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.senml.cbor.upokecenter.SenMLCborUpokecenterEncoderDecoder;
import org.eclipse.leshan.senml.json.jackson.SenMLJsonJacksonEncoderDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default {@link LwM2mDecoder}.
 * <p>
 * For {@link LwM2mNode} decoding, those formats are supported :
 * <ul>
 * <li>{@link ContentFormat#TEXT}</li>
 * <li>{@link ContentFormat#OPAQUE}</li>
 * <li>{@link ContentFormat#CBOR}</li>
 * <li>{@link ContentFormat#SENML_JSON}</li>
 * <li>{@link ContentFormat#SENML_CBOR}</li>
 * <li>{@link ContentFormat#TLV}</li>
 * <li>{@link ContentFormat#JSON} (old one from LwM2m v1.0)</li>
 * </ul>
 * <p>
 * For {@link LwM2mPath} decoding, those formats are supported :
 * <ul>
 * <li>{@link ContentFormat#SENML_JSON}</li>
 * <li>{@link ContentFormat#SENML_CBOR}</li>
 * </ul>
 */
public class DefaultLwM2mDecoder implements LwM2mDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLwM2mDecoder.class);

    public static Map<ContentFormat, NodeDecoder> getDefaultNodeDecoders(boolean supportDeprecatedContentFormat) {
        Map<ContentFormat, NodeDecoder> decoders = new HashMap<>();
        decoders.put(ContentFormat.TEXT, new LwM2mNodeTextDecoder());
        decoders.put(ContentFormat.OPAQUE, new LwM2mNodeOpaqueDecoder());
        decoders.put(ContentFormat.CBOR, new LwM2mNodeCborDecoder());
        decoders.put(ContentFormat.SENML_JSON, new LwM2mNodeSenMLDecoder(new SenMLJsonJacksonEncoderDecoder(), true));
        decoders.put(ContentFormat.SENML_CBOR,
                new LwM2mNodeSenMLDecoder(new SenMLCborUpokecenterEncoderDecoder(), false));

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

    public static Map<ContentFormat, PathDecoder> getDefaultPathDecoder() {
        Map<ContentFormat, PathDecoder> decoders = new HashMap<>();
        decoders.put(ContentFormat.SENML_JSON, new LwM2mPathSenMLDecoder(new SenMLJsonJacksonEncoderDecoder(true)));
        decoders.put(ContentFormat.SENML_CBOR,
                new LwM2mPathSenMLDecoder(new SenMLCborUpokecenterEncoderDecoder(false, true)));

        return decoders;
    }

    protected final Map<ContentFormat, NodeDecoder> nodeDecoders;
    protected final Map<ContentFormat, PathDecoder> pathDecoders;

    /**
     * Create {@link DefaultLwM2mDecoder} without support of old TLV and JSON code.
     */
    public DefaultLwM2mDecoder() {
        this(false);
    }

    /**
     * Create {@link DefaultLwM2mDecoder} allowing to enable support for old TLV and JSON code.
     * <p>
     * Those old codes was used by the LWM2M specification before the official v1.0.0 release and could still be needed
     * for backward compatibility.
     *
     * @param supportDeprecatedContentFormat True to accept to decode old code.
     */
    public DefaultLwM2mDecoder(boolean supportDeprecatedContentFormat) {
        this(getDefaultNodeDecoders(supportDeprecatedContentFormat));
    }

    public DefaultLwM2mDecoder(Map<ContentFormat, NodeDecoder> nodeDecoders) {
        this(nodeDecoders, getDefaultPathDecoder());
    }

    public DefaultLwM2mDecoder(Map<ContentFormat, NodeDecoder> nodeDecoders,
            Map<ContentFormat, PathDecoder> pathDecoders) {
        this.nodeDecoders = nodeDecoders;
        this.pathDecoders = pathDecoders;
    }

    @Override
    public LwM2mNode decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model)
            throws CodecException {
        return decode(content, format, path, model, nodeClassFromPath(path));
    }

    @Override
    public <T extends LwM2mNode> T decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model,
            Class<T> nodeClass) throws CodecException {

        LOG.trace("Decoding value for path {} and format {}: {}", path, format, content);
        Validate.notNull(path);

        if (format == null) {
            throw new CodecException("Content format is mandatory. [%s]", path);
        }

        NodeDecoder decoder = nodeDecoders.get(format);
        if (decoder == null) {
            throw new CodecException("Content format %s is not supported [%s]", format, path);
        }
        return decoder.decode(content, path, model, nodeClass);
    }

    @Override
    public Map<LwM2mPath, LwM2mNode> decodeNodes(byte[] content, ContentFormat format, List<LwM2mPath> paths,
            LwM2mModel model) throws CodecException {
        LOG.trace("Decoding value for path {} and format {}: {}", paths, format, content);
        if (paths != null)
            Validate.notEmpty(paths);

        if (format == null) {
            throw new CodecException("Content format is mandatory. [%s]", paths);
        }

        NodeDecoder decoder = nodeDecoders.get(format);
        if (decoder == null) {
            throw new CodecException("Content format %s is not supported [%s]", format, paths);
        }

        if (decoder instanceof MultiNodeDecoder) {
            return ((MultiNodeDecoder) decoder).decodeNodes(content, paths, model);
        } else {
            throw new CodecException("Decoder does not support multi node decoding for this content format %s [%s] ",
                    format, paths);
        }
    }

    @Override
    public List<TimestampedLwM2mNode> decodeTimestampedData(byte[] content, ContentFormat format, LwM2mPath path,
            LwM2mModel model) throws CodecException {
        LOG.trace("Decoding value for path {} and format {}: {}", path, format, content);
        Validate.notNull(path);

        if (format == null) {
            throw new CodecException("Content format is mandatory. [%s]", path);
        }

        NodeDecoder decoder = nodeDecoders.get(format);
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

    @Override
    public TimestampedLwM2mNodes decodeTimestampedNodes(byte[] content, ContentFormat format, LwM2mModel model)
            throws CodecException {
        LOG.trace("Decoding value for format {}: {}", format, content);

        if (format == null) {
            throw new CodecException("Content format is mandatory.");
        }

        NodeDecoder decoder = nodeDecoders.get(format);
        if (decoder == null) {
            throw new CodecException("Content format %s is not supported", format);
        }

        if (decoder instanceof TimestampedMultiNodeDecoder) {
            return ((TimestampedMultiNodeDecoder) decoder).decodeTimestampedNodes(content, model);
        } else if (decoder instanceof MultiNodeDecoder) {
            return new TimestampedLwM2mNodes.Builder()
                    .addNodes(((MultiNodeDecoder) decoder).decodeNodes(content, null, model)).build();
        } else {
            throw new CodecException(
                    "Decoder does not support multiple nodes decoding for this content format %s [%s] ", format);
        }
    }

    @Override
    public List<LwM2mPath> decodePaths(byte[] content, ContentFormat format) throws CodecException {
        LOG.trace("Decoding paths encoded with {}: {}", format, content);
        Validate.notNull(content);

        if (format == null) {
            throw new CodecException("Content format is mandatory.");
        }

        PathDecoder decoder = pathDecoders.get(format);
        if (decoder == null) {
            throw new CodecException("Content format %s is not supported for ath decoding", format);
        }
        return decoder.decode(content);
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
        } else if (path.isRoot()) {
            return LwM2mRoot.class;
        }
        throw new IllegalArgumentException("invalid path level: " + path);
    }

    @Override
    public boolean isSupported(ContentFormat format) {
        return nodeDecoders.get(format) != null;
    }

    @Override
    public Set<ContentFormat> getSupportedContentFormat() {
        return nodeDecoders.keySet();
    };
}
