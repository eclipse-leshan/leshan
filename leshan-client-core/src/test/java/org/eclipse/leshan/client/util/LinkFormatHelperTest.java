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

import java.util.List;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.junit.Test;

public class LinkFormatHelperTest {

    @Test
    public void encode_objectModel_to_linkObject_without_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        LinkObject[] linkObjects = LinkFormatHelper.getObjectDescription(locationModel, null);
        String strLinkObjects = LinkObject.serialize(linkObjects);

        assertEquals("</6>, </6/0/0>, </6/0/1>, </6/0/2>, </6/0/3>, </6/0/4>, </6/0/5>", strLinkObjects);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_simple_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        LinkObject[] linkObjects = LinkFormatHelper.getObjectDescription(locationModel, "rp");
        String strLinkObjects = LinkObject.serialize(linkObjects);

        assertEquals("</rp/6>, </rp/6/0/0>, </rp/6/0/1>, </rp/6/0/2>, </rp/6/0/3>, </rp/6/0/4>, </rp/6/0/5>",
                strLinkObjects);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_empty_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        LinkObject[] linkObjects = LinkFormatHelper.getObjectDescription(locationModel, "");
        String strLinkObjects = LinkObject.serialize(linkObjects);

        assertEquals("</6>, </6/0/0>, </6/0/1>, </6/0/2>, </6/0/3>, </6/0/4>, </6/0/5>", strLinkObjects);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_explicit_empty_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        LinkObject[] linkObjects = LinkFormatHelper.getObjectDescription(locationModel, "/");
        String strLinkObjects = LinkObject.serialize(linkObjects);

        assertEquals("</6>, </6/0/0>, </6/0/1>, </6/0/2>, </6/0/3>, </6/0/4>, </6/0/5>", strLinkObjects);
    }

    @Test
    public void encode_objectModel_to_linkObject_with_explicit_complex_root_path() {
        ObjectModel locationModel = getObjectModel(6);

        LinkObject[] linkObjects = LinkFormatHelper.getObjectDescription(locationModel, "/r/t/");
        String strLinkObjects = LinkObject.serialize(linkObjects);

        assertEquals("</r/t/6>, </r/t/6/0/0>, </r/t/6/0/1>, </r/t/6/0/2>, </r/t/6/0/3>, </r/t/6/0/4>, </r/t/6/0/5>",
                strLinkObjects);
    }

    private ObjectModel getObjectModel(int id) {
        List<ObjectModel> objectModels = ObjectLoader.loadDefault();
        for (ObjectModel objectModel : objectModels) {
            if (objectModel.id == id)
                return objectModel;
        }
        return null;
    }
}
