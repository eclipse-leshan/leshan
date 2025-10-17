/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server.redis.serialization;

import static org.eclipse.leshan.core.util.TestToolBox.uriHandler;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.AttributeSet;
import org.eclipse.leshan.core.link.attributes.ContentFormatAttribute;
import org.eclipse.leshan.core.link.attributes.QuotedStringAttribute;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.link.attributes.UnquotedStringAttribute;
import org.eclipse.leshan.core.link.attributes.ValuelessAttribute;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.registration.DefaultRegistrationDataExtractor;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationDataExtractor.RegistrationData;
import org.junit.jupiter.api.Test;

class RegistrationSerDesTest {

    private final RegistrationSerDes registrationSerDes = new RegistrationSerDes();

    @Test
    void ser_and_des_are_equals() {
        Link[] objs = new Link[2];
        AttributeSet attrs = new AttributeSet( //
                new UnquotedStringAttribute("us", "12"), //
                new QuotedStringAttribute("sq", "test"), //
                new ResourceTypeAttribute("oma.lwm2m"), //
                new ContentFormatAttribute(ContentFormat.CBOR, ContentFormat.JSON), //
                new ValuelessAttribute("hb"));
        objs[0] = new Link("/0/1024/2", attrs);
        objs[1] = new Link("/0/2");

        Registration.Builder builder = new Registration.Builder("registrationId", "endpoint",
                new IpPeer(new InetSocketAddress(Inet4Address.getLoopbackAddress(), 1)),
                uriHandler.createUri("coap://localhost:5683")).objectLinks(objs).rootPath("/")
                        .supportedContentFormats(ContentFormat.TLV, ContentFormat.TEXT);
        builder.registrationDate(new Date(100L));
        builder.lastUpdate(new Date(101L));

        RegistrationData dataFromObjectLinks = new DefaultRegistrationDataExtractor().extractDataFromObjectLinks(objs,
                LwM2mVersion.V1_0);
        builder.rootPath(dataFromObjectLinks.getAlternatePath());
        builder.supportedContentFormats(dataFromObjectLinks.getSupportedContentFormats());
        builder.supportedObjects(dataFromObjectLinks.getSupportedObjects());
        builder.availableInstances(dataFromObjectLinks.getAvailableInstances());

        IRegistration r = builder.build();

        byte[] ser = registrationSerDes.bSerialize(r);
        IRegistration r2 = registrationSerDes.deserialize(ser);

        assertEquals(r, r2);
    }

    @Test
    void ser_and_des_are_equals_with_app_data() {
        Link[] objs = new Link[2];
        AttributeSet attrs = new AttributeSet( //
                new UnquotedStringAttribute("us", "12"), //
                new QuotedStringAttribute("qt", "test"), //
                new ResourceTypeAttribute("oma.lwm2m"), //
                new ContentFormatAttribute(ContentFormat.CBOR, ContentFormat.JSON), //
                new ValuelessAttribute("hb"));
        objs[0] = new Link("/0/1024/2", attrs);
        objs[1] = new Link("/0/2");

        Map<String, String> appData = new HashMap<>();
        appData.put("string", "string test");
        appData.put("null", null);

        Registration.Builder builder = new Registration.Builder("registrationId", "endpoint",
                new IpPeer(new InetSocketAddress(Inet4Address.getLoopbackAddress(), 1)),
                uriHandler.createUri("coap://localhost:5683")).objectLinks(objs).rootPath("/")
                        .supportedContentFormats(ContentFormat.TLV, ContentFormat.TEXT).customRegistrationData(appData);

        builder.registrationDate(new Date(100L));
        builder.lastUpdate(new Date(101L));

        RegistrationData dataFromObjectLinks = new DefaultRegistrationDataExtractor().extractDataFromObjectLinks(objs,
                LwM2mVersion.V1_0);
        builder.rootPath(dataFromObjectLinks.getAlternatePath());
        builder.supportedContentFormats(dataFromObjectLinks.getSupportedContentFormats());
        builder.supportedObjects(dataFromObjectLinks.getSupportedObjects());
        builder.availableInstances(dataFromObjectLinks.getAvailableInstances());

        IRegistration r = builder.build();

        byte[] ser = registrationSerDes.bSerialize(r);
        IRegistration r2 = registrationSerDes.deserialize(ser);

        assertEquals(r, r2);
    }
}
