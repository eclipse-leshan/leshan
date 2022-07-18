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
package org.eclipse.leshan.core.link.lwm2m.attributes;

import java.util.EnumSet;

import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.parser.StringParser;

/**
 * Object Version Attribute model as defined at
 * http://www.openmobilealliance.org/release/LightweightM2M/V1_1_1-20190617-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_1_1-20190617-A.html#Table-512-1-lessPROPERTIESgreater-Class-Attributes
 */
public class ObjectVersionAttributeModel extends LwM2mAttributeModel<Version> {

    public ObjectVersionAttributeModel() {
        super(//
                "ver", //
                Attachment.OBJECT, //
                EnumSet.of(AssignationLevel.OBJECT), //
                AccessMode.R, //
                AttributeClass.PROPERTIES);
    }

    /**
     * <pre>
     * "ver" "=" 1*DIGIT "." 1*DIGIT
     * </pre>
     */
    @Override
    public <E extends Throwable> LwM2mAttribute<Version> consumeAttributeValue(StringParser<E> parser) throws E {

        // We don't handle quoted version by default for ver attribute
        // The format is not explicitly defined in v1.0 but
        // you can find some example with and without quote.
        // So we consider this is a spec bug and so we refer to specification v1.1 to know the real format.
        // https://github.com/eclipse/leshan/issues/732)
        //
        // if someone need to support quote then just extends this class overriding tolerateQuote()
        boolean quotedVersion = false;
        if (parser.nextCharIs('"')) {
            parser.consumeNextChar();
            quotedVersion = true;
        }

        // parse Major
        int start = parser.getPosition();
        parser.consumeDIGIT();
        while (parser.nextCharIsDIGIT()) {
            parser.consumeNextChar();
        }
        parser.consumeChar('.');
        parser.consumeDIGIT();
        while (parser.nextCharIsDIGIT()) {
            parser.consumeNextChar();
        }
        int end = parser.getPosition();

        // handle ending quote
        if (quotedVersion) {
            parser.consumeChar('"');
        }

        // validate version
        String strValue = parser.substring(start, end);
        String err = Version.validate(strValue);
        if (err != null) {
            parser.raiseException("Invalid version %s in %s", strValue, parser.getStringToParse());
        }

        // handle quote (see comment above)
        if (quotedVersion && !tolerateQuote()) {
            parser.raiseException("Invalid object version \"%s\" in %s : version should not be quoted", strValue,
                    parser.getStringToParse());
        }
        // create attribute
        return new LwM2mAttribute<Version>(this, new Version(strValue));
    }

    protected boolean tolerateQuote() {
        return false;
    }
}
