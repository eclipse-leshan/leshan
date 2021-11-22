package org.eclipse.leshan.core.request.execute;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ArgumentsTest {

    @Test
    public void should_create_arguments_from_map() throws InvalidArgumentException {
        Map<Integer, String> argumentsMap = new HashMap<>();
        argumentsMap.put(3, "stringValue");
        argumentsMap.put(4, null);

        Arguments arguments = new Arguments(argumentsMap);

        Map<Integer, String> newArgumentsMap = arguments.toMap();

        Map<Integer, String> expectedArgumentsMap = new HashMap<>();
        expectedArgumentsMap.put(3, "stringValue");
        expectedArgumentsMap.put(4, "");

        assertEquals(expectedArgumentsMap, newArgumentsMap);
    }

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
    public void should_allow_to_create_empty_arguments() {
        List<Argument> argumentList = new ArrayList<>();

        Arguments arguments = new Arguments(argumentList);

        Map<Integer, String> argumentsMap = arguments.toMap();

        assertEquals(0, argumentsMap.size());
    }
}