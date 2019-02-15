package org.eclipse.leshan.core.node.codec.senml;

import java.io.ByteArrayOutputStream;
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
import org.eclipse.leshan.senml.SenMLCborLabel;
import org.eclipse.leshan.senml.SenMLDataPoint;
import org.eclipse.leshan.senml.SenMLJsonLabel;
import org.eclipse.leshan.senml.SenMLLabel;
import org.eclipse.leshan.senml.SenMLRootObject;
import org.eclipse.leshan.util.Hex;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

public class LwM2mNodeSenMLEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeSenMLEncoder.class);

    private static final SenMLLabel SENML_CBOR_LABEL = new SenMLCborLabel();
    private static final SenMLLabel SENML_JSON_LABEL = new SenMLJsonLabel();

    public static byte[] encodeToCbor(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter)
            throws CodecException {
        SenMLRootObject senMLRootObj = convertToSenMLObject(node, path, model, converter);

        byte[] bytes = encodeToCbor(senMLRootObj, SENML_CBOR_LABEL);
        System.out.println(Hex.encodeHexString(bytes));

        return encodeToCbor(senMLRootObj, SENML_CBOR_LABEL);
    }

    public static byte[] encodeToJson(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter) {
        SenMLRootObject senMLRootObj = convertToSenMLObject(node, path, model, converter);

        byte[] bytes = encodeToJson(senMLRootObj, SENML_JSON_LABEL);
        System.out.println(new String(bytes));

        return encodeToJson(senMLRootObj, SENML_JSON_LABEL);
    }

    public static SenMLRootObject convertToSenMLObject(LwM2mNode node, LwM2mPath path, LwM2mModel model,
            LwM2mValueConverter converter) {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalSenMLVisitor cborVisitor = new InternalSenMLVisitor();
        cborVisitor.objectId = path.getObjectId();
        cborVisitor.model = model;
        cborVisitor.requestPath = path;
        cborVisitor.converter = converter;
        node.accept(cborVisitor);

        SenMLRootObject senMLRootObj = new SenMLRootObject();
        senMLRootObj.setDataPoints(cborVisitor.resourceList);
        senMLRootObj.setBaseName(path.toString());

        return senMLRootObj;
    }

    private static byte[] encodeToJson(SenMLRootObject obj, SenMLLabel label) throws CodecException {
        JsonArray ja = new JsonArray();
        try {
            for (int i = 0; i < obj.getDataPoints().size(); i++) {
                JsonObject jo = new JsonObject();

                if (i == 0 && obj.getBaseName() != null) {
                    jo.add(label.getBaseName(), obj.getBaseName());
                }
                if (i == 0 && obj.getBaseTime() != null) {
                    jo.add(label.getBaseTime(), obj.getBaseTime());
                }

                SenMLDataPoint jae = obj.getDataPoints().get(i);
                if (jae.getName() != null) {
                    jo.add(label.getName(), jae.getName());
                }

                if (jae.getTime() != null) {
                    jo.add(label.getTime(), jae.getTime());
                }

                Type type = jae.getType();
                if (type != null) {
                    switch (jae.getType()) {
                    case FLOAT:
                    case INTEGER:
                        jo.add(label.getNumberValue(), jae.getFloatValue().floatValue());
                        break;
                    case BOOLEAN:
                        jo.add(label.getBooleanValue(), jae.getBooleanValue());
                        break;
                    case OBJLNK:
                        jo.add(label.getDataValue(), jae.getObjectLinkValue());
                        break;
                    case OPAQUE:
                        jo.add(label.getDataValue(), Hex.encodeHexString(jae.getOpaqueValue()));
                    case STRING:
                        jo.add(label.getStringValue(), jae.getStringValue());
                        break;
                    case TIME:
                        jo.add(label.getNumberValue(), jae.getTimeValue());
                    default:
                        break;
                    }
                }

                ja.add(jo);
            }

            return ja.toString().getBytes();

        } catch (Exception e) {
            throw new CodecException(e.getLocalizedMessage(), e);
        }
    }

    private static byte[] encodeToCbor(SenMLRootObject obj, SenMLLabel label) throws CodecException {
        CBORFactory factory = new CBORFactory();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            CBORGenerator generator = factory.createGenerator(out);
            generator.writeStartArray();

            for (int i = 0; i < obj.getDataPoints().size(); i++) {
                generator.writeStartObject();
                if (i == 0 && obj.getBaseName() != null) {
                    generator.writeStringField(label.getBaseName(), obj.getBaseName());
                }

                if (i == 0 && obj.getBaseTime() != null) {
                    generator.writeNumberField(label.getBaseTime(), obj.getBaseTime());
                }

                SenMLDataPoint jae = obj.getDataPoints().get(i);

                if (jae.getName() != null && jae.getName().length() > 0) {
                    generator.writeStringField(label.getName(), jae.getName());
                }

                if (jae.getTime() != null) {
                    generator.writeNumberField(label.getTime(), jae.getTime());
                }

                Type type = jae.getType();
                if (type != null) {
                    switch (jae.getType()) {
                    case FLOAT:
                    case INTEGER:
                        generator.writeNumberField(label.getNumberValue(), jae.getFloatValue().doubleValue());
                        break;
                    case BOOLEAN:
                        generator.writeBooleanField(label.getBooleanValue(), jae.getBooleanValue());
                        break;
                    case OBJLNK:
                        generator.writeStringField(label.getStringValue(), jae.getObjectLinkValue());
                        break;
                    case OPAQUE:
                        generator.writeStringField(label.getDataValue(), Hex.encodeHexString(jae.getOpaqueValue()));
                    case STRING:
                        generator.writeStringField(label.getStringValue(), jae.getStringValue());
                        break;
                    case TIME:
                        generator.writeNumberField(label.getNumberValue(), jae.getTimeValue());
                    default:
                        break;
                    }
                }
                generator.writeEndObject();
            }

            generator.writeEndArray();
            generator.close();

            return out.toByteArray();
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
        private ArrayList<SenMLDataPoint> resourceList = null;

        @Override
        public void visit(LwM2mObject object) {
            LOG.trace("Encoding Object {} into CBOR", object);
            // Validate request path
            if (!requestPath.isObject()) {
                throw new CodecException("Invalid request path %s for CBOR object encoding", requestPath);
            }

            // Create resources
            resourceList = new ArrayList<>();
            for (LwM2mObjectInstance instance : object.getInstances().values()) {
                for (LwM2mResource resource : instance.getResources().values()) {
                    String prefixPath = Integer.toString(instance.getId()) + "/" + Integer.toString(resource.getId());
                    resourceList.addAll(lwM2mResourceToSenMLDataPoints(prefixPath, timestamp, resource));
                }
            }
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            LOG.trace("Encoding object instance {} into CBOR", instance);
            resourceList = new ArrayList<>();
            for (LwM2mResource resource : instance.getResources().values()) {
                // Validate request path & compute resource path
                String prefixPath;
                if (requestPath.isObject()) {
                    prefixPath = instance.getId() + "/" + resource.getId();
                } else if (requestPath.isObjectInstance()) {
                    prefixPath = Integer.toString(resource.getId());
                } else {
                    throw new CodecException("Invalid request path %s for JSON instance encoding", requestPath);
                }
                // Create resources
                resourceList.addAll(lwM2mResourceToSenMLDataPoints(prefixPath, timestamp, resource));
            }
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into CBOR", resource);
            if (!requestPath.isResource()) {
                throw new CodecException("Invalid request path %s for CBOR resource encoding", requestPath);
            }

            resourceList = lwM2mResourceToSenMLDataPoints("", timestamp, resource);
        }

        private ArrayList<SenMLDataPoint> lwM2mResourceToSenMLDataPoints(String resourcePath, Long timestamp,
                LwM2mResource resource) {
            // get type for this resource
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();
            ArrayList<SenMLDataPoint> resourcesList = new ArrayList<>();

            // create JSON resource element
            if (resource.isMultiInstances()) {
                for (Entry<Integer, ?> entry : resource.getValues().entrySet()) {
                    // compute resource instance path
                    String resourceInstancePath;
                    if (resourcePath == null || resourcePath.isEmpty()) {
                        resourceInstancePath = Integer.toString(entry.getKey());
                    } else {
                        resourceInstancePath = resourcePath + "/" + entry.getKey();
                    }

                    // Create resource element
                    SenMLDataPoint dataPoint = new SenMLDataPoint();
                    dataPoint.setName(resourceInstancePath);
                    dataPoint.setTime(timestamp);

                    // Convert value using expected type
                    LwM2mPath lwM2mResourceInstancePath = new LwM2mPath(resourceInstancePath);
                    Object dataPointValue = converter.convertValue(entry.getValue(), resource.getType(), expectedType,
                            lwM2mResourceInstancePath);
                    this.setResourceValue(dataPointValue, expectedType, dataPoint, lwM2mResourceInstancePath);

                    // Add it to the List
                    resourcesList.add(dataPoint);
                }
            } else {
                // Create resource element
                SenMLDataPoint dataPoint = new SenMLDataPoint();
                dataPoint.setName(resourcePath);
                dataPoint.setTime(timestamp);

                // Convert value using expected type
                LwM2mPath lwM2mResourcePath = new LwM2mPath(resourcePath);
                Object dataPointValue = converter.convertValue(resource.getValue(), resource.getType(), expectedType,
                        lwM2mResourcePath);
                this.setResourceValue(dataPointValue, expectedType, dataPoint, lwM2mResourcePath);

                // Add it to the List
                resourcesList.add(dataPoint);
            }
            return resourcesList;
        }

        private void setResourceValue(Object value, Type type, SenMLDataPoint resource, LwM2mPath resourcePath) {
            LOG.trace("Encoding value {} in CBOR", value);
            switch (type) {
            case STRING:
                resource.setStringValue((String) value);
                break;
            case INTEGER:
            case FLOAT:
                resource.setFloatValue((Number) value);
                break;
            case BOOLEAN:
                resource.setBooleanValue((Boolean) value);
                break;
            case TIME:
                resource.setTimeValue(((Date) value).getTime());
                break;
            case OPAQUE:
                resource.setStringValue(Hex.encodeHexString((byte[]) value));
                break;
            case OBJLNK:
                resource.setStringValue(value.toString());
            default:
                throw new CodecException("Invalid value type %s for %s", type, resourcePath);
            }
        }
    }
}
