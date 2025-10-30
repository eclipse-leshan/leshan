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
package org.eclipse.leshan.integration.tests.security;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.servers.security.NonUniqueSecurityInfoException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class RedisSecurityStoreTest extends SecurityStoreTest {

    @Override
    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        return super.givenServerUsing(givenProtocol).withRedisRegistrationStore().withRedisSecurityStore();
    }

    @Override
    @Test
    @Disabled("OSCORE not supported yet")
    void change_oscore_rid_cleanup() throws NonUniqueSecurityInfoException {
        // "OSCORE not supported yet"
    }

    @Override
    @Test
    @Disabled("OSCORE not supported yet")
    void nonunique_oscore_rid() throws NonUniqueSecurityInfoException {
        // "OSCORE not supported yet"
    }
}
