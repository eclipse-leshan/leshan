/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add defaultMinPeriod/defaultMaxPeriod
 *                                                     and reset() for REPLACE/UPDATE implementation
 *******************************************************************************/
package org.eclipse.leshan.client.object;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

/**
 * A simple {@link LwM2mInstanceEnabler} for the Server (1) object.
 */
public class Server extends BaseInstanceEnabler {

    private int shortServerId;
    private long lifetime;
    private Long defaultMinPeriod;
    private Long defaultMaxPeriod;
    private BindingMode binding;
    private boolean notifyWhenDisable;

    public Server(int shortServerId, long lifetime, BindingMode binding, boolean notifyWhenDisable) {
        this.shortServerId = shortServerId;
        this.lifetime = lifetime;
        this.binding = binding;
        this.notifyWhenDisable = notifyWhenDisable;
    }

    @Override
    public ReadResponse read(int resourceid) {

        switch (resourceid) {
        case 0: // short server ID
            return ReadResponse.success(resourceid, shortServerId);

        case 1: // lifetime
            return ReadResponse.success(resourceid, lifetime);

        case 2: // default min period
            if (null == defaultMinPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMinPeriod);

        case 3: // default max period
            if (null == defaultMaxPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMaxPeriod);

        case 6: // notification storing when disable or offline
            return ReadResponse.success(resourceid, notifyWhenDisable);

        case 7: // binding
            return ReadResponse.success(resourceid, binding.toString());

        default:
            return super.read(resourceid);
        }
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {

        switch (resourceid) {

        case 0:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            shortServerId = ((Long) value.getValue()).intValue();
            return WriteResponse.success();

        case 1:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            lifetime = (Long) value.getValue();
            return WriteResponse.success();

        case 2:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            defaultMinPeriod = (Long) value.getValue();
            return WriteResponse.success();

        case 3:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            defaultMaxPeriod = (Long) value.getValue();
            return WriteResponse.success();

        case 6: // notification storing when disable or offline
            if (value.getType() != Type.BOOLEAN) {
                return WriteResponse.badRequest("invalid type");
            }
            notifyWhenDisable = (boolean) value.getValue();
            return WriteResponse.success();

        case 7: // binding
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            try {
                binding = BindingMode.valueOf((String) value.getValue());
                return WriteResponse.success();
            } catch (IllegalArgumentException e) {
                return WriteResponse.badRequest("invalid value");
            }

        default:
            return super.write(resourceid, value);
        }
    }

    @Override
    public ExecuteResponse execute(int resourceid, String params) {

        if (resourceid == 8) { // registration update trigger
            // TODO implement registration update trigger executable resource
            return ExecuteResponse.internalServerError("not implemented");
        } else {
            return super.execute(resourceid, params);
        }
    }

    @Override
    public void reset(int resourceid) {
        switch (resourceid) {
        case 2:
            defaultMinPeriod = null;
            break;
        case 3:
            defaultMaxPeriod = null;
            break;
        default:
            super.reset(resourceid);
        }
    }

}
