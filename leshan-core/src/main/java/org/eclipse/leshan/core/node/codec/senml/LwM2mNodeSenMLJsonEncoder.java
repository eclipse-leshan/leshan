/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec.senml;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;

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
import org.eclipse.leshan.senml.SenMLJson;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.eclipse.leshan.util.Base64;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeSenMLJsonEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeSenMLJsonEncoder.class);

    private static final String EMPTY_RESOURCE_PATH = "";

    public static byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter) {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.objectId = path.getObjectId();
        internalEncoder.model = model;
        internalEncoder.requestPath = path;
        internalEncoder.converter = converter;
        node.accept(internalEncoder);

        SenMLPack pack = new SenMLPack();
        pack.setRecords(internalEncoder.records);
        return SenMLJson.toJsonSenML(pack).getBytes();
    }

    private static class InternalEncoder implements LwM2mNodeVisitor {
        // visitor inputs
        private int objectId;
        private LwM2mModel model;
        private LwM2mPath requestPath;
        private Long timestamp;
        private LwM2mValueConverter converter;

        // visitor output
        private ArrayList<SenMLRecord> records = new ArrayList<>();

        @Override
        public void visit(LwM2mObject object) {
            LOG.trace("Encoding Object {} into SenML JSON", object);
            // Validate request path
            if (!requestPath.isObject()) {
                throw new CodecException("Invalid request path %s for object encoding", requestPath);
            }

            // Create resources
            for (LwM2mObjectInstance instance : object.getInstances().values()) {
                for (LwM2mResource resource : instance.getResources().values()) {
                    String prefixPath = Integer.toString(instance.getId()) + "/" + Integer.toString(resource.getId());
                    lwM2mResourceToSenMLRecord(prefixPath, timestamp, resource);
                }
            }
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            LOG.trace("Encoding object instance {} into SenML JSON", instance);
            for (LwM2mResource resource : instance.getResources().values()) {
                // Validate request path & compute resource path
                String prefixPath;
                if (requestPath.isObject()) {
                    prefixPath = instance.getId() + "/" + resource.getId();
                } else if (requestPath.isObjectInstance()) {
                    prefixPath = Integer.toString(resource.getId());
                } else {
                    throw new CodecException("Invalid request path %s for instance encoding", requestPath);
                }
                // Create resources
                lwM2mResourceToSenMLRecord(prefixPath, timestamp, resource);
            }
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into SenML JSON", resource);
            if (!requestPath.isResource()) {
                throw new CodecException("Invalid request path %s for resource encoding", requestPath);
            }

            lwM2mResourceToSenMLRecord(EMPTY_RESOURCE_PATH, timestamp, resource);
        }

        private void lwM2mResourceToSenMLRecord(String resourcePath, Long timestamp, LwM2mResource resource) {
            // create resource element
            if (resource.isMultiInstances()) {
                for (Entry<Integer, ?> entry : resource.getValues().entrySet()) {
                    // compute resource instance path
                    String resourceInstancePath;
                    if (resourcePath == null || resourcePath.isEmpty()) {
                        resourceInstancePath = Integer.toString(entry.getKey());
                    } else {
                        resourceInstancePath = resourcePath + "/" + entry.getKey();
                    }

                    addSenMLRecord(resourceInstancePath, timestamp, resource, entry.getValue());
                }
            } else {
                addSenMLRecord(resourcePath, timestamp, resource, resource.getValue());
            }
        }

        private void addSenMLRecord(String resourcePath, Long timestamp, LwM2mResource resource, Object value) {
            // get type for this resource
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();

            // Create resource element
            SenMLRecord record = new SenMLRecord();
            
            if (records.isEmpty() && requestPath.isObjectInstance()) {
                record.setBaseName(requestPath.toString());
            }
            
            if(requestPath.isResource()) {
                record.setBaseName(requestPath.toString());
            }
            else if(requestPath.isObjectInstance()) {
                record.setName(resourcePath);
                record.setTime(timestamp);
            }

            // Convert value using expected type
            LwM2mPath lwM2mResourcePath = new LwM2mPath(resourcePath);
            Object convertedValue = converter.convertValue(value, resource.getType(), expectedType, lwM2mResourcePath);
            setResourceValue(convertedValue, expectedType, lwM2mResourcePath, record);

            // Add it to the List
            records.add(record);
        }

        private void setResourceValue(Object value, Type type, LwM2mPath resourcePath, SenMLRecord record) {
            LOG.trace("Encoding resource value {} in SenML JSON", value);
            switch (type) {
            case STRING:
                record.setStringValue((String) value);
                break;
            case INTEGER:
            case FLOAT:
                record.setFloatValue((Number) value);
                break;
            case BOOLEAN:
                record.setBooleanValue((Boolean) value);
                break;
            case TIME:
                record.setFloatValue((((Date) value).getTime() / 1000L));
                break;
            case OPAQUE:
                record.setStringValue(Base64.encodeBase64String((byte[]) value));
                break;
            case OBJLNK:
                record.setStringValue(value.toString());
            default:
                throw new CodecException("Invalid value type %s for %s", type, resourcePath);
            }
        }
    }
}