/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.bootstrap;

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.Before;
import org.junit.Test;

public class LeshanBootstrapServerBuilderTest {

    private LeshanBootstrapServerBuilder builder;
    private LeshanBootstrapServer server;

    @Before
    public void start() {
        builder = new LeshanBootstrapServerBuilder();
        builder.setConfigStore(new BootstrapConfigStore() {
            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {
                return null;
            }
        });
    }

    @Test
    public void create_server_minimal_parameters() {
        server = builder.build();

        assertNull(server.getSecuredAddress());
        assertNotNull(server.getUnsecuredAddress());
    }

    @Test
    public void create_server_with_securityStore() {
        builder.setSecurityStore(new BootstrapSecurityStore() {
            @Override
            public SecurityInfo getByIdentity(String pskIdentity) {
                return null;
            }

            @Override
            public List<SecurityInfo> getAllByEndpoint(String endpoint) {
                return null;
            }
        });
        server = builder.build();

        assertNotNull(server.getSecuredAddress());
        assertNotNull(server.getUnsecuredAddress());
        assertNotNull(server.getBootstrapSecurityStore());
    }

    @Test
    public void create_server_with_securityStore_and_disable_secured_endpoint() {
        builder.setSecurityStore(new BootstrapSecurityStore() {
            @Override
            public SecurityInfo getByIdentity(String pskIdentity) {
                return null;
            }

            @Override
            public List<SecurityInfo> getAllByEndpoint(String endpoint) {
                return null;
            }
        });
        builder.disableSecuredEndpoint();
        server = builder.build();

        assertNull(server.getSecuredAddress());
        assertNotNull(server.getUnsecuredAddress());
    }

    @Test
    public void create_server_with_securityStore_and_disable_unsecured_endpoint() {
        builder.setSecurityStore(new BootstrapSecurityStore() {
            @Override
            public SecurityInfo getByIdentity(String pskIdentity) {
                return null;
            }

            @Override
            public List<SecurityInfo> getAllByEndpoint(String endpoint) {
                return null;
            }
        });
        builder.disableUnsecuredEndpoint();
        server = builder.build();

        assertNotNull(server.getSecuredAddress());
        assertNull(server.getUnsecuredAddress());
    }
}
