/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.server.core.demo.cli;

import org.eclipse.leshan.server.core.demo.cli.converters.ServerCIDConverter;

import picocli.CommandLine.Option;

/**
 * Dtls section shared by server-demo and bsserver-demo
 */
public class DtlsSection {

    @Option(names = { "-cid", "--connection-id" },
            defaultValue = "on",
            description = { //
                    "Control usage of DTLS connection ID.", //
                    "- 'on' to activate Connection ID support ", //
                    "  (same as -cid 6)", //
                    "- 'off' to deactivate it", //
                    "- Positive value define the size in byte of CID generated.", //
                    "- 0 value means we accept to use CID but will not generated one for foreign peer.", //
                    "Default: on" },
            converter = ServerCIDConverter.class)
    public Integer cid;

    @Option(names = { "-oc", "--support-deprecated-ciphers" },
            description = { //
                    "Activate support of old/deprecated cipher suites." })
    public boolean supportDeprecatedCiphers;
}
