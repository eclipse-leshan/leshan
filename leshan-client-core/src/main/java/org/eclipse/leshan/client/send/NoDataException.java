/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.client.send;

/**
 * Raised when no data can be collected before to send data.
 */
public class NoDataException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoDataException() {
    }

    public NoDataException(String m) {
        super(m);
    }

    public NoDataException(String m, Object... args) {
        super(String.format(m, args));
    }

    public NoDataException(Throwable e) {
        super(e);
    }

    public NoDataException(String m, Throwable e) {
        super(m, e);
    }

    public NoDataException(Throwable e, String m, Object... args) {
        super(String.format(m, args), e);
    }
}
