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
 *     Sierra Wireless - initial API and implementation
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.senml;

/**
 * Exception thrown in case of SenML parsing error
 */
public class SenMLException extends Exception {

    private static final long serialVersionUID = 1L;

    public SenMLException(String message) {
        super(message);
    }

    public SenMLException(String message, Object... args) {
        super(String.format(message, args));
    }

    public SenMLException(Exception e, String message, Object... args) {
        super(String.format(message, args), e);
    }

    public SenMLException(String message, Exception cause) {
        super(message, cause);
    }

}
