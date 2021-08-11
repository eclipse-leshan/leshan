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
package org.eclipse.leshan.core.demo.cli;

import java.io.PrintWriter;

import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Layout;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

public class ExecutionExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) throws Exception {
        PrintWriter writer = cmd.getErr();
        if (ex.getMessage() != null)
            writer.print(cmd.getColorScheme().errorText(ex.getMessage()));
        else
            writer.print(cmd.getColorScheme().errorText(ex.getClass().getSimpleName()));
        writer.printf("%n");

        if (ex instanceof InvalidOptionsException) {
            writer.printf("%n");
            Help help = cmd.getHelpFactory().create(cmd.getCommandSpec(), cmd.getColorScheme());
            Layout layout = help.createDefaultLayout();
            for (String optionName : ((InvalidOptionsException) ex).getOptions()) {
                OptionSpec option = cmd.getCommandSpec().findOption(optionName);
                if (option != null) {
                    layout.addOption(option, help.createDefaultParamLabelRenderer());
                }
            }
            writer.print(layout.toString());
        } else {
            writer.print(cmd.getColorScheme().stackTraceText(ex));
        }

        writer.printf("%n");
        CommandSpec spec = cmd.getCommandSpec();
        writer.printf("Try '%s --help' for more information.%n", spec.qualifiedName());

        return AutoComplete.EXIT_CODE_INVALID_INPUT;
    }

}
