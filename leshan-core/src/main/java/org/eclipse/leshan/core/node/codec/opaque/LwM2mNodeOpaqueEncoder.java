/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec.opaque;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.codec.Lwm2mNodeEncoderUtil;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeOpaqueEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeOpaqueEncoder.class);

    public static byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model) {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.objectId = path.getObjectId();
        internalEncoder.model = model;
        node.accept(internalEncoder);
        return internalEncoder.encoded;
    }

    private static class InternalEncoder implements LwM2mNodeVisitor {

        int objectId;
        LwM2mModel model;

        byte[] encoded = null;

        @Override
        public void visit(LwM2mObject object) {
            throw new IllegalArgumentException("Object cannot be encoded in opaque format");
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            throw new IllegalArgumentException("Object instance cannot be encoded in opaque format");
        }

        @Override
        public void visit(LwM2mResource resource) {
            if (resource.isMultiInstances()) {
                throw new IllegalArgumentException("Mulitple instances resource cannot be encoded in opaque format");
            }
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            if (rSpec != null && rSpec.type != Type.OPAQUE) {
                throw new IllegalArgumentException("Only single opaque resource can be encoded in opaque format");
            }
            LOG.trace("Encoding resource {} into text", resource);
            Object value = Lwm2mNodeEncoderUtil.convertValue(resource.getValue(), resource.getType(), Type.OPAQUE);
            encoded = (byte[]) value;
        }
    }
}
