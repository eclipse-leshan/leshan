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
package org.eclipse.leshan.core.demo.cli;

import java.io.PrintWriter;

import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Layout;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 * A Message Handler which display usage of erroneous option only, unlike the default one which display the global help
 * usage.
 *
 */
public class ShortErrorMessageHandler implements IParameterExceptionHandler {
    @Override
    public int handleParseException(ParameterException ex, String[] args) {
        CommandLine cmd = ex.getCommandLine();
        PrintWriter writer = cmd.getErr();

        // print Error
        writer.println(cmd.getColorScheme().errorText(ex.getMessage()));
        writer.println();
        if ("DEBUG".equalsIgnoreCase(System.getProperty("leshan.cli"))) {
            writer.println(cmd.getColorScheme().stackTraceText(ex));
        }

        // print suggestions
        if (UnmatchedArgumentException.printSuggestions(ex, writer)) {
            writer.println();
        }

        // print help usage for args in error
        if (ex instanceof MultiParameterException) {
            Help help = cmd.getHelpFactory().create(cmd.getCommandSpec(), cmd.getColorScheme());
            Layout layout = help.createDefaultLayout();
            for (ArgSpec argSpec : ((MultiParameterException) ex).getArgSpecs()) {
                if (argSpec instanceof OptionSpec) {
                    layout.addOption((OptionSpec) argSpec, help.createDefaultParamLabelRenderer());
                } else if (argSpec instanceof PositionalParamSpec) {
                    layout.addPositionalParameter((PositionalParamSpec) argSpec,
                            help.createDefaultParamLabelRenderer());
                }
            }
            writer.println(layout.toString());
        } else if (ex.getArgSpec() instanceof OptionSpec) {
            Help help = cmd.getHelpFactory().create(cmd.getCommandSpec(), cmd.getColorScheme());
            Layout layout = help.createDefaultLayout();
            layout.addOption((OptionSpec) ex.getArgSpec(), help.createDefaultParamLabelRenderer());
            writer.println(layout.toString());
        } else if (ex.getArgSpec() instanceof PositionalParamSpec) {
            Help help = cmd.getHelpFactory().create(cmd.getCommandSpec(), cmd.getColorScheme());
            Layout layout = help.createDefaultLayout();
            layout.addPositionalParameter((PositionalParamSpec) ex.getArgSpec(),
                    help.createDefaultParamLabelRenderer());
            writer.println(layout.toString());
        }

        // print footer
        CommandSpec spec = cmd.getCommandSpec();
        writer.printf("Try '%s --help' for more information.%n", spec.qualifiedName());

        return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : spec.exitCodeOnInvalidInput();
    }
}
