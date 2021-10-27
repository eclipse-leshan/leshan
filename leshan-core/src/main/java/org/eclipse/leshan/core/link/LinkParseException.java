package org.eclipse.leshan.core.link;

/**
 * Exception to throw if CoRE Link parsing fails.
 */
public class LinkParseException extends Exception {

    private static final long serialVersionUID = 1L;

    public LinkParseException(String message) {
        super(message);
    }

    public LinkParseException(String message, Object... args) {
        super(String.format(message, args));
    }

    public LinkParseException(Exception e, String message, Object... args) {
        super(String.format(message, args), e);
    }

    public LinkParseException(String message, Exception e) {
        super(message, e);
    }
}
