/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.json.JsonArrayElement;
import org.eclipse.leshan.json.LwM2mJson;
import org.eclipse.leshan.json.LwM2mJsonException;
import org.eclipse.leshan.json.LwM2mJsonObject;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.eclipse.leshan.tlv.TlvDecoder;
import org.eclipse.leshan.tlv.TlvException;
import org.eclipse.leshan.util.Charsets;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeDecoder.class);

    /**
     * Deserializes a binary content into a {@link LwM2mNode}.
     *
     * @param content the content
     * @param format the content format
     * @param path the path of the node to build
     * @param model the collection of supported object models
     * @return the resulting node
     * @throws InvalidValueException
     */
    public static LwM2mNode decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model)
            throws InvalidValueException {
        LOG.debug("Decoding value for path {} and format {}: {}", path, format, content);

        Validate.notNull(path);

        // default to plain/text
        if (format == null) {
            if (path.isResource()) {
                ResourceModel rDesc = model.getResourceModel(path.getObjectId(), path.getResourceId());
                if (rDesc != null && rDesc.multiple) {
                    format = ContentFormat.TLV;
                } else {
                    if (rDesc.type == Type.OPAQUE) {
                        format = ContentFormat.OPAQUE;
                    } else {
                        format = ContentFormat.TEXT;
                    }
                }
            } else {
                // HACK: client should return a content type
                // but specific lwm2m ones are not yet defined
                format = ContentFormat.TLV;
            }
        }

        switch (format) {
        case TEXT:
            // single resource value
            Validate.notNull(path.getResourceId());
            ResourceModel rDesc = model.getResourceModel(path.getObjectId(), path.getResourceId());

            String strValue = new String(content, Charsets.UTF_8);
            Value<?> value = null;
            if (rDesc != null) {
                value = parseTextValue(strValue, rDesc.type, path);
            } else {
                // unknown resource, returning a default string value
                value = Value.newStringValue(strValue);
            }
            return new LwM2mResource(path.getResourceId(), value);

        case TLV:
            try {
                Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(content));
                return parseTlv(tlvs, path, model);
            } catch (TlvException e) {
                throw new InvalidValueException("Unable to decode tlv.", path, e);
            }
        case OPAQUE:
            // single resource value
            Validate.notNull(path.getResourceId());
            ResourceModel desc = model.getResourceModel(path.getObjectId(), path.getResourceId());
            if (desc != null && desc.type != Type.OPAQUE) {
                throw new InvalidValueException(
                        "Invalid content format, OPAQUE can only be used for single OPAQUE resource", path);
            }
            return new LwM2mResource(path.getResourceId(), Value.newBinaryValue(content));
        case JSON:
			try {
				String jsonStrValue = new String(content);
				LwM2mJsonObject json = LwM2mJson.fromJsonLwM2m(jsonStrValue);
				return parseJSON(json, path, model);
			} catch (LwM2mJsonException e) {
				throw new InvalidValueException("Unable to deSerialize json", path, e);
			}
        case LINK:
            throw new InvalidValueException("Content format " + format + " not yet implemented", path);
        }
        return null;

    }

    private static Value<?> parseTextValue(String value, Type type, LwM2mPath path) throws InvalidValueException {
        LOG.trace("TEXT value for path {} and expected type {}: {}", path, type, value);

        try {
            switch (type) {
            case STRING:
                return Value.newStringValue(value);
            case INTEGER:
                try {
                    Long lValue = Long.valueOf(value);
                    if (lValue >= Integer.MIN_VALUE && lValue <= Integer.MAX_VALUE) {
                        return Value.newIntegerValue(lValue.intValue());
                    } else {
                        return Value.newLongValue(lValue);
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidValueException("Invalid value for integer resource: " + value, path);
                }
            case BOOLEAN:
                switch (value) {
                case "0":
                    return Value.newBooleanValue(false);
                case "1":
                    return Value.newBooleanValue(true);
                default:
                    throw new InvalidValueException("Invalid value for boolean resource: " + value, path);
                }
            case FLOAT:
                try {
                    Double dValue = Double.valueOf(value);
                    if (dValue >= Float.MIN_VALUE && dValue <= Float.MAX_VALUE) {
                        return Value.newFloatValue(dValue.floatValue());
                    } else {
                        return Value.newDoubleValue(dValue);
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidValueException("Invalid value for float resource: " + value, path);
                }
            case TIME:
                // number of seconds since 1970/1/1
                return Value.newDateValue(new Date(Long.valueOf(value) * 1000L));
            case OPAQUE:
                // not specified
            case OBJECTLINK:	
            	 // not specified
            default:
                throw new InvalidValueException("Could not parse opaque value with content format " + type, path);
            }
        } catch (NumberFormatException e) {
            throw new InvalidValueException("Invalid numeric value: " + value, path, e);
        }
    }

    private static LwM2mNode parseTlv(Tlv[] tlvs, LwM2mPath path, LwM2mModel model) throws InvalidValueException {
        LOG.trace("Parsing TLV content for path {}: {}", path, tlvs);

        if (path.isObject()) {
            // object level request
            final LwM2mObjectInstance[] instances;

            // is it a mono-instance object without the containing TLV Object instance?
            ObjectModel objectModel = model.getObjectModel(path.getObjectId());
            boolean multiple = objectModel == null ? true : objectModel.multiple;

            if (!multiple && tlvs.length > 0 && tlvs[0].getType() == TlvType.MULTIPLE_RESOURCE
                    || tlvs[0].getType() == TlvType.RESOURCE_VALUE) {
                LwM2mResource[] resources = new LwM2mResource[tlvs.length];
                for (int i = 0; i < tlvs.length; i++) {
                    resources[i] = parseResourceTlv(tlvs[i], path.getObjectId(), 0, model);
                }
                instances = new LwM2mObjectInstance[] { new LwM2mObjectInstance(0, resources) };
            } else {
                instances = new LwM2mObjectInstance[tlvs.length];
                for (int i = 0; i < tlvs.length; i++) {
                    instances[i] = parseObjectInstancesTlv(tlvs[i], path.getObjectId(), model);
                }
            }
            return new LwM2mObject(path.getObjectId(), instances);

        } else if (path.isObjectInstance()) {
            // object instance level request
            LwM2mResource[] resources = new LwM2mResource[tlvs.length];
            for (int i = 0; i < tlvs.length; i++) {
                resources[i] = parseResourceTlv(tlvs[i], path.getObjectId(), path.getObjectInstanceId(), model);
            }
            return new LwM2mObjectInstance(path.getObjectInstanceId(), resources);

        } else {
            // resource level request
            if (tlvs.length == 1) {
                switch (tlvs[0].getType()) {
                case RESOURCE_VALUE:
                    // single value
                    return new LwM2mResource(tlvs[0].getIdentifier(), parseTlvValue(tlvs[0].getValue(), path, model));
                case MULTIPLE_RESOURCE:
                    // supported but not compliant with the TLV specification
                    return parseResourceTlv(tlvs[0], path.getObjectId(), path.getObjectInstanceId(), model);

                default:
                    throw new InvalidValueException("Invalid TLV type: " + tlvs[0].getType(), path);
                }
            } else {
                // array of values
                Value<?>[] values = new Value[tlvs.length];
                for (int j = 0; j < tlvs.length; j++) {
                    values[j] = parseTlvValue(tlvs[j].getValue(), path, model);
                }
                return new LwM2mResource(path.getResourceId(), values);
            }
        }
    }
    
    private static LwM2mNode parseJSON(LwM2mJsonObject jsonObject, LwM2mPath path, LwM2mModel model) throws InvalidValueException, IllegalStateException {
        LOG.trace("Parsing JSON content for path {}: {}", path, jsonObject);

        if (path.isObject()) {
           // TODO 
           // If bn is present will have multiple object instances in JSON payload
           // If JSON contains ObjLnk -> this method should return List<LwM2mNode> ???
           throw new IllegalStateException("Not implemented");
           
        } else if (path.isObjectInstance()) {
            // object instance level request
           	Map<Integer,LwM2mResource> resourceMap = parseJsonPayLoadLwM2mResources(jsonObject,  path,  model);
        	LwM2mResource[] resources = new LwM2mResource[resourceMap.size()];
        	int k=0;
        	for (Entry<Integer,LwM2mResource> entry : resourceMap.entrySet()) {
        	    LwM2mResource resource = entry.getValue();
        	    resources[k] = resource;
        	    k++;
        	}
        	return new LwM2mObjectInstance(path.getObjectInstanceId(), resources);

        } else {
            // resource level request
        	Map<Integer,LwM2mResource> resourceMap = parseJsonPayLoadLwM2mResources(jsonObject,  path,  model);
        	LwM2mResource resource = resourceMap.get(0);
        	return resource;
        }
       
    }

    
	public static Map<Integer,LwM2mResource> parseJsonPayLoadLwM2mResources(LwM2mJsonObject jsonObject, LwM2mPath path, LwM2mModel model) throws InvalidValueException
	{
		Map<Integer,LwM2mResource> lwM2mResourceMap = new HashMap<>();
		Map<Integer,List<Object>> multiResourceMap = new HashMap<>();
		
		for(int i=0;i<jsonObject.getResourceList().size();i++) {
			JsonArrayElement resourceElt =  jsonObject.getResourceList().get(i);
			String[] resourcePath = StringUtils.split(resourceElt.getName(), '/');
			Integer resourceId = Integer.valueOf(resourcePath[0]);
			
			if(!multiResourceMap.isEmpty() && multiResourceMap.get(resourceId)!=null) {
				multiResourceMap.get(resourceId).add(resourceElt.getResourceValue());
				continue;
			}
			if(resourcePath.length>1) {
				// multi resource
			    // store multi resource values in a map
				List<Object> list = new ArrayList<>();
				list.add(resourceElt.getResourceValue());
				multiResourceMap.put(resourceId, list);
			 } else {
				  //single resource	
				LwM2mPath rscPath = new LwM2mPath(path.getObjectId(), path.getObjectInstanceId(), resourceId);
				LwM2mResource res = new LwM2mResource(resourceId, parseJsonValue(resourceElt.getResourceValue(), rscPath, model));
				lwM2mResourceMap.put(resourceId, res);
			 }
		}
		
		for (Map.Entry<Integer,List<Object>> entry : multiResourceMap.entrySet()) {
		    Integer key = entry.getKey();
		    List<Object> valueList = entry.getValue();
		    
		    if(valueList!=null && !valueList.isEmpty()) {
		    	Value<?>[] values = new Value[valueList.size()];
		    	for(int j=0;j<valueList.size();j++)	{
		    		LwM2mPath rscPath = new LwM2mPath(path.getObjectId(), path.getObjectInstanceId(), key);
		    		values[j] = parseJsonValue(valueList.get(j), rscPath, model);
		    	}
		    	LwM2mResource res =  new LwM2mResource(key, values);
		    	lwM2mResourceMap.put(key, res);
		    }
		}
		return lwM2mResourceMap;
	}
	
    private static LwM2mObjectInstance parseObjectInstancesTlv(Tlv tlv, int objectId, LwM2mModel model)
            throws InvalidValueException {
        // read resources
        LwM2mResource[] resources = new LwM2mResource[tlv.getChildren().length];
        for (int i = 0; i < tlv.getChildren().length; i++) {
            resources[i] = parseResourceTlv(tlv.getChildren()[i], objectId, tlv.getIdentifier(), model);
        }
        return new LwM2mObjectInstance(tlv.getIdentifier(), resources);
    }

    private static LwM2mResource parseResourceTlv(Tlv tlv, int objectId, int objectInstanceId, LwM2mModel model)
            throws InvalidValueException {
        LwM2mPath rscPath = new LwM2mPath(objectId, objectInstanceId, tlv.getIdentifier());
        switch (tlv.getType()) {
        case MULTIPLE_RESOURCE:
            // read values
            Value<?>[] values = new Value[tlv.getChildren().length];
            for (int j = 0; j < tlv.getChildren().length; j++) {
                values[j] = parseTlvValue(tlv.getChildren()[j].getValue(), rscPath, model);
            }
            return new LwM2mResource(tlv.getIdentifier(), values);
        case RESOURCE_VALUE:
            return new LwM2mResource(tlv.getIdentifier(), parseTlvValue(tlv.getValue(), rscPath, model));
        default:
            throw new InvalidValueException("Invalid TLV value", rscPath);
        }
    }
    
    private static Value<?> parseJsonValue(Object value, LwM2mPath rscPath, LwM2mModel model)
            throws InvalidValueException {

        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());
       
        LOG.trace("JSON value for path {} and expected type {}: {}", rscPath, rscDesc.type, value);
 
        try {
            switch (rscDesc.type) {
            case INTEGER:
            	// JSON format specs said v = integer or float
            	 return Value.newIntegerValue(((Number)value).intValue());
            case BOOLEAN:
                return Value.newBooleanValue((Boolean)value);
            case FLOAT:
            	// JSON format specs said v = integer or float
               return Value.newFloatValue(((Number)value).floatValue());
            case TIME:
             	// TODO Specs page 44, Resource 13 (current time) of device object represented as Float value 
            	return Value.newDateValue(new Date(((Number)value).longValue() * 1000L));
            case OPAQUE:
            	// If the Resource data type is opaque the string value 
            	// holds the Base64 encoded representation of the Resource 
            	return Value.newBinaryValue(javax.xml.bind.DatatypeConverter.parseHexBinary((String)value));
            case OBJECTLINK:
            	try {
            	  String sValue = (String)value;
            	  String[] Objlnk=  StringUtils.split(sValue, ":");
                  if(Objlnk.length<=1) // Objlnk must contains ":"
                	  throw new InvalidValueException("Invalid value for Objlnk resource: " + value, rscPath);
                  Long ObjlnkId = Long.valueOf(Objlnk[0]);
                  Long ObjlnkInstanceId = Long.valueOf(Objlnk[1]);
                  // MAX_ID 65535 must not be used for object instance
                  if ((ObjlnkId >= Integer.MIN_VALUE && ObjlnkId <= Integer.MAX_VALUE) &&
                      (ObjlnkInstanceId >= Integer.MIN_VALUE && ObjlnkInstanceId < Integer.MAX_VALUE)) {
                       return Value.newStringValue(sValue);
                     } else {
                	  throw new InvalidValueException("Invalid value for Objlnk resource: " + value, rscPath);
                     }
                  } catch (NumberFormatException e) {
                      throw new InvalidValueException("Invalid value for Objlnk resource: " + value, rscPath);
                  }
            	  
           default:
            	// Default is Strung
            return Value.newStringValue((String)value);

            }
        } catch (Exception e) {
            throw new InvalidValueException("Invalid content for type " + rscDesc.type, rscPath, e);
        }
    }
    
    private static Value<?> parseTlvValue(byte[] value, LwM2mPath rscPath, LwM2mModel model)
            throws InvalidValueException {

        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());
        if (rscDesc == null) {
            LOG.trace("TLV value for path {} and unknown type: {}", rscPath, value);
            // no resource description... opaque
            return Value.newBinaryValue(value);
        }

        LOG.trace("TLV value for path {} and expected type {}: {}", rscPath, rscDesc.type, value);
        try {
            switch (rscDesc.type) {
            case STRING:
                return Value.newStringValue(TlvDecoder.decodeString(value));
            case INTEGER:
                Number intNb = TlvDecoder.decodeInteger(value);
                if (value.length < 8) {
                    return Value.newIntegerValue(intNb.intValue());
                } else {
                    return Value.newLongValue(intNb.longValue());
                }

            case BOOLEAN:
                return Value.newBooleanValue(TlvDecoder.decodeBoolean(value));

            case FLOAT:
                Number floatNb = TlvDecoder.decodeFloat(value);
                if (value.length < 8) {
                    return Value.newFloatValue(floatNb.floatValue());
                } else {
                    return Value.newDoubleValue(floatNb.doubleValue());
                }

            case TIME:
                return Value.newDateValue(TlvDecoder.decodeDate(value));

            case OPAQUE:
            	return Value.newBinaryValue(value);
            case OBJECTLINK:	
           	 // not specified	
            default:
            	 throw new InvalidValueException("Invalid TLV value", rscPath);
            }
        } catch (TlvException e) {
            throw new InvalidValueException("Invalid content for type " + rscDesc.type, rscPath, e);
        }
    }
}
