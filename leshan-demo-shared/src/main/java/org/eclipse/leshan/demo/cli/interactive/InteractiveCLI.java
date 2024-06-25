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

import java.io.IOException;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Appender;
import jline.TerminalFactory;
import jline.TerminalFactory.Type;
import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter.ArgumentList;
import jline.console.completer.ArgumentCompleter.WhitespaceArgumentDelimiter;
import jline.internal.Configuration;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.shell.jline2.PicocliJLineCompleter;

public class InteractiveCLI {

    private ConsoleReader console;
    private CommandLine commandLine;

    public InteractiveCLI(JLineInteractiveCommands interactivesCommands) throws IOException {

        // JLine 2 does not detect some terminal as not ANSI compatible, like Eclipse Console
        // see : https://github.com/jline/jline2/issues/185
        // So use picocli heuristic instead :
        if (!Help.Ansi.AUTO.enabled() && //
                Configuration.getString(TerminalFactory.JLINE_TERMINAL, TerminalFactory.AUTO).toLowerCase()
                        .equals(TerminalFactory.AUTO)) {
            TerminalFactory.configure(Type.NONE);
        }

        // Create Interactive Shell
        console = new ConsoleReader();
        console.setPrompt("");

        // set up the completion
        interactivesCommands.setConsole(console);
        commandLine = new CommandLine(interactivesCommands);
        interactivesCommands.setCommandLine(commandLine);

        console.addCompleter(new PicocliJLineCompleter(commandLine.getCommandSpec()));

        // Configure Terminal appender if it is present.
        Appender<?> appender = ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
                .getAppender("TERMINAL");
        if (appender instanceof TerminalAppender<?>) {
            ((TerminalAppender<?>) appender).setConsole(console);
        }
    }

    public void showHelp() {
        commandLine.usage(commandLine.getOut());
    }

    public void start() throws IOException {

        // start the shell and process input until the user quits with Ctl-D
        String line;
        while ((line = console.readLine()) != null) {
            ArgumentList list = new WhitespaceArgumentDelimiter().delimit(line, line.length());
            commandLine.execute(list.getArguments());
            console.killLine();
        }
    }
}
