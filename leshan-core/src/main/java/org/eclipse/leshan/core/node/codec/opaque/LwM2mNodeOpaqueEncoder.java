/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeOpaqueEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeOpaqueEncoder.class);

    public static byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter)
            throws CodecException {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.path = path;
        internalEncoder.model = model;
        internalEncoder.converter = converter;
        node.accept(internalEncoder);
        return internalEncoder.encoded;
    }

    private static class InternalEncoder implements LwM2mNodeVisitor {

        LwM2mPath path;
        LwM2mModel model;
        LwM2mValueConverter converter;

        byte[] encoded = null;

        @Override
        public void visit(LwM2mObject object) {
            throw new CodecException("Object %s cannot be encoded in opaque format", path);
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            throw new CodecException("Object instance %s cannot be encoded in opaque format", path);
        }

        @Override
        public void visit(LwM2mResource resource) {
            if (resource.isMultiInstances()) {
                throw new CodecException("Multiple instances resource %s cannot be encoded in opaque format", path);
            }
            ResourceModel rSpec = model.getResourceModel(path.getObjectId(), resource.getId());
            if (rSpec != null && rSpec.type != Type.OPAQUE) {
                throw new CodecException("Only single opaque resource can be encoded in opaque format. [%s]", path);
            }
            LOG.trace("Encoding resource {} into text", resource);
            Object value = converter.convertValue(resource.getValue(), resource.getType(), Type.OPAQUE, path);
            encoded = (byte[]) value;
        }
    }
}
