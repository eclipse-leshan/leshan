package org.eclipse.leshan.server.bootstrap.demo;

import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.attributes.AccessMode;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.bootstrap.demo.json.ByteArraySerializer;
import org.eclipse.leshan.server.bootstrap.demo.json.EnumSetDeserializer;
import org.eclipse.leshan.server.bootstrap.demo.json.EnumSetSerializer;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class LeshanBootstrapServerDemoTest {

    @Test
    public void enumDeserializer() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        module.addDeserializer(EnumSet.class, new EnumSetDeserializer());

        mapper.registerModule(module);

        TypeReference<Map<String, FooClass>> fooMapTypeRef = new TypeReference<Map<String, FooClass>>() {};
        Map<String, FooClass> fooClass = mapper.readValue("{\"x\":{\"binding\": \"UT\" , \"accessModes\": [\"R\"],\"secretKey\":[0,255] }}", fooMapTypeRef);

        assertArrayEquals(fooClass.get("x").secretKey, new byte[]{0, -1});
    }

    @Test
    public void enumSerializer() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        module.addSerializer(new EnumSetSerializer());
        module.addSerializer(new ByteArraySerializer(ByteArraySerializer.ByteMode.UNSIGNED));

        mapper.registerModule(module);

        FooClass fooClass = new FooClass();
        fooClass.accessModes = EnumSet.of(AccessMode.R);
        fooClass.binding = EnumSet.of(BindingMode.U, BindingMode.T);
        fooClass.secretKey = new byte[] {0x00, (byte)0xff};
        Map<String, FooClass> map = new HashMap<>();
        map.put("x", fooClass);

        String result = mapper.writeValueAsString(map);

        assertEquals("{\"x\":{\"binding\":\"UT\",\"accessModes\":[\"R\"],\"secretKey\":[0,255]}}", result);
    }

    private static class FooClass {
        public EnumSet<BindingMode> binding;
        public EnumSet<AccessMode> accessModes;

        public byte[] secretKey;
    }

}