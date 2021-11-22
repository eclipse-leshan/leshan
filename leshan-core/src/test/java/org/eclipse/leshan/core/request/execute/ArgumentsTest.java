package org.eclipse.leshan.core.request.execute;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ArgumentsTest {

    @Test
    public void should_create_map_from_arguments() throws InvalidArgumentException {
        Arguments arguments = Arguments.builder() //
                .addArgument(3, "stringValue") //
                .addArgument(4) //
                .build();

        Map<Integer, String> argumentsMap = arguments.toMap();

        assertEquals(2, argumentsMap.size());
        assertEquals("stringValue", argumentsMap.get(3));
        assertEquals("", argumentsMap.get(4));
    }

    @Test
    public void should_allow_to_create_empty_arguments() throws InvalidArgumentException {
        Map<Integer, String> argumentsMap = Arguments.builder().build().toMap();

        assertEquals(0, argumentsMap.size());
    }
}