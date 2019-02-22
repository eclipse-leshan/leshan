/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.Link;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.BaseInstanceEnablerFactory;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
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

        assertEquals("</6>, </6/0>, </6/0/0>, </6/0/1>, </6/0/2>, </6/0/3>, </6/0/4>, </6/0/5>, </6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_simple_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "rp");
        String strLinks = Link.serialize(links);

        assertEquals(
                "</rp/6>, </rp/6/0>, </rp/6/0/0>, </rp/6/0/1>, </rp/6/0/2>, </rp/6/0/3>, </rp/6/0/4>, </rp/6/0/5>, </rp/6/0/6>",
                strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_empty_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "");
        String strLinks = Link.serialize(links);

        assertEquals("</6>, </6/0>, </6/0/0>, </6/0/1>, </6/0/2>, </6/0/3>, </6/0/4>, </6/0/5>, </6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_explicit_empty_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/");
        String strLinks = Link.serialize(links);

        assertEquals("</6>, </6/0>, </6/0/0>, </6/0/1>, </6/0/2>, </6/0/3>, </6/0/4>, </6/0/5>, </6/0/6>", strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_version2_0() {
        ObjectModel locationModel = getVersionedObjectModel(6, "2.0");

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/");
        String strLinks = Link.serialize(links);

        assertEquals("</6>;ver=\"2.0\", </6/0>, </6/0/0>, </6/0/1>, </6/0/2>, </6/0/3>, </6/0/4>, </6/0/5>, </6/0/6>",
                strLinks);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_explicit_complex_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        Link[] links = LinkFormatHelper.getObjectDescription(createObjectEnabler(locationModel), "/r/t/");
        String strLinks = Link.serialize(links);

        assertEquals(
                "</r/t/6>, </r/t/6/0>, </r/t/6/0/0>, </r/t/6/0/1>, </r/t/6/0/2>, </r/t/6/0/3>, </r/t/6/0/4>, </r/t/6/0/5>, </r/t/6/0/6>",
                strLinks);
    }

    @Test
    public void encode_client_description_with_version_1_0() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        objectEnablers.add(new ObjectEnabler(6, getObjectModel(6), instancesMap, null, ContentFormat.DEFAULT));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null);
        String strLinks = Link.serialize(links);

        assertEquals("</>;rt=\"oma.lwm2m\", </6/0>", strLinks);
    }

    @Test
    public void encode_client_description_with_version_2_0() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        instancesMap.put(0, new BaseInstanceEnabler());
        instancesMap.put(1, new BaseInstanceEnabler());
        objectEnablers.add(
                new ObjectEnabler(6, getVersionedObjectModel(6, "2.0"), instancesMap, null, ContentFormat.DEFAULT));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null);
        String strLinks = Link.serialize(links);

        assertEquals("</>;rt=\"oma.lwm2m\", </6>;ver=\"2.0\", </6/0>, </6/1>", strLinks);
    }

    @Test
    public void encode_client_description_with_version_2_0_no_instances() {
        List<LwM2mObjectEnabler> objectEnablers = new ArrayList<>();

        Map<Integer, LwM2mInstanceEnabler> instancesMap = new HashMap<>();
        objectEnablers.add(
                new ObjectEnabler(6, getVersionedObjectModel(6, "2.0"), instancesMap, null, ContentFormat.DEFAULT));

        Link[] links = LinkFormatHelper.getClientDescription(objectEnablers, null);
        String strLinks = Link.serialize(links);

        assertEquals("</>;rt=\"oma.lwm2m\", </6>;ver=\"2.0\"", strLinks);
    }

    private ObjectModel getObjectModel(int id) {
        List<ObjectModel> objectModels = ObjectLoader.loadDefault();
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
        List<ObjectModel> objectModels = ObjectLoader.loadDefault();
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
