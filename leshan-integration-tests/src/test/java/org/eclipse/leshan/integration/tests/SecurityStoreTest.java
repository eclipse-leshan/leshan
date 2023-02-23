/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.util.Credentials.BAD_ENDPOINT;
import static org.eclipse.leshan.integration.tests.util.Credentials.BAD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_ENDPOINT;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_ID;
import static org.eclipse.leshan.integration.tests.util.Credentials.GOOD_PSK_KEY;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.integration.tests.util.Credentials;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SecurityStoreTest {

    LeshanTestServer server;

    @BeforeEach
    public void start() {
        server = givenServerUsing(Protocol.COAPS).with("Californium").build();
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (server != null)
            server.destroy();
    }

    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        return new LeshanTestServerBuilder(givenProtocol).with(new InMemorySecurityStore());
    }

    @Test
    public void nonunique_psk_identity() throws NonUniqueSecurityInfoException {
        EditableSecurityStore ess = server.getSecurityStore();

        ess.add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));
        try {
            ess.add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));
            fail("Non-unique PSK identity should throw exception on add");
        } catch (NonUniqueSecurityInfoException e) {
        }
    }

    @Test
    public void change_psk_identity_cleanup() throws NonUniqueSecurityInfoException {

        EditableSecurityStore ess = server.getSecurityStore();

        ess.add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, BAD_PSK_ID, Credentials.BAD_PSK_KEY));
        // Change PSK id for endpoint
        ess.add(SecurityInfo.newPreSharedKeyInfo(GOOD_ENDPOINT, GOOD_PSK_ID, GOOD_PSK_KEY));
        // Original/old PSK id should not be reserved any more
        try {
            ess.add(SecurityInfo.newPreSharedKeyInfo(BAD_ENDPOINT, BAD_PSK_ID, Credentials.BAD_PSK_KEY));
        } catch (NonUniqueSecurityInfoException e) {
            fail("PSK identity change for existing endpoint should have cleaned up old PSK identity");
        }
    }
}
