package org.eclipse.leshan.core.link;

public interface LinkParser {
    Link[] parse(byte[] bytes);
}
