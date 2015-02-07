/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.junit.Assert.assertTrue;

import org.eclipse.leshan.client.response.OperationResponse;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class BootstrapTest {

    private final IntegrationTestHelper helper = new IntegrationTestHelper(true);

    @After
    public void stop() {
        helper.stop();
    }

    @Ignore
    @Test
    public void boostrap_device_exists() {
        final OperationResponse bootstrap = helper.bootstrap();

        assertTrue(bootstrap.isSuccess());
    }
}
