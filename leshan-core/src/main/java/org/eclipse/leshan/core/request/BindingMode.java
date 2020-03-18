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
 *******************************************************************************/
package org.eclipse.leshan.core.request;

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

    public boolean useSMS() {
        return equals(S) || equals(SQ) || equals(UQS);
    }

    public boolean useQueueMode() {
        return equals(UQ) || equals(SQ) || equals(UQS);
    }

    public boolean useUDP() {
        return equals(U) || equals(UQ) || equals(UQS);
    }
}
