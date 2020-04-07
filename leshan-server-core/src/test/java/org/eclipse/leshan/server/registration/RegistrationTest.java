/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.util.Map;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.registration.Registration.Builder;
import org.junit.Test;

public class RegistrationTest {

    String tto = "</>;rt=\"oma.lwm2m\";ct=100, </1/101>,</1/102>, </2/0>, </2/1> ;empty";

    @Test
    public void test_supported_object_given_an_object_link_without_version() {
        Registration reg = given_a_registration_with_object_link_like("</1/0>,</3/0>");

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(2, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(3));
    }

    @Test
    public void test_supported_object_given_an_object_link_with_rootpath() {
        Registration reg = given_a_registration_with_object_link_like("</root>;rt=\"oma.lwm2m\", </root/1/0>,</3/0>");

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(1, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertNull(supportedObject.get(3));
    }

    @Test
    public void test_supported_object_given_an_object_link_with_unquoted_rootpath() {
        Registration reg = given_a_registration_with_object_link_like("</root>;rt=oma.lwm2m, </root/1/0>,</3/0>");

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(1, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertNull(supportedObject.get(3));
    }

    @Test
    public void test_supported_object_given_an_object_link_with_regexp_rootpath() {
        Registration reg = given_a_registration_with_object_link_like(
                "</r(\\d+)oot>;rt=\"oma.lwm2m\", </r(\\d+)oot/1/0>,</3/0>");

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(1, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertNull(supportedObject.get(3));
    }

    @Test
    public void test_supported_object_given_an_object_link_with_version() {
        Registration reg = given_a_registration_with_object_link_like("</1/0>,</3>;ver=\"1.1\",</3/0>");

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(2, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertEquals("1.1", supportedObject.get(3));
    }

    @Test
    public void test_supported_object_given_an_object_link_with_not_lwm2m_url() {
        Registration reg = given_a_registration_with_object_link_like(
                "<text>,</1/text/0/in/path>,empty,</2/O/test/in/path>,</3/0>;ver=\"1.1\",<4/0/0/>");

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(1, supportedObject.size());
        assertEquals("1.1", supportedObject.get(3));
    }

    private Registration given_a_registration_with_object_link_like(String objectLinks) {
        Builder builder = new Registration.Builder("id", "endpoin",
                Identity.unsecure(InetSocketAddress.createUnresolved("localhost", 0)));

        builder.objectLinks(Link.parse(objectLinks.getBytes()));
        return builder.build();
    }
}
