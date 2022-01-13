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

/**
 * A String Attribute described as ptoken in RFC6690.
 * <p>
 * See <a href="https://datatracker.ietf.org/doc/html/RFC6690#section-2"> RFC6690#section-2</a>.
 * 
 * <pre>
 * ptoken         = 1*ptokenchar
 * ptokenchar     = "!" / "#" / "$" / "%" / "{@code &}" / "'" / "("
 *                    / ")" / "*" / "+" / "-" / "." / "/" / DIGIT
 *                    / ":" / "{@code <}" / "=" / "{@code >}" / "?" / "@" / ALPHA
 *                    / "[" / "]" / "^" / "_" / "`" / "{" / "|"
 *                    / "}" / "~"
 * </pre>
 */
public class UnquotedStringAttribute extends BaseAttribute {

    public UnquotedStringAttribute(String name, String value) {
        super(name, value);
        // TODO add validation
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
        return getValue();
    }
}
