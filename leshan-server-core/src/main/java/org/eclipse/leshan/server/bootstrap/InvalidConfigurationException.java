/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap;

/**
 * Exception raised when {@link BootstrapConfig} is invalid.
 */
public class InvalidConfigurationException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidConfigurationException() {
    }

    public InvalidConfigurationException(String m) {
        super(m);
    }

    public InvalidConfigurationException(String m, Object... args) {
        super(String.format(m, args));
    }

    public InvalidConfigurationException(Throwable e) {
        super(e);
    }

    public InvalidConfigurationException(String m, Throwable e) {
        super(m, e);
    }

    public InvalidConfigurationException(Throwable e, String m, Object... args) {
        super(String.format(m, args), e);
    }
}