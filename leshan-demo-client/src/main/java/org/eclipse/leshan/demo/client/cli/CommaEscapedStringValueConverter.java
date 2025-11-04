package org.eclipse.leshan.demo.client.cli;

import org.eclipse.leshan.demo.cli.converters.EscapedStringValueConverter;

public class CommaEscapedStringValueConverter extends EscapedStringValueConverter {

    public CommaEscapedStringValueConverter() {
        super('\\', true, ',');
    }
}
