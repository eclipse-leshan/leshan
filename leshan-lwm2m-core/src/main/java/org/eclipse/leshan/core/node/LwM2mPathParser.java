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

import org.eclipse.leshan.core.parser.StringParser;
import org.eclipse.leshan.core.util.Validate;

public class LwM2mPathParser {

    public enum Start {
        WITH_SLASH, WITHOUT_SLASH, WITH_OR_WITHOUT_SLASH
    }

    private final Start startMode;

    public LwM2mPathParser() {
        this(Start.WITH_SLASH);
    }

    public LwM2mPathParser(Start startMode) {
        this.startMode = startMode;
    }

    public LwM2mPath parse(String path) {
        Validate.notNull(path);

        // create a String Parser
        StringParser<InvalidLwM2mPathException> parser = new StringParser<InvalidLwM2mPathException>(path) {
            @Override
            public void raiseException(String message, Exception cause) throws InvalidLwM2mPathException {
                throw new InvalidLwM2mPathException(message, cause);
            }
        };

        // Parse path segment
        LwM2mPath lwm2mPath = consumeLwM2mPath(parser);
        if (parser.hasMoreChar()) {
            parser.raiseException("Unable to parse [%s] : Unexpected charaters '%s' after '%s'",
                    parser.getStringToParse(), parser.getNextChar(), parser.getAlreadyParsedString());
        }
        return lwm2mPath;
    }

    public <T extends Throwable> LwM2mPath consumeLwM2mPath(StringParser<T> parser) throws T {

        // consume starting /
        consumeStartingSlash(parser);
        if (!parser.nextCharIsDIGIT()) {
            return LwM2mPath.ROOTPATH;
        }

        // consume object id
        int objectId = consumeNodeId(parser);
        if (!hasMoreNodeId(parser)) {
            return new LwM2mPath(objectId);
        }

        // consume object instance id
        parser.consumeChar('/');
        int objectInstanceId = consumeNodeId(parser);
        if (!hasMoreNodeId(parser)) {
            return new LwM2mPath(objectId, objectInstanceId);
        }

        // consume resource id
        parser.consumeChar('/');
        int resourceId = consumeNodeId(parser);
        if (!hasMoreNodeId(parser)) {
            return new LwM2mPath(objectId, objectInstanceId, resourceId);
        }
        // consume resource instance id
        parser.consumeChar('/');
        int resourceInstanceId = consumeNodeId(parser);
        return new LwM2mPath(objectId, objectInstanceId, resourceId, resourceInstanceId);
    }

    protected <T extends Throwable> void consumeStartingSlash(StringParser<T> parser) throws T {
        switch (startMode) {
        case WITHOUT_SLASH:
            // Do Nothing...
            break;
        case WITH_OR_WITHOUT_SLASH:
            if (parser.nextCharIs('/')) {
                parser.consumeChar('/');
            }
            break;
        case WITH_SLASH:
        default:
            parser.consumeChar('/');
            break;
        }
    }

    protected <T extends Throwable> boolean hasMoreNodeId(StringParser<T> parser) throws T {
        if (parser.nextCharIs('/')) {
            parser.consumeChar('/');
            boolean result = parser.nextCharIsDIGIT();
            parser.backtrackLastChar(); // cancel '/' consumption
            return result;
        } else {
            return false;
        }
    }

    protected <T extends Throwable> int consumeNodeId(StringParser<T> parser) throws T {
        return Integer.valueOf(parser.consumeCardinal());
    }
}
