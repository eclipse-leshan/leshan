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

import java.util.EnumSet;

import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.engine.RegistrationEngine;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.listener.ObjectsListener;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BindingMode;

/**
 * A class responsible to handle Registration Update. It should listen LWM2M Object Tree for changing and send
 * registration Update if needed.
 */
public class RegistrationUpdateHandler {

    private final RegistrationEngine engine;
    private final BootstrapHandler bsHandler;

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
                            LinkFormatHelper.getClientDescription(objecTree.getObjectEnablers().values(), null, null)));
            }

            @Override
            public void objectInstancesAdded(LwM2mObjectEnabler object, int... instanceIds) {
                if (!bsHandler.isBootstrapping())
                    engine.triggerRegistrationUpdate(new RegistrationUpdate(
                            LinkFormatHelper.getClientDescription(objecTree.getObjectEnablers().values(), null, null)));
            }

            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                if (!bsHandler.isBootstrapping())
                    engine.triggerRegistrationUpdate(new RegistrationUpdate(
                            LinkFormatHelper.getClientDescription(objecTree.getObjectEnablers().values(), null, null)));
            }

            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                if (!bsHandler.isBootstrapping())
                    engine.triggerRegistrationUpdate(new RegistrationUpdate(
                            LinkFormatHelper.getClientDescription(objecTree.getObjectEnablers().values(), null, null)));
            }

            @Override
            public void resourceChanged(LwM2mPath... paths) {
                if (!bsHandler.isBootstrapping())
                    // TODO we should be smarter here and manage the case where server lifetime and supportedBinding is
                    // modified at same time.
                    // /!\ lifetime trigger update to only 1 server and supportedBinding to all the registered ones.
                    for (LwM2mPath path : paths) {
                        if (path.isResource()) {
                            if (path.getObjectId() == LwM2mId.SERVER) {
                                // handle lifetime changes
                                if (path.getResourceId() == LwM2mId.SRV_LIFETIME) {
                                    LwM2mObjectEnabler enabler = objecTree.getObjectEnabler(LwM2mId.SERVER);
                                    if (enabler != null) {
                                        Long lifetime = ServersInfoExtractor.getLifeTime(enabler,
                                                path.getObjectInstanceId());
                                        Long serverId = ServersInfoExtractor.getServerId(enabler,
                                                path.getObjectInstanceId());
                                        if (lifetime != null && serverId != null) {
                                            ServerIdentity server = engine.getRegisteredServer(serverId);
                                            if (server != null) {
                                                engine.triggerRegistrationUpdate(server,
                                                        new RegistrationUpdate(lifetime, null, null, null, null));
                                                return;
                                            }
                                        }
                                    }
                                }
                            } else if (path.getObjectId() == LwM2mId.DEVICE) {
                                // handle supported binding changes
                                EnumSet<BindingMode> bindingMode = null;
                                if (path.getResourceId() == LwM2mId.DVC_SUPPORTED_BINDING) {
                                    LwM2mObjectEnabler enabler = objecTree.getObjectEnabler(LwM2mId.DEVICE);
                                    if (enabler != null) {
                                        bindingMode = ServersInfoExtractor.getDeviceSupportedBindingMode(enabler,
                                                path.getObjectInstanceId());
                                    }
                                }

                                if (bindingMode != null) {
                                    engine.triggerRegistrationUpdate(
                                            new RegistrationUpdate(null, null, bindingMode, null, null));
                                    return;
                                }
                            }
                        }
                    }
            }
        });
    }
}
