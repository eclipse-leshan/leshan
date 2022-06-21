package org.eclipse.leshan.core.demo.cli.converters;

import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.LwM2mPath;

import picocli.CommandLine.ITypeConverter;

public class LwM2mPathConverter implements ITypeConverter<LwM2mPath> {

    @Override
    public LwM2mPath convert(String value) throws InvalidLwM2mPathException {
        return new LwM2mPath(value);
    }
}
