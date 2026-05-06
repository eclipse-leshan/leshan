/*******************************************************************************
 * Copyright (c) 2026 Sierra Wireless and others.
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

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.util.Validate;

public class EndDeviceData {
    private final String endpoint;
    private final String prefix;
    private final Link[] objectLinks;

    public EndDeviceData(String endpoint, String prefix, Link[] objectLinks) {
        Validate.notNull(endpoint);
        Validate.notNull(prefix);
        Validate.notEmpty(objectLinks);
        this.endpoint = endpoint;
        this.prefix = prefix;
        this.objectLinks = objectLinks;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getPrefix() {
        return prefix;
    }

    public Link[] getObjectLinks() {
        return objectLinks;
    }
}
