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
package org.eclipse.leshan.core.request;

import java.util.Collection;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * A Composite {@link DownlinkRequest} which targets several nodes.
 */
public interface CompositeDownlinkRequest<T extends LwM2mResponse> extends DownlinkRequest<T> {

    /**
     * @return the collection of node path targeted by the request.
     */
    Collection<LwM2mPath> getPaths();
}
