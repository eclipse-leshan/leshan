/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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

import org.eclipse.leshan.integration.tests.util.RedisSecureIntegrationTestHelper;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class RedisSecurityTest extends SecurityTest {
    public RedisSecurityTest() {
        helper = new RedisSecureIntegrationTestHelper();
    }

    @Test
    @Disabled
    @Override
    public void registered_device_with_oscore_to_server_with_oscore()
            throws NonUniqueSecurityInfoException, InterruptedException {
        // TODO OSCORE : https://github.com/eclipse/leshan/pull/1180#issuecomment-1007587985
        // Redis security store does support oscore for now.
    }

    @Test
    @Disabled
    @Override
    public void registered_device_with_oscore_to_server_with_oscore_then_removed_security_info_then_server_fails_to_send_request()
            throws NonUniqueSecurityInfoException, InterruptedException {
        // TODO OSCORE : https://github.com/eclipse/leshan/pull/1180#issuecomment-1007587985
        // Redis security store does support oscore for now.
    }

    @Test
    @Disabled
    @Override
    public void registered_device_with_oscore_to_server_with_oscore_then_removed_security_info_then_client_fails_to_update()
            throws NonUniqueSecurityInfoException, InterruptedException {
        // TODO OSCORE : https://github.com/eclipse/leshan/pull/1180#issuecomment-1007587985
        // Redis security store does support oscore for now.
    }
}
