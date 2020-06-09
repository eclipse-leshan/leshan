/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.core.model;

/**
 * Raised by {@link DDFFileValidator} if a DDF file is invalid
 * 
 * @since 1.1
 */
public class InvalidDDFFileException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidDDFFileException() {
    }

    public InvalidDDFFileException(String m) {
        super(m);
    }

    public InvalidDDFFileException(String m, Object... args) {
        super(String.format(m, args));
    }

    public InvalidDDFFileException(Throwable e) {
        super(e);
    }

    public InvalidDDFFileException(String m, Throwable e) {
        super(m, e);
    }

    public InvalidDDFFileException(Throwable e, String m, Object... args) {
        super(String.format(m, args), e);
    }
}
