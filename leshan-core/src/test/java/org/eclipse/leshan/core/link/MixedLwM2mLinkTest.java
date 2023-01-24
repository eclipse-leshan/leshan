/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.link;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.link.lwm2m.MixedLwM2mLink;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.jupiter.api.Test;

public class MixedLwM2mLinkTest {

    @Test
    public void check_uri_reference() {
        Link link = new MixedLwM2mLink("/root", LwM2mPath.ROOTPATH, new ResourceTypeAttribute("oma.lwm2m"));
        assertEquals("/root", link.getUriReference());

        link = new MixedLwM2mLink("/root", new LwM2mPath(2));
        assertEquals("/root/2", link.getUriReference());

        link = new MixedLwM2mLink("/root/", new LwM2mPath(2));
        assertEquals("/root//2", link.getUriReference());

        link = new MixedLwM2mLink("/", new LwM2mPath(2));
        assertEquals("/2", link.getUriReference());

        link = new MixedLwM2mLink(null, new LwM2mPath(2));
        assertEquals("/2", link.getUriReference());
    }
}
