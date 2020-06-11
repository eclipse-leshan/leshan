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
 * An exception raised when {@link ObjectModelValidator} detect an error.
 * 
 * @since 1.1
 */
public class InvalidModelException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidModelException(String m) {
        super(m);
    }

    public InvalidModelException(String m, Object... args) {
        super(String.format(m, args));
    }

    public InvalidModelException(Throwable e) {
        super(e);
    }

    public InvalidModelException(String m, Throwable e) {
        super(m, e);
    }

    public InvalidModelException(Throwable e, String m, Object... args) {
        super(String.format(m, args), e);
    }
}
