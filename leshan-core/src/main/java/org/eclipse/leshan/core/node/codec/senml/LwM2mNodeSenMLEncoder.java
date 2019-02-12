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
import org.eclipse.leshan.senml.SenMLDataPoint;
import org.eclipse.leshan.senml.SenMLRootObject;
import org.eclipse.leshan.util.Base64;
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

    public static byte[] encodeToCbor(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter)
            throws CodecException {
        SenMLRootObject senMLRootObj = convertToSenMLObject(node, path, model, converter);

        byte[] bytes = encodeToCbor(senMLRootObj);
        System.out.println(Hex.encodeHexString(bytes));

        return encodeToCbor(senMLRootObj);
    }

    public static byte[] encodeToJson(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter) {
        SenMLRootObject senMLRootObj = convertToSenMLObject(node, path, model, converter);

        byte[] bytes = encodeToJson(senMLRootObj);
        System.out.println(new String(bytes));

        return encodeToJson(senMLRootObj);
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
        senMLRootObj.setResourceList(cborVisitor.dataPoints);
        senMLRootObj.setBaseName(path.toString());

        return senMLRootObj;
    }

    private static byte[] encodeToJson(SenMLRootObject obj) throws CodecException {
        JsonArray ja = new JsonArray();
        try {
            for (int i = 0; i < obj.getResourceList().size(); i++) {
                JsonObject jo = new JsonObject();

                if (i == 0 && obj.getBaseName() != null) {
                    jo.add("bn", obj.getBaseName());
                }
                if (i == 0 && obj.getBaseTime() != null) {
                    jo.add("bt", obj.getBaseTime());
                }

                SenMLDataPoint jae = obj.getResourceList().get(i);
                Type type = jae.getType();
                if (type != null) {
                    switch (jae.getType()) {
                    case FLOAT:
                        jo.add("v", jae.getFloatValue().floatValue());
                        break;
                    case BOOLEAN:
                        jo.add("vb", jae.getBooleanValue());
                        break;
                    case OBJLNK:
                        jo.add("vd", jae.getObjectLinkValue());
                        break;
                    case OPAQUE:
                        jo.add("vd", "TODO");
                    case STRING:
                        jo.add("vs", jae.getStringValue());
                        break;
                    case TIME:
                        jo.add("t", jae.getTime());
                    default:
                        break;
                    }
                }

                ja.add(jo);
            }

        } catch (Exception e) {
            throw new CodecException(e.getLocalizedMessage(), e);
        }

        System.out.println(ja.toString());

        return ja.toString().getBytes();
    }

    private static byte[] encodeToCbor(SenMLRootObject obj) throws CodecException {
        CBORFactory factory = new CBORFactory();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            CBORGenerator generator = factory.createGenerator(out);
            generator.writeStartArray();

            for (int i = 0; i < obj.getResourceList().size(); i++) {
                generator.writeStartObject();
                if (i == 0 && obj.getBaseName() != null) {
                    generator.writeStringField("-2", obj.getBaseName());
                }

                if (i == 0 && obj.getBaseTime() != null) {
                    generator.writeNumberField("-3", obj.getBaseTime());
                }

                SenMLDataPoint jae = obj.getResourceList().get(i);

                if (jae.getName() != null && jae.getName().length() > 0) {
                    generator.writeStringField("0", jae.getName());
                }

                Type type = jae.getType();
                if (type != null) {
                    switch (jae.getType()) {
                    case FLOAT:
                        generator.writeNumberField("2", jae.getFloatValue().doubleValue());
                        break;
                    case BOOLEAN:
                        generator.writeBooleanField("4", jae.getBooleanValue());
                        break;
                    case OBJLNK:
                        generator.writeStringField("8", jae.getObjectLinkValue());
                        break;
                    case OPAQUE:
                        generator.writeStringField("8", "TODO");
                    case STRING:
                        generator.writeStringField("3", jae.getStringValue());
                        break;
                    case TIME:
                        generator.writeNumberField("6", jae.getTime());
                    default:
                        break;
                    }
                }
                generator.writeEndObject();
            }

            generator.writeEndArray();
            generator.close();

        } catch (Exception e) {
            throw new CodecException(e.getLocalizedMessage(), e);
        }

        return out.toByteArray();
    }

    private static class InternalSenMLVisitor implements LwM2mNodeVisitor {
        // visitor inputs
        private int objectId;
        private LwM2mModel model;
        private LwM2mPath requestPath;
        private Long timestamp;
        private LwM2mValueConverter converter;

        // visitor output
        private ArrayList<SenMLDataPoint> dataPoints = null;

        @Override
        public void visit(LwM2mObject object) {
            LOG.trace("Encoding Object {} into CBOR", object);
            // Validate request path
            if (!requestPath.isObject()) {
                throw new CodecException("Invalid request path %s for CBOR object encoding", requestPath);
            }

            // Create resources
            dataPoints = new ArrayList<>();
            for (LwM2mObjectInstance instance : object.getInstances().values()) {
                for (LwM2mResource resource : instance.getResources().values()) {
                    String prefixPath = Integer.toString(instance.getId()) + "/" + Integer.toString(resource.getId());
                    dataPoints.addAll(lwM2mResourceToSenMLDataPoints(prefixPath, timestamp, resource));
                }
            }
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            LOG.trace("Encoding object instance {} into CBOR", instance);
            dataPoints = new ArrayList<>();
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
                dataPoints.addAll(lwM2mResourceToSenMLDataPoints(prefixPath, timestamp, resource));
            }
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into CBOR", resource);
            if (!requestPath.isResource()) {
                throw new CodecException("Invalid request path %s for CBOR resource encoding", requestPath);
            }

            dataPoints = lwM2mResourceToSenMLDataPoints("", timestamp, resource);
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
                resource.setTime(((Date) value).getTime());
                break;
            case OPAQUE:
                resource.setStringValue(Base64.encodeBase64String((byte[]) value));
                break;
            default:
                throw new CodecException("Invalid value type %s for %s", type, resourcePath);
            }
        }
    }
}
