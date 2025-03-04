/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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

import org.eclipse.leshan.core.parser.StringParser;

public final class AttributeParserUtil {

    private AttributeParserUtil() {
    }

    // For Number consumption I guess consuming CARDINAL should be better but we strictly follow the specification here.
    // See {@link StringParser#consumeCardinal())
    // See :https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/592

    /**
     * Consume a Positive integer number
     *
     * <pre>
     * 1 * DIGIT
     * </pre>
     */
    public static <E extends Throwable> void consumePositiveIntegerNumber(StringParser<E> parser) throws E {
        parser.consumeDIGIT();
        while (parser.nextCharIsDIGIT()) {
            parser.consumeNextChar();
        }
    }

    /**
     * Consume a integer number
     *
     * <pre>
     *  ["-"] 1 * DIGIT
     * </pre>
     */
    public static <E extends Throwable> void consumeIntegerNumber(StringParser<E> parser) throws E {
        parser.optionallyConsumeChar('-');
        consumePositiveIntegerNumber(parser);
    }

    /**
     * Consume a positive decimal number
     *
     * <pre>
     * 1*DIGIT ["." 1*DIGIT]
     * </pre>
     */
    public static <E extends Throwable> void consumePositiveDecimalNumber(StringParser<E> parser) throws E {
        consumePositiveIntegerNumber(parser);
        if (parser.optionallyConsumeChar('.')) {
            consumePositiveIntegerNumber(parser);
        }
    }

    /**
     * Consume a decimal number
     *
     * <pre>
     *  ["-"]  1*DIGIT ["." 1*DIGIT]
     * </pre>
     */
    public static <E extends Throwable> void consumeDecimalNumber(StringParser<E> parser) throws E {
        parser.optionallyConsumeChar('-');
        consumePositiveDecimalNumber(parser);
    }
}
