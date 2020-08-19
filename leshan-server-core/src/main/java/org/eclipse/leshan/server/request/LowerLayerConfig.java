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
package org.eclipse.leshan.server.request;

/**
 * Allow to apply custom configuration to lower layer.
 * <p>
 * If you are using LWM2M over CoAP this setter will help you to apply CoAP setting to you request.
 * 
 * @since 1.2
 */
public interface LowerLayerConfig {

    /**
     * @param lowerRequest the lower layer request. E.g. could be a CoAP request.
     */
    void apply(Object lowerRequest);
}
