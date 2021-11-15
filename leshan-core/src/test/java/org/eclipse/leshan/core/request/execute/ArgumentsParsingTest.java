package org.eclipse.leshan.core.request.execute;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class ArgumentsParsingTest {

    @Test
    public void should_parse_text_into_arguments() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3='stringValue'");

        assertNotNull(arguments);
        assertEquals(1, arguments.size());

        List<Argument> caughtArguments = new ArrayList<>();
        for(Argument arg: arguments) {
            caughtArguments.add(arg);
        }

        assertEquals(1, caughtArguments.size());
        assertEquals(new Argument(3, "stringValue"), caughtArguments.get(0));
        assertEquals(3, caughtArguments.get(0).getDigit());
        assertEquals("stringValue", caughtArguments.get(0).getValue());
    }

    @Test
    public void should_parse_text_into_arguments2() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3='stringValue',4");

        assertNotNull(arguments);
        assertEquals(2, arguments.size());

        List<Argument> caughtArguments = new ArrayList<>();
        for(Argument arg: arguments) {
            caughtArguments.add(arg);
        }

        assertEquals(2, caughtArguments.size());
        assertEquals(new Argument(3, "stringValue"), caughtArguments.get(0));
        assertEquals(new Argument(4, null), caughtArguments.get(1));
    }

    @Test
    public void should_parse_null_into_arguments() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse(null);

        assertNotNull(arguments);
        assertEquals(0, arguments.size());
    }

    @Test
    public void should_parse_empty_text_into_arguments() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("");

        assertNotNull(arguments);
        assertEquals(0, arguments.size());
    }

    @Test
    public void should_parse_text_into_argument_with_empty_value() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3=''");

        assertNotNull(arguments);
        assertEquals(1, arguments.size());
        Argument argument = arguments.iterator().next();
        assertEquals(3, argument.getDigit());
        assertEquals("", argument.getValue());
    }

    @Test
    public void should_parse_text_into_argument_with_comma_value() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3=',',4");
        Iterator<Argument> argumentIterator = arguments.iterator();

        assertNotNull(arguments);
        assertEquals(2, arguments.size());
        Argument argument = argumentIterator.next();
        assertEquals(3, argument.getDigit());
        assertEquals(",", argument.getValue());

        argument = argumentIterator.next();
        assertEquals(4, argument.getDigit());
        assertNull(argument.getValue());
    }

    @Test
    public void should_parse_text_into_argument_with_equal_value() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3='=',4");
        Iterator<Argument> argumentIterator = arguments.iterator();

        assertNotNull(arguments);
        assertEquals(2, arguments.size());
        Argument argument = argumentIterator.next();
        assertEquals(3, argument.getDigit());
        assertEquals("=", argument.getValue());

        argument = argumentIterator.next();
        assertEquals(4, argument.getDigit());
        assertNull(argument.getValue());
    }

    @Test(expected = InvalidArgumentException.class)
    public void should_not_parse_text_into_argument_with_value_without_equal() throws InvalidArgumentException {
        Arguments.parse("3'hello'");
    }

    @Test(expected = InvalidArgumentException.class)
    public void should_not_parse_text_into_argument_with_value_without_quotes() throws InvalidArgumentException {
        Arguments.parse("3=hello");
    }
}