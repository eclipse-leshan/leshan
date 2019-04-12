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

package org.eclipse.leshan.senml;

import java.io.ByteArrayOutputStream;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.util.Base64;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

public class SenMLPackSerDes {

    public String serializeToJson(SenMLPack pack) {
        JsonArray jsonArray = new JsonArray();

        for (SenMLRecord record : pack.getRecords()) {
            JsonObject jsonObj = new JsonObject();

            if (record.getBaseName() != null && record.getBaseName().length() > 0) {
                jsonObj.add("bn", record.getBaseName());
            }

            if (record.getBaseTime() != null) {
                jsonObj.add("bt", record.getBaseTime());
            }

            if (record.getName() != null && record.getName().length() > 0) {
                jsonObj.add("n", record.getName());
            }

            if (record.getTime() != null) {
                jsonObj.add("t", record.getTime());
            }

            Type type = record.getType();
            if (type != null) {
                switch (record.getType()) {
                case FLOAT:
                    jsonObj.add("v", record.getFloatValue().floatValue());
                    break;
                case BOOLEAN:
                    jsonObj.add("vb", record.getBooleanValue());
                    break;
                case OBJLNK:
                    jsonObj.add("vlo", record.getObjectLinkValue());
                    break;
                case OPAQUE:
                    jsonObj.add("vd", Base64.encodeBase64String(record.getOpaqueValue()));
                case STRING:
                    jsonObj.add("vs", record.getStringValue());
                    break;
                default:
                    break;
                }
            }

            jsonArray.add(jsonObj);
        }

        return jsonArray.toString();
    }

    public SenMLPack deserializeFromJson(JsonArray array) {

        if (array == null)
            return null;

        SenMLPack pack = new SenMLPack();

        for (JsonValue value : array.values()) {
            JsonObject o = value.asObject();

            SenMLRecord record = new SenMLRecord();

            JsonValue bn = o.get("bn");
            if (bn != null && bn.isString())
                record.setBaseName(bn.asString());

            JsonValue bt = o.get("bt");
            if (bt != null && bt.isNumber())
                record.setBaseTime(bt.asLong());

            JsonValue n = o.get("n");
            if (n != null && n.isString())
                record.setName(n.asString());

            JsonValue t = o.get("t");
            if (t != null && t.isNumber())
                record.setTime(t.asLong());

            JsonValue v = o.get("v");
            if (v != null && v.isNumber())
                record.setFloatValue(v.asFloat());

            JsonValue vb = o.get("vb");
            if (vb != null && vb.isBoolean())
                record.setBooleanValue(vb.asBoolean());

            JsonValue vs = o.get("vs");
            if (vs != null && vs.isString())
                record.setStringValue(vs.asString());

            JsonValue vlo = o.get("vlo");
            if (vlo != null && vlo.isString())
                record.setObjectLinkValue(vlo.asString());

            JsonValue vd = o.get("vd");
            if (vd != null && vd.isString())
                record.setOpaqueValue(Base64.decodeBase64(vd.asString()));

            pack.addRecord(record);
        }

        return pack;
    }

    public byte[] serializeToCbor(SenMLPack pack) {
        CBORFactory factory = new CBORFactory();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            CBORGenerator generator = factory.createGenerator(out);
            generator.writeStartArray();

            for (SenMLRecord record : pack.getRecords()) {
                generator.writeStartObject();

                if (record.getBaseName() != null && record.getBaseName().length() > 0) {
                    generator.writeStringField("-2", record.getBaseName());
                }

                if (record.getBaseTime() != null) {
                    generator.writeNumberField("-3", record.getBaseTime());
                }

                if (record.getName() != null && record.getName().length() > 0) {
                    generator.writeStringField("0", record.getName());
                }

                if (record.getTime() != null) {
                    generator.writeNumberField("6", record.getTime());
                }

                Type type = record.getType();
                if (type != null) {
                    switch (record.getType()) {
                    case FLOAT:
                        generator.writeNumberField("2", record.getFloatValue().floatValue());
                        break;
                    case BOOLEAN:
                        generator.writeBooleanField("4", record.getBooleanValue());
                        break;
                    case OBJLNK:
                        generator.writeStringField("vlo", record.getObjectLinkValue());
                        break;
                    case OPAQUE:
                        generator.writeStringField("8", Base64.encodeBase64String(record.getOpaqueValue()));
                    case STRING:
                        generator.writeStringField("3", record.getStringValue());
                        break;
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
}
