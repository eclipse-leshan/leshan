package org.eclipse.leshan.core.request.execute;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.LwM2mId;
import org.junit.Test;

public class DefaultArgumentSerializerTest {

    @Test
    public void should_serialize_arguments_into_text() {
        List<SingleArgument> argumentList = new ArrayList<>();
        argumentList.add(new SingleArgument(3, "stringValue"));

        ArgumentSerializer serializer = new DefaultArgumentSerializer();

        byte[] content = serializer.serialize(new Arguments(argumentList));

        assertArrayEquals("3='stringValue'".getBytes(StandardCharsets.UTF_8), content);
    }

    @Test
    public void should_serialize_multiple_arguments_into_text() {
        List<SingleArgument> argumentList = new ArrayList<>();
        argumentList.add(new SingleArgument(3, "stringValue"));
        argumentList.add(new SingleArgument(4));

        ArgumentSerializer serializer = new DefaultArgumentSerializer();

        byte[] content = serializer.serialize(new Arguments(argumentList));

        assertArrayEquals("3='stringValue',4".getBytes(StandardCharsets.UTF_8), content);
    }

    @Test
    public void should_serialize_argument_with_empty_string_value_into_text() {
        List<SingleArgument> argumentList = new ArrayList<>();
        argumentList.add(new SingleArgument(3, ""));

        ArgumentSerializer serializer = new DefaultArgumentSerializer();

        byte[] content = serializer.serialize(new Arguments(argumentList));

        assertArrayEquals("3=''".getBytes(StandardCharsets.UTF_8), content);
    }

    @Test
    public void should_serialize_as_null_empty_arguments() {
        List<SingleArgument> argumentList = new ArrayList<>();

        ArgumentSerializer serializer = new DefaultArgumentSerializer();

        byte[] content = serializer.serialize(new Arguments(argumentList));

        assertNull(content);
    }
}