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
package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;

/**
 * Enable request on root path.
 */
public interface LwM2mRootEnabler {

    ReadCompositeResponse read(ServerIdentity identity, ReadCompositeRequest request);

    WriteCompositeResponse write(ServerIdentity identity, WriteCompositeRequest request);

    LwM2mModel getModel();
}
