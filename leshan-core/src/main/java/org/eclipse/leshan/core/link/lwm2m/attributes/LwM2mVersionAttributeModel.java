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

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.parser.StringParser;

public class LwM2mVersionAttributeModel extends LwM2mAttributeModel<LwM2mVersion> {

    public LwM2mVersionAttributeModel() {
        super("lwm2m", //
                Attachment.ROOT, //
                EnumSet.of(AssignationLevel.ROOT), //
                AccessMode.R, //
                AttributeClass.PROPERTIES);
    }

    /**
     * <pre>
     * "ver" "=" 1*DIGIT "." 1*DIGIT
     * </pre>
     */
    @Override
    public <E extends Throwable> LwM2mAttribute<LwM2mVersion> consumeAttributeValue(StringParser<E> parser) throws E {
        // We handle opening quote for v1.0 because of specification ambiguity.
        // The format is not explicitly defined in v1.0 but all example are using quote
        // https://github.com/eclipse/leshan/issues/732)
        // Since v1.1 the format is clearly defined and no quote MUST be used.
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
            parser.raiseException("Invalid lwm2m version %s in %s", strValue, parser.getStringToParse());
        }

        // handle quote (see comment above)
        LwM2mVersion lwM2mVersion = LwM2mVersion.get(strValue);
        if (quotedVersion && !tolerateQuote(lwM2mVersion)) {
            parser.raiseException("Invalid lwm2m version \"%s\" in %s : version should not be quoted", strValue,
                    parser.getStringToParse());
        }

        // create attribute
        return new LwM2mAttribute<LwM2mVersion>(this, lwM2mVersion);
    }

    protected boolean tolerateQuote(LwM2mVersion attributeValue) {
        return attributeValue.equals(LwM2mVersion.V1_0);
    }
}
