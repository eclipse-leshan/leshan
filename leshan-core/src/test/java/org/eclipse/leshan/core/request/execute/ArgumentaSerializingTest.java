package org.eclipse.leshan.core.request.execute;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ArgumentaSerializingTest {

    @Test
    public void should_serialize_arguments_into_text() {
        List<Argument> argumentList = new ArrayList<>();
        argumentList.add(new Argument(3, "stringValue"));

        byte[] content = new Arguments(argumentList).serialize();

        assertArrayEquals("3='stringValue'".getBytes(StandardCharsets.UTF_8), content);
    }

    @Test
    public void should_serialize_multiple_arguments_into_text() {
        List<Argument> argumentList = new ArrayList<>();
        argumentList.add(new Argument(3, "stringValue"));
        argumentList.add(new Argument(4));

        byte[] content = new Arguments(argumentList).serialize();

        assertArrayEquals("3='stringValue',4".getBytes(StandardCharsets.UTF_8), content);
    }

    @Test
    public void should_serialize_argument_with_empty_string_value_into_text() {
        List<Argument> argumentList = new ArrayList<>();
        argumentList.add(new Argument(3, ""));

        byte[] content = new Arguments(argumentList).serialize();

        assertArrayEquals("3=''".getBytes(StandardCharsets.UTF_8), content);
    }

    @Test
    public void should_serialize_as_null_empty_arguments() {
        List<Argument> argumentList = new ArrayList<>();

        byte[] content = new Arguments(argumentList).serialize();

        assertNull(content);
    }
}