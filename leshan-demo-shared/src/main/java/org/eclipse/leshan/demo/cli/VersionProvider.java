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
package org.eclipse.leshan.core.demo.cli;

import org.eclipse.leshan.core.demo.LeshanProperties;

import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        LeshanProperties leshanProperties = new LeshanProperties();
        leshanProperties.load();

        return new String[] { //
                String.format("@|bold ${COMMAND-NAME}|@ @|bold,yellow v%s|@", leshanProperties.getVersion()), //
                "", //
                String.format("@|italic Commit ID : %s|@", leshanProperties.getCommitId()), //
                String.format("@|italic Build Date: %s (%d)|@", leshanProperties.getBuildDateAsString(),
                        leshanProperties.getTimestamp()), //
                "", //
                "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})", //
                "OS: ${os.name} ${os.version} ${os.arch}", //
                "", //
                String.format("@|italic Code Source: %s|@", leshanProperties.getCodeURL()) };
    }
}
