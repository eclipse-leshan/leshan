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
import org.eclipse.leshan.client.resource.integer.IntegerLwM2mResource;
import org.eclipse.leshan.client.resource.string.StringLwM2mExchange;
import org.eclipse.leshan.client.resource.string.StringLwM2mResource;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.client.response.WriteResponse;
import org.junit.Test;

public class StringLwM2mResourceTest {

    @Test
    public void testWriteGoodValue() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        final String valueToWrite = "zeus";
        when(exchange.getRequestPayload()).thenReturn(valueToWrite.getBytes());

        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource();
        testResource.write(exchange);

        assertEquals(valueToWrite, testResource.value);
        verify(exchange).respond(WriteResponse.success());
    }

    @Test
    public void testRead() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);

        final String initialValue = "redballoon";
        final ReadableWriteableTestResource testResource = new ReadableWriteableTestResource(initialValue);
        testResource.read(exchange);

        assertEquals(initialValue, testResource.value);
        verify(exchange).respond(ReadResponse.success(initialValue.getBytes()));
    }

    @Test
    public void testDefaultPermissionsRead() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);

        final DefaultTestResource testResource = new DefaultTestResource();
        testResource.read(exchange);

        verify(exchange).respond(ReadResponse.notAllowed());
    }

    @Test
    public void testDefaultPermissionsWrite() {
        final LwM2mExchange exchange = mock(LwM2mExchange.class);
        when(exchange.getRequestPayload()).thenReturn("badwolf".getBytes());

        final DefaultTestResource testResource = new DefaultTestResource();
        testResource.write(exchange);

        verify(exchange).respond(WriteResponse.notAllowed());
    }

    private class ReadableWriteableTestResource extends StringLwM2mResource {

        private String value;

        public ReadableWriteableTestResource(final String newValue) {
            value = newValue;
        }

        public ReadableWriteableTestResource() {
        }

        @Override
        protected void handleWrite(final StringLwM2mExchange exchange) {
            this.value = exchange.getRequestPayload();
            exchange.respondSuccess();
        }

        @Override
        protected void handleRead(final StringLwM2mExchange exchange) {
            exchange.respondContent(value);
        }

    }

    private class DefaultTestResource extends IntegerLwM2mResource {

    }

}
