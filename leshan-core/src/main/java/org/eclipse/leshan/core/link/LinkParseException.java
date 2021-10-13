package org.eclipse.leshan.core.link;

/**
 * Exception to throw if CoRE Link parsing fails.
 */
public class LinkParseException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public LinkParseException(String message) {
        super(message);
    }
}
