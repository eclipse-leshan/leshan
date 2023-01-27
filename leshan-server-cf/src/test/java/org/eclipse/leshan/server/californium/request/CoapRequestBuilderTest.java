/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.server.californium.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.core.californium.identity.DefaultCoapIdentityHandler;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.tlv.Tlv;
import org.eclipse.leshan.core.tlv.Tlv.TlvType;
import org.eclipse.leshan.core.tlv.TlvDecoder;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.Registration.Builder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CoapRequestBuilder}
 */
public class CoapRequestBuilderTest {

    private static LwM2mModel model;
    private static LwM2mEncoder encoder;
    private static IdentityHandler identityHandler;

    @BeforeAll
    public static void loadModel() {
        model = new StaticModel(ObjectLoader.loadDefault());
        encoder = new DefaultLwM2mEncoder();
        identityHandler = new DefaultCoapIdentityHandler();
    }

    private Registration newRegistration() throws UnknownHostException {
        return newRegistration(null);
    }

    private Registration newRegistration(String rootpath) throws UnknownHostException {
        Builder b = new Registration.Builder("regid", "endpoint",
                Identity.unsecure(Inet4Address.getLoopbackAddress(), 12354),
                EndpointUriUtil.createUri("coap://localhost:5683"));
        b.extractDataFromObjectLink(true);
        if (rootpath != null) {
            b.objectLinks(new Link[] { new Link(rootpath, new ResourceTypeAttribute("oma.lwm2m")) });
        }
        return b.build();
    }

    @Test
    public void build_read_request() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        ReadRequest request = new ReadRequest(3, 0);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.GET, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals("coap://127.0.0.1:12354/3/0", coapRequest.getURI());
    }

    @Test
    public void build_read_request_with_non_default_object_path() throws Exception {
        Registration reg = newRegistration("/lwm2m");

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        ReadRequest request = new ReadRequest(3, 0, 1);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals("coap://127.0.0.1:12354/lwm2m/3/0/1", coapRequest.getURI());
    }

    @Test
    public void build_read_request_with_root_path() throws Exception {
        Registration reg = newRegistration("/");

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        ReadRequest request = new ReadRequest(3);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals("coap://127.0.0.1:12354/3", coapRequest.getURI());
    }

    @Test
    public void build_discover_request() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        DiscoverRequest request = new DiscoverRequest(3, 0);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.GET, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals(MediaTypeRegistry.APPLICATION_LINK_FORMAT, coapRequest.getOptions().getAccept());
        assertEquals("coap://127.0.0.1:12354/3/0", coapRequest.getURI());
    }

    @Test
    public void build_write_request() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        WriteRequest request = new WriteRequest(Mode.UPDATE, 3, 0, LwM2mSingleResource.newStringResource(15, "value"));
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.POST, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals(ContentFormat.TLV.getCode(), coapRequest.getOptions().getContentFormat());
        assertNotNull(coapRequest.getPayload());
        // assert it is encoded as array of resources TLV
        Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(coapRequest.getPayload()));
        assertEquals(TlvType.RESOURCE_VALUE, tlvs[0].getType());
        assertEquals("value", TlvDecoder.decodeString(tlvs[0].getValue()));
        assertEquals("coap://127.0.0.1:12354/3/0", coapRequest.getURI());
    }

    @Test
    public void build_write_request_replace() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        WriteRequest request = new WriteRequest(3, 0, 14, "value");
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.PUT, coapRequest.getCode());
    }

    @Test
    public void build_write_attribute_request() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        LwM2mAttributeSet attributes = new LwM2mAttributeSet(
                new LwM2mAttribute<Long>(LwM2mAttributes.MINIMUM_PERIOD, 10L),
                new LwM2mAttribute<Long>(LwM2mAttributes.MAXIMUM_PERIOD, 100L));
        WriteAttributesRequest request = new WriteAttributesRequest(3, 0, 14, attributes);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.PUT, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals("coap://127.0.0.1:12354/3/0/14?pmin=10&pmax=100", coapRequest.getURI());
    }

    @Test
    public void build_unset_write_attribute_request() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        LwM2mAttributeSet attributes = new LwM2mAttributeSet(new LwM2mAttribute<Long>(LwM2mAttributes.MINIMUM_PERIOD),
                new LwM2mAttribute<Long>(LwM2mAttributes.MAXIMUM_PERIOD));
        WriteAttributesRequest request = new WriteAttributesRequest(3, 0, 14, attributes);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.PUT, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals("coap://127.0.0.1:12354/3/0/14?pmin&pmax", coapRequest.getURI());
    }

    @Test
    public void build_execute_request() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        ExecuteRequest request = new ExecuteRequest(3, 0, 12, "0='params'");
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.POST, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals("coap://127.0.0.1:12354/3/0/12", coapRequest.getURI());
        assertEquals("0='params'", coapRequest.getPayloadString());
    }

    @Test
    public void build_create_request__without_instance_id() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        CreateRequest request = new CreateRequest(12, LwM2mSingleResource.newStringResource(0, "value"));
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.POST, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals("coap://127.0.0.1:12354/12", coapRequest.getURI());
        assertEquals(ContentFormat.TLV.getCode(), coapRequest.getOptions().getContentFormat());
        assertNotNull(coapRequest.getPayload());
        // assert it is encoded as array of resources TLV
        Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(coapRequest.getPayload()));
        assertEquals(TlvType.RESOURCE_VALUE, tlvs[0].getType());
    }

    @Test
    public void build_create_request__with_instance_id() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        CreateRequest request = new CreateRequest(12,
                new LwM2mObjectInstance(26, LwM2mSingleResource.newStringResource(0, "value")));
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.POST, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals("coap://127.0.0.1:12354/12", coapRequest.getURI());
        assertEquals(ContentFormat.TLV.getCode(), coapRequest.getOptions().getContentFormat());
        assertNotNull(coapRequest.getPayload());
        // assert it is encoded as array of instance TLV
        Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(coapRequest.getPayload()));
        assertEquals(TlvType.OBJECT_INSTANCE, tlvs[0].getType());
        assertEquals(26, tlvs[0].getIdentifier());
    }

    @Test
    public void build_delete_request() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        DeleteRequest request = new DeleteRequest(12, 0);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.DELETE, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals("coap://127.0.0.1:12354/12/0", coapRequest.getURI());
    }

    @Test
    public void build_observe_request() throws Exception {
        Registration reg = newRegistration();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(reg.getIdentity(), reg.getRootPath(), reg.getId(),
                reg.getEndpoint(), model, encoder, false, null, identityHandler);
        ObserveRequest request = new ObserveRequest(12, 0);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.GET, coapRequest.getCode());
        assertEquals(0, coapRequest.getOptions().getObserve().intValue());
        assertEquals("127.0.0.1", coapRequest.getDestinationContext().getPeerAddress().getAddress().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationContext().getPeerAddress().getPort());
        assertEquals("coap://127.0.0.1:12354/12/0", coapRequest.getURI());
    }
}
