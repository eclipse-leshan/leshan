/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.html.
 *******************************************************************************/
package org.eclipse.leshan.standalone;

import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.impl.ClientRegistryImpl;
import org.junit.Assert;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;

public class LeshanStandaloneTest extends Assert {

    LeshanStandalone server = new LeshanStandalone();

    @Test
    public void shouldUseInMemoryClientRegistry() throws InterruptedException {
        // When
        server.start();
        SECONDS.sleep(1);
        server.stop();

        // Then
        assertTrue(server.leshanServer().getClientRegistry() instanceof ClientRegistryImpl);
    }

    @Test
    public void shouldUseGivenClientRegistry() throws InterruptedException {
        // Given
        ClientRegistry mockClientRegistry = mock(ClientRegistry.class);
        server.clientRegistry(mockClientRegistry);

        // When
        server.start();
        SECONDS.sleep(1);
        server.stop();

        // Then
        assertSame(mockClientRegistry, server.leshanServer().getClientRegistry());
    }

}