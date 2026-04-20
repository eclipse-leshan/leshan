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
package org.eclipse.leshan.demo.cli;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Option;

/**
 * Mixing about stand helps Options.
 */
public class StandardHelpOptions {

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    @Option(names = { "-V", "--version" }, description = "Print version information and exit.", versionHelp = true)
    private boolean versionRequested;

    private int verboseLevel = 0;

    @Option(names = { "-v", "--verbose" },
            description = { "Specify multiple -v options to increase verbosity.", //
                    "For example, `-v -v -v` or `-vvv`", //
                    "", //
                    "You can adjust more precisely log output using log4j2 configuration file," + //
                            " see 'How to activate more log ?' in FAQ:", //
                    "  https://github.com/eclipse/leshan/wiki/F.A.Q./" })
    public void setVerbose(boolean[] verbose) {
        verboseLevel = verbose.length;

        // set CLI verbosity. (See ShortErrorMessageHandler)
        if (verbose.length > 0) {
            System.setProperty("leshan.cli", "DEBUG");
        }

        // change application log level.
        if (verbose.length > 0) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            switch (verbose.length) {
            case 1:
                setLogLevel(loggerContext, "org.eclipse.leshan", Level.INFO);
                setLogLevel(loggerContext, "org.eclipse.californium", Level.INFO);
                break;
            case 2:
                setLogLevel(loggerContext, "org.eclipse.leshan", Level.DEBUG);
                setLogLevel(loggerContext, "org.eclipse.californium", Level.DEBUG);
                break;
            case 3:
                setLogLevel(loggerContext, "org.eclipse.leshan", Level.TRACE);
                setLogLevel(loggerContext, "org.eclipse.californium", Level.TRACE);
                break;
            case 4:
                setLogLevel(loggerContext, "org.eclipse.leshan", Level.TRACE);
                setLogLevel(loggerContext, "org.eclipse.californium", Level.TRACE);
                setLogLevel(loggerContext, LogManager.ROOT_LOGGER_NAME, Level.TRACE);
                break;
            }
        }
    }

    private void setLogLevel(LoggerContext ctx, String loggerName, Level level) {
        org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);

        // Jeśli logger nie istnieje, tworzymy własny
        if (!loggerConfig.getName().equals(loggerName)) {
            loggerConfig = new LoggerConfig(loggerName, level, true);
            config.addLogger(loggerName, loggerConfig);
        } else {
            loggerConfig.setLevel(level);
        }

        ctx.updateLoggers();
    }

    public int getVerboseLevel() {
        return verboseLevel;
    }
}
