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
     * A bootstrap session is already started for the given device.
     */
    ALREADY_STARTED,
    /**
     * The Bootstrap Server could not find a configuration to send to the device
     */
    NO_BOOTSTRAP_CONFIG,
    /**
     * A delete request failed
     */
    DELETE_FAILED,
    /**
     * Object 2 (ACL) could not be written on the device
     */
    WRITE_ACL_FAILED,
    /**
     * Object 1 (Server) could not be written on the device
     */
    WRITE_SERVER_FAILED,
    /**
     * Object 0 (Security) could not be written on the device
     */
    WRITE_SECURITY_FAILED,
    /**
     * 'Bootstrap Finish' failed
     */
    FINISH_FAILED,
    /**
     * An unexpected error occured
     */
    INTERNAL_SERVER_ERROR,
}
