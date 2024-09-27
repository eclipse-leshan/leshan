/*******************************************************************************
 * Copyright (c) 2024 Semtech and others.
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
 *     Semtech - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util;

import org.eclipse.leshan.core.ResponseCode;

public class Failure {
    private final Exception exception;
    private final ResponseCode code;

    public Failure(Exception exception, ResponseCode code) {
        this.exception = exception;
        this.code = code;
    }

    public ResponseCode getCode() {
        return code;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return String.format("Failure [exception=%s, code=%s]", exception, code);
    }
}
