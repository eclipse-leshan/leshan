package org.eclipse.leshan.client.demo.cli;

import java.io.PrintWriter;

import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.Layout;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.TypeConversionException;
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
        if (ex.getCause() instanceof TypeConversionException || ex.getCause() instanceof IllegalArgumentException) {
            writer.print(cmd.getColorScheme().errorText(ex.getCause().getMessage()));
        } else {
            writer.print(cmd.getColorScheme().errorText(ex.getMessage()));
            writer.printf("%n%n");
            writer.print(cmd.getColorScheme().stackTraceText(ex));
        }

        writer.printf("%n");
        UnmatchedArgumentException.printSuggestions(ex, writer);

        if (ex.getArgSpec() instanceof OptionSpec) {
            writer.printf("%n");
            Help help = cmd.getHelpFactory().create(cmd.getCommandSpec(), cmd.getColorScheme());
            Layout layout = help.createDefaultLayout();
            layout.addOption((OptionSpec) ex.getArgSpec(), help.createDefaultParamLabelRenderer());
            writer.print(layout.toString());
        }

        writer.printf("%n");
        CommandSpec spec = cmd.getCommandSpec();
        writer.printf("Try '%s --help' for more information.%n", spec.qualifiedName());

        return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : spec.exitCodeOnInvalidInput();
    }
}