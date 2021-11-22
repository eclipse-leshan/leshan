package org.eclipse.leshan.core.request.execute;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ArgumentaSerializingTest {

    @Test
    public void should_serialize_arguments_into_text() throws InvalidArgumentException {
        byte[] content = Arguments.builder() //
                .addArgument(3, "stringValue") //
                .build().serialize();

        assertArrayEquals("3='stringValue'".getBytes(StandardCharsets.UTF_8), content);
    }

    @Test
    public void should_serialize_multiple_arguments_into_text() throws InvalidArgumentException {
        byte[] content = Arguments.builder() //
                .addArgument(3, "stringValue") //
                .addArgument(4) //
                .build().serialize();

        assertArrayEquals("3='stringValue',4".getBytes(StandardCharsets.UTF_8), content);
    }

    @Test
    public void should_serialize_argument_with_empty_string_value_into_text() throws InvalidArgumentException {
        byte[] content = Arguments.builder() //
                .addArgument(3, "") //
                .build().serialize();

        assertArrayEquals("3=''".getBytes(StandardCharsets.UTF_8), content);
    }

    @Test
    public void should_serialize_as_null_empty_arguments() throws InvalidArgumentException {
        byte[] content = Arguments.builder() //
                .build().serialize();

        assertNull(content);
    }
}