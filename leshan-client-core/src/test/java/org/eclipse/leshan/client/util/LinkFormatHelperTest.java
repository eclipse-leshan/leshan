/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.client.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.BaseInstanceEnablerFactory;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.ContentFormat;
import org.junit.Test;

public class LinkFormatHelperTest {

    @Test
    public void encode_objectModel_to_linkObject_without_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), null);
        String strLinks = Link.serialize(links);

        assertEquals("</6>,</6/0>,</6/0/0>,</6/0/1>,</6/0/2>,</6/0/3>,</6/0/4>,</6/0/5>,</6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_simple_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "rp");
        String strLinks = Link.serialize(links);

        assertEquals(
                "</rp/6>,</rp/6/0>,</rp/6/0/0>,</rp/6/0/1>,</rp/6/0/2>,</rp/6/0/3>,</rp/6/0/4>,</rp/6/0/5>,</rp/6/0/6>",
                strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_empty_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "");
        String strLinks = Link.serialize(links);

        assertEquals("</6>,</6/0>,</6/0/0>,</6/0/1>,</6/0/2>,</6/0/3>,</6/0/4>,</6/0/5>,</6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_explicit_empty_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/");
        String strLinks = Link.serialize(links);

        assertEquals("</6>,</6/0>,</6/0/0>,</6/0/1>,</6/0/2>,</6/0/3>,</6/0/4>,</6/0/5>,</6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_version2_0() {
        ObjectModel locationModel = getVersionedObjectModel(6, "2.0");

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/");
        String strLinks = Link.serialize(links);

        assertEquals("</6>;ver=2.0,</6/0>,</6/0/0>,</6/0/1>,</6/0/2>,</6/0/3>,</6/0/4>,</6/0/5>,</6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_explicit_complex_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/r/t/");
        String strLinks = Link.serialize(links);

        assertEquals(
                "</r/t/6>,</r/t/6/0>,</r/t/6/0/0>,</r/t/6/0/1>,</r/t/6/0/2>,</r/t/6/0/3>,</r/t/6/0/4>,</r/t/6/0/5>,</r/t/6/0/6>",
                strLinks);
    }

    @Test
    public void encode_client_description_with_version_1_0() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        objectEnablers.add(new ObjectEnabler(6, getObjectModel(6), instancesMap, null, ContentFormat.DEFAULT));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null, null);
        String strLinks = Link.serialize(links);

        assertEquals("</>;rt=\"oma.lwm2m\",</6/0>", strLinks);
    }

    @Test
    public void encode_client_description_with_version_2_0() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        instancesMap.put(1, new BaseInstanceEnabler());
        objectEnablers.add(
                new ObjectEnabler(6, getVersionedObjectModel(6, "2.0"), instancesMap, null, ContentFormat.DEFAULT));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null, null);
        String strLinks = Link.serialize(links);

        assertEquals("</>;rt=\"oma.lwm2m\",</6>;ver=2.0,</6/0>,</6/1>", strLinks);
    }

    @Test
    public void encode_client_description_with_version_2_0_no_instances() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        objectEnablers.add(
                new ObjectEnabler(6, getVersionedObjectModel(6, "2.0"), instancesMap, null, ContentFormat.DEFAULT));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null, null);
        String strLinks = Link.serialize(links);

        assertEquals("</>;rt=\"oma.lwm2m\",</6>;ver=2.0", strLinks);
    }

    @Test
    public void encode_1_content_format() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();
        Map<Integer, LwM2mInstanceEnabler> instancesMap = Collections.emptyMap();
        objectEnablers.add(
                new ObjectEnabler(6, getVersionedObjectModel(6, "1.0"), instancesMap, null, ContentFormat.DEFAULT));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null, Arrays.asList(ContentFormat.TLV));

        assertArrayEquals(Link.parse("</>;rt=\"oma.lwm2m\";ct=11542,</6>".getBytes()), links);
    }

    @Test
    public void encode_several_content_format() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();
        Map<Integer, LwM2mInstanceEnabler> instancesMap = Collections.emptyMap();
        objectEnablers.add(
                new ObjectEnabler(6, getVersionedObjectModel(6, "1.0"), instancesMap, null, ContentFormat.DEFAULT));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null,
                Arrays.asList(ContentFormat.TLV, ContentFormat.JSON, ContentFormat.OPAQUE));

        assertArrayEquals(Link.parse("</>;rt=\"oma.lwm2m\";ct=\"11542 11543 42\",</6>".getBytes()), links);
    }

    @Test
    public void encode_bootstrap_object() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        ObjectEnabler objectEnabler = new ObjectEnabler(3, getObjectModel(3), instancesMap, null,
                ContentFormat.DEFAULT);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = Link.serialize(links);

        assertEquals("</>;lwm2m=1.0,</3/0>", strLinks);
    }

    @Test
    public void encode_bootstrap_object_with_version_and_no_instance() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        ObjectEnabler objectEnabler = new ObjectEnabler(3, getVersionedObjectModel(3, "2.0"), instancesMap, null,
                ContentFormat.DEFAULT);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = Link.serialize(links);

        assertEquals("</>;lwm2m=1.0,</3>;ver=2.0", strLinks);
    }

    @Test
    public void encode_bootstrap_object_with_version_and_instance() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        ObjectEnabler objectEnabler = new ObjectEnabler(3, getVersionedObjectModel(3, "2.0"), instancesMap, null,
                ContentFormat.DEFAULT);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = Link.serialize(links);

        assertEquals("</>;lwm2m=1.0,</3>;ver=2.0,</3/0>", strLinks);
    }

    @Test
    public void encode_bootstrap_server_object() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new Server(333, 120));
        ObjectEnabler objectEnabler = new ObjectEnabler(1, getObjectModel(1), instancesMap, null,
                ContentFormat.DEFAULT);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = Link.serialize(links);

        assertEquals("</>;lwm2m=1.0,</1/0>;ssid=333", strLinks);
    }

    @Test
    public void encode_bootstrap_server_object_with_version() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new Server(333, 120));
        ObjectEnabler objectEnabler = new ObjectEnabler(1, getVersionedObjectModel(1, "2.0"), instancesMap, null,
                ContentFormat.DEFAULT);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = Link.serialize(links);

        assertEquals("</>;lwm2m=1.0,</1>;ver=2.0,</1/0>;ssid=333", strLinks);
    }

    @Test
    public void encode_bootstrap_security_object() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, Security.noSec("coap://localhost:11", 111));
        instancesMap.put(1, Security.noSecBootstap("coap://localhost:1"));
        instancesMap.put(2, Security.noSec("coap://localhost:22", 222));
        instancesMap.put(3, Security.noSec("coap://localhost:33", 333));
        ObjectEnabler objectEnabler = new ObjectEnabler(0, getObjectModel(0), instancesMap, null,
                ContentFormat.DEFAULT);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = Link.serialize(links);

        assertEquals("</>;lwm2m=1.0,</0/0>;ssid=111;uri=\"coap://localhost:11\"," //
                + "</0/1>;uri=\"coap://localhost:1\"," //
                + "</0/2>;ssid=222;uri=\"coap://localhost:22\"," //
                + "</0/3>;ssid=333;uri=\"coap://localhost:33\"" //
                , strLinks);

    }

    @Test
    public void encode_bootstrap_root() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        // object 0
        Map<Integer, LwM2mInstanceEnabler> securityInstances = new HashMap<>();
        securityInstances.put(0, Security.noSec("coap://localhost:11", 111));
        securityInstances.put(1, Security.noSecBootstap("coap://localhost:1"));
        securityInstances.put(2, Security.noSec("coap://localhost:22", 222));
        securityInstances.put(3, Security.noSec("coap://localhost:33", 333));
        ObjectEnabler securityObjectEnabler = new ObjectEnabler(0, getObjectModel(0), securityInstances, null,
                ContentFormat.DEFAULT);
        objectEnablers.add(securityObjectEnabler);

        // object 1
        Map<Integer, LwM2mInstanceEnabler> serverInstances = new HashMap<>();
        serverInstances.put(0, new Server(333, 120));
        ObjectEnabler serverObjectEnabler = new ObjectEnabler(1, getVersionedObjectModel(1, "2.0"), serverInstances,
                null, ContentFormat.DEFAULT);
        objectEnablers.add(serverObjectEnabler);

        // object 2
        ObjectEnabler aclObjectEnabler = new ObjectEnabler(2, getVersionedObjectModel(2, "2.0"),
                new HashMap<Integer, LwM2mInstanceEnabler>(), null, ContentFormat.DEFAULT);
        objectEnablers.add(aclObjectEnabler);

        // object 3
        Map<Integer, LwM2mInstanceEnabler> deviceInstances = new HashMap<>();
        deviceInstances.put(0, new BaseInstanceEnabler());
        ObjectEnabler deviceObjectEnabler = new ObjectEnabler(3, getObjectModel(3), deviceInstances, null,
                ContentFormat.DEFAULT);
        objectEnablers.add(deviceObjectEnabler);

        Link[] links = LinkFormatHelper.getBootstrapClientDescription(objectEnablers);
        String strLinks = Link.serialize(links);

        assertEquals("</>;lwm2m=1.0,</0/0>;ssid=111;uri=\"coap://localhost:11\"," //
                + "</0/1>;uri=\"coap://localhost:1\"," //
                + "</0/2>;ssid=222;uri=\"coap://localhost:22\"," //
                + "</0/3>;ssid=333;uri=\"coap://localhost:33\"," //
                + "</1>;ver=2.0,</1/0>;ssid=333,</2>;ver=2.0,</3/0>", strLinks);
    }

    private ObjectModel getObjectModel(int id) {
        List<ObjectModel> objectModels = ObjectLoader.loadDefault(LwM2mVersion.V1_0);
        for (ObjectModel objectModel : objectModels) {
            if (objectModel.id == id)
                return objectModel;
        }
        return null;
    }

    /**
     * Gets a default object model by id and manipulates its version.
     */
    private ObjectModel getVersionedObjectModel(int id, String version) {
        List<ObjectModel> objectModels = ObjectLoader.loadDefault(LwM2mVersion.V1_0);
        for (ObjectModel om : objectModels) {
            if (om.id == id)
                return new ObjectModel(om.id, om.name, om.description, version, om.multiple, om.mandatory,
                        om.resources.values());
        }
        return null;
    }

    /**
     * create a objectEnabler with 1 instance of the given model
     */
    private LwM2mObjectEnabler createObjectEnabler(ObjectModel objectModel) {
        // create factory
        BaseInstanceEnablerFactory factory = new BaseInstanceEnablerFactory() {
            @Override
            public LwM2mInstanceEnabler create() {
                return new DummyInstanceEnabler();
            }
        };

        // create first instance
        Map<Integer, LwM2mInstanceEnabler> instances = new HashMap<>();
        LwM2mInstanceEnabler instance = factory.create();
        instance.setId(0);
        instance.setModel(objectModel);
        instances.put(0, instance);

        // create objectEnabler
        return new ObjectEnabler(objectModel.id, objectModel, instances, factory, ContentFormat.TLV);
    }
}
