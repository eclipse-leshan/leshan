package org.eclipse.leshan.server.demo.servlet.json;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.server.registration.RegistrationUpdate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JacksonRegistrationUpdateSerializer extends StdSerializer<RegistrationUpdate> {

    private static final long serialVersionUID = -2828961931685566265L;

    protected JacksonRegistrationUpdateSerializer(Class<RegistrationUpdate> t) {
        super(t);
    }

    public JacksonRegistrationUpdateSerializer() {
        this(null);
    }

    @Override
    public void serialize(RegistrationUpdate src, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("registrationId", src.getRegistrationId());

        map.put("address", src.getAddress().getHostAddress() + ":" + src.getPort());
        map.put("smsNumber", src.getSmsNumber());
        map.put("lifetime", src.getLifeTimeInSec());

        map.put("alternatePath", src.getAlternatePath());
        map.put("objectLinks", src.getObjectLinks());
        // TODO secure means over TLS (not OSCORE) but this is not clear so maybe we need to change this.
        map.put("secure", src.getClientTransportData().getIdentity().isSecure()
                && !(src.getClientTransportData().getIdentity() instanceof OscoreIdentity));
        map.put("additionalAttributes", src.getAdditionalAttributes());

        gen.writeObject(map);
    }
}
