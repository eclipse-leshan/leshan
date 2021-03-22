/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.senml.cbor.jackson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

public class SenMLCborPackSerDes {

    public SenMLPack deserializeFromCbor(Iterator<JsonNode> nodes) throws SenMLException {
        SenMLPack senMLPack = new SenMLPack();
        while (nodes.hasNext()) {
            JsonNode o = nodes.next();
            if (o == null)
                return null;

            SenMLRecord record = new SenMLRecord();

            JsonNode bn = o.get("-2");
            if (bn != null && bn.isTextual())
                record.setBaseName(bn.asText());

            JsonNode bt = o.get("-3");
            if (bt != null && bt.isNumber())
                record.setBaseTime(bt.asLong());

            JsonNode n = o.get("0");
            if (n != null && n.isTextual())
                record.setName(n.asText());

            JsonNode t = o.get("6");
            if (t != null && t.isNumber())
                record.setTime(t.asLong());

            JsonNode v = o.get("2");
            boolean hasValue = false;
            if (v != null && v.isNumber()) {
                record.setFloatValue(v.numberValue());
                hasValue = true;
            }

            JsonNode vb = o.get("4");
            if (vb != null && vb.isBoolean()) {
                record.setBooleanValue(vb.asBoolean());
                hasValue = true;
            }

            JsonNode vs = o.get("3");
            if (vs != null && vs.isTextual()) {
                record.setStringValue(vs.asText());
                hasValue = true;
            }

            JsonNode vlo = o.get("vlo");
            if (vlo != null && vlo.isTextual()) {
                record.setObjectLinkValue(vlo.asText());
                hasValue = true;
            }

            JsonNode vd = o.get("8");
            if (vd != null && vd.isBinary()) {
                try {
                    record.setOpaqueValue(vd.binaryValue());
                } catch (IOException e) {
                    throw new SenMLException("Invalid SenML record : unable to get binary value for %s", o);
                }
                hasValue = true;
            }

            if (!hasValue)
                throw new SenMLException("Invalid SenML record : record must have a value (v,vb,vlo,vd,vs) : %s", o);

            senMLPack.addRecord(record);
        }
        return senMLPack;
    }

    public byte[] serializeToCbor(SenMLPack pack) throws SenMLException {
        CBORFactory factory = new CBORFactory();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (CBORGenerator generator = factory.createGenerator(out)) {
            generator.writeStartArray(pack.getRecords().size());

            for (SenMLRecord record : pack.getRecords()) {
                boolean hasBaseName = false;
                boolean hasBaseTime = false;
                boolean hasName = false;
                boolean hasTime = false;
                int objectSize = 1;

                if (record.getBaseName() != null && !record.getBaseName().isEmpty()) {
                    hasBaseName = true;
                    objectSize++;
                }

                if (record.getBaseTime() != null) {
                    hasBaseTime = true;
                    objectSize++;
                }

                if (record.getName() != null && !record.getName().isEmpty()) {
                    hasName = true;
                    objectSize++;
                }

                if (record.getTime() != null) {
                    hasTime = true;
                    objectSize++;
                }

                generator.writeStartObject(objectSize);

                if (hasBaseName) {
                    generator.writeFieldId(-2);
                    generator.writeString(record.getBaseName());
                }

                if (hasBaseTime) {
                    generator.writeFieldId(-3);
                    generator.writeNumber(record.getBaseTime());
                }

                if (hasName) {
                    generator.writeFieldId(0);
                    generator.writeString(record.getName());
                }

                if (hasTime) {
                    generator.writeFieldId(6);
                    generator.writeNumber(record.getTime());
                }

                Type type = record.getType();
                if (type != null) {
                    switch (record.getType()) {
                    case FLOAT:
                        Number value = record.getFloatValue();
                        generator.writeFieldId(2);
                        if (value instanceof Byte) {
                            generator.writeNumber(value.byteValue());
                        } else if (value instanceof Short) {
                            generator.writeNumber(value.shortValue());
                        } else if (value instanceof Integer) {
                            generator.writeNumber(value.intValue());
                        } else if (value instanceof Long) {
                            generator.writeNumber(value.longValue());
                        } else if (value instanceof BigInteger) {
                            generator.writeNumber((BigInteger) value);
                        }
                        // unsigned integer
                        else if (value instanceof ULong) {
                            generator.writeNumber(((ULong) value).toBigInteger());
                        }
                        // floating-point
                        else if (value instanceof Float) {
                            generator.writeNumber(value.floatValue());
                        } else if (value instanceof Double) {
                            generator.writeNumber(value.doubleValue());
                        } else if (value instanceof BigDecimal) {
                            generator.writeNumber((BigDecimal) value);
                        }
                        break;
                    case BOOLEAN:
                        generator.writeFieldId(4);
                        generator.writeBoolean(record.getBooleanValue());
                        break;
                    case OBJLNK:
                        generator.writeStringField("vlo", record.getObjectLinkValue());
                        break;
                    case OPAQUE:
                        generator.writeFieldId(8);
                        generator.writeBinary(record.getOpaqueValue());
                        break;
                    case STRING:
                        generator.writeFieldId(3);
                        generator.writeString(record.getStringValue());
                        break;
                    default:
                        break;
                    }
                }
                generator.writeEndObject();
            }

            generator.writeEndArray();
        } catch (Exception ex) {
            throw new SenMLException(ex, "Impossible to encode pack to CBOR: %s", pack, ex);
        }

        return out.toByteArray();
    }
}
