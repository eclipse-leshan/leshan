package org.eclipse.leshan.core.request.execute;

public interface ArgumentSerializer {
    byte[] serialize(Arguments arguments);
}
