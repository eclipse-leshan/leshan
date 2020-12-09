/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec.cbor;

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
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.node.codec.NodeEncoder;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.core.util.datatype.NumberUtil;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upokecenter.cbor.CBORObject;

public class LwM2mNodeCborEncoder implements NodeEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeCborEncoder.class);

    @Override
    public byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter)
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
        // visitor inputs
        private LwM2mPath path;
        private LwM2mModel model;
        private LwM2mValueConverter converter;

        // visitor output
        private byte[] encoded = null;

        @Override
        public void visit(LwM2mObject object) {
            throw new CodecException("Object %s cannot be encoded in cbor format", path);
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            throw new CodecException("Object instance %s cannot be encoded in cbor format", path);
        }

        @Override
        public void visit(LwM2mResource resource) {
            if (resource.isMultiInstances()) {
                throw new CodecException("Multiple instances resource %s cannot be encoded in cbor format", path);
            }
            LOG.trace("Encoding resource {} into cbor", resource);

            ResourceModel rSpec = model.getResourceModel(path.getObjectId(), resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();
            Object val = converter.convertValue(resource.getValue(), resource.getType(), expectedType, path);

            if (expectedType == null) {
                throw new CodecException(
                        "Unable to encode value for resource {} without type(probably a executable one)", path);
            }

            CBORObject cbor = getCborValue(expectedType, val);

            encoded = cbor.EncodeToBytes();
        }

        @Override
        public void visit(LwM2mResourceInstance instance) {
            LOG.trace("Encoding resource instance {} into cbor", instance);

            ResourceModel rSpec = model.getResourceModel(path.getObjectId(), path.getResourceId());
            Type expectedType = rSpec != null ? rSpec.type : instance.getType();
            Object val = converter.convertValue(instance.getValue(), instance.getType(), expectedType, path);

            if (expectedType == null) {
                throw new CodecException(
                        "Unable to encode value for resource {} without type(probably a executable one)", path);
            }

            CBORObject cbor = getCborValue(expectedType, val);

            encoded = cbor.EncodeToBytes();
        }

        private CBORObject getCborValue(Type expectedType, Object val) {
            CBORObject cbor;
            if (val == null) {
                return CBORObject.Null;
            }
            switch (expectedType) {
            case INTEGER:
                cbor = CBORObject.FromObject((long) val);
                break;
            case FLOAT:
                cbor = CBORObject.FromObject((double) val);
                break;
            case STRING:
                cbor = CBORObject.FromObject((String) val);
                break;
            case UNSIGNED_INTEGER:
                cbor = CBORObject.FromObject(NumberUtil.unsignedLongToEInteger(((ULong) val).longValue()));
                break;
            case BOOLEAN:
                cbor = ((Boolean) val) ? CBORObject.True : CBORObject.False;
                break;
            case TIME:
                // see https://tools.ietf.org/html/rfc7049#section-2.4.1
                // number of seconds since 1970/1/1
                long time = ((Date) val).getTime() / 1000;
                cbor = CBORObject.FromObjectAndTag(time, 1);

                // maybe a new API will be available in new version of CBOR-java :
                // https://github.com/peteroupc/CBOR-Java/issues/14#issuecomment-737114305

                // using this could works but encode the date as string sounds a bit less straightforward.
                // cbor = CBORObject.FromObject(val); // val must be a Date
                break;
            case OBJLNK:
                ObjectLink objlnk = (ObjectLink) val;
                cbor = CBORObject.FromObject(objlnk.encodeToString());
                break;
            case OPAQUE:
                cbor = CBORObject.FromObject((byte[]) val);
                break;
            default:
                throw new CodecException("Cannot encode %s in cbor format for %s", val, path);
            }
            return cbor;
        }
    }
}
