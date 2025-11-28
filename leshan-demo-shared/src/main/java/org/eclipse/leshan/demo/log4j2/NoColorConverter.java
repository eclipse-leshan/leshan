/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.demo.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

@Plugin(name = "NoColor", category = "Converter")
public class NoColorConverter extends LogEventPatternConverter {

    protected NoColorConverter() {
        super("NoColor", "noColor");
    }

    @PluginFactory
    public static NoColorConverter newInstance() {
        return new NoColorConverter();
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        // po prostu zwracamy surowy message bez modyfikacji
        toAppendTo.append(event.getMessage().getFormattedMessage());
    }
}
