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
 *******************************************************************************/

package org.eclipse.leshan.server.registration;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.request.Identity;
import org.junit.Assert;
import org.junit.Test;

public class RegistrationSortObjectLinksTest {

    @Test
    public void sort_link_object_on_get() throws UnknownHostException {
        Link[] objs = new Link[3];
        objs[0] = new Link("/0/1024/2");
        objs[1] = new Link("/0/2");
        objs[2] = null;

        Registration.Builder builder = new Registration.Builder("registrationId", "endpoint",
                Identity.unsecure(Inet4Address.getLocalHost(), 1)).objectLinks(objs);

        Registration r = builder.build();

        Link[] res = r.getSortedObjectLinks();
        Assert.assertEquals(3, res.length);
        Assert.assertNull(res[0]);
        Assert.assertEquals("/0/2", res[1].getUrl());
        Assert.assertEquals("/0/1024/2", res[2].getUrl());
    }
}
