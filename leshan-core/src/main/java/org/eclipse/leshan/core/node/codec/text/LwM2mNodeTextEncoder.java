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
package org.eclipse.leshan.core.node.codec.text;

import java.util.Date;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.Lwm2mNodeEncoderUtil;
import org.eclipse.leshan.util.Charsets;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeTextEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTextEncoder.class);

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
        // visitor inputs
        private int objectId;
        private LwM2mModel model;

        // visitor output
        private byte[] encoded = null;

        @Override
        public void visit(LwM2mObject object) {
            throw new IllegalArgumentException("Object cannot be encoded in text format");
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            throw new IllegalArgumentException("Object instance cannot be encoded in text format");
        }

        @Override
        public void visit(LwM2mResource resource) {
            if (resource.isMultiInstances()) {
                throw new IllegalArgumentException("Mulitple instances resource cannot be encoded in text format");
            }
            LOG.trace("Encoding resource {} into text", resource);

            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();
            Object val = Lwm2mNodeEncoderUtil.convertValue(resource.getValue(), resource.getType(), expectedType);

            String strValue = null;
            switch (expectedType) {
            case INTEGER:
            case FLOAT:
            case STRING:
                strValue = String.valueOf(val);
                break;
            case BOOLEAN:
                strValue = ((Boolean) val) ? "1" : "0";
                break;
            case TIME:
                // number of seconds since 1970/1/1
                strValue = String.valueOf(((Date) val).getTime() / 1000L);
                break;
            case OBJLNK:
                ObjectLink objlnk = (ObjectLink) val;
                strValue = String.valueOf(objlnk.getObjectId() + ":" + objlnk.getObjectInstanceId());
                break;
            default:
                throw new IllegalArgumentException("Cannot encode " + val + " in text format");
            }

            encoded = strValue.getBytes(Charsets.UTF_8);
        }
    }
}