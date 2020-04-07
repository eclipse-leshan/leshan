/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.client;

import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.engine.RegistrationEngine;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.listener.ObjectsListener;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.request.BindingMode;

/**
 * A class responsible to handle Registration Update. It should listen LWM2M Object Tree for changing and send
 * registration Update if needed.
 */
public class RegistrationUpdateHandler {

    private RegistrationEngine engine;
    private BootstrapHandler bsHandler;

    public RegistrationUpdateHandler(RegistrationEngine engine, BootstrapHandler bsHandler) {
        this.engine = engine;
        this.bsHandler = bsHandler;
    }

    public void listen(final LwM2mObjectTree objecTree) {
        objecTree.addListener(new ObjectsListener() {
            @Override
            public void objectInstancesRemoved(LwM2mObjectEnabler object, int... instanceIds) {
                if (!bsHandler.isBootstrapping())
                    engine.triggerRegistrationUpdate(new RegistrationUpdate(
                            LinkFormatHelper.getClientDescription(objecTree.getObjectEnablers().values(), null)));
            }

            @Override
            public void objectInstancesAdded(LwM2mObjectEnabler object, int... instanceIds) {
                if (!bsHandler.isBootstrapping())
                    engine.triggerRegistrationUpdate(new RegistrationUpdate(
                            LinkFormatHelper.getClientDescription(objecTree.getObjectEnablers().values(), null)));
            }

            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                if (!bsHandler.isBootstrapping())
                    engine.triggerRegistrationUpdate(new RegistrationUpdate(
                            LinkFormatHelper.getClientDescription(objecTree.getObjectEnablers().values(), null)));
            }

            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                if (!bsHandler.isBootstrapping())
                    engine.triggerRegistrationUpdate(new RegistrationUpdate(
                            LinkFormatHelper.getClientDescription(objecTree.getObjectEnablers().values(), null)));
            }

            @Override
            public void resourceChanged(LwM2mObjectEnabler object, int instanceId, int... resourceIds) {
                if (!bsHandler.isBootstrapping())
                    if (object.getId() == LwM2mId.SERVER) {
                        Long lifetime = null;
                        BindingMode bindingMode = null;
                        for (int i = 0; i < resourceIds.length; i++) {
                            if (resourceIds[i] == LwM2mId.SRV_LIFETIME) {
                                lifetime = ServersInfoExtractor.getLifeTime(object, instanceId);
                            } else if (resourceIds[i] == LwM2mId.SRV_BINDING) {
                                bindingMode = ServersInfoExtractor.getBindingMode(object, instanceId);
                            }
                        }

                        if (bindingMode != null || lifetime != null) {
                            Long serverId = null;
                            serverId = ServersInfoExtractor.getServerId(object, instanceId);
                            if (serverId != null) {
                                ServerIdentity server = engine.getRegisteredServer(serverId);
                                if (server != null)
                                    engine.triggerRegistrationUpdate(server,
                                            new RegistrationUpdate(lifetime, null, bindingMode, null, null));
                            }
                        }

                    }
            }
        });
    }
}
