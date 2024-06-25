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

import java.util.Collection;

import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * a pure LwM2mLink which use only {@link LwM2mAttribute}
 */
public class LwM2mLink extends MixedLwM2mLink {

    public LwM2mLink(String rootPath, LwM2mPath path, LwM2mAttribute<?>... attributes) {
        this(rootPath, path, new LwM2mAttributeSet(attributes));
    }

    public LwM2mLink(String rootPath, LwM2mPath path, Collection<? extends LwM2mAttribute<?>> attributes) {
        this(rootPath, path, new LwM2mAttributeSet(attributes));
    }

    public LwM2mLink(String rootPath, LwM2mPath path, LwM2mAttributeSet attributes) {
        super(rootPath, path, attributes);
    }

    @Override
    public LwM2mAttributeSet getAttributes() {
        return (LwM2mAttributeSet) super.getAttributes();
    }
}
