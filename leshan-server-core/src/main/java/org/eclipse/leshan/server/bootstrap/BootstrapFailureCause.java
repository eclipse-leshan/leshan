/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 * Different causes for a bootstrap failure.
 */
public enum BootstrapFailureCause {
    /**
     * The device presented wrong credentials
     */
    UNAUTHORIZED,
    /**
     * The Boostrap Server could not find a configuration to send to the device
     */
    NO_BOOTSTRAP_CONFIG,
    /**
     * The "/" object could not be deleted on the device
     */
    DELETE_FAILED,
    /**
     * Object 1 (Server) could not be written on the device
     */
    WRITE_SERVER_FAILED,
    /**
     * Object 0 (Security) could not be written on the device
     */
    WRITE_SECURITY_FAILED,
    /**
     * 'Bootstrap Finish' message count not be sent to the device
     */
    SEND_FINISH_FAILED,
    /**
     * The device responded to 'Bootstrap Finish' with an error code
     */
    FINISHED_WITH_ERROR
}
