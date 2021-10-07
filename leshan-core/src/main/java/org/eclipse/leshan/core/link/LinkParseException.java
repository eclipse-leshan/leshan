package org.eclipse.leshan.core.link;

/**
 * Exception to throw if CoRE Link parsing fails.
 */
public class LinkParseException extends IllegalArgumentException {

    public LinkParseException(String message) {
        super(message);
    }
}
