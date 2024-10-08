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
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mRoot;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.node.codec.NodeEncoder;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeOpaqueEncoder implements NodeEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeOpaqueEncoder.class);

    @Override
    public byte[] encode(LwM2mNode node, String rootPath, LwM2mPath path, LwM2mModel model,
            LwM2mValueConverter converter) throws CodecException {
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
        public void visit(LwM2mRoot root) {
            throw new CodecException("LWM2M Root Node cannot be encoded in opaque format");
        }

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
            LOG.trace("Encoding resource {} into Bytes(OPAQUE format)", resource);
            Object value = converter.convertValue(resource.getValue(), resource.getType(), Type.OPAQUE, path);
            encoded = (byte[]) value;
        }

        @Override
        public void visit(LwM2mResourceInstance instance) {
            ResourceModel rSpec = model.getResourceModel(path.getObjectId(), path.getResourceId());
            if (rSpec != null && rSpec.type != Type.OPAQUE) {
                throw new CodecException("Only opaque resource instance can be encoded in opaque format. [%s]", path);
            }
            LOG.trace("Encoding resource instance {} into Bytes(OPAQUE format)", instance);
            Object value = converter.convertValue(instance.getValue(), instance.getType(), Type.OPAQUE, path);
            encoded = (byte[]) value;
        }
    }
}
