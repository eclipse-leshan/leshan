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
package org.eclipse.leshan.core.demo.cli.interactive;

import java.io.PrintWriter;

import jline.console.ConsoleReader;
import picocli.CommandLine;

public class JLineInteractiveCommands {

    private PrintWriter out;
    private CommandLine commandLine;

    void setConsole(ConsoleReader console) {
        out = new PrintWriter(console.getOutput());
    }

    void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    public PrintWriter getConsoleWriter() {
        return out;
    }

    /**
     * Print Help Usage in the console.
     */
    public void printUsageMessage() {
        out.print(commandLine.getUsageMessage());
        out.flush();
    }

    /**
     * A convenience method to write a formatted string to this writer using the specified format string and arguments.
     * <p>
     * See {@link PrintWriter#printf(String, Object...)}
     */
    public PrintWriter printf(String format, Object... args) {
        return out.printf(format, args);
    }

    /**
     * A convenience method to write a formatted string to this writer using the specified format string and arguments.
     * <p>
     * This function support ANSI color tag, see https://picocli.info/#_usage_help_with_styles_and_colors
     * <p>
     * See {@link PrintWriter#printf(String, Object...)}
     */
    public PrintWriter printfAnsi(String format, Object... args) {
        return out.printf(commandLine.getColorScheme().ansi().string(format), args);
    }

    /**
     * A convenience method to write a formatted string to this writer using the specified format string and arguments.
     * <p>
     * The error style from the {@link CommandLine#getColorScheme()} will be used.
     * <p>
     * See {@link PrintWriter#printf(String, Object...)}
     */
    public PrintWriter printfError(String string, Object... args) {
        out.printf(commandLine.getColorScheme().errorText(string).toString(), args);
        return out;
    }

    /**
     * Flush the stream on the console output.
     */
    public void flush() {
        out.flush();
    }
}
