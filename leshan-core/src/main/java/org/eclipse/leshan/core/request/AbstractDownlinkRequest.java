/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.util.Validate;

/**
 * A base class for concrete LWM2M Downlink request types.
 *
 * Provides generic support for specifying the target client and the resource path.
 */
public abstract class AbstractDownlinkRequest<T extends LwM2mResponse> implements DownlinkRequest<T> {

    private final LwM2mPath path;

    protected AbstractDownlinkRequest(final LwM2mPath path) {
        Validate.notNull(path);
        this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LwM2mPath getPath() {
        return this.path;
    }

}