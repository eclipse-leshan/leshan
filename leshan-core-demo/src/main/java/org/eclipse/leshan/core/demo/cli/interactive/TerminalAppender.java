/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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
package org.eclipse.leshan.core.demo.cli.interactive;

import java.io.IOException;

import ch.qos.logback.core.ConsoleAppender;
import jline.console.ConsoleReader;

/**
 * A logback Console appender compatible with a Jline 2 Console reader.
 */
public class TerminalAppender<E> extends ConsoleAppender<E> {

    private ConsoleReader console;
    private String prompt;

    @Override
    protected void subAppend(E event) {
        if (console == null || !console.getTerminal().isAnsiSupported())
            super.subAppend(event);
        else {
            // stash prompt
            String stashed = "";
            try {
                stashed = console.getCursorBuffer().copy().toString();
                console.resetPromptLine("", "", -1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Display logs
            super.subAppend(event);

            // unstash prompt
            try {
                console.resetPromptLine(prompt, stashed, -1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setConsole(ConsoleReader console) {
        this.console = console;
        this.prompt = console.getPrompt();
    }
}
