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
package org.eclipse.leshan.senml.cbor.upokecenter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.datatype.NumberUtil;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;

import com.upokecenter.cbor.CBORNumber;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;

public class SenMLCborPackSerDes {

    private final boolean allowNoValue;

    public SenMLCborPackSerDes() {
        this(false);
    }

    /**
     * Create SenML-CBOR serializer/deserializer based on CBOR-JAVA.
     * <p>
     * SenML value is defined as mandatory in <a href="https://tools.ietf.org/html/rfc8428#section-4.2">rfc8428</a>, but
     * SenML records used with a Read-Composite operation do not contain any value field, so
     * <code>allowNoValue=true</code> can be used skip this validation.
     * 
     * @param allowNoValue <code>True</code> to not check if there is a value for each SenML record.
     */
    public SenMLCborPackSerDes(boolean allowNoValue) {
        this.allowNoValue = allowNoValue;
    }

    public SenMLPack deserializeFromCbor(Collection<CBORObject> objects) throws SenMLException {
        try {
            SenMLPack senMLPack = new SenMLPack();
            for (CBORObject o : objects) {
                SenMLRecord record = new SenMLRecord();

                CBORObject bn = o.get(-2);
                if (bn != null && bn.getType() == CBORType.TextString)
                    record.setBaseName(bn.AsString());

                CBORObject bt = o.get(-3);
                if (bt != null && bt.isNumber())
                    record.setBaseTime(bt.AsNumber().ToInt64Checked());

                CBORObject n = o.get(0);
                if (n != null && n.getType() == CBORType.TextString)
                    record.setName(n.AsString());

                CBORObject t = o.get(6);
                if (t != null && t.isNumber())
                    record.setTime(t.AsNumber().ToInt64Checked());

                CBORObject v = o.get(2);
                boolean hasValue = false;
                if (v != null && v.isNumber()) {
                    CBORNumber number = v.AsNumber();
                    if (number.IsInteger()) {
                        if (number.IsNegative()) {
                            if (number.CanFitInInt64()) {
                                record.setFloatValue(number.ToInt64Unchecked());
                            } else {
                                record.setFloatValue((BigInteger) v.ToObject(BigInteger.class));
                            }
                        } else {
                            if (number.CanFitInInt64()) {
                                record.setFloatValue(number.ToInt64Unchecked());
                            } else if (number.ToEIntegerIfExact().GetSignedBitLengthAsInt64() == 64) {
                                record.setFloatValue(ULong.valueOf(number.ToInt64Unchecked()));
                            } else {
                                record.setFloatValue((BigInteger) v.ToObject(BigInteger.class));
                            }
                        }
                    } else {
                        if (v.AsNumber().CanFitInDouble()) {
                            record.setFloatValue(v.AsDoubleValue());
                        } else {
                            record.setFloatValue((BigDecimal) v.ToObject(BigDecimal.class));
                        }
                    }
                    hasValue = true;
                }

                CBORObject vb = o.get(4);
                if (vb != null && vb.getType() == CBORType.Boolean) {
                    record.setBooleanValue(vb.AsBoolean());
                    hasValue = true;
                }

                CBORObject vs = o.get(3);
                if (vs != null && vs.getType() == CBORType.TextString) {
                    record.setStringValue(vs.AsString());
                    hasValue = true;
                }

                CBORObject vlo = o.get("vlo");
                if (vlo != null && vlo.getType() == CBORType.TextString) {
                    record.setObjectLinkValue(vlo.AsString());
                    hasValue = true;
                }

                CBORObject vd = o.get(8);
                if (vd != null && vd.getType() == CBORType.ByteString) {
                    record.setOpaqueValue(vd.GetByteString());
                    hasValue = true;
                }

                if (!allowNoValue && !hasValue)
                    throw new SenMLException("Invalid SenML record : record must have a value (v,vb,vlo,vd,vs) : %s",
                            o);

                senMLPack.addRecord(record);
            }
            return senMLPack;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new SenMLException(e, "Unable to serialize SenML in CBOR");
        }

    }

    public byte[] serializeToCbor(SenMLPack pack) throws SenMLException {
        try (OutputStream stream = new ByteArrayOutputStream()) {
            CBORObject cborArray = CBORObject.NewArray();
            for (SenMLRecord record : pack.getRecords()) {
                CBORObject cborRecord = newMap();
                if (record.getBaseName() != null && !record.getBaseName().isEmpty()) {
                    cborRecord.Add(-2, record.getBaseName());
                }

                if (record.getBaseTime() != null) {
                    cborRecord.Add(-3, record.getBaseTime());
                }

                if (record.getName() != null && !record.getName().isEmpty()) {
                    cborRecord.Add(0, record.getName());
                }

                if (record.getTime() != null) {
                    cborRecord.Add(6, record.getTime());
                }

                Type type = record.getType();
                if (type != null) {
                    switch (record.getType()) {
                    case FLOAT:
                        Number value = record.getFloatValue();
                        if (value instanceof Byte) {
                            cborRecord.Add(2, value.byteValue());
                        } else if (value instanceof Short) {
                            cborRecord.Add(2, value.shortValue());
                        } else if (value instanceof Integer) {
                            cborRecord.Add(2, value.intValue());
                        } else if (value instanceof Long) {
                            cborRecord.Add(2, value.longValue());
                        } else if (value instanceof BigInteger) {
                            cborRecord.Add(2, value);
                        }
                        // unsigned integer
                        else if (value instanceof ULong) {
                            cborRecord.Add(2, NumberUtil.unsignedLongToEInteger(((ULong) value).longValue()));
                        }
                        // floating-point
                        else if (value instanceof Float) {
                            cborRecord.Add(2, value.floatValue());
                        } else if (value instanceof Double) {
                            cborRecord.Add(2, value.doubleValue());
                        } else if (value instanceof BigDecimal) {
                            cborRecord.Add(2, value);
                        }
                        break;
                    case BOOLEAN:
                        cborRecord.Add(4, record.getBooleanValue());
                        break;
                    case OBJLNK:
                        cborRecord.Add("vlo", record.getObjectLinkValue());
                        break;
                    case OPAQUE:
                        cborRecord.Add(8, record.getOpaqueValue());
                        break;
                    case STRING:
                        cborRecord.Add(3, record.getStringValue());
                        break;
                    default:
                        break;
                    }
                } else {
                    if (!allowNoValue) {
                        throw new SenMLException(
                                "Invalid SenML record : record must have a value (v,vb,vlo,vd,vs) : %s", record);
                    }
                }
                cborArray.Add(cborRecord);
            }
            return cborArray.EncodeToBytes();
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            throw new SenMLException(e, "Unable to serialize SenML in CBOR");
        }
    }

    CBORObject newMap() {
        return CBORObject.NewMap();
    }
}
