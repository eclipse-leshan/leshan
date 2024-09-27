/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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

import org.assertj.core.api.AbstractAssert;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.integration.tests.util.Failure;

public class FailureAssert extends AbstractAssert<FailureAssert, Failure> {

    public FailureAssert(Failure actual) {
        super(actual, FailureAssert.class);
    }

    public FailureAssert failedWith(ResponseCode expectedCode) {
        isNotNull();

        if (actual.getCode() == null) {
            failWithMessage("Code Failure is expected and so MUST NOT be <null>");
        }
        if (!actual.getCode().equals(expectedCode)) {
            failWithMessage("Expected <%s> ResponseCode for <%s> failure", expectedCode, actual);
        }
        return this;
    }

    public FailureAssert failedWith(Class<? extends Throwable> expectedException) {
        isNotNull();

        if (actual.getException() == null) {
            failWithMessage("Exception Failure is expected and so MUST NOT be <null>");
        }
        objects.assertIsExactlyInstanceOf(info, actual.getException(), expectedException);
        return this;
    }
}
