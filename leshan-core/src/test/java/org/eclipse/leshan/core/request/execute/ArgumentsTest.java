package org.eclipse.leshan.core.request.execute;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ArgumentsTest {

    @Test
    public void should_create_arguments_from_map() {
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
    public void should_create_map_from_arguments() {
        List<SingleArgument> argumentList = new ArrayList<>();
        argumentList.add(new SingleArgument(3, "stringValue"));
        argumentList.add(new SingleArgument(4));

        Arguments arguments = new Arguments(argumentList);

        Map<Integer, String> argumentsMap = arguments.toMap();

        assertEquals(2, argumentsMap.size());
        assertEquals("stringValue", argumentsMap.get(3));
        assertEquals("", argumentsMap.get(4));
    }

    @Test
    public void should_allow_to_create_empty_arguments() {
        List<SingleArgument> argumentList = new ArrayList<>();

        Arguments arguments = new Arguments(argumentList);

        Map<Integer, String> argumentsMap = arguments.toMap();

        assertEquals(0, argumentsMap.size());
    }
}