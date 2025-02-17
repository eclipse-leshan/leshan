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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import org.eclipse.leshan.core.util.datatype.NumberUtil;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.eclipse.leshan.senml.SenMLRecord.Type;

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
                senMLPack.addRecord(deserializeRecord(o));
            }
            return senMLPack;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new SenMLException(e, "Unable to serialize SenML in CBOR");
        }

    }

    protected SenMLRecord deserializeRecord(CBORObject o) throws SenMLException {

        String recordBaseName = deserializeBaseName(o);
        BigDecimal recordBaseTime = deserializeBaseTime(o);
        String recordName = deserializeName(o);
        BigDecimal recordTime = deserializeTime(o);
        Number recordNumberValue = deserializeValue(o);
        Boolean recordBooleanValue = deserializeBooleanValue(o);
        String recordStringValue = deserializeStringValue(o);
        String recordObjectLinkValue = deserializeObjectLinkValue(o);
        byte[] recordOpaqueValue = deserializeOpaqueValue(o);

        boolean hasValue = (recordNumberValue != null || recordBooleanValue != null || recordStringValue != null
                || recordObjectLinkValue != null || recordOpaqueValue != null);

        if (!allowNoValue && !hasValue) {
            throw new SenMLException(
                    "Invalid SenML record: record must have a value, meaning one of those field must be present v(number:2), vb(number:4), vlo(string:vlo) ,vd(number:8) or vs(number:3): %s",
                    o);
        }

        return new SenMLRecord(recordBaseName, recordBaseTime, recordName, recordTime, recordNumberValue,
                recordBooleanValue, recordObjectLinkValue, recordStringValue, recordOpaqueValue);
    }

    protected String deserializeBaseName(CBORObject o) throws SenMLException {
        CBORObject bn = o.get(-2);
        if (bn != null) {
            return deserializeString(bn, "bn");
        }
        return null;
    }

    protected BigDecimal deserializeBaseTime(CBORObject o) throws SenMLException {
        CBORObject bt = o.get(-3);
        if (bt != null) {
            return deserializeTime(bt, "bt");
        }
        return null;
    }

    protected String deserializeName(CBORObject o) throws SenMLException {
        CBORObject n = o.get(0);
        if (n != null) {
            return deserializeString(n, "n");
        }
        return null;
    }

    protected BigDecimal deserializeTime(CBORObject o) throws SenMLException {
        CBORObject t = o.get(6);
        if (t != null) {
            return deserializeTime(t, "t");
        }
        return null;
    }

    protected Number deserializeValue(CBORObject o) throws SenMLException {
        CBORObject v = o.get(2);
        if (v != null) {
            return deserializeNumber(v, "v");
        }
        return null;

    }

    protected Boolean deserializeBooleanValue(CBORObject o) throws SenMLException {
        CBORObject vb = o.get(4);
        if (vb != null) {
            if (!(vb.getType() == CBORType.Boolean)) {
                throw new SenMLException(
                        "Invalid SenML record : 'boolean' type was expected but was '%s' for 'vb' field",
                        o.getType().toString());
            }
            return vb.AsBoolean();
        }
        return null;
    }

    protected String deserializeStringValue(CBORObject o) throws SenMLException {
        CBORObject vs = o.get(3);
        if (vs != null) {
            return deserializeString(vs, "vs");
        }
        return null;
    }

    protected String deserializeObjectLinkValue(CBORObject o) throws SenMLException {
        CBORObject vlo = o.get("vlo");
        if (vlo != null) {
            return deserializeString(vlo, "vlo");
        }
        return null;
    }

    protected byte[] deserializeOpaqueValue(CBORObject o) throws SenMLException {
        // The RFC says : https://datatracker.ietf.org/doc/html/rfc8428#section-6
        //
        // > Octets in the Data Value are encoded using
        // > a byte string with a definite length (major type 2).
        //
        // So we should check this but there is no way to check this with current cbor library.
        // https://github.com/peteroupc/CBOR-Java/issues/28
        CBORObject vd = o.get(8);
        if (vd != null) {
            if (!(vd.getType() == CBORType.ByteString)) {
                throw new SenMLException(
                        "Invalid SenML record : 'byteString' type was expected but was '%s' for 'vd' field",
                        o.getType().toString());
            }
            return vd.GetByteString();
        }
        return null;
    }

    protected String deserializeString(CBORObject o, String fieldname) throws SenMLException {
        // The RFC says : https://datatracker.ietf.org/doc/html/rfc8428#section-6
        //
        // > Characters in the String Value are encoded using a text string
        // > with a definite length (major type 3).
        //
        // So we should check this but there is no way to check this with current cbor library.
        // https://github.com/peteroupc/CBOR-Java/issues/28
        if (!(o.getType() == CBORType.TextString)) {
            throw new SenMLException("Invalid SenML record : 'string' type was expected but was '%s' for '%s' field",
                    o.getType().toString(), fieldname);
        }
        return o.AsString();
    }

    protected BigDecimal deserializeTime(CBORObject o, String fieldname) throws SenMLException {
        // Time should be deserialized like Number
        return NumberUtil.numberToBigDecimal(deserializeNumber(o, fieldname));
    }

    protected Number deserializeNumber(CBORObject o, String fieldname) throws SenMLException {
        // The RFC says : https://datatracker.ietf.org/doc/html/rfc8428#section-6
        //
        // > For JSON Numbers, the CBOR representation can use integers,
        // > floating-point numbers, or decimal fractions (CBOR Tag 4);
        // > however, a representation SHOULD be chosen such that when the CBOR
        // > value is converted to an IEEE double-precision, floating-point
        // > value, it has exactly the same value as the original JSON Number
        // > converted to that form.
        //
        // So we should check this but there is no way to check this with current cbor library.
        // https://github.com/peteroupc/CBOR-Java/issues/28

        if (!o.isNumber()) {
            throw new SenMLException("Invalid SenML record : number was expected for '%s' field", fieldname);
        }

        CBORNumber number = o.AsNumber();
        switch (number.getKind()) {
        case Integer:
        case EInteger:
            if (number.IsNegative()) {
                if (number.CanFitInInt64()) {
                    return number.ToInt64Unchecked();
                } else {
                    return (BigInteger) o.ToObject(BigInteger.class);
                }
            } else {
                if (number.CanFitInInt64()) {
                    return number.ToInt64Unchecked();
                } else if (number.ToEIntegerIfExact().GetSignedBitLengthAsInt64() == 64) {
                    return ULong.valueOf(number.ToInt64Unchecked());
                } else {
                    return (BigInteger) o.ToObject(BigInteger.class);
                }
            }
        case Double:
        case EFloat:
        case EDecimal:
            if (number.CanFitInDouble()) {
                return o.AsDoubleValue();
            } else {
                return (BigDecimal) o.ToObject(BigDecimal.class);
            }
        default:
            throw new SenMLException("Invalid SenML record: unexpected kind of number %s is not supported in %s",
                    number.getKind(), o);
        }
    }

    public byte[] serializeToCbor(SenMLPack pack) throws SenMLException {
        try {
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
                    case NUMBER:
                        Number value = record.getNumberValue();
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
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new SenMLException(e, "Unable to serialize SenML in CBOR");
        }
    }

    protected CBORObject newMap() {
        return CBORObject.NewMap();
    }
}
