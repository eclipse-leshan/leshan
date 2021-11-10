package org.eclipse.leshan.core.request.execute;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class DefaultArgumentParserTest {

    @Test
    public void should_parse_text_into_arguments() {
        ArgumentParser argumentParser = new DefaultArgumentParser();

        Arguments arguments = argumentParser.parse("3='stringValue'");

        assertNotNull(arguments);
        assertEquals(1, arguments.size());

        List<SingleArgument> caughtArguments = new ArrayList<>();
        for(SingleArgument arg: arguments) {
            caughtArguments.add(arg);
        }

        assertEquals(1, caughtArguments.size());
        assertEquals(new SingleArgument(3, "stringValue"), caughtArguments.get(0));
        assertEquals(3, caughtArguments.get(0).getDigit());
        assertEquals("stringValue", caughtArguments.get(0).getValue());
    }

    @Test
    public void should_parse_text_into_arguments2() {
        ArgumentParser argumentParser = new DefaultArgumentParser();

        Arguments arguments = argumentParser.parse("3='stringValue',4");

        assertNotNull(arguments);
        assertEquals(2, arguments.size());

        List<SingleArgument> caughtArguments = new ArrayList<>();
        for(SingleArgument arg: arguments) {
            caughtArguments.add(arg);
        }

        assertEquals(2, caughtArguments.size());
        assertEquals(new SingleArgument(3, "stringValue"), caughtArguments.get(0));
        assertEquals(new SingleArgument(4, null), caughtArguments.get(1));
    }

    @Test
    public void should_parse_null_into_arguments() {
        ArgumentParser argumentParser = new DefaultArgumentParser();

        Arguments arguments = argumentParser.parse(null);

        assertNotNull(arguments);
        assertEquals(0, arguments.size());
    }

    @Test
    public void should_parse_empty_string_into_arguments() {
        ArgumentParser argumentParser = new DefaultArgumentParser();

        Arguments arguments = argumentParser.parse("");

        assertNotNull(arguments);
        assertEquals(0, arguments.size());
    }
}