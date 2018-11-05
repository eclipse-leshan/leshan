package org.eclipse.leshan.server.californium;

import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.leshan.Link;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.server.californium.impl.CoapRequestBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by HQ.heqing
 * on 2018/10/29  下午4:54
 */
public class DataParseTest {

    public static void main(String[] args) throws UnknownHostException {

        LwM2mModel model = new LwM2mModel(ObjectLoader.loadDefault());
        LwM2mNodeEncoder encoder = new DefaultLwM2mNodeEncoder();

        final Registration reg = newRegistration();

        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder);
        WriteRequest request = new WriteRequest(1, 3303, 0, 123);

        builder.visit(request);

        // verify
        final Request coapRequest = builder.getRequest();
        coapRequest.setToken(Token.EMPTY);
        coapRequest.setMID(65535);
        byte[] byteArray = new UdpDataSerializer().getByteArray(coapRequest);

       final Message message = new UdpDataParser().parseMessage(byteArray);

        System.out.println(message.equals(coapRequest));

        System.out.println();

        final LeshanServer server = new LeshanServerBuilder().build();

        server.getRegistrationService().addListener(new RegistrationListener() {

            @Override
            public void registered(Registration registration, Registration previousReg, Collection<Observation> previousObsersations) {

            }

            @Override
            public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {

            }

            @Override
            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired, Registration newReg) {
            }
        });



//        try{
//            Object coapResponse = response.getCoapResponse();
//            byte[] byteArray = new UdpDataSerializer().getByteArray((Message) coapResponse);
//
//
//            char[] chars = Hex.encodeHex(byteArray);
//
//            Message message = new UdpDataParser().parseMessage(byteArray);
//
//            System.out.println();
//        }catch (Exception e){
//            e.printStackTrace();
//        }


    }

    private static Registration newRegistration() throws UnknownHostException {
        return newRegistration(null);
    }

    private static Registration newRegistration(String rootpath) throws UnknownHostException {
        Registration.Builder b = new Registration.Builder("regid", "endpoint",
                Identity.unsecure(Inet4Address.getLoopbackAddress(), 12354), new InetSocketAddress(0));
        if (rootpath != null) {
            Map<String, String> attr = new HashMap<>();
            attr.put("rt", "oma.lwm2m");
            b.objectLinks(new Link[] { new Link(rootpath, attr) });
        }
        return b.build();
    }
}
