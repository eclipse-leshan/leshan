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
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
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

        // create attribute
        String strValue = parser.substring(start, end);

        // validate version
        String err = Version.validate(strValue);
        if (err != null) {
            parser.raiseException("Invalid lwm2m version %s in %s", strValue, parser.getStringToParse());
        }

        return new LwM2mAttribute<LwM2mVersion>(this, LwM2mVersion.get(strValue));
    }

    @Override
    public LwM2mAttribute<LwM2mVersion> createEmptyAttribute() throws InvalidAttributeException {
        throw new InvalidAttributeException("Attribute %s must have a value", getName());
    }
}
