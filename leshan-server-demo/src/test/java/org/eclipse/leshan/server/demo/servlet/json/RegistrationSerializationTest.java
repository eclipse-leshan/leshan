package org.eclipse.leshan.server.demo.servlet.json;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.attributes.AccessMode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.core.demo.json.JacksonSecuritySerializer;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.queue.PresenceService;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class RegistrationSerializationTest {

    @Test
    public void jacksonSerializer() throws JsonProcessingException {
        Registration registration = getExampleRegistration();

        ObjectMapper mapper = getObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addSerializer(SecurityInfo.class, new JacksonSecuritySerializer());
        mapper.registerModule(module);

        String jsonText = mapper.writeValueAsString(registration);

        assertEquals(getExpectedResult(), jsonText);
    }

    @Test
    public void jacksonDeserializer1() throws JsonProcessingException {
        ObjectMapper mapper = getObjectMapper();

        String jsonText = "{\"id\":\"0\",\"resources\":[{\"id\":\"5750\",\"value\":\"ewq\"}]}";

        LwM2mNode lwM2mNode = mapper.readValue(jsonText, LwM2mNode.class);
    }

    @Test
    public void jacksonDeserializer2() throws JsonProcessingException {
        ObjectMapper mapper = getObjectMapper();

        String jsonText = "{\"id\":\"0\",\"resources\":[{\"id\":\"5750\",\"values\":{\"1\":2,\"2\":1}}]}";

        LwM2mNode lwM2mNode = mapper.readValue(jsonText, LwM2mNode.class);
    }

    @Test
    public void jacksonDeserializer3() throws JsonProcessingException {
        ObjectMapper mapper = getObjectMapper();

        String jsonText = "{\"id\":\"0\",\"instances\":[{\"resources\":[{\"id\":\"5750\",\"values\":{\"1\":2,\"2\":1}}]}]}";

        LwM2mNode lwM2mNode = mapper.readValue(jsonText, LwM2mNode.class);
    }



    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
        SimpleModule module = new SimpleModule();
        module.addSerializer(Registration.class, new JacksonRegistrationSerializer(new FakePresenceService()));
        module.addDeserializer(LwM2mNode.class, new JacksonLwM2mNodeDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    private String getExpectedResult() {
        return "{\"endpoint\":\"michal-Latitude-5510\",\"registrationId\":\"JvqZfmaa2e\","
                + "\"registrationDate\":\"2021-10-06T13:57:02+02:00\",\"lastUpdate\":\"2021-10-06T13:57:02+02:00\","
                + "\"address\":\"127.0.0.1:48069\",\"lwM2mVersion\":\"1.0\",\"lifetime\":86400,\"bindingMode\":\"U\","
                + "\"rootPath\":\"/\",\"objectLinks\":[{\"url\":\"/\",\"attributes\":{"
                + "\"ct\":\"\\\"60 110 112 11542 11543\\\"\",\"rt\":\"\\\"oma.lwm2m\\\"\"}},"
                + "{\"url\":\"/1\",\"attributes\":{\"ver\":\"1.1\"}},{\"url\":\"/1/0\",\"attributes\":{}},"
                + "{\"url\":\"/3\",\"attributes\":{\"ver\":\"1.1\"}},{\"url\":\"/3/0\",\"attributes\":{}},"
                + "{\"url\":\"/6/0\",\"attributes\":{}},{\"url\":\"/3303\",\"attributes\":{\"ver\":\"1.1\"}},"
                + "{\"url\":\"/3303/0\",\"attributes\":{}}],\"secure\":false,\"additionalRegistrationAttributes\":{},"
                + "\"queuemode\":false}";
    }

    private Registration getExampleRegistration() {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 48069);
        Registration.Builder builder = new Registration.Builder("JvqZfmaa2e", "michal-Latitude-5510",
                Identity.unsecure(address));
        Link[] links = Link.parse(
                "</>;ct=\"60 110 112 11542 11543\";rt=\"oma.lwm2m\", </1>;ver=1.1, </1/0>, </3>;ver=1.1, </3/0>, </6/0>, </3303>;ver=1.1, </3303/0>".getBytes());
        builder.objectLinks(links);

        builder.supportedContentFormats(ContentFormat.TLV, ContentFormat.JSON, ContentFormat.LINK, ContentFormat.OPAQUE, ContentFormat.CBOR, ContentFormat.SENML_JSON,
                ContentFormat.SENML_CBOR, ContentFormat.TEXT);

        Map<Integer, String> supportedObjects = new HashMap<>();
        supportedObjects.put(1, "1.1");
        supportedObjects.put(3, "1.1");
        supportedObjects.put(6, "1.0");
        supportedObjects.put(3303, "1.1");
        builder.supportedObjects(supportedObjects);

        Set<LwM2mPath> availableInstances = new HashSet<>();
        availableInstances.add(new LwM2mPath("/1/0"));
        availableInstances.add(new LwM2mPath("/3/0"));
        availableInstances.add(new LwM2mPath("/6/0"));
        availableInstances.add(new LwM2mPath("/3303/0"));
        builder.availableInstances(availableInstances);

        builder.lastUpdate(new Date(1633521422366L));
        builder.registrationDate(new Date(1633521422366L));

        Registration registration = builder.build();
        return registration;
    }

    private static class FakePresenceService implements PresenceService {

        @Override
        public void addListener(PresenceListener listener) {

        }

        @Override
        public void removeListener(PresenceListener listener) {

        }

        @Override
        public boolean isClientAwake(Registration registration) {
            return true;
        }
    }
}