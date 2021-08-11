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
package org.eclipse.leshan.core.demo.cli;

/**
 * An exception raised to warn about invalid CLI options. <br>
 * Generally this exception concern validation about more than 1 options.
 */
public class InvalidOptionsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String[] options;

    /**
     * @param msg the error message
     * @param options list of option concerned by the exception
     */
    public InvalidOptionsException(String msg, String... options) {
        super(msg);
        this.options = options;
    }

    /**
     * @param msg the error message
     * @param options list of option concerned by the exception
     */
    public InvalidOptionsException(String msg, Exception cause, String... options) {
        super(msg, cause);
        this.options = options;
    }

    public String[] getOptions() {
        return options;
    }
}
