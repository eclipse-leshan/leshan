/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import picocli.CommandLine;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.ParseResult;

public class PicocliUtil {

    /**
     * Apply given function to any <strong>called</strong> sub-command which implement given type.
     * <p>
     * (This is direct sub-command, not recursive search here)
     */
    public static <T> void applyTo(ParseResult result, Class<T> searchedType,
            BiConsumer<ParseResult, T> functionToApply) {

        if (result.hasSubcommand()) {
            for (ParseResult subResult : result.subcommands()) {
                Object userDefinedCommand = subResult.commandSpec().userObject();
                if (searchedType.isInstance(userDefinedCommand)) {
                    functionToApply.accept(subResult, searchedType.cast(userDefinedCommand));
                }
            }
        }
    }

    /**
     * Apply given function to any <strong>registered</strong> (not necessarily called) sub-command which implement
     * given type.
     * <p>
     * (This is direct sub-command, not recursive search here)
     */
    public static <T> void applyTo(CommandLine cmdline, Class<T> searchedType,
            BiConsumer<CommandLine, T> functionToApply) {

        Collection<CommandLine> subcommands = cmdline.getSubcommands().values();
        if (!subcommands.isEmpty()) {
            for (CommandLine subCommandLine : subcommands) {
                Object userDefinedCommand = subCommandLine.getCommandSpec().userObject();
                if (searchedType.isInstance(userDefinedCommand)) {
                    functionToApply.accept(subCommandLine, searchedType.cast(userDefinedCommand));
                }
            }
        }
    }

    /**
     * Search in <strong>registered</strong> (not necessarily called) sub-command the first one which implement given
     * type, then apply the given function to it which will create returned result.
     * <p>
     * (This is direct sub-command, not recursive search here)
     */
    public static <T, R> R reduceTo(CommandLine cmdline, Class<T> searchedType,
            BiFunction<CommandLine, T, R> functionToApply) {

        Collection<CommandLine> subcommands = cmdline.getSubcommands().values();
        if (!subcommands.isEmpty()) {
            for (CommandLine subCommandLine : subcommands) {
                Object userDefinedCommand = subCommandLine.getCommandSpec().userObject();
                if (searchedType.isInstance(userDefinedCommand)) {
                    return functionToApply.apply(subCommandLine, searchedType.cast(userDefinedCommand));
                }
            }
        }
        return null;
    }

    /**
     * Return true if help is requested
     */
    public static <T> boolean isHelpRequested(CommandLine cmdline) {
        List<CommandLine> calledCommands = cmdline.getParseResult().asCommandLineList();

        for (CommandLine calledCommand : calledCommands) {
            if (calledCommand.isUsageHelpRequested()
                    || calledCommand.getCommandSpec().userObject() instanceof HelpCommand) {
                return true;
            }
        }
        return false;
    }
}
