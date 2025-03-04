/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeUtil;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.SendResponse;

/**
 * The "Send" operation is used by the LwM2M Client to send data to the LwM2M Server without explicit request by that
 * Server.
 * <p>
 * The "Send" operation can be used by the LwM2M Client to report values for Resources and Resource Instances of LwM2M
 * Object Instance(s) to the LwM2M Server.
 */
public class SendRequest extends AbstractLwM2mRequest<SendResponse>
        implements UplinkDeviceManagementRequest<SendResponse> {

    private final ContentFormat format;
    private final TimestampedLwM2mNodes timestampedNodes;

    private static final TimestampedLwM2mNodes mapToTimestampedNodes(Map<LwM2mPath, LwM2mNode> nodes) {
        try {
            return TimestampedLwM2mNodes.builder().addNodes(nodes).build();
        } catch (Exception e) {
            throw new InvalidRequestException(e, "Invalid nodes");
        }
    }

    /**
     * @param format {@link ContentFormat} used to encode data. It MUST be {@link ContentFormat#SENML_CBOR} or
     *        {@link ContentFormat#SENML_JSON}
     * @param nodes The {@link LwM2mNode} to write as a the Map of string path to {@link LwM2mNode}. Value can not be
     *        <code>null</code>.
     */
    public SendRequest(ContentFormat format, Map<LwM2mPath, LwM2mNode> nodes) {
        this(format, nodes, null);
    }

    public SendRequest(ContentFormat format, Map<LwM2mPath, LwM2mNode> nodes, Object coapRequest) {
        this(format, mapToTimestampedNodes(nodes), coapRequest);
    }

    public SendRequest(ContentFormat format, TimestampedLwM2mNodes timestampedNodes, Object coapRequest) {
        super(coapRequest);
        this.timestampedNodes = timestampedNodes;
        // Validate Format
        if (format == null || !(format.equals(ContentFormat.SENML_CBOR) || format.equals(ContentFormat.SENML_JSON))) {
            throw new InvalidRequestException("Content format MUST be SenML_CBOR or SenML_JSON but was " + format);
        }
        // Validate Nodes
        validateNodes(timestampedNodes);

        this.format = format;
    }

    private void validateNodes(TimestampedLwM2mNodes timestampedNodes) {
        if (timestampedNodes == null || timestampedNodes.isEmpty() || timestampedNodes.getFlattenNodes().isEmpty()) {
            throw new InvalidRequestException(
                    "SendRequest MUST NOT have empty payload (at least 1 node should be present)");
        }

        for (Instant t : timestampedNodes.getTimestamps()) {
            for (Entry<LwM2mPath, LwM2mNode> entry : timestampedNodes.getNodesAt(t).entrySet()) {
                LwM2mPath path = entry.getKey();
                LwM2mNode node = entry.getValue();
                if (path == null) {
                    throw new InvalidRequestException("Invalid key for entry (null, %s) : path MUST NOT be null", node);
                }
                if (node == null) {
                    throw new InvalidRequestException("Invalid value for entry (%s, null) :  node MUST NOT be null ",
                            path);
                }

                // check node are supported by this operation
                String unsupportedNodeCause = LwM2mNodeUtil.getUnsupportedNodeCause(node,
                        Arrays.asList(LwM2mObject.class, LwM2mObjectInstance.class, LwM2mSingleResource.class,
                                LwM2mResourceInstance.class));
                if (unsupportedNodeCause != null) {
                    throw new InvalidRequestException(unsupportedNodeCause);
                }

                // check path match node
                String invalidPath = LwM2mNodeUtil.getInvalidPathForNodeCause(node, path);
                if (invalidPath != null) {
                    throw new InvalidRequestException(invalidPath);
                }
            }
        }
    }

    public TimestampedLwM2mNodes getTimestampedNodes() {
        return timestampedNodes;
    }

    public ContentFormat getFormat() {
        return format;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(UplinkDeviceManagementRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("SendRequest [format=%s, timestampedNodes=%s]", format, timestampedNodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SendRequest))
            return false;
        SendRequest that = (SendRequest) o;
        return that.canEqual(this) && Objects.equals(format, that.format)
                && Objects.equals(timestampedNodes, that.timestampedNodes);
    }

    public boolean canEqual(Object o) {
        return (o instanceof SendRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, timestampedNodes);
    }
}
