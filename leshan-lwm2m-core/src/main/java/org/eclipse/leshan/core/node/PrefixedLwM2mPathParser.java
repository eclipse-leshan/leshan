/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.node.LwM2mPathParser.Start;
import org.eclipse.leshan.core.parser.StringParser;
import org.eclipse.leshan.core.util.Validate;

public class PrefixedLwM2mPathParser {

    private final Start startMode;
    private final LwM2mPathParser pathParser = new LwM2mPathParser(Start.WITHOUT_SLASH);

    public PrefixedLwM2mPathParser() {
        this(Start.WITH_SLASH);
    }

    public PrefixedLwM2mPathParser(Start startMode) {
        this.startMode = startMode;
    }

    public PrefixedLwM2mPath parsePrefixedPath(String path) {
        Validate.notNull(path);

        // create a String Parser
        StringParser<InvalidLwM2mPathException> parser = new StringParser<InvalidLwM2mPathException>(path) {
            @Override
            public void raiseException(String message, Exception cause) throws InvalidLwM2mPathException {
                throw new InvalidLwM2mPathException(message, cause);
            }
        };

        // Parse path segment
        PrefixedLwM2mPath lwm2mPath = consumePrefixedLwM2mPath(parser);
        if (parser.hasMoreChar()) {
            parser.raiseException("Unable to parse LWM2M path [%s] : Unexpected charaters '%s' after '%s'",
                    parser.getStringToParse(), parser.getNextChar(), parser.getAlreadyParsedString());
        }
        return lwm2mPath;
    }

    public List<String> parsePrefix(String pathPrefix) {
        Validate.notNull(pathPrefix);

        // create a String Parser
        StringParser<InvalidLwM2mPathException> parser = new StringParser<InvalidLwM2mPathException>(pathPrefix) {
            @Override
            public void raiseException(String message, Exception cause) throws InvalidLwM2mPathException {
                throw new InvalidLwM2mPathException(message, cause);
            }
        };

        // Parse path segment
        List<String> prefix = consumePrefix(parser);
        if (parser.hasMoreChar()) {
            parser.raiseException("Unable to parse LWM2M path prefix [%s] : Unexpected charaters '%s' after '%s'",
                    parser.getStringToParse(), parser.getNextChar(), parser.getAlreadyParsedString());
        }
        return prefix;
    }

    public <T extends Throwable> PrefixedLwM2mPath consumePrefixedLwM2mPath(StringParser<T> parser) throws T {

        // Consume prefix segments
        List<String> prefix = consumePrefix(parser);

        // Consume path
        LwM2mPath path = null;
        // If there is no prefix then consume starting slash
        if (prefix.isEmpty()) {
            consumeStartingSlash(parser);
            path = consumeLwM2mPath(parser);
        } else {
            // if there is a prefix consume slash separator
            if (parser.nextCharIs('/')) {
                parser.consumeChar('/');
                path = consumeLwM2mPath(parser);
            } else {
                path = LwM2mPath.ROOTPATH;
            }
        }

        return new PrefixedLwM2mPath(prefix, path);
    }

    public <T extends Throwable> List<String> consumePrefix(StringParser<T> parser) throws T {

        // consume starting '/'
        boolean slashConsumed = consumeStartingSlash(parser);

        // Consume URI segments
        List<String> prefix = new ArrayList<>();
        while (true) {
            String segment = tryConsumePrefixSegment(parser);
            // if no more segment we stop
            if (segment == null) {
                // we don't want to consume last slash
                if (slashConsumed) {
                    parser.backtrackLastChar();
                }
                break;
            }
            // else we add segment
            prefix.add(segment);

            if (parser.hasMoreChar() && parser.nextCharIs('/')) {
                parser.consumeChar('/');
                slashConsumed = true;
            } else {
                // if no more char we stop
                break;
            }
        }

        return prefix;
    }

    private <T extends Throwable> LwM2mPath consumeLwM2mPath(StringParser<T> parser) throws T {
        return pathParser.consumeLwM2mPath(parser);
    }

    /**
     * Try to consume a segment of a prefix.
     *
     * @return the segment parsed OR <code>null</code> if there is no more segment to consume.
     */
    protected <T extends Throwable> String tryConsumePrefixSegment(StringParser<T> parser) throws T {
        int begin = parser.getPosition();

        // the spec says : link (talking about alternate path) MUST NOT contain numerical URI segment.
        // https://www.openmobilealliance.org/release/LightweightM2M/V1_2_1-20221209-A/HTML-Version/OMA-TS-LightweightM2M_Transport-V1_2_1-20221209-A.html#6-4-1-0-641-Alternate-Path
        // We suppose this is true for all prefixed segment.

        boolean isNumericalSegment = true;
        while (parser.hasMoreChar() && !parser.nextCharIs('/')) {
            if (!parser.nextCharIsDIGIT()) {
                isNumericalSegment = false;
            }
            parser.consumeNextChar();
        }

        // if it is a numerical segment we failed to consume a prefixed segment, then rollback
        if (isNumericalSegment) {
            parser.backtrackTo(begin);
            return null;
        }
        // else
        int end = parser.getPosition();
        if (end == begin) {
            parser.raiseException(
                    "Unable to parse [%s] : URI Segment (at char index %d) prefixing LWM2M Path can not be empty",
                    parser.getStringToParse(), begin);
        }
        return parser.substring(begin, end);
    }

    /**
     * consume starting slash if needed.
     *
     * @return true if slash is consumed.
     */
    protected <T extends Throwable> boolean consumeStartingSlash(StringParser<T> parser) throws T {
        switch (startMode) {
        case WITHOUT_SLASH:
            // Do Nothing...
            return false;
        case WITH_OR_WITHOUT_SLASH:
            if (parser.nextCharIs('/')) {
                parser.consumeChar('/');
                return true;
            } else {
                return false;
            }
        case WITH_SLASH:
        default:
            parser.consumeChar('/');
            return true;
        }
    }
}
