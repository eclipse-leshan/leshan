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
import org.eclipse.leshan.senml.SenMLDataPoint;
import org.eclipse.leshan.senml.SenMLJsonLabel;
import org.eclipse.leshan.senml.SenMLLabel;
import org.eclipse.leshan.senml.SensorMeasurementList;
import org.eclipse.leshan.util.Base64;
import org.eclipse.leshan.util.Hex;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class LwM2mNodeSenMLJsonEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeSenMLJsonEncoder.class);

    private static final SenMLLabel SENML_JSON_LABEL = new SenMLJsonLabel();
    private static final String EMPTY_RESOURCE_PATH = "";

    public static byte[] encodeToJson(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter) {
        SensorMeasurementList sml = new SensorMeasurementList();
        readDataFromLwM2MNode(node, path, model, converter, sml);
        return encodeToJson(sml, SENML_JSON_LABEL);
    }

    public static SensorMeasurementList readDataFromLwM2MNode(LwM2mNode node, LwM2mPath path, LwM2mModel model,
            LwM2mValueConverter converter, SensorMeasurementList sml) {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalSenMLVisitor senMLVisitor = new InternalSenMLVisitor();
        senMLVisitor.objectId = path.getObjectId();
        senMLVisitor.model = model;
        senMLVisitor.requestPath = path;
        senMLVisitor.converter = converter;
        node.accept(senMLVisitor);

        sml.setBaseName(path.toString());
        sml.setDataPoints(senMLVisitor.dataPoints);

        return sml;
    }

    private static byte[] encodeToJson(SensorMeasurementList sml, SenMLLabel label) throws CodecException {
        JsonArray jsonArray = new JsonArray();
        try {
            for (int i = 0; i < sml.getDataPoints().size(); i++) {
                JsonObject jsonObj = new JsonObject();

                if (i == 0 && sml.getBaseName() != null) {
                    jsonObj.add(label.getBaseName(), sml.getBaseName());
                }
                if (i == 0 && sml.getBaseTime() != null) {
                    jsonObj.add(label.getBaseTime(), sml.getBaseTime());
                }

                SenMLDataPoint dataPoint = sml.getDataPoints().get(i);
                if (dataPoint.getName() != null && dataPoint.getName().length() > 0) {
                    jsonObj.add(label.getName(), dataPoint.getName());
                }

                if (dataPoint.getTime() != null) {
                    jsonObj.add(label.getTime(), dataPoint.getTime());
                }

                Type type = dataPoint.getType();
                if (type != null) {
                    switch (dataPoint.getType()) {
                    case FLOAT:
                    case INTEGER:
                        jsonObj.add(label.getNumberValue(), dataPoint.getFloatValue().floatValue());
                        break;
                    case BOOLEAN:
                        jsonObj.add(label.getBooleanValue(), dataPoint.getBooleanValue());
                        break;
                    case OBJLNK:
                        jsonObj.add(label.getDataValue(), dataPoint.getObjectLinkValue());
                        break;
                    case OPAQUE:
                        jsonObj.add(label.getDataValue(), Hex.encodeHexString(dataPoint.getOpaqueValue()));
                    case STRING:
                        jsonObj.add(label.getStringValue(), dataPoint.getStringValue());
                        break;
                    case TIME:
                        jsonObj.add(label.getNumberValue(), dataPoint.getTimeValue());
                    default:
                        break;
                    }
                }

                jsonArray.add(jsonObj);
            }

            return jsonArray.toString().getBytes();

        } catch (Exception e) {
            throw new CodecException(e.getLocalizedMessage(), e);
        }
    }
    
    private static class InternalSenMLVisitor implements LwM2mNodeVisitor {
        // visitor inputs
        private int objectId;
        private LwM2mModel model;
        private LwM2mPath requestPath;
        private Long timestamp;
        private LwM2mValueConverter converter;

        // visitor output
        private ArrayList<SenMLDataPoint> dataPoints = new ArrayList<>();

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
                    lwM2mResourceToSenMLDataPoint(prefixPath, timestamp, resource);
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
                lwM2mResourceToSenMLDataPoint(prefixPath, timestamp, resource);
            }
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into SenML JSON", resource);
            if (!requestPath.isResource()) {
                throw new CodecException("Invalid request path %s for resource encoding", requestPath);
            }

            lwM2mResourceToSenMLDataPoint(EMPTY_RESOURCE_PATH, timestamp, resource);
        }

        private void lwM2mResourceToSenMLDataPoint(String resourcePath, Long timestamp, LwM2mResource resource) {
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

                    addDataPoint(resourceInstancePath, timestamp, resource, entry.getValue());
                }
            } else {
                addDataPoint(resourcePath, timestamp, resource, resource.getValue());
            }
        }

        private void addDataPoint(String resourcePath, Long timestamp, LwM2mResource resource, Object value) {
            // get type for this resource
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();

            // Create resource element
            SenMLDataPoint dataPoint = new SenMLDataPoint();
            dataPoint.setName(resourcePath);
            dataPoint.setTime(timestamp);

            // Convert value using expected type
            LwM2mPath lwM2mResourcePath = new LwM2mPath(resourcePath);
            Object convertedValue = converter.convertValue(value, resource.getType(), expectedType, lwM2mResourcePath);
            setDataPointValue(convertedValue, expectedType, lwM2mResourcePath, dataPoint);

            // Add it to the List
            dataPoints.add(dataPoint);
        }

        private void setDataPointValue(Object value, Type type, LwM2mPath resourcePath, SenMLDataPoint dataPoint) {
            LOG.trace("Encoding resource value {} in SenML JSON", value);
            switch (type) {
            case STRING:
                dataPoint.setStringValue((String) value);
                break;
            case INTEGER:
            case FLOAT:
                dataPoint.setFloatValue((Number) value);
                break;
            case BOOLEAN:
                dataPoint.setBooleanValue((Boolean) value);
                break;
            case TIME:
                dataPoint.setTimeValue(((Date) value).getTime());
                break;
            case OPAQUE:
                dataPoint.setStringValue(Base64.encodeBase64String((byte[]) value));
                break;
            default:
                throw new CodecException("Invalid value type %s for %s", type, resourcePath);
            }
        }
    }
}