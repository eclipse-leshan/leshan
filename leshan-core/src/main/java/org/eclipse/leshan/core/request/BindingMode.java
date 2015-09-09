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
 *     Alexander Ellwein (Bosch Software Innovations GmbH) 
 *                     - extended for using with queue mode
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import java.util.EnumSet;

/**
 * Transport binding and Queue Mode
 */
public enum BindingMode {

    /** UDP */
    U,

    /** UDP with Queue Mode */
    UQ,

    /** SMS */
    S,

    /** SMS with Queue Mode */
    SQ,

    /** UDP and SMS */
    US,

    /** UDP with Queue Mode and SMS */
    UQS;

    private static final EnumSet<BindingMode> QUEUE_MODES = EnumSet.of(UQ, SQ, UQS);

    /**
     * @return true, if a binding mode is a queue mode, otherwise false.
     */
    public boolean isQueueMode() {
        return QUEUE_MODES.contains(this);
    }
}
