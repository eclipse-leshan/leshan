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
            defaultConverterMap.put("black", NoColorConverter.class.getName());
            defaultConverterMap.put("red", NoColorConverter.class.getName());
            defaultConverterMap.put("green", NoColorConverter.class.getName());
            defaultConverterMap.put("yellow", NoColorConverter.class.getName());
            defaultConverterMap.put("blue", NoColorConverter.class.getName());
            defaultConverterMap.put("magenta", NoColorConverter.class.getName());
            defaultConverterMap.put("cyan", NoColorConverter.class.getName());
            defaultConverterMap.put("white", NoColorConverter.class.getName());
            defaultConverterMap.put("gray", NoColorConverter.class.getName());
            defaultConverterMap.put("boldRed", NoColorConverter.class.getName());
            defaultConverterMap.put("boldGreen", NoColorConverter.class.getName());
            defaultConverterMap.put("boldYellow", NoColorConverter.class.getName());
            defaultConverterMap.put("boldBlue", NoColorConverter.class.getName());
            defaultConverterMap.put("boldMagenta", NoColorConverter.class.getName());
            defaultConverterMap.put("boldCyan", NoColorConverter.class.getName());
            defaultConverterMap.put("boldWhite", NoColorConverter.class.getName());
            defaultConverterMap.put("highlight", NoColorConverter.class.getName());
        }
    }
}
