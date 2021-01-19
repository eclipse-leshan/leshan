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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.core.util.datatype.ULong;

/**
 * The "Write-Composite" operation can be used by the LwM2M Server to update values of a number of different Resources
 * across different Instances of one or more Objects. Similar to Read-Composite the Write-Composite operation provides a
 * list of all resources to be updated, and their new values, using the SenML JSON/CBOR format. Unlike for Write
 * operation, the Resources that are not provided are not impacted by the operation.
 * <p>
 * The "Write-Composite" operation is atomic and cannot have partial success.
 */
public class WriteCompositeRequest extends AbstractLwM2mRequest<WriteCompositeResponse>
        implements CompositeDownlinkRequest<WriteCompositeResponse> {

    private final ContentFormat contentFormat;
    private final Map<LwM2mPath, LwM2mNode> nodes;

    /**
     * Create WriteComposite Request.
     * <p>
     * 
     * @param values The values to write as a the Map of string path to value. Value can not be <code>null</code>. And
     *        must be {@link Boolean} or {@link String} or a byte array or {@link Date} or {@link Long} or
     *        {@link Double} or {@link ULong} or {@link ObjectLink}.
     * @param contentFormat The {@link ContentFormat} used to encode this value
     * 
     */
    public WriteCompositeRequest(ContentFormat contentFormat, Map<String, Object> values) {
        super(null);
        HashMap<LwM2mPath, LwM2mNode> internalNodes = new HashMap<>();
        for (Entry<String, Object> entry : values.entrySet()) {
            LwM2mPath path = new LwM2mPath(entry.getKey());
            if (path.isResource()) {
                internalNodes.put(path, LwM2mSingleResource.newResource(path.getResourceId(), entry.getValue()));
            } else if (path.isResourceInstance()) {
                internalNodes.put(path, LwM2mResourceInstance.newInstance(path.getResourceId(), entry.getValue()));
            } else {
                new InvalidRequestException("Invalid value : path (%s) should target a resource or resource instance");
            }
        }
        this.nodes = internalNodes;
        this.contentFormat = contentFormat;
    }

    /**
     * Create WriteComposite Request.
     * <p>
     * This constructor is more for internal usage.
     * 
     * @param nodes The {@link LwM2mNode} to write as a the Map of {@link LwM2mPath} to {@link LwM2mNode}. Value can not
     *        be <code>null</code>. LwM2mNode MUST be {@link LwM2mSingleResource} or {@link LwM2mResourceInstance}.
     * @param contentFormat The {@link ContentFormat} used to encode the Map of {@link LwM2mNode}
     * @param coapRequest the underlying request.
     * 
     */
    public WriteCompositeRequest(ContentFormat contentFormat, Map<LwM2mPath, LwM2mNode> nodes, Object coapRequest) {
        super(coapRequest);

        validateNodes(nodes);
        this.contentFormat = contentFormat;
        this.nodes = nodes;
    }

    private void validateNodes(Map<LwM2mPath, LwM2mNode> nodes) {
        Validate.notEmpty(nodes);
        for (Entry<LwM2mPath, LwM2mNode> entry : nodes.entrySet()) {
            LwM2mPath path = entry.getKey();
            LwM2mNode node = entry.getValue();
            Validate.notNull(path);
            Validate.notNull(node);

            if (path.isResource() && node instanceof LwM2mSingleResource)
                return;
            if (path.isResourceInstance() && node instanceof LwM2mResourceInstance)
                return;

            throw new InvalidRequestException("Invalid value : path (%s) should not refer to a %s value", path,
                    node.getClass().getSimpleName());
        }
    }

    public Map<LwM2mPath, LwM2mNode> getNodes() {
        return nodes;
    }

    @Override
    public List<LwM2mPath> getPaths() {
        return new ArrayList<>(nodes.keySet());
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }
}
