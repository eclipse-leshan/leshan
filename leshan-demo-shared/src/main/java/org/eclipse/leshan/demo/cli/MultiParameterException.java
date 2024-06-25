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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;

public class MultiParameterException extends ParameterException {

    private static final long serialVersionUID = 1L;

    private List<ArgSpec> argSpecs;

    public MultiParameterException(CommandLine commandLine, String msg, String... optionNames) {
        this(commandLine, msg, namesToSpecs(commandLine, optionNames));
    }

    public MultiParameterException(CommandLine commandLine, String msg, ArgSpec... argsInError) {
        this(commandLine, msg, Arrays.asList(argsInError));

    }

    public MultiParameterException(CommandLine commandLine, String msg, List<ArgSpec> argsInError) {
        this(commandLine, msg, argsInError, null);
    }

    public MultiParameterException(CommandLine commandLine, String msg, Throwable cause, String... optionNames) {
        this(commandLine, msg, namesToSpecs(commandLine, optionNames), cause);
    }

    public MultiParameterException(CommandLine commandLine, String msg, List<ArgSpec> argsInError, Throwable cause) {
        super(commandLine, msg, cause);
        argSpecs = argsInError;
    }

    public List<ArgSpec> getArgSpecs() {
        return argSpecs;
    }

    private static List<ArgSpec> namesToSpecs(CommandLine commandLine, String... optionNames) {
        List<ArgSpec> optionSpecs = new ArrayList<>(optionNames.length);
        for (String optionName : optionNames) {
            OptionSpec optionSpec = commandLine.getCommandSpec().findOption(optionName);
            if (optionSpec == null) {
                throw new IllegalArgumentException(
                        String.format("Option [%s] does not exist for %s", optionName, commandLine.getCommandName()));
            }
            optionSpecs.add(optionSpec);
        }
        return optionSpecs;
    }
}
