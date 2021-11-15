package org.eclipse.leshan.core.request.execute;

public class InvalidArgumentException extends Exception {

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Object... args) {
        super(String.format(message, args));
    }
}
