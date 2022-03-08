/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Orange - Send with multiple-timestamped values
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.response.ReadResponse;

public class TimestampedInstanceEnabler extends BaseInstanceEnabler {

    public TimestampedInstanceEnabler(int id) {
        super(id);
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        LwM2mSingleResource lwM2mSingleResource = LwM2mSingleResource.newFloatResource(resourceid, 111.1);
        return ReadResponse.success(lwM2mSingleResource);
    }
}
