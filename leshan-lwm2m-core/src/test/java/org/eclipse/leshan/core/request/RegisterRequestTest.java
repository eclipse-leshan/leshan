/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Natalia Krzykała Orange Polska S.A. - initial implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import java.util.EnumSet;
import java.util.Map;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class RegisterRequestTest {

    private class ExtendedRegisterRequest extends RegisterRequest {
        ExtendedRegisterRequest(String endpointName, Long lifetime, String lwVersion, EnumSet<BindingMode> bindingMode,
                Boolean queueMode, String smsNumber, Link[] objectLinks, Map<String, String> additionalAttributes)
                throws InvalidRequestException {
            super(endpointName, lifetime, lwVersion, bindingMode, queueMode, smsNumber, objectLinks,
                    additionalAttributes, null);
        }

        @Override
        public boolean canEqual(Object o) {
            return (o instanceof ExtendedRegisterRequest);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(RegisterRequest.class).withRedefinedSubclass(ExtendedRegisterRequest.class)
                .withIgnoredFields("coapRequest").verify();
    }
}
