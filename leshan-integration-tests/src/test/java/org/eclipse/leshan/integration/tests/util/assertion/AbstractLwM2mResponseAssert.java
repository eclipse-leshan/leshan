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
package org.eclipse.leshan.integration.tests.util.assertion;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ObjectAssert;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.LwM2mResponse;

import com.mbed.coap.packet.CoapResponse;

public abstract class AbstractLwM2mResponseAssert<SELF extends AbstractLwM2mResponseAssert<SELF, ACTUAL>, ACTUAL extends LwM2mResponse>
        extends AbstractObjectAssert<SELF, ACTUAL> {

    protected AbstractLwM2mResponseAssert(ACTUAL actual, Class<SELF> selfType) {
        super(actual, selfType);
    }

    @SuppressWarnings("unchecked")
    protected SELF mySelf() {
        return (SELF) this;
    }

    public SELF isSuccess() {
        isNotNull();

        if (!actual.isSuccess()) {
            failWithMessage("Expected successful Response");
        }

        return mySelf();
    }

    public SELF hasCode(ResponseCode expectedCode) {
        isNotNull();

        if (actual.getCode() == null) {
            failWithMessage("Response MUST NOT have <null> response code");
        }
        if (!actual.getCode().equals(expectedCode)) {
            failWithMessage("Expected <%s> ResponseCode for <%s> response", expectedCode, actual);
        }
        return mySelf();
    }

    public SELF hasValidUnderlyingResponseFor(String givenEndpointProvider) {
        ObjectAssert<Object> assertThatUnderlyingResponse = Assertions.assertThat(actual.getCoapResponse())
                .as("Underlying Response");
        switch (givenEndpointProvider) {
        case "Californium":
        case "Californium-OSCORE":
            assertThatUnderlyingResponse.isExactlyInstanceOf(Response.class);
            break;
        case "java-coap":
            assertThatUnderlyingResponse.isExactlyInstanceOf(CoapResponse.class);
            break;
        default:
            throw new IllegalStateException(String.format("Unsupported endpoint provider : %s", givenEndpointProvider));
        }
        return mySelf();
    }

    public SELF hasContentFormat(ContentFormat format, String givenEndpointProvider) {
        ObjectAssert<Object> assertThatUnderlyingResponse = Assertions.assertThat(actual.getCoapResponse())
                .as("Underlying Response");
        switch (givenEndpointProvider) {
        case "Californium":
        case "Californium-OSCORE":
            assertThatUnderlyingResponse.isInstanceOfSatisfying(Response.class, r -> {
                Assertions.assertThat(r.getOptions().hasContentFormat());
                Assertions.assertThat(r.getOptions().getContentFormat()).as("Content Format")
                        .isEqualTo(format.getCode());
            });
            break;
        case "java-coap":
            assertThatUnderlyingResponse.isInstanceOfSatisfying(CoapResponse.class, r -> {
                Assertions.assertThat(r.options().getContentFormat()).as("Content Format")//
                        .isNotNull().isEqualTo((short) format.getCode());
            });
            break;
        default:
            throw new IllegalStateException(String.format("Unsupported endpoint provider : %s", givenEndpointProvider));
        }
        return mySelf();
    }

    public SELF hasErrorMessage(String expectedErrorMessage) {
        isNotNull();
        Assertions.assertThat(actual.getErrorMessage()).as("Error Message").isEqualTo(expectedErrorMessage);
        return mySelf();
    }
}
