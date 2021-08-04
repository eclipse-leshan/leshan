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
