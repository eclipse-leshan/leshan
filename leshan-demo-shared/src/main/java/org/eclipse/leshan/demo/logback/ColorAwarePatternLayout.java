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
package org.eclipse.leshan.core.demo.logback;

import ch.qos.logback.classic.PatternLayout;
import picocli.CommandLine.Help.Ansi;

/**
 * A Logback Pattern Layout which use Picocli ANSI color heuristic to apply ANSI color only on terminal which support
 * it.
 */
public class ColorAwarePatternLayout extends PatternLayout {

    static {
        if (!Ansi.AUTO.enabled()) {
            DEFAULT_CONVERTER_MAP.put("black", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("red", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("green", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("yellow", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("blue", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("magenta", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("cyan", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("white", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("gray", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldRed", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldGreen", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldYellow", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldBlue", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldMagenta", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldCyan", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldWhite", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("highlight", NoColorConverter.class.getName());
        }
    }
}
