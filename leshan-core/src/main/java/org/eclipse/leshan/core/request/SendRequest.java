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

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.util.Validate;

/**
 * The "Send" operation is used by the LwM2M Client to send data to the LwM2M Server without explicit request by that
 * Server.
 * <p>
 * The "Send" operation can be used by the LwM2M Client to report values for Resources and Resource Instances of LwM2M
 * Object Instance(s) to the LwM2M Server.
 */
public class SendRequest implements UplinkRequest<SendResponse> {

    private final ContentFormat format;
    private final Map<LwM2mPath, LwM2mNode> nodes;
    private final Object coapRequest;

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
        // Validate Format
        if (format == null || !(format.equals(ContentFormat.SENML_CBOR) || format.equals(ContentFormat.SENML_JSON))) {
            throw new InvalidRequestException("Content format MUST be SenML_CBOR or SenML_JSON but was " + format);
        }
        // Validate Nodes
        validateNodes(nodes);

        this.format = format;
        this.nodes = nodes;
        this.coapRequest = coapRequest;
    }

    private void validateNodes(Map<LwM2mPath, LwM2mNode> nodes) {
        Validate.notEmpty(nodes);
        for (Entry<LwM2mPath, LwM2mNode> entry : nodes.entrySet()) {
            LwM2mPath path = entry.getKey();
            LwM2mNode node = entry.getValue();
            Validate.notNull(path);
            Validate.notNull(node);

            if (path.isObject() && node instanceof LwM2mObject)
                return;
            if (path.isObjectInstance() && node instanceof LwM2mObjectInstance)
                return;
            if (path.isResource() && node instanceof LwM2mSingleResource)
                return;
            if (path.isResourceInstance() && node instanceof LwM2mResourceInstance)
                return;

            throw new InvalidRequestException("Invalid value : path (%s) should not refer to a %s value", path,
                    node.getClass().getSimpleName());
        }
    }

    @Override
    public Object getCoapRequest() {
        return coapRequest;
    }

    public Map<LwM2mPath, LwM2mNode> getNodes() {
        return nodes;
    }

    public ContentFormat getFormat() {
        return format;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("SendRequest [format=%s, nodes=%s]", format, nodes);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SendRequest other = (SendRequest) obj;
        if (format == null) {
            if (other.format != null)
                return false;
        } else if (!format.equals(other.format))
            return false;
        if (nodes == null) {
            if (other.nodes != null)
                return false;
        } else if (!nodes.equals(other.nodes))
            return false;
        return true;
    }
}
