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
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.registration.Registration.Builder;
import org.junit.Test;

public class RegistrationTest {

    @Test
    public void test_object_links_without_version_nor_rootpath() {
        Registration reg = given_a_registration_with_object_link_like("</1/0>,</3/0>");

        // check root path
        assertEquals("/", reg.getRootPath());

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(2, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(3));

        // ensure available instances are correct
        Set<LwM2mPath> availableInstances = reg.getAvailableInstances();
        assertEquals(2, availableInstances.size());
        assertTrue(availableInstances.containsAll(Arrays.asList(new LwM2mPath(1, 0), new LwM2mPath(3, 0))));
    }

    @Test
    public void test_object_links_with_ct_but_with_rt() {
        Registration reg = given_a_registration_with_object_link_like("</>;ct=0 42 11543,</1/0>,</3/0>");

        // check root path
        assertEquals("/", reg.getRootPath());

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(2, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(3));

        // Check Supported Content format
        Set<ContentFormat> supportedContentFormats = reg.getSupportedContentFormats();
        assertEquals(4, supportedContentFormats.size()); // 3 + 1 mandatory TLV content format for LWM2M v1.0
        assertTrue(supportedContentFormats.containsAll(
                Arrays.asList(ContentFormat.TLV, ContentFormat.TEXT, ContentFormat.OPAQUE, ContentFormat.JSON)));

        // ensure available instances are correct
        Set<LwM2mPath> availableInstances = reg.getAvailableInstances();
        assertEquals(2, availableInstances.size());
        assertTrue(availableInstances.containsAll(Arrays.asList(new LwM2mPath(1, 0), new LwM2mPath(3, 0))));
    }

    @Test
    public void test_object_links_with_default_rootpath() {
        Registration reg = given_a_registration_with_object_link_like(
                "</>;rt=\"oma.lwm2m\";ct=0 42 11543,</1/0>,</3/0>");

        // check root path
        assertEquals("/", reg.getRootPath());

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(2, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(3));

        // Check Supported Content format
        Set<ContentFormat> supportedContentFormats = reg.getSupportedContentFormats();
        assertEquals(4, supportedContentFormats.size()); // 3 + 1 mandatory TLV content format for LWM2M v1.0
        assertTrue(supportedContentFormats.containsAll(
                Arrays.asList(ContentFormat.TLV, ContentFormat.TEXT, ContentFormat.OPAQUE, ContentFormat.JSON)));

        // ensure available instances are correct
        Set<LwM2mPath> availableInstances = reg.getAvailableInstances();
        assertEquals(2, availableInstances.size());
        assertTrue(availableInstances.containsAll(Arrays.asList(new LwM2mPath(1, 0), new LwM2mPath(3, 0))));
    }

    @Test
    public void test_object_links_with_rootpath() {
        Registration reg = given_a_registration_with_object_link_like("</root>;rt=\"oma.lwm2m\", </root/1/0>,</3/0>");

        // check root path
        assertEquals("/root/", reg.getRootPath());

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(1, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertNull(supportedObject.get(3));

        // ensure available instances are correct
        Set<LwM2mPath> availableInstances = reg.getAvailableInstances();
        assertEquals(1, availableInstances.size());
        assertTrue(availableInstances.containsAll(Arrays.asList(new LwM2mPath(1, 0))));
    }

    @Test
    public void test_object_links_with_unquoted_rootpath() {
        Registration reg = given_a_registration_with_object_link_like("</root>;rt=oma.lwm2m, </root/1/0>,</3/0>");

        // check root path
        assertEquals("/root/", reg.getRootPath());

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(1, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertNull(supportedObject.get(3));

        // ensure available instances are correct
        Set<LwM2mPath> availableInstances = reg.getAvailableInstances();
        assertEquals(1, availableInstances.size());
        assertTrue(availableInstances.containsAll(Arrays.asList(new LwM2mPath(1, 0))));
    }

    @Test
    public void test_object_links_with_regexp_rootpath() {
        Registration reg = given_a_registration_with_object_link_like(
                "</r(\\d+)oot>;rt=\"oma.lwm2m\", </r(\\d+)oot/1/0>,</3/0>");

        // check root path
        assertEquals("/r(\\d+)oot/", reg.getRootPath());

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(1, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertNull(supportedObject.get(3));

        // ensure available instances are correct
        Set<LwM2mPath> availableInstances = reg.getAvailableInstances();
        assertEquals(1, availableInstances.size());
        assertTrue(availableInstances.containsAll(Arrays.asList(new LwM2mPath(1, 0))));
    }

    @Test
    public void test_object_links_with_version() {
        Registration reg = given_a_registration_with_object_link_like("</1/0>,</3>;ver=\"1.1\",</3/0>");

        // check root path
        assertEquals("/", reg.getRootPath());

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(2, supportedObject.size());
        assertEquals(ObjectModel.DEFAULT_VERSION, supportedObject.get(1));
        assertEquals("1.1", supportedObject.get(3));

        // ensure available instances are correct
        Set<LwM2mPath> availableInstances = reg.getAvailableInstances();
        assertEquals(2, availableInstances.size());
        assertTrue(availableInstances.containsAll(Arrays.asList(new LwM2mPath(1, 0), new LwM2mPath(3, 0))));
    }

    @Test
    public void test_object_links_with_text_in_not_lwm2m_path() {
        Registration reg = given_a_registration_with_object_link_like(
                "</root>;rt=\"oma.lwm2m\",<text>,</1/text/0/in/path>,empty,</2/O/test/in/path>,</root/3/0>;ver=\"1.1\",</root/4/0/0/>");

        // check root path
        assertEquals("/root/", reg.getRootPath());

        // Ensure supported objects are correct
        Map<Integer, String> supportedObject = reg.getSupportedObject();
        assertEquals(1, supportedObject.size());
        assertEquals("1.1", supportedObject.get(3));

        // ensure available instances are correct
        Set<LwM2mPath> availableInstances = reg.getAvailableInstances();
        assertEquals(1, availableInstances.size());
        assertTrue(availableInstances.containsAll(Arrays.asList(new LwM2mPath(3, 0))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_object_links_with_text_in_lwm2m_path() {
        given_a_registration_with_object_link_like(
                "<text>,</1/text/0/in/path>,empty,</2/O/test/in/path>,</3/0>;ver=\"1.1\",</4/0/0/>");
    }

    private Registration given_a_registration_with_object_link_like(String objectLinks) {
        Builder builder = new Registration.Builder("id", "endpoint",
                Identity.unsecure(InetSocketAddress.createUnresolved("localhost", 0)));
        builder.extractDataFromObjectLink(true);
        builder.objectLinks(Link.parse(objectLinks.getBytes()));
        return builder.build();
    }
}
