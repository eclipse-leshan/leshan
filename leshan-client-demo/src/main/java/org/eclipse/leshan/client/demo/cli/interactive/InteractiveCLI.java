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
package org.eclipse.leshan.client.demo.cli.interactive;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.TailTipWidgets;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.Appender;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;

public class InteractiveCLI {

    private CommandLine commandLine;
    private LineReader reader;
    private SystemRegistryImpl systemRegistry;
    private String prompt;

    public InteractiveCLI(LeshanClient client, LwM2mModel model) throws IOException {
        Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
        // set up picocli commands
        InteractiveCommands commands = new InteractiveCommands(client, model);
        PicocliCommandsFactory factory = new PicocliCommandsFactory();
        commandLine = new CommandLine(commands, factory);
        PicocliCommands picocliCommands = new PicocliCommands(commandLine);

        Parser parser = new DefaultParser();
        try (Terminal terminal = TerminalBuilder.builder().dumb(true).build()) {
            systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
            systemRegistry.setCommandRegistries(picocliCommands);
            systemRegistry.register("help", picocliCommands);

            reader = LineReaderBuilder.builder().terminal(terminal).completer(systemRegistry.completer()).parser(parser)
                    .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
                    .build();
            factory.setTerminal(terminal);
            TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5,
                    TailTipWidgets.TipType.COMPLETER);
            widgets.enable();
            KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
            keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

            prompt = "prompt> ";

            // Configure Terminal appender if it is present.
            Appender<?> appender = ((ch.qos.logback.classic.Logger) LoggerFactory
                    .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).getAppender("TERMINAL");
            if (appender instanceof TerminalAppender<?>) {
                ((TerminalAppender<?>) appender).setReader(reader);
            }
        }
    }

    public void showHelp() {
        commandLine.usage(commandLine.getOut());
    }

    public void start() throws IOException {
        // start the shell and process input until the user quits with Ctrl-D
        String line;
        while (true) {
            try {
                systemRegistry.cleanUp();
                line = reader.readLine(prompt, null, (MaskingCallback) null, null);
                systemRegistry.execute(line);
            } catch (UserInterruptException e) {
                // Ignore
            } catch (EndOfFileException e) {
                return;
            } catch (Exception e) {
                systemRegistry.trace(e);
            }
        }
    }
}
