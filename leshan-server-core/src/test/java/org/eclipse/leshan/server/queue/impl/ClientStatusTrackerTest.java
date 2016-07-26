/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
 * <p/>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * <p/>
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.html.
 * <p/>
 * Contributors:
 * Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 * - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests whether client state transitions are handled properly in {@link ClientStatusTracker}
 */
public class ClientStatusTrackerTest {
    public static final String ENDPOINT = "urn:testEndpoint";
    private ClientStatusTracker instanceUnderTest;

    @Before
    public void setup() throws Exception {
        instanceUnderTest = new ClientStatusTracker();
    }

    @Test
    public void set_client_unreachable() throws Exception {
        // clients initial starting state.
        assertTrue(instanceUnderTest.setClientReachable(ENDPOINT));
        // set state of the client in Receiving before setting unreachable.
        assertTrue(instanceUnderTest.startClientReceiving(ENDPOINT));
        // now set the client as unreachable.
        instanceUnderTest.setClientUnreachable(ENDPOINT);
    }

    @Test
    public void set_client_reachable() throws Exception {
        assertTrue("Expected client status set to reachable", instanceUnderTest.setClientReachable(ENDPOINT));
    }

    @Test
    public void set_client_unreachable_not_allowed() throws Exception {
        assertTrue(instanceUnderTest.setClientReachable(ENDPOINT));
        instanceUnderTest.clearClientState(ENDPOINT);
        // Now when client state is not known, state cannot be set to unreachable.
        assertFalse(instanceUnderTest.setClientUnreachable(ENDPOINT));
    }

    @Test
    public void set_start_client_receiving() throws Exception {
        instanceUnderTest.setClientReachable(ENDPOINT);
        assertTrue("Expected client status set to receiving", instanceUnderTest.startClientReceiving(ENDPOINT));
    }

    @Test
    public void set_stop_client_receiving() throws Exception {
        instanceUnderTest.setClientReachable(ENDPOINT);
        instanceUnderTest.startClientReceiving(ENDPOINT);
        assertTrue("Expected client status set to reachable", instanceUnderTest.stopClientReceiving(ENDPOINT));
    }
}