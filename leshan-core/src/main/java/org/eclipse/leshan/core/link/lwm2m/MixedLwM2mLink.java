/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.core.link.lwm2m;

import org.eclipse.leshan.core.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.attributes.MixedLwM2mAttributeSet;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * a LWM2M Link which can contain {@link LwM2mAttribute} but also tolerate not LWM2M {@link Attribute} but can also have
 * a rootpath.
 */
public class MixedLwM2mLink extends Link {

    private LwM2mPath path;
    private Link rootResource;

    public MixedLwM2mLink(Link rootResource, LwM2mPath path, MixedLwM2mAttributeSet attributes) {
        super(path.toString(), attributes);
        this.path = path;
        this.rootResource = rootResource;
    }

    public LwM2mPath getPath() {
        return path;
    }

    public Link getRootResource() {
        return rootResource;
    }
}
