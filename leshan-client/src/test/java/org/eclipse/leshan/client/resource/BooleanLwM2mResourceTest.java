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
package org.eclipse.leshan.client.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.resource.bool.BooleanLwM2mExchange;
import org.eclipse.leshan.client.resource.bool.BooleanLwM2mResource;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.client.response.WriteResponse;
import org.junit.Test;

public class BooleanLwM2mResourceTest {

    @Test
    public void testReadTrue() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource(true);
        testResource.read(exchange);

        assertEquals(true, testResource.value);
        verify(exchange).respond(ReadResponse.success("1".getBytes()));
    }

    @Test
    public void testReadFalse() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource(false);
        testResource.read(exchange);

        assertEquals(false, testResource.value);
        verify(exchange).respond(ReadResponse.success("0".getBytes()));
    }

    @Test
    public void testWriteTrue() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        when(exchange.getRequestPayload()).thenReturn("1".getBytes());

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource(false);
        testResource.write(exchange);

        assertEquals(true, testResource.value);
        verify(exchange).respond(WriteResponse.success());
    }

    @Test
    public void testWriteFalse() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        when(exchange.getRequestPayload()).thenReturn("0".getBytes());

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource(true);
        testResource.write(exchange);

        assertEquals(false, testResource.value);
        verify(exchange).respond(WriteResponse.success());
    }

    @Test
    public void testWriteInvalid() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        when(exchange.getRequestPayload()).thenReturn("lolzors".getBytes());

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource(true);
        testResource.write(exchange);

        assertEquals(true, testResource.value);
        verify(exchange).respond(WriteResponse.badRequest());
    }

    @Test
    public void testWriteInvalid2() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        when(exchange.getRequestPayload()).thenReturn("lolzors".getBytes());

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource(false);
        testResource.write(exchange);

        assertEquals(false, testResource.value);
        verify(exchange).respond(WriteResponse.badRequest());
    }

    private class ReadableWriteableTestResource extends BooleanLwM2mResource {

        private boolean value;

        public ReadableWriteableTestResource(final boolean newValue) {
            value = newValue;
        }

        @Override
        protected void handleWrite(final BooleanLwM2mExchange exchange) {
            this.value = exchange.getRequestPayload();
            exchange.respondSuccess();
        }

        @Override
        protected void handleRead(final BooleanLwM2mExchange exchange) {
            exchange.respondContent(value);
        }

    }

}
