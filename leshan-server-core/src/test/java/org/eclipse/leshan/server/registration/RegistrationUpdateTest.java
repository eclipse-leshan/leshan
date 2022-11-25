/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.queue.PresenceService;
import org.junit.Assert;
import org.junit.Test;

/**
 * tests the implementation of {@link PresenceService}
 *
 */
public class RegistrationUpdateTest {

    @Test
    public void testAdditionalAttributesUpdate() throws Exception {
        Registration.Builder builder = new Registration.Builder("registrationId", "endpoint",
                Identity.unsecure(Inet4Address.getLocalHost(), 1));

        Map<String, String> additionalAttributes = new HashMap<String, String>();
        additionalAttributes.put("x", "1");
        additionalAttributes.put("y", "10");
        additionalAttributes.put("z", "100");
        builder.additionalRegistrationAttributes(additionalAttributes);

        Registration r = builder.build();

        Map<String, String> updateAdditionalAttributes = new HashMap<String, String>();
        updateAdditionalAttributes.put("x", "2");
        updateAdditionalAttributes.put("y", "11");
        updateAdditionalAttributes.put("z", "101");
        updateAdditionalAttributes.put("h", "hello");

        RegistrationUpdate updateReg = new RegistrationUpdate(r.getId(), r.getIdentity(), null, null, null, null,
                updateAdditionalAttributes, null);

        r = updateReg.update(r);

        Map<String, String> updatedAdditionalAttributes = r.getAdditionalRegistrationAttributes();

        Assert.assertEquals("2", updatedAdditionalAttributes.get("x"));
        Assert.assertEquals("11", updatedAdditionalAttributes.get("y"));
        Assert.assertEquals("101", updatedAdditionalAttributes.get("z"));
        Assert.assertTrue(updatedAdditionalAttributes.containsKey("h"));
        Assert.assertEquals("hello", updatedAdditionalAttributes.get("h"));
    }

    @Test
    public void testApplicationDataUpdate() throws Exception {

        Registration.Builder builder = new Registration.Builder("registrationId", "endpoint",
                Identity.unsecure(Inet4Address.getLocalHost(), 1));
        Map<String, String> appData = new HashMap<String, String>();
        appData.put("x", "1");
        appData.put("y", "10");
        appData.put("z", "100");
        builder.applicationData(appData);
        Registration r = builder.build();

        RegistrationUpdate updateReg = new RegistrationUpdate(r.getId(), r.getIdentity(), null, null, null, null, null,
                null);

        r = updateReg.update(r);

        Map<String, String> updatedAppData = r.getApplicationData();

        Assert.assertEquals("1", updatedAppData.get("x"));
        Assert.assertEquals("10", updatedAppData.get("y"));
        Assert.assertEquals("100", updatedAppData.get("z"));
    }
}
