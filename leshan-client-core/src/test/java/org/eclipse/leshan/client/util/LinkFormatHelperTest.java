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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.client.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.client.object.Oscore;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.BaseInstanceEnablerFactory;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.link.DefaultLinkSerializer;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.oscore.OscoreSetting;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Hex;
import org.junit.jupiter.api.Test;

public class LinkFormatHelperTest {

    private final LinkParser parser = new DefaultLwM2mLinkParser();
    private final LinkSerializer serializer = new DefaultLinkSerializer();

    @Test
    public void encode_objectModel_to_linkObject_without_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), null);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</6>,</6/0>,</6/0/0>,</6/0/1>,</6/0/2>,</6/0/3>,</6/0/4>,</6/0/5>,</6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_simple_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/rp");
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals(
                "</rp/6>,</rp/6/0>,</rp/6/0/0>,</rp/6/0/1>,</rp/6/0/2>,</rp/6/0/3>,</rp/6/0/4>,</rp/6/0/5>,</rp/6/0/6>",
                strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_empty_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "");
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</6>,</6/0>,</6/0/0>,</6/0/1>,</6/0/2>,</6/0/3>,</6/0/4>,</6/0/5>,</6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_explicit_empty_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/");
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</6>,</6/0>,</6/0/0>,</6/0/1>,</6/0/2>,</6/0/3>,</6/0/4>,</6/0/5>,</6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_version2_0() {
        ObjectModel locationModel = getVersionedObjectModel(6, "2.0");

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/");
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</6>;ver=2.0,</6/0>,</6/0/0>,</6/0/1>,</6/0/2>,</6/0/3>,</6/0/4>,</6/0/5>,</6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_explicit_complex_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/r/t");
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals(
                "</r/t/6>,</r/t/6/0>,</r/t/6/0/0>,</r/t/6/0/1>,</r/t/6/0/2>,</r/t/6/0/3>,</r/t/6/0/4>,</r/t/6/0/5>,</r/t/6/0/6>",
                strLinks);
    }

    @Test
    public void encode_client_description_with_version_1_0() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        objectEnablers.add(createObjectEnabler(getObjectModel(6), instancesMap));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null, null);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</>;rt=\"oma.lwm2m\",</6/0>", strLinks);
    }

    @Test
    public void encode_client_description_with_version_2_0() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        instancesMap.put(1, new BaseInstanceEnabler());
        objectEnablers.add(createObjectEnabler(getVersionedObjectModel(6, "2.0"), instancesMap));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null, null);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</>;rt=\"oma.lwm2m\",</6>;ver=2.0,</6/0>,</6/1>", strLinks);
    }

    @Test
    public void encode_client_description_with_version_2_0_no_instances() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        objectEnablers.add(createObjectEnabler(getVersionedObjectModel(6, "2.0"), instancesMap));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null, null);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</>;rt=\"oma.lwm2m\",</6>;ver=2.0", strLinks);
    }

    @Test
    public void encode_1_content_format() throws LinkParseException {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();
        Map<Integer, LwM2mInstanceEnabler> instancesMap = Collections.emptyMap();
        objectEnablers.add(createObjectEnabler(getVersionedObjectModel(6, "1.0"), instancesMap));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null, Arrays.asList(ContentFormat.TLV));

        assertArrayEquals(parser.parseCoreLinkFormat("</>;rt=\"oma.lwm2m\";ct=11542,</6>".getBytes()), links);
    }

    @Test
    public void encode_several_content_format() throws LinkParseException {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();
        Map<Integer, LwM2mInstanceEnabler> instancesMap = Collections.emptyMap();
        objectEnablers.add(createObjectEnabler(getVersionedObjectModel(6, "1.0"), instancesMap));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null,
                Arrays.asList(ContentFormat.TLV, ContentFormat.JSON, ContentFormat.OPAQUE));

        assertArrayEquals(parser.parseCoreLinkFormat("</>;rt=\"oma.lwm2m\";ct=\"11542 11543 42\",</6>".getBytes()),
                links);
    }

    @Test
    public void encode_bootstrap_object() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        LwM2mObjectEnabler objectEnabler = createObjectEnabler(getObjectModel(3), instancesMap);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</>;lwm2m=1.0,</3/0>", strLinks);
    }

    @Test
    public void encode_bootstrap_object_with_version_and_no_instance() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        LwM2mObjectEnabler objectEnabler = createObjectEnabler(getVersionedObjectModel(3, "2.0"), instancesMap);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</>;lwm2m=1.0,</3>;ver=2.0", strLinks);
    }

    @Test
    public void encode_bootstrap_object_with_version_and_instance() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        LwM2mObjectEnabler objectEnabler = createObjectEnabler(getVersionedObjectModel(3, "2.0"), instancesMap);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</>;lwm2m=1.0,</3>;ver=2.0,</3/0>", strLinks);
    }

    @Test
    public void encode_bootstrap_server_object() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new Server(333, 120));
        LwM2mObjectEnabler objectEnabler = createObjectEnabler(getObjectModel(1), instancesMap);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</>;lwm2m=1.0,</1/0>;ssid=333", strLinks);
    }

    @Test
    public void encode_bootstrap_server_object_with_version() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new Server(333, 120));
        LwM2mObjectEnabler objectEnabler = createObjectEnabler(getVersionedObjectModel(1, "2.0"), instancesMap);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        assertEquals("</>;lwm2m=1.0,</1>;ver=2.0,</1/0>;ssid=333", strLinks);
    }

    @Test
    public void encode_bootstrap_security_object() {
        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, Security.noSec("coap://localhost:11", 111));
        instancesMap.put(1, Security.noSecBootstrap("coap://localhost:1"));
        instancesMap.put(2, Security.noSec("coap://localhost:22", 222));
        instancesMap.put(3, Security.noSec("coap://localhost:33", 333));
        LwM2mObjectEnabler objectEnabler = new ObjectEnabler(0, getObjectModel(0), instancesMap, null,
                ContentFormat.DEFAULT);

        Link[] links = LinkFormatHelper.getBootstrapObjectDescription(objectEnabler);
        String strLinks = serializer.serializeCoreLinkFormat(links);

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
        securityInstances.put(1, Security.noSecBootstrap("coap://localhost:1"));
        securityInstances.put(2, Security.noSec("coap://localhost:22", 222));
        securityInstances.put(3, Security.noSec("coap://localhost:33", 333));
        LwM2mObjectEnabler securityObjectEnabler = createObjectEnabler(getObjectModel(0), securityInstances);
        objectEnablers.add(securityObjectEnabler);

        // object 1
        Map<Integer, LwM2mInstanceEnabler> serverInstances = new HashMap<>();
        serverInstances.put(0, new Server(333, 120));
        LwM2mObjectEnabler serverObjectEnabler = createObjectEnabler(getVersionedObjectModel(1, "2.0"),
                serverInstances);
        objectEnablers.add(serverObjectEnabler);

        // object 2
        LwM2mObjectEnabler aclObjectEnabler = createObjectEnabler(getVersionedObjectModel(2, "2.0"),
                new HashMap<Integer, LwM2mInstanceEnabler>());
        objectEnablers.add(aclObjectEnabler);

        // object 3
        Map<Integer, LwM2mInstanceEnabler> deviceInstances = new HashMap<>();
        deviceInstances.put(0, new BaseInstanceEnabler());
        LwM2mObjectEnabler deviceObjectEnabler = createObjectEnabler(getObjectModel(3), deviceInstances);
        objectEnablers.add(deviceObjectEnabler);

        Link[] links = LinkFormatHelper.getBootstrapClientDescription(objectEnablers);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        // TODO : handle version correctly
        assertEquals("</>;lwm2m=1.0,</0/0>;ssid=111;uri=\"coap://localhost:11\"," //
                + "</0/1>;uri=\"coap://localhost:1\"," //
                + "</0/2>;ssid=222;uri=\"coap://localhost:22\"," //
                + "</0/3>;ssid=333;uri=\"coap://localhost:33\"," //
                + "</1>;ver=2.0,</1/0>;ssid=333,</2>;ver=2.0,</3/0>", strLinks);
    }

    @Test
    public void encode_bootstrap_root_with_oscore() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        // object 0
        Map<Integer, LwM2mInstanceEnabler> securityInstances = new HashMap<>();
        securityInstances.put(0, Security.oscoreOnly("coap://localhost:11", 111, 10));
        securityInstances.put(1, Security.oscoreOnlyBootstrap("coap://localhost:1", 11));
        securityInstances.put(2, Security.noSec("coap://localhost:22", 222));
        LwM2mObjectEnabler securityObjectEnabler = createObjectEnabler(getObjectModel(0, "1.2"), securityInstances);
        objectEnablers.add(securityObjectEnabler);

        // object 1
        Map<Integer, LwM2mInstanceEnabler> serverInstances = new HashMap<>();
        serverInstances.put(0, new Server(333, 120));
        LwM2mObjectEnabler serverObjectEnabler = createObjectEnabler(getVersionedObjectModel(1, "2.0"),
                serverInstances);
        objectEnablers.add(serverObjectEnabler);

        // object 3
        Map<Integer, LwM2mInstanceEnabler> deviceInstances = new HashMap<>();
        deviceInstances.put(0, new BaseInstanceEnabler());
        LwM2mObjectEnabler deviceObjectEnabler = createObjectEnabler(getObjectModel(3), deviceInstances);
        objectEnablers.add(deviceObjectEnabler);

        // object 21
        Map<Integer, LwM2mInstanceEnabler> oscoreInstances = new HashMap<>();
        oscoreInstances.put(10, new Oscore(10, new OscoreSetting(Hex.decodeHex("AA".toCharArray()),
                Hex.decodeHex("BB".toCharArray()), Hex.decodeHex("CC".toCharArray()))));
        oscoreInstances.put(11, new Oscore(11, new OscoreSetting(Hex.decodeHex("11".toCharArray()),
                Hex.decodeHex("22".toCharArray()), Hex.decodeHex("33".toCharArray()))));
        LwM2mObjectEnabler oscoreObjectEnabler = createObjectEnabler(getObjectModel(21, "2.0"), oscoreInstances);
        objectEnablers.add(oscoreObjectEnabler);

        Link[] links = LinkFormatHelper.getBootstrapClientDescription(objectEnablers);
        String strLinks = serializer.serializeCoreLinkFormat(links);

        // TODO : handle version correctly
        assertEquals("</>;lwm2m=1.0,</0>;ver=1.2,</0/0>;ssid=111;uri=\"coap://localhost:11\"," //
                + "</0/1>;uri=\"coap://localhost:1\"," //
                + "</0/2>;ssid=222;uri=\"coap://localhost:22\"," //
                + "</1>;ver=2.0,</1/0>;ssid=333,</3/0>," //
                + "</21>;ver=2.0," //
                + "</21/10>;ssid=111;uri=\"coap://localhost:11\"," //
                + "</21/11>;uri=\"coap://localhost:1\"" //
                , strLinks);
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

    private ObjectModel getObjectModel(int id, String version) {
        List<ObjectModel> objectModels = ObjectLoader.loadAllDefault();
        for (ObjectModel om : objectModels) {
            if (om.id == id && version.equals(om.version))
                return om;
        }
        return null;
    }

    /**
     * create a objectEnabler with 1 instance of the given model
     */
    private LwM2mObjectEnabler createObjectEnabler(ObjectModel objectModel,
            Map<Integer, LwM2mInstanceEnabler> instances) {

        // create factory
        BaseInstanceEnablerFactory factory = new BaseInstanceEnablerFactory() {
            @Override
            public LwM2mInstanceEnabler create() {
                return new DummyInstanceEnabler();
            }
        };

        // set-up instances
        for (Entry<Integer, LwM2mInstanceEnabler> instanceEntry : instances.entrySet()) {
            instanceEntry.getValue().setId(instanceEntry.getKey());
            instanceEntry.getValue().setModel(objectModel);
        }

        // create enabler;
        return new ObjectEnabler(objectModel.id, objectModel, instances, factory, ContentFormat.DEFAULT);
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
