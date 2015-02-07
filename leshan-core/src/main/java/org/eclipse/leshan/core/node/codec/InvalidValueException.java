/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec;

import org.eclipse.leshan.core.node.LwM2mPath;

public class InvalidValueException extends Exception {

    private static final long serialVersionUID = 1L;

    private final LwM2mPath path;

    public InvalidValueException(String message, LwM2mPath path) {
        super(message);
        this.path = path;
    }

    public InvalidValueException(String message, LwM2mPath path, Exception e) {
        super(message, e);
        this.path = path;
    }

    /**
     * @return the path of the resource with an invalid value
     */
    public LwM2mPath getPath() {
        return path;
    }

}
