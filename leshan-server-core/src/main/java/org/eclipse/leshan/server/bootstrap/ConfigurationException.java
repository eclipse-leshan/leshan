/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

/**
 * Exception raised when {@link BootstrapConfig} is invalid.
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ConfigurationException() {
    }

    public ConfigurationException(String m) {
        super(m);
    }

    public ConfigurationException(String m, Object... args) {
        super(String.format(m, args));
    }

    public ConfigurationException(Throwable e) {
        super(e);
    }

    public ConfigurationException(String m, Throwable e) {
        super(m, e);
    }

    public ConfigurationException(Throwable e, String m, Object... args) {
        super(String.format(m, args), e);
    }
}