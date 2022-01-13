/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.link.attributes;

import org.eclipse.leshan.core.util.Validate;

/**
 * a String Attribute described as quoted-string in RFC2616.
 * <p>
 * See <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-2.2"> rfc2616#section-2.2</a>.
 * 
 * <pre>
 * {@code
 * quoted-string  = ( <"> *(qdtext | quoted-pair ) <"> )
 * qdtext         = <any TEXT except <">>
 * quoted-pair    = "\" CHAR   
 * }
 * </pre>
 * 
 * About quoted-pair we only escaped/unescaped {@code <">} char to have a lossless conversion.
 */
public class QuotedStringAttribute extends BaseAttribute {

    public QuotedStringAttribute(String name, String value) {
        super(name, value);
        Validate.notEmpty(value);
    }

    @Override
    public String getValue() {
        return (String) super.getValue();
    }

    @Override
    public String getStringValue() {
        return getValue();
    }

    @Override
    public String getCoreLinkValue() {
        String valueEscaped = getValue().replace("\"", "\\\"");
        return "\"" + valueEscaped + "\"";
    }

    public static QuotedStringAttribute fromCoreLinkCoreValue(String name, String coreLinkValue) {
        String unquoted = coreLinkValue.substring(1, coreLinkValue.length() - 1);
        String unescaped = unquoted.replace("\\\"", "\"");
        return new QuotedStringAttribute(name, unescaped);
    }
}
