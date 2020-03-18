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
package org.eclipse.leshan.server.bootstrap;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ACLConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;

public class BootstrapUtil {
    public static LwM2mObjectInstance convertToSecurityInstance(int instanceId, ServerSecurity securityConfig) {
        Collection<LwM2mResource> resources = new ArrayList<>();

        if (securityConfig.uri != null)
            resources.add(LwM2mSingleResource.newStringResource(0, securityConfig.uri));
        resources.add(LwM2mSingleResource.newBooleanResource(1, securityConfig.bootstrapServer));
        if (securityConfig.securityMode != null)
            resources.add(LwM2mSingleResource.newIntegerResource(2, securityConfig.securityMode.code));
        if (securityConfig.publicKeyOrId != null)
            resources.add(LwM2mSingleResource.newBinaryResource(3, securityConfig.publicKeyOrId));
        if (securityConfig.serverPublicKey != null)
            resources.add(LwM2mSingleResource.newBinaryResource(4, securityConfig.serverPublicKey));
        if (securityConfig.secretKey != null)
            resources.add(LwM2mSingleResource.newBinaryResource(5, securityConfig.secretKey));
        if (securityConfig.smsSecurityMode != null)
            resources.add(LwM2mSingleResource.newIntegerResource(6, securityConfig.smsSecurityMode.code));
        if (securityConfig.smsBindingKeyParam != null)
            resources.add(LwM2mSingleResource.newBinaryResource(7, securityConfig.smsBindingKeyParam));
        if (securityConfig.smsBindingKeySecret != null)
            resources.add(LwM2mSingleResource.newBinaryResource(8, securityConfig.smsBindingKeySecret));
        if (securityConfig.serverSmsNumber != null)
            resources.add(LwM2mSingleResource.newStringResource(9, securityConfig.serverSmsNumber));
        if (securityConfig.serverId != null)
            resources.add(LwM2mSingleResource.newIntegerResource(10, securityConfig.serverId));
        if (securityConfig.clientOldOffTime != null)
            resources.add(LwM2mSingleResource.newIntegerResource(11, securityConfig.clientOldOffTime));
        if (securityConfig.bootstrapServerAccountTimeout != null)
            resources.add(LwM2mSingleResource.newIntegerResource(12, securityConfig.bootstrapServerAccountTimeout));

        return new LwM2mObjectInstance(instanceId, resources);
    }

    public static LwM2mObjectInstance convertToServerInstance(int instanceId, ServerConfig serverConfig) {
        Collection<LwM2mResource> resources = new ArrayList<>();

        resources.add(LwM2mSingleResource.newIntegerResource(0, serverConfig.shortId));
        resources.add(LwM2mSingleResource.newIntegerResource(1, serverConfig.lifetime));
        if (serverConfig.defaultMinPeriod != null)
            resources.add(LwM2mSingleResource.newIntegerResource(2, serverConfig.defaultMinPeriod));
        if (serverConfig.defaultMaxPeriod != null)
            resources.add(LwM2mSingleResource.newIntegerResource(3, serverConfig.defaultMaxPeriod));
        if (serverConfig.disableTimeout != null)
            resources.add(LwM2mSingleResource.newIntegerResource(5, serverConfig.disableTimeout));
        resources.add(LwM2mSingleResource.newBooleanResource(6, serverConfig.notifIfDisabled));
        if (serverConfig.binding != null)
            resources.add(LwM2mSingleResource.newStringResource(7, serverConfig.binding.name()));

        return new LwM2mObjectInstance(instanceId, resources);
    }

    public static LwM2mObjectInstance convertToAclInstance(int instanceId, ACLConfig aclConfig) {
        Collection<LwM2mResource> resources = new ArrayList<>();

        resources.add(LwM2mSingleResource.newIntegerResource(0, aclConfig.objectId));
        resources.add(LwM2mSingleResource.newIntegerResource(1, aclConfig.objectInstanceId));
        if (aclConfig.acls != null)
            resources.add(LwM2mMultipleResource.newIntegerResource(2, aclConfig.acls));
        if (aclConfig.AccessControlOwner != null)
            resources.add(LwM2mSingleResource.newIntegerResource(3, aclConfig.AccessControlOwner));

        return new LwM2mObjectInstance(instanceId, resources);
    }
}
