/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.server.demo.transport.californium;

import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.endpoint.Protocol;

import picocli.CommandLine.Command;

@Command(name = "add", description = "add endpoint.")

public class CoapAddCommand extends AbstractAddCommand {

    public CoapAddCommand() {
        super(LwM2m.DEFAULT_COAP_PORT);
    }

    @Override
    protected Protocol getProtocol() {
        return Protocol.COAP;
    }
}
